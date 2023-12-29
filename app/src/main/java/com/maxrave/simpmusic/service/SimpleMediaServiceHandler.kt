package com.maxrave.simpmusic.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.MEDIA_CUSTOM_COMMAND
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.connectArtists
import com.maxrave.simpmusic.extension.toListName
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.first

@UnstableApi
class SimpleMediaServiceHandler constructor(
    val player: ExoPlayer,
    private val mediaSession: MediaSession,
    mediaSessionCallback: SimpleMediaSessionCallback,
    private val dataStoreManager: DataStoreManager,
    private val mainRepository: MainRepository,
    var coroutineScope: LifecycleCoroutineScope,
    private val context: Context
) : Player.Listener {

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var volumeNormalizationJob: Job? = null

    private var sleepTimerJob: Job? = null

    private val _simpleMediaState = MutableStateFlow<SimpleMediaState>(SimpleMediaState.Initial)
    val simpleMediaState = _simpleMediaState.asStateFlow()

    private var _nowPlaying = MutableStateFlow(player.currentMediaItem)
    val nowPlaying = _nowPlaying.asSharedFlow()

    private val _nextTrackAvailable = MutableStateFlow<Boolean>(false)
    val nextTrackAvailable = _nextTrackAvailable.asSharedFlow()

    private val _previousTrackAvailable = MutableStateFlow<Boolean>(false)
    val previousTrackAvailable = _previousTrackAvailable.asSharedFlow()

    private val _shuffle = MutableStateFlow<Boolean>(false)
    val shuffle = _shuffle.asSharedFlow()

    private val _repeat = MutableStateFlow<RepeatState>(RepeatState.None)
    val repeat = _repeat.asSharedFlow()

    private val _sleepMinutes = MutableStateFlow<Int>(0)
    val sleepMinutes = _sleepMinutes.asSharedFlow()

    private val _sleepDone = MutableStateFlow<Boolean>(false)
    val sleepDone = _sleepDone.asSharedFlow()

    private val _liked = MutableStateFlow(false)
    val liked = _liked.asSharedFlow()

    private var skipSilent = false

    private var normalizeVolume = false

    private var job: Job? = null

    private var updateNotificationJob: Job? = null

    private var toggleLikeJob: Job? = null

    private var loadJob : Job? = null

    //Add MusicSource to this
    var catalogMetadata: ArrayList<Track> = (arrayListOf())

    var added: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var _stateFlow = MutableStateFlow<StateSource>(StateSource.STATE_CREATED)
    val stateFlow = _stateFlow.asStateFlow()
    private var _currentSongIndex = MutableStateFlow<Int>(0)
    val currentSongIndex = _currentSongIndex.asSharedFlow()

    init {
        player.addListener(this)
        job = Job()
        sleepTimerJob = Job()
        volumeNormalizationJob = Job()
        updateNotificationJob = Job()
        toggleLikeJob = Job()
        loadJob = Job()
        skipSilent = runBlocking { dataStoreManager.skipSilent.first() == DataStoreManager.TRUE }
        normalizeVolume = runBlocking { dataStoreManager.normalizeVolume.first() == DataStoreManager.TRUE }
        if (runBlocking{ dataStoreManager.saveStateOfPlayback.first() } == DataStoreManager.TRUE ) {
            Log.d("CHECK INIT", "TRUE")
            val shuffleKey = runBlocking { dataStoreManager.shuffleKey.first() }
            val repeatKey = runBlocking { dataStoreManager.repeatKey.first() }
            Log.d("CHECK INIT", "Shuffle: $shuffleKey")
            Log.d("CHECK INIT", "Repeat: $repeatKey")
            player.shuffleModeEnabled = shuffleKey == DataStoreManager.TRUE
            player.repeatMode = when (repeatKey) {
                DataStoreManager.REPEAT_ONE -> Player.REPEAT_MODE_ONE
                DataStoreManager.REPEAT_ALL -> Player.REPEAT_MODE_ALL
                DataStoreManager.REPEAT_MODE_OFF -> Player.REPEAT_MODE_OFF
                else -> {Player.REPEAT_MODE_OFF}
            }
        }
        _shuffle.value = player.shuffleModeEnabled
        _repeat.value = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatState.One
            Player.REPEAT_MODE_ALL -> RepeatState.All
            Player.REPEAT_MODE_OFF -> RepeatState.None
            else -> {RepeatState.None}
        }
        _nowPlaying.value = player.currentMediaItem
        mediaSessionCallback.apply {
            toggleLike = ::toggleLike
        }
    }
    private fun toggleLike() {
        toggleLikeJob?.cancel()
        toggleLikeJob = coroutineScope.launch {
            mainRepository.updateLikeStatus(player.currentMediaItem?.mediaId ?: "", if (!(_liked.value)) 1 else 0)
        }
        _liked.value = !(_liked.value)
        updateNotification()
    }

    fun like(liked: Boolean) {
        _liked.value = liked
        updateNotification()
    }
    //Set sleep timer
    @OptIn(DelicateCoroutinesApi::class)
    fun sleepStart(minutes: Int) {
        _sleepDone.value = false
        sleepTimerJob?.cancel()
        sleepTimerJob = coroutineScope.launch(Dispatchers.Main) {
            _sleepMinutes.value = minutes
            var count = minutes
            while (count > 0) {
                delay(60 * 1000L)
                count--
                _sleepMinutes.value = count
            }
            player.pause()
            _sleepMinutes.value = 0
            _sleepDone.value = true
        }
    }
    fun sleepStop() {
        _sleepDone.value = false
        sleepTimerJob?.cancel()
        _sleepMinutes.value = 0
    }

    private fun updateNextPreviousTrackAvailability() {
        _nextTrackAvailable.value = player.hasNextMediaItem()
        _previousTrackAvailable.value = player.hasPreviousMediaItem()
    }

    fun getMediaItemWithIndex(index: Int): MediaItem {
        return player.getMediaItemAt(index)
    }

    fun removeMediaItem(position: Int) {
        player.removeMediaItem(position)
        catalogMetadata.removeAt(position)
        _currentSongIndex.value = currentIndex()
    }

    fun addMediaItem(mediaItem: MediaItem, playWhenReady: Boolean = true) {
        player.clearMediaItems()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    fun addMediaItemNotSet(mediaItem: MediaItem) {
        player.addMediaItem(mediaItem)
        if (player.mediaItemCount == 1){
            player.prepare()
            player.playWhenReady = true
        }
        updateNextPreviousTrackAvailability()
    }

    fun clearMediaItems() {
        player.clearMediaItems()
    }

    fun addMediaItemList(mediaItemList: List<MediaItem>) {
        for (mediaItem in mediaItemList) {
            addMediaItemNotSet(mediaItem)
        }
        Log.d("Media Item List", "addMediaItemList: ${player.mediaItemCount}")
    }

    fun playMediaItemInMediaSource(index: Int){
        player.seekTo(index, 0)
        player.prepare()
        player.playWhenReady = true
    }

    fun moveMediaItem(fromIndex: Int, newIndex: Int) {
        player.moveMediaItem(fromIndex, newIndex)
        _currentSongIndex.value = currentIndex()
    }

    suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            PlayerEvent.Backward -> player.seekBack()
            PlayerEvent.Forward -> player.seekForward()
            PlayerEvent.PlayPause -> {
                if (player.isPlaying) {
                    player.pause()
                    stopProgressUpdate()
                } else {
                    player.play()
                    _simpleMediaState.value = SimpleMediaState.Playing(isPlaying = true)
                    startProgressUpdate()
                }
            }
            PlayerEvent.Next -> player.seekToNext()
            PlayerEvent.Previous -> player.seekToPrevious()
            PlayerEvent.Stop -> {stopProgressUpdate()
            player.stop()}
            is PlayerEvent.UpdateProgress -> player.seekTo((player.duration * playerEvent.newProgress/100).toLong())
            PlayerEvent.Shuffle -> {
                if (player.shuffleModeEnabled) {
                    player.shuffleModeEnabled = false
                    _shuffle.value = false
                } else {
                    player.shuffleModeEnabled = true
                    _shuffle.value = true
                }
            }
            PlayerEvent.Repeat -> {
                when (player.repeatMode) {
                    ExoPlayer.REPEAT_MODE_OFF -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                        _repeat.value = RepeatState.One
                    }
                    ExoPlayer.REPEAT_MODE_ONE -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                        _repeat.value = RepeatState.All
                    }
                    ExoPlayer.REPEAT_MODE_ALL -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                        _repeat.value = RepeatState.None
                    }
                    else -> {
                        when(_repeat.value) {
                            RepeatState.None -> {
                                player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                                _repeat.value = RepeatState.One
                            }
                            RepeatState.One -> {
                                player.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                                _repeat.value = RepeatState.All
                            }
                            RepeatState.All -> {
                                player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                                _repeat.value = RepeatState.None
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        val shouldBePlaying = !(player.playbackState == Player.STATE_ENDED || !player.playWhenReady)
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY
            )
        ) {
            if (shouldBePlaying) {
                sendOpenEqualizerIntent()
            } else {
                sendCloseEqualizerIntent()
            }
        }
    }

    override fun onTracksChanged(tracks: Tracks) {
        Log.d("Tracks", "onTracksChanged: ${tracks.groups.size}")
        super.onTracksChanged(tracks)
    }

    override fun onPlayerError(error: PlaybackException) {
        when(error.errorCode) {
            PlaybackException.ERROR_CODE_TIMEOUT -> {
                Log.e("Player Error", "onPlayerError: ${error.message}")
                Toast.makeText(context,
                    context.getString(R.string.time_out_check_internet_connection_or_change_piped_instance_in_settings), Toast.LENGTH_LONG).show()
                player.pause()
            }
            else -> {
                Log.e("Player Error", "onPlayerError: ${error.message}")
                Toast.makeText(context,
                    context.getString(R.string.time_out_check_internet_connection_or_change_piped_instance_in_settings), Toast.LENGTH_LONG).show()
                player.pause()
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Log.w("Smooth Switching Transition", "Current Position: ${player.currentPosition}")
        mayBeNormalizeVolume()
        Log.w("REASON", "onMediaItemTransition: $reason")
        Log.d("Media Item Transition", "Media Item: ${mediaItem?.mediaMetadata?.title}")
        _nowPlaying.value = mediaItem
        updateNextPreviousTrackAvailability()
        updateNotification()
    }


    @SuppressLint("SwitchIntDef")
    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_IDLE -> _simpleMediaState.value = SimpleMediaState.Initial
            ExoPlayer.STATE_ENDED -> _simpleMediaState.value = SimpleMediaState.Ended
            ExoPlayer.STATE_BUFFERING -> _simpleMediaState.value =
                SimpleMediaState.Buffering(player.currentPosition)
            ExoPlayer.STATE_READY -> _simpleMediaState.value =
                SimpleMediaState.Ready(player.duration)
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        when (shuffleModeEnabled){
            true -> {
                _shuffle.value = true
            }
            false ->{
                _shuffle.value = false
            }
        }
        updateNextPreviousTrackAvailability()
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        when (repeatMode){
            ExoPlayer.REPEAT_MODE_OFF -> _repeat.value = RepeatState.None
            ExoPlayer.REPEAT_MODE_ONE -> _repeat.value = RepeatState.One
            ExoPlayer.REPEAT_MODE_ALL -> _repeat.value = RepeatState.All
        }
        updateNextPreviousTrackAvailability()
        updateNotification()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _simpleMediaState.value = SimpleMediaState.Playing(isPlaying = isPlaying)
        if (isPlaying) {
            coroutineScope.launch(Dispatchers.Main) {
                startProgressUpdate()
            }
        } else {
            stopProgressUpdate()
        }
        updateNextPreviousTrackAvailability()
    }

    private suspend fun startProgressUpdate() = job.run {
        while (true) {
            delay(100)
            _simpleMediaState.value = SimpleMediaState.Progress(player.currentPosition)
        }
    }

    private suspend fun startBufferedUpdate() = job.run {
        while (true){
            delay(500)
            _simpleMediaState.value = SimpleMediaState.Loading(player.bufferedPercentage, player.duration)
        }
    }
    fun currentIndex(): Int {
        return player.currentMediaItemIndex
    }

    fun mediaListSize(): Int {
        return player.mediaItemCount
    }

    fun getCurrentMediaItem(): MediaItem? {
        return player.currentMediaItem
    }

    private fun stopProgressUpdate() {
        job?.cancel()
        _simpleMediaState.value = SimpleMediaState.Playing(isPlaying = false)
    }
    private fun stopBufferedUpdate() {
        job?.cancel()
        _simpleMediaState.value = SimpleMediaState.Loading(player.bufferedPercentage, player.duration)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onIsLoadingChanged(isLoading: Boolean) {
        super.onIsLoadingChanged(isLoading)
        _simpleMediaState.value = SimpleMediaState.Loading(player.bufferedPercentage, player.duration)
        if (isLoading) {
            coroutineScope.launch(Dispatchers.Main) {
                startBufferedUpdate()
            }
        } else {
            stopBufferedUpdate()
        }
    }

    private fun mayBeNormalizeVolume() {
        runBlocking { normalizeVolume = dataStoreManager.normalizeVolume.first() == DataStoreManager.TRUE }
        if (!normalizeVolume) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            player.volume = 1f
            return
        }

        if (loudnessEnhancer == null) {
            loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        }

        player.currentMediaItem?.mediaId?.let { songId ->
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = coroutineScope.launch(Dispatchers.Main) {
                mainRepository.getFormat(songId).cancellable().first().let { format ->
                    if (format != null) {
                        try {
                            loudnessEnhancer?.setTargetGain(-((format.loudnessDb ?: 0f) * 100).toInt() + 500)
                            Log.w("Loudness", "mayBeNormalizeVolume: ${loudnessEnhancer?.targetGain}")
                            loudnessEnhancer?.enabled = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
    private fun maybeSkipSilent() {
        skipSilent = runBlocking { dataStoreManager.skipSilent.first() } == DataStoreManager.TRUE
        player.skipSilenceEnabled = skipSilent
    }
    fun mayBeSaveRecentSong() {
        runBlocking {
            if (dataStoreManager.saveRecentSongAndQueue.first() == DataStoreManager.TRUE) {
                dataStoreManager.saveRecentSong(player.currentMediaItem?.mediaId ?: "", player.contentPosition)
                Log.d("Check saved", player.currentMediaItem?.mediaMetadata?.title.toString())
                val temp: ArrayList<Track> = ArrayList()
                temp.clear()
                Queue.getNowPlaying()?.let { nowPlaying ->
                    if (nowPlaying.videoId != player.currentMediaItem?.mediaId) {
                        temp += nowPlaying
                    }
                }
                temp += Queue.getQueue()
                Log.d("Check queue", Queue.getQueue().toString())
                temp.find { it.videoId == player.currentMediaItem?.mediaId }?.let { track ->
                    temp.remove(track)
                }
                Log.w("Check recover queue", temp.toString())
                mainRepository.recoverQueue(temp)
                dataStoreManager.putString(DataStoreManager.RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE, DataStoreManager.FALSE)
            }
        }
    }
    fun mayBeSavePlaybackState() {
        if (runBlocking{ dataStoreManager.saveStateOfPlayback.first() } == DataStoreManager.TRUE ) {
            runBlocking { dataStoreManager.recoverShuffleAndRepeatKey(player.shuffleModeEnabled, player.repeatMode) }
        }
    }
    fun editSkipSilent(skip: Boolean) {
        skipSilent = skip
        maybeSkipSilent()
    }
    fun editNormalizeVolume(normalize: Boolean) {
        normalizeVolume = normalize
    }

    fun seekTo(position: String)  {
        player.seekTo(position.toLong())
        Log.d("Check seek", "seekTo: ${player.currentPosition}")
    }
    fun skipSegment(position: Long) {
        if (position in 0..player.duration) {
            player.seekTo(position)
        }
        else if (position > player.duration) {
            player.seekToNext()
        }
    }
    private fun sendOpenEqualizerIntent() {
        context.sendBroadcast (
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }
    private fun sendCloseEqualizerIntent() {
        context.sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
            }
        )
    }
    fun release() {
        player.stop()
        player.playWhenReady = false
        player.removeListener(this)
        sendCloseEqualizerIntent()
        if (job?.isActive == true) {
            job?.cancel()
            job = null
        }
        if (sleepTimerJob?.isActive == true) {
            sleepTimerJob?.cancel()
            sleepTimerJob = null
        }
        if (volumeNormalizationJob?.isActive == true) {
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = null
        }
        if (toggleLikeJob?.isActive == true) {
            toggleLikeJob?.cancel()
            toggleLikeJob = null
        }
        if (updateNotificationJob?.isActive == true) {
            updateNotificationJob?.cancel()
            updateNotificationJob = null
        }
        if (loadJob?.isActive == true) {
            loadJob?.cancel()
            loadJob = null
        }
        Log.w("Service", "Check job: ${job?.isActive}")
        Log.w("Service", "scope is active: ${coroutineScope?.isActive}")
    }

    private fun updateNotification() {
        updateNotificationJob?.cancel()
        updateNotificationJob = coroutineScope.launch {
            val liked = mainRepository.getSongById(player.currentMediaItem?.mediaId ?: "").first()?.liked
            if (liked != null) {
                _liked.value = liked
            }
            mediaSession.setCustomLayout(
                listOf(
                    CommandButton.Builder()
                        .setDisplayName(if (liked == true) context.getString(R.string.liked) else context.getString(R.string.like))
                        .setIconResId(if (liked == true) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24)
                        .setSessionCommand(SessionCommand(MEDIA_CUSTOM_COMMAND.LIKE, Bundle()))
                        .build(),
                    CommandButton.Builder()
                        .setDisplayName(
                            when (player.repeatMode) {
                                Player.REPEAT_MODE_ONE -> context.getString(androidx.media3.ui.R.string.exo_controls_repeat_one_description)
                                Player.REPEAT_MODE_ALL -> context.getString(androidx.media3.ui.R.string.exo_controls_repeat_all_description)
                                else -> context.getString(androidx.media3.ui.R.string.exo_controls_repeat_off_description)
                            }
                        )
                        .setSessionCommand(SessionCommand(MEDIA_CUSTOM_COMMAND.REPEAT, Bundle()))
                        .setIconResId(
                            when (player.repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.baseline_repeat_one_24
                                Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                else -> R.drawable.baseline_repeat_24_enable
                            }
                        )
                        .build()
                )
            )
        }
    }

    fun getPlayerDuration(): Long {
        return player.duration
    }

    fun getProgress(): Long {
        return player.currentPosition
    }

    fun changeAddedState() {
        added.value = false
    }

    fun addFirstMetadata(it: Track) {
        added.value = true
        catalogMetadata.add(0, it)
        Log.d("MusicSource", "addFirstMetadata: ${it.title}, ${catalogMetadata.size}")
    }

    @UnstableApi
    fun moveItemUp(position: Int) {
        moveMediaItem(position, position - 1)
        val temp = catalogMetadata[position]
        catalogMetadata[position] = catalogMetadata[position - 1]
        catalogMetadata[position - 1] = temp
        _currentSongIndex.value = currentIndex()
    }

    @UnstableApi
    fun moveItemDown(position: Int) {
        moveMediaItem(position, position + 1)
        val temp = catalogMetadata[position]
        catalogMetadata[position] = catalogMetadata[position + 1]
        catalogMetadata[position + 1] = temp
        _currentSongIndex.value = currentIndex()
    }

    @UnstableApi
    fun addFirstMediaItemToIndex(mediaItem: MediaItem?, index: Int) {
        if (mediaItem != null){
            Log.d("MusicSource", "addFirstMediaItem: ${mediaItem.mediaId}")
            moveMediaItem(0, index)
        }
    }
    fun reset() {
        _currentSongIndex.value = 0
        catalogMetadata.clear()
        _stateFlow.value = StateSource.STATE_CREATED
    }

    @UnstableApi
    suspend fun load(downloaded: Int = 0, index: Int? = null) {
        updateCatalog(downloaded).let {
            _stateFlow.value = StateSource.STATE_INITIALIZED
            if (index != null) {
                when (index) {
                    -1 -> {

                    }
                    else -> {
                        Log.w("Check index", "load: $index")
                        addFirstMediaItemToIndex(getMediaItemWithIndex(0), index)
                        Queue.getNowPlaying().let { song ->
                            if (song != null) {
                                catalogMetadata.removeAt(0)
                                catalogMetadata.add(index, song)
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun loadMoreCatalog(listTrack: ArrayList<Track>) {
        _stateFlow.value = StateSource.STATE_INITIALIZING
        for (i in 0 until listTrack.size) {
            val track = listTrack[i]
            var thumbUrl = track.thumbnails?.last()?.url
                ?: "http://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
            if (thumbUrl.contains("w120")) {
                thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
            }
            val artistName: String = track.artists.toListName().connectArtists()
            if (!catalogMetadata.contains(track)) {
                if (track.artists.isNullOrEmpty()) {
                    mainRepository.getFormat(track.videoId).cancellable().first().let { format ->
                        if (format != null) {
                            catalogMetadata.add(
                                track.copy(
                                    artists = listOf(
                                        Artist(
                                            format.uploaderId,
                                            format.uploader ?: ""
                                        )
                                    )
                                )
                            )
                            addMediaItemNotSet(
                                MediaItem.Builder().setUri(track.videoId)
                                    .setMediaId(track.videoId)
                                    .setCustomCacheKey(track.videoId)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(track.title)
                                            .setArtist(format.uploader)
                                            .setArtworkUri(thumbUrl.toUri())
                                            .setAlbumTitle(track.album?.name)
                                            .build()
                                    )
                                    .build()
                            )
                        } else {
                            val mediaItem = MediaItem.Builder()
                                .setMediaId(track.videoId)
                                .setUri(track.videoId)
                                .setCustomCacheKey(track.videoId)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setArtworkUri(thumbUrl.toUri())
                                        .setAlbumTitle(track.album?.name)
                                        .setTitle(track.title)
                                        .setArtist("Various Artists")
                                        .build()
                                )
                                .build()
                            addMediaItemNotSet(mediaItem)
                            catalogMetadata.add(
                                track.copy(
                                    artists = listOf(Artist("", "Various Artists"))
                                )
                            )
                        }
                    }
                } else {
                    addMediaItemNotSet(
                        MediaItem.Builder().setUri(track.videoId)
                            .setMediaId(track.videoId)
                            .setCustomCacheKey(track.videoId)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(track.title)
                                    .setArtist(artistName)
                                    .setArtworkUri(thumbUrl.toUri())
                                    .setAlbumTitle(track.album?.name)
                                    .build()
                            )
                            .build()
                    )
                    catalogMetadata.add(track)
                }
                Log.d(
                    "MusicSource",
                    "updateCatalog: ${track.title}, ${catalogMetadata.size}"
                )
                added.value = true
                Log.d("MusicSource", "updateCatalog: ${track.title}")
            }
        }
        _stateFlow.value = StateSource.STATE_INITIALIZED
    }

    @UnstableApi
    suspend fun updateCatalog(downloaded: Int = 0): Boolean {
        _stateFlow.value = StateSource.STATE_INITIALIZING
        val tempQueue: ArrayList<Track> = arrayListOf()
        tempQueue.addAll(Queue.getQueue())
        for (i in 0 until tempQueue.size) {
            val track = tempQueue[i]
            var thumbUrl = track.thumbnails?.last()?.url
                ?: "http://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
            if (thumbUrl.contains("w120")) {
                thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
            }
            if (downloaded == 1) {
                if (track.artists.isNullOrEmpty())
                {
                    mainRepository.getFormat(track.videoId).cancellable().first().let { format ->
                        if (format != null) {
                            val mediaItem = MediaItem.Builder()
                                .setMediaId(track.videoId)
                                .setUri(track.videoId)
                                .setCustomCacheKey(track.videoId)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setArtworkUri(thumbUrl.toUri())
                                        .setAlbumTitle(track.album?.name)
                                        .setTitle(track.title)
                                        .setArtist(format.uploader)
                                        .build()
                                )
                                .build()
                            addMediaItemNotSet(mediaItem)
                            catalogMetadata.add(track.copy(
                                artists = listOf(Artist(format.uploaderId, format.uploader?: "" ))
                            ))
                        }
                        else {
                            val mediaItem = MediaItem.Builder()
                                .setMediaId(track.videoId)
                                .setUri(track.videoId)
                                .setCustomCacheKey(track.videoId)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setArtworkUri(thumbUrl.toUri())
                                        .setAlbumTitle(track.album?.name)
                                        .setTitle(track.title)
                                        .setArtist("Various Artists")
                                        .build()
                                )
                                .build()
                            addMediaItemNotSet(mediaItem)
                            catalogMetadata.add(track.copy(
                                artists = listOf(Artist("", "Various Artists" ))
                            ))
                        }
                    }
                }
                else {
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(track.videoId)
                        .setUri(track.videoId)
                        .setCustomCacheKey(track.videoId)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setArtworkUri(thumbUrl.toUri())
                                .setAlbumTitle(track.album?.name)
                                .setTitle(track.title)
                                .setArtist(track.artists.toListName().connectArtists())
                                .build()
                        )
                        .build()
                    addMediaItemNotSet(mediaItem)
                    catalogMetadata.add(track)
                }
                Log.d("MusicSource", "updateCatalog: ${track.title}, ${catalogMetadata.size}")
                added.value = true
            }
            else {
                val artistName: String = track.artists.toListName().connectArtists()
                if (!catalogMetadata.contains(track)) {
                    if (track.artists.isNullOrEmpty())
                    {
                        mainRepository.getFormat(track.videoId).cancellable().first().let { format ->
                            if (format != null) {
                                catalogMetadata.add(
                                    track.copy(
                                        artists = listOf(
                                            Artist(
                                                format.uploaderId,
                                                format.uploader ?: ""
                                            )
                                        )
                                    )
                                )
                                addMediaItemNotSet(
                                    MediaItem.Builder().setUri(track.videoId)
                                        .setMediaId(track.videoId)
                                        .setCustomCacheKey(track.videoId)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(track.title)
                                                .setArtist(format.uploader)
                                                .setArtworkUri(thumbUrl.toUri())
                                                .setAlbumTitle(track.album?.name)
                                                .build()
                                        )
                                        .build()
                                )
                            }
                            else {
                                val mediaItem = MediaItem.Builder()
                                    .setMediaId(track.videoId)
                                    .setUri(track.videoId)
                                    .setCustomCacheKey(track.videoId)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setArtworkUri(thumbUrl.toUri())
                                            .setAlbumTitle(track.album?.name)
                                            .setTitle(track.title)
                                            .setArtist("Various Artists")
                                            .build()
                                    )
                                    .build()
                                addMediaItemNotSet(mediaItem)
                                catalogMetadata.add(track.copy(
                                    artists = listOf(Artist("", "Various Artists" ))
                                ))
                            }
                        }
                    }
                    else {
                        addMediaItemNotSet(
                            MediaItem.Builder().setUri(track.videoId)
                                .setMediaId(track.videoId)
                                .setCustomCacheKey(track.videoId)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(track.title)
                                        .setArtist(artistName)
                                        .setArtworkUri(thumbUrl.toUri())
                                        .setAlbumTitle(track.album?.name)
                                        .build()
                                )
                                .build()
                        )
                        catalogMetadata.add(track)
                    }
                    Log.d(
                        "MusicSource",
                        "updateCatalog: ${track.title}, ${catalogMetadata.size}"
                    )
                    added.value = true
                    Log.d("MusicSource", "updateCatalog: ${track.title}")
                }
            }
        }
        return true
    }
    fun addQueueToPlayer() {
        Log.d("Check Queue in handler", Queue.getQueue().toString())
        loadJob?.cancel()
        loadJob = coroutineScope.launch {
            load()
        }
    }

    fun loadPlaylistOrAlbum(index: Int? = null) {
        loadJob?.cancel()
        loadJob = coroutineScope.launch {
            load(index = index)
        }
    }

    fun setCurrentSongIndex(index: Int) {
        _currentSongIndex.value = index
    }

    fun updateSubtitle(url: String) {
        val index = currentIndex()
        val mediaItem = player.currentMediaItem
        Log.w("Subtitle", "updateSubtitle: $url")
        val subtitle = SubtitleConfiguration.Builder(url.plus("&fmt=ttml").toUri())
            .setId(mediaItem?.mediaId)
            .setMimeType(MimeTypes.APPLICATION_TTML)
            .setLanguage("en")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val new = mediaItem?.buildUpon()?.setSubtitleConfigurations(listOf(subtitle))?.build()
        if (new != null) {
            player.replaceMediaItem(
                index,
                new
            )
            println("update subtitle" + player.currentMediaItem?.localConfiguration?.subtitleConfigurations?.firstOrNull()?.uri.toString())
        }
    }

}

sealed class RepeatState {
    data object None : RepeatState()
    data object All : RepeatState()
    data object One : RepeatState()
}

sealed class PlayerEvent {
    data object PlayPause : PlayerEvent()
    data object Backward : PlayerEvent()
    data object Forward : PlayerEvent()
    data object Stop : PlayerEvent()
    data object Next : PlayerEvent()
    data object Previous : PlayerEvent()
    data object Shuffle : PlayerEvent()
    data object Repeat : PlayerEvent()
    data class UpdateProgress(val newProgress: Float) : PlayerEvent()
}

sealed class SimpleMediaState {
    data object Initial : SimpleMediaState()
    data object Ended : SimpleMediaState()
    data class Ready(val duration: Long) : SimpleMediaState()
    data class Loading(val bufferedPercentage: Int, val duration: Long): SimpleMediaState()
    data class Progress(val progress: Long) : SimpleMediaState()
    data class Buffering(val position: Long) : SimpleMediaState()
    data class Playing(val isPlaying: Boolean) : SimpleMediaState()
}
enum class StateSource {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}
