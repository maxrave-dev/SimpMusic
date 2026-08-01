package com.simpmusic.media_jvm

import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.GenericPlaybackParameters
import com.maxrave.domain.data.player.PlayerConstants
import com.maxrave.domain.data.player.PlayerError
import com.maxrave.domain.extension.isVideo
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.player.MediaPlayerInterface
import com.maxrave.domain.mediaservice.player.MediaPlayerListener
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.logger.Logger
import com.simpmusic.media_jvm.download.getDownloadPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JPanel
import com.sun.jna.NativeLibrary

private const val TAG = "VlcPlayerAdapter"

class VlcPlayerAdapter(
    private val coroutineScope: CoroutineScope,
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
) : MediaPlayerInterface {
    
    private enum class InternalState {
        IDLE, PREPARING, READY, PLAYING, PAUSED, ENDED, ERROR,
    }

    private fun InternalState.isInReadyState(): Boolean = this == InternalState.READY || this == InternalState.PLAYING || this == InternalState.PAUSED

    private val mediaPlayerFactory: MediaPlayerFactory

    init {
        try {
            val rootDir = File(System.getProperty("user.dir"))
            val localVlcDir = File(rootDir, "vlc")
            if (localVlcDir.exists() && localVlcDir.isDirectory) {
                val vlcPath = localVlcDir.absolutePath
                System.setProperty("jna.library.path", vlcPath)
                System.setProperty("VLC_PLUGIN_PATH", File(vlcPath, "plugins").absolutePath)
                NativeLibrary.addSearchPath("libvlc", vlcPath)
                NativeLibrary.addSearchPath("libvlccore", vlcPath)
                Logger.d(TAG, "Bundled VLC loaded successfully from: $vlcPath")
            } else {
                Logger.w(TAG, "Bundled VLC directory not found at $localVlcDir")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load bundled VLC: ${e.message}")
        }

        System
            .getProperty("compose.application.resources.dir")
            ?.let { System.setProperty("jna.library.path", it) }
            
        val discovery =
            NativeDiscovery(
                DefaultVlcDiscoverer(),
                MacOsVlcDiscoverer(),
            )
        val found = discovery.discover()
        if (!found) {
            Logger.e(TAG, "VLC native libraries not found! Please install VLC media player.")
        }

        val factoryArgs =
            mutableListOf(
                "--no-video-title-show",
                "--quiet",
                "--no-metadata-network-access",
                "--network-caching=10000",
            )

        mediaPlayerFactory = MediaPlayerFactory(discovery, *factoryArgs.toTypedArray())

        coroutineScope.launch {
            dataStoreManager.crossfadeEnabled.collect { enabled ->
                crossfadeEnabled = (enabled == DataStoreManager.TRUE)
                Logger.d(TAG, "Crossfade enabled: $crossfadeEnabled")
            }
        }

        coroutineScope.launch {
            dataStoreManager.crossfadeDuration.collect { duration ->
                crossfadeDurationMs = duration
                Logger.d(TAG, "Crossfade duration: $crossfadeDurationMs ms")
            }
        }
    }

    private val listeners = mutableListOf<MediaPlayerListener>()

    @Volatile
    private var currentPlayer: VlcPlayer? = null

    @Volatile
    private var currentPlayerIsVideo = false

    @Volatile
    private var internalState = InternalState.IDLE

    @Volatile
    private var internalPlayWhenReady = true

    @Volatile
    private var internalVolume = 1.0f

    @Volatile
    private var internalRepeatMode = PlayerConstants.REPEAT_MODE_OFF

    @Volatile
    private var internalShuffleModeEnabled = false

    @Volatile
    private var internalPlaybackSpeed = 1.0f

    @Volatile
    private var cachedPosition = 0L

    @Volatile
    private var cachedDuration = 0L

    @Volatile
    private var cachedBufferedPosition = 0L

    @Volatile
    private var cachedIsLoading = false

    private var positionUpdateJob: Job? = null

    private data class PrecachedPlayer(
        val player: VlcPlayer,
        val mediaItem: GenericMediaItem,
        val source: PlayableSource,
    )

    private val precachedPlayers = ConcurrentHashMap<String, PrecachedPlayer>()
    private var precacheEnabled = true
    private val maxPrecacheCount = 2
    private var precacheJob: Job? = null

    @Volatile
    private var crossfadeEnabled = false

    @Volatile
    private var crossfadeDurationMs = 5000

    @Volatile
    private var secondaryPlayer: VlcPlayer? = null

    @Volatile
    private var crossfadeJob: Job? = null

    @Volatile
    private var isCrossfading = false

    @Volatile
    private var crossfadeFromIndex = -1

    private fun setCrossfading(value: Boolean) {
        if (isCrossfading != value) {
            isCrossfading = value
            notifyListeners { onCrossfadeStateChanged(value) }
            if (value) {
                Logger.w(TAG, "=== CROSSFADE START - THREAD DUMP ===")
                Thread.getAllStackTraces().forEach { (thread, stack) ->
                    if (thread.name.contains("AWT-EventQueue") ||
                        thread.name.contains("main") ||
                        thread.name.contains("VLC") ||
                        thread.name.contains("Compose")
                    ) {
                        Logger.w(TAG, "Thread: ${thread.name} state=${thread.state}")
                        stack.take(10).forEach { frame ->
                            Logger.w(TAG, "  at $frame")
                        }
                    }
                }
                Logger.w(TAG, "=== END THREAD DUMP ===")
            }
        }
    }

    private var retryCount = 0
    private var retryVideoId: String? = null
    private val maxRetryCount = 2

    private val playlist = mutableListOf<GenericMediaItem>()
    private var localCurrentMediaItemIndex = -1

    private var shuffleIndices = mutableListOf<Int>()
    private var shuffleOrder = mutableListOf<Int>()

    private var currentLoadJob: Job? = null

    fun getCurrentPlayer(): VlcPlayer? = currentPlayer

    private val _currentVideoSurface = MutableStateFlow<Component?>(null)
    val currentVideoSurface: StateFlow<Component?> = _currentVideoSurface.asStateFlow()

    private data class PlayableSource(
        val isVideo: Boolean,
        val url: String,
        val audioSlaveUrl: String? = null,
    )

    override fun play() {
        Logger.d(TAG, "play() called (current state: $internalState)")
        coroutineScope.launch {
            when (internalState) {
                InternalState.READY, InternalState.ENDED, InternalState.PAUSED -> {
                    currentPlayer?.let { player ->
                        Logger.d(TAG, "Play: calling VLC play")
                        player.play()
                        transitionToState(InternalState.PLAYING)
                        internalPlayWhenReady = true
                    } ?: Logger.w(TAG, "Play called but currentPlayer is null")
                }

                InternalState.PREPARING -> {
                    if (!cachedIsLoading) {
                        cachedIsLoading = true
                        notifyListeners { onIsLoadingChanged(true) }
                    }
                    internalPlayWhenReady = true
                    Logger.d(TAG, "Play: During PREPARING - will auto-play when ready")
                }

                InternalState.PLAYING -> {
                    internalPlayWhenReady = true
                    cachedIsLoading = false
                }

                else -> {
                    Logger.w(TAG, "Play: Called in invalid state: $internalState")
                }
            }
        }
    }

    override fun pause() {
        Logger.d(TAG, "pause() called (current state: $internalState)")
        coroutineScope.launch {
            if (isCrossfading) {
                Logger.d(TAG, "Pause: Cancelling crossfade")
                cancelCrossfadeAndCleanup(revertIndex = true)
            }

            when (internalState) {
                InternalState.PLAYING, InternalState.READY -> {
                    currentPlayer?.let { player ->
                        Logger.d(TAG, "Pause: calling VLC pause")
                        player.pause()
                        transitionToState(InternalState.PAUSED)
                        internalPlayWhenReady = false
                    }
                }

                InternalState.PREPARING -> {
                    internalPlayWhenReady = false
                    Logger.d(TAG, "Pause: During PREPARING - will not auto-play")
                }

                else -> {
                    Logger.w(TAG, "Pause: Called in invalid state: $internalState")
                }
            }
        }
    }

    override fun stop() {
        coroutineScope.launch {
            currentPlayer?.let { player ->
                Logger.d(TAG, "Stop called")
                player.stop()
                transitionToState(InternalState.IDLE)
                stopPositionUpdates()
                notifyEqualizerIntent(false)
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        currentPlayer?.let { player ->
            try {
                player.seekTo(positionMs)
                cachedPosition = positionMs
                Logger.d(TAG, "Seeked to position: $positionMs")
            } catch (e: Exception) {
                Logger.e(TAG, "Seek exception: ${e.message}", e)
            }
        }
    }

    override fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
    ) {
        if (mediaItemIndex !in playlist.indices) return

        coroutineScope.launch {
            val shouldPlay = internalPlayWhenReady

            if (isCrossfading) {
                Logger.d(TAG, "seekTo: Cancelling crossfade")
                cancelCrossfadeAndCleanup(revertIndex = false)
            }

            currentLoadJob?.cancel()

            localCurrentMediaItemIndex = mediaItemIndex
            currentPlayer?.release()
            currentPlayer = null
            currentPlayerIsVideo = false
            _currentVideoSurface.value = null
            loadAndPlayTrackInternal(mediaItemIndex, positionMs, shouldPlay)
        }
    }

    override fun seekBack() {
        val newPosition = (cachedPosition - 5000).coerceAtLeast(0)
        seekTo(newPosition)
    }

    override fun seekForward() {
        val newPosition = (cachedPosition + 5000).coerceAtMost(cachedDuration)
        seekTo(newPosition)
    }

    override fun seekToNext() {
        if (hasNextMediaItem()) {
            if (isCrossfading) {
                val targetIndex = localCurrentMediaItemIndex
                Logger.d(TAG, "seekToNext: Cancelling crossfade, seeking to track we're fading in (index $targetIndex)")
                coroutineScope.launch {
                    cancelCrossfadeAndCleanup(revertIndex = false)
                    seekTo(targetIndex, 0)
                }
                return
            }

            val nextIndex = getNextMediaItemIndex()
            seekTo(nextIndex, 0)
        }
    }

    override fun seekToPrevious() {
        coroutineScope.launch {
            if (isCrossfading) {
                Logger.d(TAG, "seekToPrevious: Cancelling crossfade")
                cancelCrossfadeAndCleanup(revertIndex = true)
            }

            val positionThresholdMs = 3000L
            if (cachedPosition > positionThresholdMs) {
                Logger.d(TAG, "seekToPrevious: pos=${cachedPosition}ms > ${positionThresholdMs}ms — seeking to start")
                currentPlayer?.seekTo(0)
                cachedPosition = 0
            } else if (hasPreviousMediaItem()) {
                Logger.d(TAG, "seekToPrevious: pos=${cachedPosition}ms <= ${positionThresholdMs}ms — going to previous track")
                val prevIndex = getPreviousMediaItemIndex()
                seekTo(prevIndex, 0)
            } else {
                Logger.d(TAG, "seekToPrevious: No previous item, seeking to start")
                currentPlayer?.seekTo(0)
                cachedPosition = 0
            }
        }
    }

    override fun seekToPreviousMediaItem() {
        coroutineScope.launch {
            if (isCrossfading) {
                Logger.d(TAG, "seekToPreviousMediaItem: Cancelling crossfade")
                cancelCrossfadeAndCleanup(revertIndex = true)
            }

            if (hasPreviousMediaItem()) {
                Logger.d(TAG, "seekToPreviousMediaItem: going to previous track")
                val prevIndex = getPreviousMediaItemIndex()
                seekTo(prevIndex, 0)
            } else {
                Logger.d(TAG, "seekToPreviousMediaItem: No previous item, seeking to start")
                currentPlayer?.seekTo(0)
                cachedPosition = 0
            }
        }
    }

    override fun prepare() {
        if (playlist.isNotEmpty() && localCurrentMediaItemIndex >= 0) {
            coroutineScope.launch {
                loadAndPlayTrackInternal(localCurrentMediaItemIndex, 0, false)
            }
        }
    }

    override fun setMediaItem(mediaItem: GenericMediaItem) {
        coroutineScope.launch {
            currentLoadJob?.cancel()
            cancelPrecaching()

            playlist.clear()
            clearAllPrecacheInternal()
            playlist.add(mediaItem)
            localCurrentMediaItemIndex = 0

            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
            loadAndPlayTrackInternal(0, 0, internalPlayWhenReady)
        }
    }

    override fun addMediaItem(mediaItem: GenericMediaItem) {
        playlist.add(mediaItem)

        if (internalShuffleModeEnabled) {
            createShuffleOrder()
        }

        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

        if (playlist.size - 1 - currentMediaItemIndex <= maxPrecacheCount) {
            coroutineScope.launch {
                clearPrecacheExceptCurrentInternal()
                triggerPrecachingInternal()
            }
        }
    }

    override fun addMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        if (index in 0..playlist.size) {
            val currentIndexBeforeInsert = localCurrentMediaItemIndex

            playlist.add(index, mediaItem)

            if (index <= localCurrentMediaItemIndex) {
                localCurrentMediaItemIndex++
            }

            if (internalShuffleModeEnabled) {
                if (currentIndexBeforeInsert >= 0 && index == currentIndexBeforeInsert + 1) {
                    val currentShufflePos = shuffleIndices.getOrNull(currentIndexBeforeInsert) ?: 0
                    insertIntoShuffleOrder(index, currentShufflePos)
                } else {
                    createShuffleOrder()
                }
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            if (index - 1 - currentMediaItemIndex <= maxPrecacheCount) {
                coroutineScope.launch {
                    clearPrecacheExceptCurrentInternal()
                    triggerPrecachingInternal()
                }
            }
        }
    }

    override fun removeMediaItem(index: Int) {
        if (index !in playlist.indices) return

        coroutineScope.launch {
            val track = playlist.removeAt(index)

            precachedPlayers.remove(track.mediaId)?.let { cached ->
                cleanupPlayerInternal(cached.player)
            }

            when {
                index < localCurrentMediaItemIndex -> {
                    localCurrentMediaItemIndex--
                    clearPrecacheExceptCurrentInternal()
                    triggerPrecachingInternal()
                }

                index == localCurrentMediaItemIndex -> {
                    if (localCurrentMediaItemIndex >= playlist.size) {
                        localCurrentMediaItemIndex = playlist.size - 1
                    }
                    if (localCurrentMediaItemIndex >= 0) {
                        loadAndPlayTrackInternal(localCurrentMediaItemIndex, 0, internalPlayWhenReady)
                    } else {
                        cleanupCurrentPlayerInternal()
                    }
                }

                else -> {
                    clearPrecacheExceptCurrentInternal()
                    triggerPrecachingInternal()
                }
            }

            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
        }
    }

    override fun moveMediaItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (fromIndex !in playlist.indices || toIndex !in playlist.indices) return

        coroutineScope.launch {
            val item = playlist.removeAt(fromIndex)
            playlist.add(toIndex, item)

            localCurrentMediaItemIndex =
                when {
                    localCurrentMediaItemIndex == fromIndex -> {
                        toIndex
                    }
                    fromIndex < localCurrentMediaItemIndex && toIndex >= localCurrentMediaItemIndex -> {
                        localCurrentMediaItemIndex - 1
                    }
                    fromIndex > localCurrentMediaItemIndex && toIndex <= localCurrentMediaItemIndex -> {
                        localCurrentMediaItemIndex + 1
                    }
                    else -> {
                        localCurrentMediaItemIndex
                    }
                }

            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            clearPrecacheExceptCurrentInternal()
            triggerPrecachingInternal()
        }
    }

    override fun clearMediaItems() {
        coroutineScope.launch {
            playlist.clear()
            localCurrentMediaItemIndex = -1
            clearShuffleOrder()
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
            cleanupCurrentPlayerInternal()
            clearAllPrecacheInternal()
        }
    }

    override fun replaceMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        if (index !in playlist.indices) return

        coroutineScope.launch {
            playlist[index] = mediaItem

            precachedPlayers.remove(mediaItem.mediaId)?.let { cached ->
                cleanupPlayerInternal(cached.player)
            }

            if (internalShuffleModeEnabled) {
                createShuffleOrder()
            }

            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")

            if (index == localCurrentMediaItemIndex) {
                loadAndPlayTrackInternal(index, 0, internalPlayWhenReady)
            } else {
                triggerPrecachingInternal()
            }
        }
    }

    override fun getMediaItemAt(index: Int): GenericMediaItem? = playlist.getOrNull(index)

    override fun getCurrentMediaTimeLine(): List<GenericMediaItem> =
        if (internalShuffleModeEnabled) {
            shuffleOrder.mapNotNull { shuffledIndex -> playlist.getOrNull(shuffledIndex) }
        } else {
            playlist.toList()
        }

    override fun getUnshuffledIndex(shuffledIndex: Int): Int =
        if (internalShuffleModeEnabled) {
            shuffleOrder.getOrNull(shuffledIndex) ?: -1
        } else {
            shuffledIndex
        }

    override val isPlaying: Boolean
        get() = internalState == InternalState.PLAYING

    override val currentPosition: Long
        get() = cachedPosition

    override val duration: Long
        get() = cachedDuration

    override val bufferedPosition: Long
        get() = cachedBufferedPosition

    override val bufferedPercentage: Int
        get() {
            val dur = duration
            if (dur <= 0) return 0
            return ((bufferedPosition * 100) / dur).toInt().coerceIn(0, 100)
        }

    override val currentMediaItem: GenericMediaItem?
        get() = playlist.getOrNull(localCurrentMediaItemIndex)

    override val currentMediaItemIndex: Int
        get() = localCurrentMediaItemIndex

    override val mediaItemCount: Int
        get() = playlist.size

    override val contentPosition: Long
        get() = cachedPosition

    override val playbackState: Int
        get() =
            when (internalState) {
                InternalState.IDLE -> PlayerConstants.STATE_IDLE
                InternalState.PREPARING -> PlayerConstants.STATE_BUFFERING
                InternalState.READY -> PlayerConstants.STATE_READY
                InternalState.PLAYING -> PlayerConstants.STATE_READY
                InternalState.ENDED -> PlayerConstants.STATE_ENDED
                InternalState.ERROR -> PlayerConstants.STATE_IDLE
                InternalState.PAUSED -> PlayerConstants.STATE_READY
            }

    override fun hasNextMediaItem(): Boolean =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> true
            PlayerConstants.REPEAT_MODE_ALL -> true
            else -> localCurrentMediaItemIndex < playlist.size - 1
        }

    override fun hasPreviousMediaItem(): Boolean =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> true
            PlayerConstants.REPEAT_MODE_ALL -> true
            else -> localCurrentMediaItemIndex > 0
        }

    private fun getNextMediaItemIndex(): Int =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> {
                localCurrentMediaItemIndex
            }
            PlayerConstants.REPEAT_MODE_ALL -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val nextShufflePos = (currentShufflePos + 1) % shuffleOrder.size
                    shuffleOrder.getOrNull(nextShufflePos) ?: localCurrentMediaItemIndex
                } else {
                    if (localCurrentMediaItemIndex < playlist.size - 1) {
                        localCurrentMediaItemIndex + 1
                    } else {
                        0
                    }
                }
            }
            else -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val nextShufflePos = currentShufflePos + 1
                    if (nextShufflePos < shuffleOrder.size) {
                        shuffleOrder.getOrNull(nextShufflePos) ?: localCurrentMediaItemIndex
                    } else {
                        localCurrentMediaItemIndex
                    }
                } else {
                    (localCurrentMediaItemIndex + 1).coerceAtMost(playlist.size - 1)
                }
            }
        }

    private fun getPreviousMediaItemIndex(): Int =
        when (internalRepeatMode) {
            PlayerConstants.REPEAT_MODE_ONE -> {
                localCurrentMediaItemIndex
            }
            PlayerConstants.REPEAT_MODE_ALL -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val prevShufflePos =
                        if (currentShufflePos > 0) {
                            currentShufflePos - 1
                        } else {
                            shuffleOrder.size - 1
                        }
                    shuffleOrder.getOrNull(prevShufflePos) ?: localCurrentMediaItemIndex
                } else {
                    if (localCurrentMediaItemIndex > 0) {
                        localCurrentMediaItemIndex - 1
                    } else {
                        playlist.size - 1
                    }
                }
            }
            else -> {
                if (internalShuffleModeEnabled && shuffleOrder.isNotEmpty()) {
                    val currentShufflePos = shuffleIndices.getOrNull(localCurrentMediaItemIndex) ?: 0
                    val prevShufflePos = currentShufflePos - 1
                    if (prevShufflePos >= 0) {
                        shuffleOrder.getOrNull(prevShufflePos) ?: localCurrentMediaItemIndex
                    } else {
                        localCurrentMediaItemIndex
                    }
                } else {
                    (localCurrentMediaItemIndex - 1).coerceAtLeast(0)
                }
            }
        }

    override var shuffleModeEnabled: Boolean
        get() = internalShuffleModeEnabled
        set(value) {
            if (internalShuffleModeEnabled == value) return

            internalShuffleModeEnabled = value

            if (value) {
                createShuffleOrder()
            } else {
                clearShuffleOrder()
            }

            val mediaItemList = getShuffledMediaItemList()
            notifyListeners { onShuffleModeEnabledChanged(value, mediaItemList) }
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
        }

    override var repeatMode: Int
        get() = internalRepeatMode
        set(value) {
            if (internalRepeatMode == value) return
            internalRepeatMode = value
            notifyListeners { onRepeatModeChanged(value) }
        }

    override var playWhenReady: Boolean
        get() = internalPlayWhenReady
        set(value) {
            internalPlayWhenReady = value
            if (value) play() else pause()
        }

    override var playbackParameters: GenericPlaybackParameters
        get() = GenericPlaybackParameters(internalPlaybackSpeed, internalPlaybackSpeed)
        set(value) {
            internalPlaybackSpeed = value.speed
            currentPlayer?.let { player ->
                try {
                    player.mediaPlayer.controls().setRate(value.speed)
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to set playback speed: ${e.message}")
                }
            }
        }

    override val audioSessionId: Int
        get() = 0

    override var volume: Float
        get() = internalVolume
        set(value) {
            Logger.w(TAG, "Setting volume to $value")
            internalVolume = value.coerceIn(0f, 1f)
            currentPlayer?.setVolume((internalVolume * 100).toInt())
            notifyListeners { onVolumeChanged(internalVolume) }
        }

    override var skipSilenceEnabled: Boolean = false

    override fun addListener(listener: MediaPlayerListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: MediaPlayerListener) {
        listeners.remove(listener)
    }

    override fun release() {
        currentLoadJob?.cancel()
        precacheJob?.cancel()
        positionUpdateJob?.cancel()
        crossfadeJob?.cancel()

        secondaryPlayer?.release()
        secondaryPlayer = null
        isCrossfading = false
        crossfadeFromIndex = -1

        coroutineScope.cancel()
        cleanupCurrentPlayerInternal()
        clearAllPrecacheInternal()
        listeners.clear()

        try {
            mediaPlayerFactory.release()
        } catch (e: Exception) {
            Logger.w(TAG, "Error releasing VLC factory: ${e.message}")
        }
    }

    private fun transitionToState(newState: InternalState) {
        if (internalState == newState) return

        val oldState = internalState
        internalState = newState

        Logger.d(TAG, "State transition: $oldState -> $newState (playWhenReady=$internalPlayWhenReady)")

        currentPlayer?.let { player ->
            val dur = player.length
            if (dur > 0L) {
                cachedDuration = dur
            }
        }

        when (newState) {
            InternalState.PAUSED -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_READY) }
                listeners.forEach { it.onIsPlayingChanged(false) }
            }

            InternalState.IDLE -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_IDLE) }
                listeners.forEach { it.onIsPlayingChanged(false) }
            }

            InternalState.PREPARING -> {
                Logger.d(TAG, "transitionToState PREPARING -> isLoading=true")
                cachedIsLoading = true
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_BUFFERING) }
                listeners.forEach { it.onIsLoadingChanged(true) }
            }

            InternalState.READY -> {
                Logger.d(TAG, "transitionToState READY -> isLoading=false")
                cachedIsLoading = false
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_READY) }
                listeners.forEach { it.onIsLoadingChanged(false) }
                if (internalPlayWhenReady) {
                    play()
                } else {
                    listeners.forEach { it.onIsPlayingChanged(false) }
                }
            }

            InternalState.PLAYING -> {
                Logger.d(TAG, "transitionToState PLAYING -> isLoading=false")
                cachedIsLoading = false
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_READY) }
                listeners.forEach { it.onIsLoadingChanged(false) }
                listeners.forEach { it.onIsPlayingChanged(true) }
            }

            InternalState.ENDED -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_ENDED) }
                listeners.forEach { it.onIsPlayingChanged(false) }
            }

            InternalState.ERROR -> {
                listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_IDLE) }
                listeners.forEach { it.onIsPlayingChanged(false) }
                listeners.forEach {
                    it.onPlayerError(
                        PlayerError(
                            errorCode = 403,
                            errorCodeName = "ERROR_UNKNOWN",
                            message = "Can not extract playable URL or playback error",
                        ),
                    )
                }
            }
        }
    }

    private fun loadAndPlayTrackInternal(
        index: Int,
        startPositionMs: Long,
        shouldPlay: Boolean,
    ) {
        if (index !in playlist.indices) return

        val mediaItem = playlist[index]
        val videoId = mediaItem.mediaId

        currentLoadJob?.cancel()

        currentLoadJob =
            coroutineScope.launch {
                try {
                    transitionToState(InternalState.PREPARING)

                    notifyListeners {
                        onMediaItemTransition(
                            mediaItem,
                            PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_AUTO,
                        )
                    }

                    cleanupEventListenerInternal()
                    stopPositionUpdates()

                    val cachedPrecache = precachedPlayers.remove(videoId)
                    var resolvedSource: PlayableSource? = null
                    val player =
                        if (cachedPrecache?.player != null) {
                            cachedPrecache.player
                        } else {
                            var source = withContext(Dispatchers.IO) { extractPlayableUrl(mediaItem) }
                            if (source == null || source.url.isEmpty()) {
                                Logger.w(TAG, "First extract failed for $videoId, invalidating and retrying...")
                                withContext(Dispatchers.IO) {
                                    streamRepository.invalidateFormat(videoId)
                                    streamRepository.invalidateFormat("${MERGING_DATA_TYPE.VIDEO}$videoId")
                                }
                                source = withContext(Dispatchers.IO) { extractPlayableUrl(mediaItem) }
                            }
                            if (source == null || source.url.isEmpty()) {
                                Logger.e(TAG, "Failed to extract playable URL for $videoId after retry")
                                transitionToState(InternalState.ERROR)
                                return@launch
                            }
                            resolvedSource = source
                            createMediaPlayerInternal(source)
                        }

                    cleanupCurrentPlayerInternal()
                    currentPlayer = player
                    currentPlayerIsVideo = player.videoSurface != null
                    _currentVideoSurface.value = player.videoSurface
                    setupPlayerEventsInternal(player)
                    player.setVolume((internalVolume * 100).toInt())

                    if (cachedPrecache != null) {
                        if (shouldPlay) {
                            player.mediaPlayer.controls().play()
                        }
                        Logger.d(TAG, "Playing from precache for $videoId")
                    } else {
                        val source = resolvedSource
                        if (source != null) {
                            val options = buildVlcOptions(source)
                            if (shouldPlay) {
                                player.mediaPlayer.media().play(source.url, *options)
                            } else {
                                player.mediaPlayer.media().startPaused(source.url, *options)
                            }
                        } else {
                            Logger.e(TAG, "resolvedSource is null — should not happen")
                            transitionToState(InternalState.ERROR)
                            return@launch
                        }
                    }

                    if (startPositionMs > 0) {
                        delay(100)
                        player.seekTo(startPositionMs)
                        cachedPosition = startPositionMs
                    }

                    transitionToState(InternalState.READY)

                    startPositionUpdates()

                    triggerPrecachingInternal()
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Logger.e(TAG, "Load track error: ${e.message}", e)
                        transitionToState(InternalState.ERROR)
                    }
                }
            }
    }

    private fun buildVlcOptions(source: PlayableSource): Array<String> {
        val options = mutableListOf<String>()
        if (source.audioSlaveUrl != null) {
            options.add(":input-slave=${source.audioSlaveUrl}")
        }
        if (source.isVideo) {
            options.add(":network-caching=15000")
            options.add(":http-reconnect")
        }
        return options.toTypedArray()
    }

    private fun createMediaPlayerInternal(source: PlayableSource): VlcPlayer {
        if (source.isVideo) {
            Logger.d(TAG, "Creating video player with callback surface")
            val videoPanel = VlcVideoSurfacePanel()

            val embeddedPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer()
            val surface = videoPanel.createVideoSurface(mediaPlayerFactory)
            embeddedPlayer.videoSurface().set(surface)

            return VlcPlayer(
                mediaPlayer = embeddedPlayer,
                videoSurface = videoPanel,
            )
        }

        Logger.d(TAG, "Creating audio-only player")
        val player = mediaPlayerFactory.mediaPlayers().newMediaPlayer()
        return VlcPlayer(
            mediaPlayer = player,
            videoSurface = null,
        )
    }

    private fun setupPlayerEventsInternal(player: VlcPlayer) {
        cleanupEventListenerInternal()

        val listener =
            object : MediaPlayerEventAdapter() {
                override fun finished(mediaPlayer: MediaPlayer) {
                    Logger.d(TAG, "End of stream reached")
                    coroutineScope.launch {
                        if (currentPlayer?.mediaPlayer != mediaPlayer) {
                            Logger.w(TAG, "Ignoring finished() from stale player")
                            return@launch
                        }
                        if (isCrossfading) {
                            Logger.d(TAG, "Ignoring finished() during crossfade (old player ended)")
                            return@launch
                        }
                        transitionToState(InternalState.ENDED)
                        handleTrackEndInternal()
                    }
                }

                override fun error(mediaPlayer: MediaPlayer) {
                    Logger.e(TAG, "VLC playback error")
                    coroutineScope.launch {
                        if (currentPlayer?.mediaPlayer != mediaPlayer) {
                            Logger.w(TAG, "Ignoring error() from stale player")
                            return@launch
                        }
                        if (isCrossfading) {
                            Logger.w(TAG, "Ignoring error() during crossfade (old player error)")
                            return@launch
                        }
                        val currentVideoId = playlist.getOrNull(localCurrentMediaItemIndex)?.mediaId
                        if (currentVideoId != null) {
                            if (retryVideoId != currentVideoId) {
                                retryVideoId = currentVideoId
                                retryCount = 0
                            }
                            if (retryCount < maxRetryCount) {
                                retryCount++
                                Logger.w(TAG, "Retrying playback (attempt $retryCount/$maxRetryCount) for $currentVideoId")
                                try {
                                    streamRepository.invalidateFormat(currentVideoId)
                                    streamRepository.invalidateFormat("${MERGING_DATA_TYPE.VIDEO}$currentVideoId")
                                    precachedPlayers.remove(currentVideoId)?.player?.release()
                                    loadAndPlayTrackInternal(localCurrentMediaItemIndex, 0L, shouldPlay = true)
                                    return@launch
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    Logger.e(TAG, "Retry failed: ${e.message}", e)
                                }
                            }
                            Logger.e(TAG, "Max retries ($maxRetryCount) exhausted for $currentVideoId")
                        }
                        val error =
                            PlayerError(
                                errorCode = PlayerConstants.ERROR_CODE_TIMEOUT,
                                errorCodeName = "VLC_ERROR",
                                message = "Playback error",
                            )
                        listeners.forEach { it.onPlayerError(error) }
                        transitionToState(InternalState.ERROR)
                    }
                }

                override fun playing(mediaPlayer: MediaPlayer) {
                    coroutineScope.launch {
                        if (currentPlayer?.mediaPlayer != mediaPlayer) return@launch
                        if (internalState != InternalState.PLAYING) {
                            transitionToState(InternalState.PLAYING)
                            notifyEqualizerIntent(true)
                            retryCount = 0
                            retryVideoId = null
                        }
                    }
                }

                override fun paused(mediaPlayer: MediaPlayer) {
                    coroutineScope.launch {
                        if (currentPlayer?.mediaPlayer != mediaPlayer) return@launch
                        if (internalState == InternalState.PLAYING) {
                            transitionToState(InternalState.PAUSED)
                            notifyEqualizerIntent(false)
                        }
                    }
                }

                override fun stopped(mediaPlayer: MediaPlayer) {
                    coroutineScope.launch {
                        notifyEqualizerIntent(false)
                    }
                }

                override fun timeChanged(
                    mediaPlayer: MediaPlayer,
                    newTime: Long,
                ) {
                    if (!isCrossfading) {
                        cachedPosition = newTime
                    }
                }

                override fun lengthChanged(
                    mediaPlayer: MediaPlayer,
                    newLength: Long,
                ) {
                    if (!isCrossfading && newLength > 0) {
                        cachedDuration = newLength
                    }
                }

                override fun buffering(
                    mediaPlayer: MediaPlayer,
                    newCache: Float,
                ) {
                    if (isCrossfading) return

                    if (cachedDuration > 0) {
                        cachedBufferedPosition = (cachedDuration * newCache / 100f).toLong()
                    }

                    val isStalled = newCache < 100f && cachedBufferedPosition <= cachedPosition
                    Logger.d(
                        TAG,
                        "buffering: cache=$newCache%, bufferedPos=$cachedBufferedPosition, currentPos=$cachedPosition, isStalled=$isStalled, cachedIsLoading=$cachedIsLoading",
                    )
                    if (isStalled != cachedIsLoading) {
                        Logger.w(TAG, "isLoading changed: $cachedIsLoading -> $isStalled")
                        cachedIsLoading = isStalled
                        notifyListeners { onIsLoadingChanged(isStalled) }
                    }
                }

                override fun opening(mediaPlayer: MediaPlayer) {
                    Logger.d(TAG, "VLC opening media")
                }
            }

        player.setEventListener(listener)
    }

    private fun cleanupEventListenerInternal() {
        currentPlayer?.setEventListener(null)
    }

    private fun cleanupPlayerInternal(player: VlcPlayer) {
        try {
            player.release()
        } catch (e: Exception) {
            Logger.w(TAG, "Error cleaning up player: ${e.message}")
        }
    }

    private fun cleanupCurrentPlayerInternal() {
        stopPositionUpdates()
        cleanupEventListenerInternal()

        crossfadeJob?.cancel()
        crossfadeJob = null
        setCrossfading(false)

        currentPlayer?.let { cleanupPlayerInternal(it) }
        currentPlayer = null
        currentPlayerIsVideo = false
        _currentVideoSurface.value = null
    }

    private fun handleTrackEndInternal() {
        if (isCrossfading) {
            Logger.d(TAG, "handleTrackEndInternal: crossfade in progress, ignoring track end")
            return
        }

        val shouldCrossfade =
            crossfadeEnabled &&
                hasNextMediaItem()

        if (shouldCrossfade) {
            val nextIndex = getNextMediaItemIndex()
            triggerCrossfadeTransition(nextIndex)
        } else {
            when (internalRepeatMode) {
                PlayerConstants.REPEAT_MODE_ONE -> {
                    seekTo(localCurrentMediaItemIndex, 0)
                }

                PlayerConstants.REPEAT_MODE_ALL -> {
                    if (hasNextMediaItem()) {
                        seekToNext()
                    }
                }

                else -> {
                    if (localCurrentMediaItemIndex < playlist.size - 1) {
                        seekToNext()
                    } else {
                        notifyEqualizerIntent(false)
                    }
                }
            }
        }
    }

    private fun triggerCrossfadeTransition(nextIndex: Int) {
        if (nextIndex !in playlist.indices || isCrossfading) return

        crossfadeJob =
            coroutineScope.launch {
                try {
                    setCrossfading(true)
                    val nextMediaItem = playlist[nextIndex]
                    val nextVideoId = nextMediaItem.mediaId

                    Logger.d(TAG, "Starting crossfade to track $nextIndex")

                    val cachedPrecache = precachedPlayers.remove(nextVideoId)
                    val nextPlayer: VlcPlayer? =
                        if (cachedPrecache?.player != null) {
                            Logger.d(TAG, "Using precached player for crossfade")
                            cachedPrecache.player
                        } else {
                            val nextSource = withContext(Dispatchers.IO) { extractPlayableUrl(nextMediaItem) }
                            if (nextSource == null || nextSource.url.isEmpty()) {
                                Logger.e(TAG, "Failed to extract URL for crossfade")
                                null
                            } else {
                                createMediaPlayerInternal(nextSource).also { newPlayer ->
                                    val options = buildVlcOptions(nextSource)
                                    newPlayer.mediaPlayer.media().startPaused(nextSource.url, *options)
                                }
                            }
                        }

                    if (nextPlayer != null) {
                        secondaryPlayer = nextPlayer
                        nextPlayer.setEventListener(
                            object : MediaPlayerEventAdapter() {
                                override fun error(mediaPlayer: MediaPlayer) {
                                    Logger.e(TAG, "Secondary player error during crossfade")
                                    coroutineScope.launch {
                                        crossfadeJob?.cancel()
                                        secondaryPlayer?.release()
                                        secondaryPlayer = null
                                        setCrossfading(false)
                                        seekTo(nextIndex, 0)
                                    }
                                }
                            },
                        )
                        nextPlayer.setVolume(0)
                        nextPlayer.mediaPlayer.controls().play()
                    }

                    if (nextPlayer == null) {
                        setCrossfading(false)
                        seekTo(nextIndex, 0)
                        return@launch
                    }

                    crossfadeFromIndex = localCurrentMediaItemIndex

                    localCurrentMediaItemIndex = nextIndex
                    if (nextPlayer.videoSurface != null) {
                        _currentVideoSurface.value = nextPlayer.videoSurface
                    }
                    notifyListeners {
                        onMediaItemTransition(
                            nextMediaItem,
                            PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_AUTO,
                        )
                    }

                    Logger.d(TAG, "Now playing updated to track $nextIndex during crossfade")

                    val actualTimeRemaining =
                        currentPlayer?.let { player ->
                            val dur = player.length
                            val pos = player.time
                            if (dur > 0 && pos >= 0) (dur - pos) else crossfadeDurationMs.toLong()
                        } ?: crossfadeDurationMs.toLong()

                    val effectiveCrossfadeDurationMs =
                        minOf(
                            crossfadeDurationMs.toLong(),
                            actualTimeRemaining,
                        ).coerceAtLeast(1000L).toInt()

                    Logger.d(
                        TAG,
                        "Crossfade duration: configured=${crossfadeDurationMs}ms, " +
                            "actualRemaining=${actualTimeRemaining}ms, effective=${effectiveCrossfadeDurationMs}ms",
                    )

                    performCrossfade(nextIndex, nextPlayer, effectiveCrossfadeDurationMs)
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Logger.e(TAG, "Crossfade error: ${e.message}", e)
                        setCrossfading(false)
                        seekTo(nextIndex, 0)
                    }
                }
            }
    }

    private suspend fun cancelCrossfadeAndCleanup(revertIndex: Boolean) {
        val job = crossfadeJob
        crossfadeJob = null
        job?.cancel()
        job?.join()
        secondaryPlayer?.release()
        secondaryPlayer = null
        if (revertIndex && crossfadeFromIndex >= 0) {
            localCurrentMediaItemIndex = crossfadeFromIndex
            playlist.getOrNull(crossfadeFromIndex)?.let { mediaItem ->
                notifyListeners {
                    onMediaItemTransition(
                        mediaItem,
                        PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_SEEK,
                    )
                }
            }
        }
        crossfadeFromIndex = -1
        setCrossfading(false)
    }

    private suspend fun performCrossfade(
        nextIndex: Int,
        nextPlayer: VlcPlayer,
        effectiveDurationMs: Int,
    ) {
        val steps = 50
        val delayPerStep = (effectiveDurationMs / steps).coerceAtLeast(20)
        val targetVolume = (internalVolume * 100).toInt()
        Logger.d(
            TAG,
            "Crossfade animation: ${effectiveDurationMs}ms, $steps steps, ${delayPerStep}ms/step, internalVolume=$internalVolume, targetVolume=$targetVolume",
        )

        try {
            for (step in 0..steps) {
                currentCoroutineContext().ensureActive()

                val progress = step.toFloat() / steps
                val angle = progress * Math.PI / 2.0

                val fadeOutVolume = (targetVolume * kotlin.math.cos(angle)).toInt()
                currentPlayer?.setVolume(fadeOutVolume)

                val fadeInVolume = (targetVolume * kotlin.math.sin(angle)).toInt()
                nextPlayer.setVolume(fadeInVolume)

                delay(delayPerStep.toLong())
            }

            finalizeCrossfade(nextIndex, nextPlayer)
        } catch (e: CancellationException) {
            Logger.d(TAG, "Crossfade cancelled")
            nextPlayer.release()
            secondaryPlayer = null
            setCrossfading(false)
        }
    }

    private fun finalizeCrossfade(
        nextIndex: Int,
        nextPlayer: VlcPlayer,
    ) {
        Logger.d(TAG, "Crossfade complete, swapping players")

        stopPositionUpdates()

        currentPlayer?.let { oldPlayer ->
            try {
                oldPlayer.release()
            } catch (e: Exception) {
                Logger.w(TAG, "Error cleaning up old player: ${e.message}")
            }
        }

        currentPlayer = nextPlayer
        currentPlayerIsVideo = nextPlayer.videoSurface != null
        _currentVideoSurface.value = nextPlayer.videoSurface
        secondaryPlayer = null

        setupPlayerEventsInternal(nextPlayer)

        currentPlayer?.setVolume((internalVolume * 100).toInt())

        setCrossfading(false)
        crossfadeFromIndex = -1
        transitionToState(InternalState.PLAYING)

        startPositionUpdates()

        triggerPrecachingInternal()
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()

        positionUpdateJob =
            coroutineScope.launch {
                while (isActive && currentPlayer != null) {
                    try {
                        if (internalState == InternalState.PLAYING ||
                            internalState == InternalState.READY ||
                            internalState == InternalState.PAUSED
                        ) {
                            if (isCrossfading) {
                                val nextPlayer = secondaryPlayer
                                if (nextPlayer != null) {
                                    val pos = nextPlayer.time
                                    val dur = nextPlayer.length
                                    if (pos > 0) cachedPosition = pos
                                    if (dur > 0) cachedDuration = dur
                                }
                            } else {
                                val player = currentPlayer
                                if (player != null) {
                                    val pos = player.time
                                    val dur = player.length
                                    if (pos > 0) cachedPosition = pos
                                    if (dur > 0) cachedDuration = dur
                                }
                            }

                            if (crossfadeEnabled &&
                                !isCrossfading
                            ) {
                                val player = currentPlayer
                                if (player != null) {
                                    val dur = player.length
                                    val pos = player.time
                                    if (dur > 0 && pos > 0) {
                                        val timeRemaining = dur - pos
                                        val nextVideoId = playlist.getOrNull(getNextMediaItemIndex())?.mediaId
                                        val isPrecached = nextVideoId != null && precachedPlayers.containsKey(nextVideoId)
                                        val preparationBufferMs = if (isPrecached) 0L else 3000L
                                        val triggerThreshold = crossfadeDurationMs.toLong() + preparationBufferMs
                                        if (timeRemaining in 1..triggerThreshold) {
                                            if (hasNextMediaItem()) {
                                                val nextIndex = getNextMediaItemIndex()
                                                triggerCrossfadeTransition(nextIndex)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }

                    delay(200)
                }
            }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun triggerPrecachingInternal() {
        if (!precacheEnabled || playlist.isEmpty()) return

        cancelPrecaching()
        Logger.d(TAG, "Trigger precache")
        precacheJob =
            coroutineScope.launch {
                try {
                    val indicesToPrecache = mutableListOf<Int>()

                    val index = localCurrentMediaItemIndex
                    for (i in 1..maxPrecacheCount) {
                        val nextIndex =
                            when (internalRepeatMode) {
                                PlayerConstants.REPEAT_MODE_ALL -> {
                                    (index + i) % playlist.size
                                }
                                else -> {
                                    val next = index + i
                                    if (next < playlist.size) next else break
                                }
                            }

                        if (nextIndex != localCurrentMediaItemIndex &&
                            !precachedPlayers.containsKey(playlist.getOrNull(nextIndex)?.mediaId)
                        ) {
                            indicesToPrecache.add(nextIndex)
                        }
                    }

                    for (idx in indicesToPrecache) {
                        if (!isActive) break

                        val mediaItem = playlist.getOrNull(idx) ?: continue

                        val source = withContext(Dispatchers.IO) { extractPlayableUrl(mediaItem) }

                        if (source != null && source.url.isNotEmpty()) {
                            try {
                                val player = createMediaPlayerInternal(source)
                                val options = buildVlcOptions(source)
                                player.mediaPlayer.media().prepare(source.url, *options)
                                precachedPlayers[mediaItem.mediaId] =
                                    PrecachedPlayer(player, mediaItem, source)
                                Logger.d(TAG, "Precached player for index $idx")
                            } catch (e: Exception) {
                                Logger.e(TAG, "Precaching error for $idx: ${e.message}")
                            }
                        }

                        delay(100)
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Logger.e(TAG, "Precaching error: ${e.message}")
                    }
                }
            }
    }

    private fun cancelPrecaching() {
        precacheJob?.cancel()
        precacheJob = null
    }

    private fun clearPrecacheExceptCurrentInternal() {
        Logger.d(TAG, "Clearing precache")
        precachedPlayers.entries.removeIf { (videoId, cached) ->
            if (videoId != currentMediaItem?.mediaId) {
                cleanupPlayerInternal(cached.player)
                true
            } else {
                false
            }
        }
    }

    private fun clearAllPrecacheInternal() {
        Logger.d(TAG, "Clearing all precache")
        precachedPlayers.values.forEach { cleanupPlayerInternal(it.player) }
        precachedPlayers.clear()
    }

    private fun notifyListeners(block: MediaPlayerListener.() -> Unit) {
        coroutineScope.launch {
            listeners.forEach(block)
        }
    }

    private fun notifyEqualizerIntent(shouldOpen: Boolean) {
        notifyListeners { shouldOpenOrCloseEqualizerIntent(shouldOpen) }
    }

    private fun createShuffleOrder() {
        if (playlist.isEmpty()) {
            shuffleIndices.clear()
            shuffleOrder.clear()
            return
        }

        val indices = playlist.indices.toMutableList()
        val currentIndex = localCurrentMediaItemIndex
        if (currentIndex in indices) {
            indices.removeAt(currentIndex)
        }

        indices.shuffle()

        if (currentIndex in playlist.indices) {
            indices.add(0, currentIndex)
        }

        shuffleOrder.clear()
        shuffleOrder.addAll(indices)

        shuffleIndices.clear()
        shuffleIndices.addAll(List(playlist.size) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, originalIndex ->
            shuffleIndices[originalIndex] = shuffledPos
        }

        Logger.d(TAG, "Created shuffle order: $shuffleOrder")
    }

    private fun clearShuffleOrder() {
        shuffleIndices.clear()
        shuffleOrder.clear()
    }

    private fun insertIntoShuffleOrder(
        insertedOriginalIndex: Int,
        afterShufflePos: Int,
    ) {
        if (playlist.isEmpty() || insertedOriginalIndex !in playlist.indices) return

        for (i in shuffleOrder.indices) {
            if (shuffleOrder[i] >= insertedOriginalIndex) {
                shuffleOrder[i]++
            }
        }

        val insertPos = (afterShufflePos + 1).coerceIn(0, shuffleOrder.size)
        shuffleOrder.add(insertPos, insertedOriginalIndex)

        shuffleIndices.clear()
        shuffleIndices.addAll(List(playlist.size) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, origIndex ->
            if (origIndex < shuffleIndices.size) {
                shuffleIndices[origIndex] = shuffledPos
            }
        }
    }

    private fun getShuffledMediaItemList(): List<GenericMediaItem> {
        if (!internalShuffleModeEnabled || shuffleOrder.isEmpty()) {
            return playlist.toList()
        }
        return shuffleOrder.mapNotNull { playlist.getOrNull(it) }
    }

    private fun notifyTimelineChanged(reason: String) {
        val list = getShuffledMediaItemList()
        notifyListeners { onTimelineChanged(list, reason) }
    }

    fun setPrecachingEnabled(enabled: Boolean) {
        precacheEnabled = enabled
        if (!enabled) {
            clearPrecacheExceptCurrentInternal()
        } else {
            triggerPrecachingInternal()
        }
    }

    fun setMaxPrecacheCount(count: Int) {
    }

    private suspend fun extractPlayableUrl(mediaItem: GenericMediaItem): PlayableSource? {
        Logger.w(TAG, "Extracting playable URL for ${mediaItem.mediaId}")
        val shouldFindVideo =
            mediaItem.isVideo() &&
                dataStoreManager.watchVideoInsteadOfPlayingAudio.first() == DataStoreManager.TRUE
        val videoId = mediaItem.mediaId

        val downloadFiles =
            File(getDownloadPath()).listFiles()?.filter {
                it.name.contains(videoId)
            }
        if (!downloadFiles.isNullOrEmpty()) {
            val audioFile = downloadFiles.firstOrNull { !it.name.contains(MERGING_DATA_TYPE.VIDEO) }
            if (audioFile != null && audioFile.length() > 0 && audioFile.exists()) {
                return PlayableSource(isVideo = false, url = audioFile.absolutePath)
            }
        }

        streamRepository.getNewFormat(videoId).lastOrNull()?.let { format ->
            val audioUrl = format.audioUrl
            val videoUrl = format.videoUrl

            if (shouldFindVideo && !videoUrl.isNullOrEmpty()) {
                val is403Video = streamRepository.is403Url(videoUrl).firstOrNull() != false
                if (!is403Video) {
                    val audioSlave =
                        if (!audioUrl.isNullOrEmpty()) {
                            val is403Audio = streamRepository.is403Url(audioUrl).firstOrNull() != false
                            if (!is403Audio) audioUrl else null
                        } else {
                            null
                        }

                    Logger.w("Stream", "Video from format (with audio slave: ${audioSlave != null})")
                    return PlayableSource(
                        isVideo = true,
                        url = videoUrl,
                        audioSlaveUrl = audioSlave,
                    )
                }
            } else if (!shouldFindVideo && !audioUrl.isNullOrEmpty()) {
                val is403Url = streamRepository.is403Url(audioUrl).firstOrNull() != false
                if (!is403Url) {
                    Logger.w("Stream", "Audio from format")
                    return PlayableSource(isVideo = false, url = audioUrl)
                }
            }
        }

        if (shouldFindVideo) {
            val videoUrl =
                streamRepository
                    .getStream(
                        dataStoreManager,
                        videoId,
                        isDownloading = false,
                        isVideo = true,
                        muxed = true,
                    ).lastOrNull()
            if (videoUrl != null) {
                Logger.d(TAG, "Stream Video $videoUrl")
                return PlayableSource(isVideo = true, url = videoUrl)
            }
        } else {
            val audioUrl =
                streamRepository
                    .getStream(
                        dataStoreManager,
                        videoId,
                        isDownloading = false,
                        isVideo = false,
                    ).lastOrNull()
            if (audioUrl != null) {
                Logger.d(TAG, "Stream Audio $audioUrl")
                return PlayableSource(isVideo = false, url = audioUrl)
            }
        }

        return null
    }
}

class VlcPlayer(
    val mediaPlayer: MediaPlayer,
    val videoSurface: Component? = null,
) {
    companion object {
        private const val TAG = "VlcPlayer"
    }

    @Volatile
    var isReleased = false
        private set

    private var eventListener: MediaPlayerEventAdapter? = null

    fun setEventListener(listener: MediaPlayerEventAdapter?) {
        eventListener?.let {
            try {
                mediaPlayer.events().removeMediaPlayerEventListener(it)
            } catch (_: Exception) {
            }
        }
        eventListener = listener
        listener?.let {
            try {
                mediaPlayer.events().addMediaPlayerEventListener(it)
            } catch (_: Exception) {
            }
        }
    }

    fun play() {
        if (isReleased) return
        try {
            mediaPlayer.controls().play()
        } catch (e: Exception) {
            Logger.w(TAG, "Error playing: ${e.message}")
        }
    }

    fun pause() {
        if (isReleased) return
        try {
            mediaPlayer.controls().setPause(true)
        } catch (e: Exception) {
            Logger.w(TAG, "Error pausing: ${e.message}")
        }
    }

    fun stop() {
        if (isReleased) return
        try {
            mediaPlayer.controls().stop()
        } catch (e: Exception) {
            Logger.w(TAG, "Error stopping: ${e.message}")
        }
    }

    fun setVolume(volume: Int) {
        if (isReleased) return
        try {
            mediaPlayer.audio().setVolume(volume)
        } catch (e: Exception) {
            Logger.w(TAG, "Error setting volume: ${e.message}")
        }
    }

    fun seekTo(timeMs: Long) {
        if (isReleased) return
        try {
            mediaPlayer.controls().setTime(timeMs)
        } catch (e: Exception) {
            Logger.w(TAG, "Error seeking: ${e.message}")
        }
    }

    val time: Long
        get() =
            if (isReleased) {
                0L
            } else {
                try {
                    mediaPlayer.status().time()
                } catch (_: Exception) {
                    0L
                }
            }

    val length: Long
        get() =
            if (isReleased) {
                0L
            } else {
                try {
                    mediaPlayer.status().length()
                } catch (_: Exception) {
                    0L
                }
            }

    fun release() {
        if (isReleased) return
        isReleased = true
        try {
            setEventListener(null)
            Thread {
                try {
                    mediaPlayer.controls().stop()
                    mediaPlayer.release()
                } catch (e: Exception) {
                    Logger.w(TAG, "Error in async release: ${e.message}")
                }
            }.start()
        } catch (e: Exception) {
            Logger.w(TAG, "Error releasing player: ${e.message}")
        }
    }
}

class VlcVideoSurfacePanel : JPanel() {
    @Volatile
    private var videoImage: BufferedImage? = null

    @Volatile
    private var videoWidth = 0

    @Volatile
    private var videoHeight = 0

    private val bufferFormatCb =
        object : BufferFormatCallback {
            override fun getBufferFormat(
                sourceWidth: Int,
                sourceHeight: Int,
            ): BufferFormat {
                videoWidth = sourceWidth
                videoHeight = sourceHeight
                videoImage = BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_ARGB)
                return RV32BufferFormat(sourceWidth, sourceHeight)
            }

            override fun newFormatSize(
                p0: Int,
                p1: Int,
                p2: Int,
                p3: Int,
            ) {
            }

            override fun allocatedBuffers(buffers: Array<out ByteBuffer>) {
            }
        }

    private val renderCb =
        object : RenderCallback {
            override fun display(
                p0: MediaPlayer,
                p1: Array<ByteBuffer>,
                p2: BufferFormat,
                p3: Int,
                p4: Int,
            ) {
                val img = videoImage ?: return
                try {
                    val rgbArray = (img.raster.dataBuffer as DataBufferInt).data
                    val intBuffer = p1[0].asIntBuffer()
                    intBuffer.get(rgbArray, 0, minOf(rgbArray.size, intBuffer.remaining()))
                    repaint()
                } catch (_: Exception) {
                }
            }

            override fun lock(p0: MediaPlayer?) {
            }

            override fun unlock(p0: MediaPlayer?) {
            }
        }

    @Volatile
    private var videoSurfaceRef: Any? = null

    init {
        background = Color.BLACK
        isOpaque = true
    }

    fun createVideoSurface(factory: MediaPlayerFactory): uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurface {
        val surface =
            factory.videoSurfaces().newVideoSurface(
                bufferFormatCb,
                renderCb,
                true,
            )
        videoSurfaceRef = surface
        return surface
    }

    override fun getPreferredSize(): java.awt.Dimension =
        if (videoWidth > 0 && videoHeight > 0) {
            java.awt.Dimension(videoWidth, videoHeight)
        } else {
            java.awt.Dimension(640, 360)
        }

    override fun getMinimumSize(): java.awt.Dimension = java.awt.Dimension(1, 1)

    override fun getMaximumSize(): java.awt.Dimension = java.awt.Dimension(Int.MAX_VALUE, Int.MAX_VALUE)

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val img = videoImage ?: return
        val g2 = g as Graphics2D
        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
        )
        val panelW = width.toDouble()
        val panelH = height.toDouble()
        val imgW = img.width.toDouble()
        val imgH = img.height.toDouble()
        if (imgW <= 0 || imgH <= 0) return

        val scale = minOf(panelW / imgW, panelH / imgH)
        val drawW = (imgW * scale).toInt()
        val drawH = (imgH * scale).toInt()
        val x = ((panelW - drawW) / 2).toInt()
        val y = ((panelH - drawH) / 2).toInt()

        g2.drawImage(img, x, y, drawW, drawH, null)
    }
}