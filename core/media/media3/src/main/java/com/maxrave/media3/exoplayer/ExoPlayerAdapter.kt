package com.maxrave.media3.exoplayer

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.GenericMediaMetadata
import com.maxrave.domain.data.player.GenericPlaybackParameters
import com.maxrave.domain.data.player.GenericTracks
import com.maxrave.domain.data.player.PlayerConstants
import com.maxrave.domain.data.player.PlayerError
import com.maxrave.domain.mediaservice.player.MediaPlayerInterface
import com.maxrave.domain.mediaservice.player.MediaPlayerListener
import com.maxrave.logger.Logger
import com.maxrave.media3.utils.BetterShuffleOrder

/**
 * ExoPlayer implementation of MediaPlayerInterface
 * Handles all Media3-specific logic and conversions
 */
private const val TAG = "ExoPlayerAdapter"

@SuppressLint("UnsafeOptInUsageError")
class ExoPlayerAdapter(
    private var exoPlayer: ExoPlayer,
    private val context: Context
) : MediaPlayerInterface {
    private val listeners = mutableListOf<MediaPlayerListener>()
    private val exoPlayerListener = ExoPlayerListenerImpl()
    override var onPlayerRefChanged: ((Any) -> Unit)? = null // Implémentation du callback (Any pour compatibilité multiplateforme)

    // Shuffle management
    // Maps original playlist index -> shuffled position
    private var shuffleIndices = mutableListOf<Int>()

    // Maps shuffled position -> original playlist index
    private var shuffleOrder = mutableListOf<Int>()

    init {
        exoPlayer.addListener(exoPlayerListener)
    }

    // Playback control
    override fun play() = exoPlayer.play()

    override fun pause() = exoPlayer.pause()

    override fun stop() = exoPlayer.stop()

    override fun seekTo(positionMs: Long) = exoPlayer.seekTo(positionMs)

    override fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
    ) = exoPlayer.seekTo(mediaItemIndex, positionMs)

    override fun seekBack() = exoPlayer.seekBack()

    override fun seekForward() = exoPlayer.seekForward()

    override fun seekToNext() = exoPlayer.seekToNext()

    override fun seekToPrevious() = exoPlayer.seekToPrevious()

    override fun prepare() = exoPlayer.prepare()

    // Media item management
    override fun setMediaItem(mediaItem: GenericMediaItem) {
        exoPlayer.setMediaItem(mediaItem.toMedia3MediaItem())
        if (shuffleModeEnabled) {
            createShuffleOrder()
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun addMediaItem(mediaItem: GenericMediaItem) {
        exoPlayer.addMediaItem(mediaItem.toMedia3MediaItem())
        if (shuffleModeEnabled) {
            createShuffleOrder()
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
        
        // Précharger la piste suivante en arrière-plan
        Logger.d(TAG, "Préchargement activé pour la playlist de ${mediaItemCount} pistes")
        
        // Le crossfade automatique est maintenant géré par checkCrossfadeTiming()
        // Plus besoin de planifier manuellement ici
    }

    override fun addMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        exoPlayer.addMediaItem(index, mediaItem.toMedia3MediaItem())
        val currentIndexBeforeInsert = currentMediaItemIndex
        // Update shuffle order if enabled
        if (shuffleModeEnabled) {
            // Check if this is "play next" (inserting right after current playing song)
            if (currentIndexBeforeInsert >= 0 && index == currentIndexBeforeInsert + 1) {
                // This is "play next" - insert into shuffle order right after current song
                val currentShufflePos = shuffleIndices.getOrNull(currentIndexBeforeInsert) ?: 0
                insertIntoShuffleOrder(index, currentShufflePos)
            } else {
                // Not "play next" - recreate entire shuffle order
                createShuffleOrder()
            }
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun removeMediaItem(index: Int) {
        exoPlayer.removeMediaItem(index)
        if (shuffleModeEnabled) {
            removeFromShuffleOrder(index)
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun moveMediaItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (shuffleModeEnabled) {
            moveShuffleOrder(fromIndex, toIndex)
        } else {
            exoPlayer.moveMediaItem(fromIndex, toIndex)
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun clearMediaItems() {
        exoPlayer.clearMediaItems()
        clearShuffleOrder()
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun replaceMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        exoPlayer.replaceMediaItem(index, mediaItem.toMedia3MediaItem())
        if (shuffleModeEnabled) {
            createShuffleOrder()
        }
        notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
    }

    override fun getMediaItemAt(index: Int): GenericMediaItem? =
        if (index in 0..<exoPlayer.mediaItemCount) {
            exoPlayer.getMediaItemAt(index).toGenericMediaItem()
        } else {
            null
        }

    override fun getCurrentMediaTimeLine(): List<GenericMediaItem> =
        if (shuffleModeEnabled) {
            shuffleOrder.map { shuffledIndex -> exoPlayer.getMediaItemAt(shuffledIndex).toGenericMediaItem() }
        } else {
            List(exoPlayer.mediaItemCount) { i -> exoPlayer.getMediaItemAt(i).toGenericMediaItem() }
        }

    override fun getUnshuffledIndex(shuffledIndex: Int): Int =
        if (shuffleModeEnabled) {
            shuffleOrder.getOrNull(shuffledIndex) ?: -1
        } else {
            shuffledIndex
        }

    // Playback state properties
    override val isPlaying: Boolean get() = exoPlayer.isPlaying
    override val currentPosition: Long get() = exoPlayer.currentPosition
    override val duration: Long get() = exoPlayer.duration
    override val bufferedPosition: Long get() = exoPlayer.bufferedPosition
    override val bufferedPercentage: Int get() = exoPlayer.bufferedPercentage
    override val currentMediaItem: GenericMediaItem? get() = exoPlayer.currentMediaItem?.toGenericMediaItem()
    override val currentMediaItemIndex: Int get() = exoPlayer.currentMediaItemIndex
    override val mediaItemCount: Int get() = exoPlayer.mediaItemCount
    override val contentPosition: Long get() = exoPlayer.contentPosition
    override val playbackState: Int get() = exoPlayer.playbackState

    // Navigation
    override fun hasNextMediaItem(): Boolean = exoPlayer.hasNextMediaItem()

    override fun hasPreviousMediaItem(): Boolean = exoPlayer.hasPreviousMediaItem()

    // Playback modes
    override var shuffleModeEnabled: Boolean
        get() = exoPlayer.shuffleModeEnabled
        set(value) {
            exoPlayer.shuffleModeEnabled = value
        }

    override var repeatMode: Int
        get() = exoPlayer.repeatMode
        set(value) {
            exoPlayer.repeatMode = value
        }

    override var playWhenReady: Boolean
        get() = exoPlayer.playWhenReady
        set(value) {
            exoPlayer.playWhenReady = value
        }

    override var playbackParameters: GenericPlaybackParameters
        get() = exoPlayer.playbackParameters.toGenericPlaybackParameters()
        set(value) {
            exoPlayer.playbackParameters = value.toMedia3PlaybackParameters()
        }

    // Audio settings
    override val audioSessionId: Int get() = exoPlayer.audioSessionId
    override var volume: Float
        get() = exoPlayer.volume
        set(value) {
            exoPlayer.volume = value
        }

    override var skipSilenceEnabled: Boolean
        get() = exoPlayer.skipSilenceEnabled
        set(value) {
            exoPlayer.skipSilenceEnabled = value
        }

    // Listener management
    override fun addListener(listener: MediaPlayerListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: MediaPlayerListener) {
        listeners.remove(listener)
    }

    // Release resources
    override fun release() {
        exoPlayer.removeListener(exoPlayerListener)
        listeners.clear()
        exoPlayer.release()
    }

    private fun getShuffledMediaItemList(): List<GenericMediaItem> {
        val list = mutableListOf<GenericMediaItem>()
        val s = exoPlayer.shuffleModeEnabled
        val timeline = exoPlayer.currentTimeline
        var i = timeline.getFirstWindowIndex(s)
        while (i != C.INDEX_UNSET) {
            getMediaItemAt(i)?.let { list.add(it) }
            i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, s)
        }
        return list
    }

    /**
     * Notify timeline changed with current order (shuffled or not)
     */
    private fun notifyTimelineChanged(reason: String) {
        val list = getShuffledMediaItemList()
        listeners.forEach { it.onTimelineChanged(list, reason) }
    }

    // Internal ExoPlayer listener that converts events to generic events
    private inner class ExoPlayerListenerImpl : Player.Listener {
//        override fun onTimelineChanged(
//            timeline: Timeline,
//            reason: Int,
//        ) {
//            super.onTimelineChanged(timeline, reason)
//            val list = mutableListOf<GenericMediaItem>()
//            val s = exoPlayer.shuffleModeEnabled
//            var i = timeline.getFirstWindowIndex(s)
//            while (i != C.INDEX_UNSET) {
//                getMediaItemAt(i)?.let { list.add(it) }
//                i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, s)
//            }
//            listeners.forEach {
//                it.onTimelineChanged(
//                    list,
//                    when (reason) {
//                        Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED -> "TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED"
//                        Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE -> "TIMELINE_CHANGE_REASON_SOURCE_UPDATE"
//                        else -> "Unknown"
//                    },
//                )
//            }
//        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val domainState =
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        PlayerConstants.STATE_IDLE
                    }

                    Player.STATE_ENDED -> {
                        PlayerConstants.STATE_ENDED
                    }

                    Player.STATE_READY -> {
                        PlayerConstants.STATE_READY
                    }

                    else -> {
                        playbackState
                    }
                }
            listeners.forEach { it.onPlaybackStateChanged(domainState) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            listeners.forEach { it.onIsPlayingChanged(isPlaying) }
        }

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int,
        ) {
            val genericMediaItem = mediaItem?.toGenericMediaItem()
            val domainReason =
                when (reason) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_AUTO
                    Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_SEEK
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
                    else -> reason
                }
            
            // Réinitialiser les variables de crossfade pour la nouvelle chanson
            crossfadeScheduled = false
            preloadScheduled = false
            nextMediaItemForCrossfade = null
            
            Logger.d(TAG, "🎧 DJ CROSSFADE - Réinitialisation crossfade pour nouvelle chanson: ${genericMediaItem?.metadata?.title}")
            
            listeners.forEach { it.onMediaItemTransition(genericMediaItem, domainReason) }
        }

        override fun onTracksChanged(tracks: Tracks) {
            val genericTracks = tracks.toGenericTracks()
            listeners.forEach { it.onTracksChanged(genericTracks) }
        }

        override fun onPlayerError(error: PlaybackException) {
            val domainErrorCode =
                when (error.errorCode) {
                    PlaybackException.ERROR_CODE_TIMEOUT -> PlayerConstants.ERROR_CODE_TIMEOUT
                    else -> error.errorCode
                }
            val genericError =
                PlayerError(
                    errorCode = domainErrorCode,
                    errorCodeName = error.errorCodeName,
                    message = error.message,
                )
            listeners.forEach { it.onPlayerError(genericError) }
        }

        override fun onEvents(
            player: Player,
            events: Player.Events,
        ) {
            val shouldBePlaying = !(player.playbackState == Player.STATE_ENDED || !player.playWhenReady)
            if (events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                )
            ) {
                if (shouldBePlaying) {
                    listeners.forEach {
                        it.shouldOpenOrCloseEqualizerIntent(true)
                    }
                } else {
                    listeners.forEach {
                        it.shouldOpenOrCloseEqualizerIntent(false)
                    }
                }
            }
            
            // Vérifier le timing pour le crossfade automatique
            checkCrossfadeTiming()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (shuffleModeEnabled) {
                createShuffleOrder()
            } else {
                clearShuffleOrder()
            }
            val list = getShuffledMediaItemList()
            listeners.forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled, list) }
            notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val domainRepeatMode =
                when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> PlayerConstants.REPEAT_MODE_OFF
                    Player.REPEAT_MODE_ONE -> PlayerConstants.REPEAT_MODE_ONE
                    Player.REPEAT_MODE_ALL -> PlayerConstants.REPEAT_MODE_ALL
                    else -> repeatMode
                }
            listeners.forEach { it.onRepeatModeChanged(domainRepeatMode) }
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            listeners.forEach { it.onIsLoadingChanged(isLoading) }
        }
    }

    /**
     * Create shuffle order for current playlist
     * Keeps the current track at its position and shuffles the rest
     */
    private fun createShuffleOrder() {
        if (mediaItemCount == 0) {
            shuffleIndices.clear()
            shuffleOrder.clear()
            return
        }

        // Create list of all indices
        val indices = (0..<mediaItemCount).toMutableList()

        // If we have a current track, keep it at current position
        val currentIndex = currentMediaItemIndex
        if (currentIndex in indices) {
            indices.removeAt(currentIndex)
        }

        // Shuffle the remaining indices
        indices.shuffle()

        // If we have a current track, insert it at the beginning
        if (currentIndex in (0..<mediaItemCount)) {
            indices.add(0, currentIndex)
        }

        // Store the shuffle order
        shuffleOrder.clear()
        shuffleOrder.addAll(indices)

        // Create reverse mapping (original index -> shuffled position)
        shuffleIndices.clear()
        shuffleIndices.addAll(List(mediaItemCount) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, originalIndex ->
            shuffleIndices[originalIndex] = shuffledPos
        }

        exoPlayer.shuffleOrder = BetterShuffleOrder(shuffleOrder.toIntArray())

        Logger.d(TAG, "Created shuffle order: $shuffleOrder")
    }

    /**
     * Clear shuffle order
     */
    private fun clearShuffleOrder() {
        shuffleIndices.clear()
        shuffleOrder.clear()
        exoPlayer.shuffleOrder.cloneAndClear()
        Logger.d(TAG, "Cleared shuffle order")
    }

    /**
     * Insert item into shuffle order at specific position
     * Used for "play next" functionality when shuffle is enabled
     *
     * @param insertedOriginalIndex The index in the original playlist where item was inserted
     * @param afterShufflePos The shuffle position after which to insert (typically current song's position)
     */
    private fun insertIntoShuffleOrder(
        insertedOriginalIndex: Int,
        afterShufflePos: Int,
    ) {
        if (mediaItemCount == 0 || insertedOriginalIndex !in 0..<mediaItemCount) {
            return
        }

        // Step 1: Adjust all existing shuffle order indices that are >= insertedOriginalIndex
        // (because we inserted a new item, all indices after it shift up by 1)
        for (i in shuffleOrder.indices) {
            if (shuffleOrder[i] >= insertedOriginalIndex) {
                shuffleOrder[i]++
            }
        }

        // Step 2: Insert the new item right after the specified shuffle position
        val insertPos = (afterShufflePos + 1).coerceIn(0, shuffleOrder.size)
        shuffleOrder.add(insertPos, insertedOriginalIndex)

        // Step 3: Rebuild the reverse mapping
        shuffleIndices.clear()
        shuffleIndices.addAll(List(mediaItemCount) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, origIndex ->
            if (origIndex < shuffleIndices.size) {
                shuffleIndices[origIndex] = shuffledPos
            }
        }
        exoPlayer.shuffleOrder = BetterShuffleOrder(shuffleOrder.toIntArray())
        Logger.d(TAG, "Inserted index $insertedOriginalIndex into shuffle at position $insertPos (after shuffle pos $afterShufflePos)")
    }

    private fun moveShuffleOrder(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (fromIndex !in shuffleOrder.indices || toIndex !in shuffleOrder.indices) {
            return
        }

        val item = shuffleOrder.removeAt(fromIndex)
        shuffleOrder.add(toIndex, item)

        // Rebuild reverse mapping
        shuffleIndices.clear()
        shuffleIndices.addAll(List(mediaItemCount) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, origIndex ->
            if (origIndex < shuffleIndices.size) {
                shuffleIndices[origIndex] = shuffledPos
            }
        }
        exoPlayer.shuffleOrder = BetterShuffleOrder(shuffleOrder.toIntArray())
        Logger.d(TAG, "Moved shuffle order item from $fromIndex to $toIndex")
    }

    private fun removeFromShuffleOrder(originalIndex: Int) {
        val shufflePos = shuffleIndices.getOrNull(originalIndex) ?: return
        shuffleOrder.removeAt(shufflePos)

        // Rebuild reverse mapping
        shuffleIndices.clear()
        shuffleIndices.addAll(List(mediaItemCount) { 0 })
        shuffleOrder.forEachIndexed { shuffledPos, origIndex ->
            if (origIndex < shuffleIndices.size) {
                shuffleIndices[origIndex] = shuffledPos
            }
        }
        exoPlayer.shuffleOrder = BetterShuffleOrder(shuffleOrder.toIntArray())
        Logger.d(TAG, "Removed original index $originalIndex from shuffle order")
    }

    // Karaoke filter implementation
    override fun setKaraokeFilter(enabled: Boolean) {
        // TODO: Implement karaoke filter functionality
        // This would typically involve audio processing to remove vocals
        // For now, this is a placeholder implementation
        Logger.d(TAG, "Karaoke filter enabled: $enabled")
    }

        override fun setKaraokeFilterStrength(strength: Float) {
        // TODO: Implement karaoke filter strength adjustment
        // This would control how much of the vocals to remove (0.0 = none, 1.0 = maximum)
        // For now, this is a placeholder implementation
        Logger.d(TAG, "Karaoke filter strength: $strength")
    }
    
    /**
     * VRAI CROSSFADE DJ - Crée un deuxième player pour transition simultanée
     */
    private var crossfadePlayer: ExoPlayer? = null
    private var isCrossfading = false
    private var crossfadeListener: Player.Listener? = null
    
    // Crossfade timing settings (dynamiques selon la durée de la musique)
    private val PRELOAD_START_TIME_MS = 90000L // 1m30s = 90000ms avant la fin (préchargement)
    private var CROSSFADE_START_TIME_MS = 30000L // 30s avant la fin (début transition - sera ajusté dynamiquement)
    private var CROSSFADE_DURATION_MS = 30000L // 30 secondes pour le mixage DJ (sera ajusté dynamiquement)
    private var nextMediaItemForCrossfade: GenericMediaItem? = null
    private var crossfadeScheduled = false
    private var preloadScheduled = false
    
    /**
     * Mettre à jour les timings de crossfade selon la durée de la musique actuelle
     */
    private fun updateCrossfadeTimingForCurrentTrack() {
        try {
            val currentDuration = exoPlayer.duration
            val trackDurationSeconds = currentDuration / 1000
            
            val (startTime, duration) = when {
                trackDurationSeconds < 60 -> {
                    // Musique courte (< 1min) : mixer 30s avant
                    Pair(30000L, 30000L)
                }
                trackDurationSeconds < 120 -> {
                    // Musique moyenne (1-2min) : mixer 30s avant
                    Pair(30000L, 30000L)
                }
                trackDurationSeconds < 180 -> {
                    // Musique normale (2-3min) : mixer 30s avant
                    Pair(30000L, 30000L)
                }
                else -> {
                    // Musique longue (> 3min) : mixer 30s avant
                    Pair(30000L, 30000L)
                }
            }
            
            CROSSFADE_START_TIME_MS = startTime
            CROSSFADE_DURATION_MS = duration
            
            Logger.d(TAG, "🎧 DJ CROSSFADE - Timing mis à jour: Musique ${trackDurationSeconds}s -> Mixage ${duration/1000}s avant la fin")
            
        } catch (e: Exception) {
            Logger.e(TAG, "🎧 DJ CROSSFADE - Erreur mise à jour timing: ${e.message}")
            // Valeurs par défaut
            CROSSFADE_START_TIME_MS = 30000L
            CROSSFADE_DURATION_MS = 30000L
        }
    }
    
    /**
     * Vérifier si on doit commencer le préchargement ou le crossfade automatique
     */
    private fun checkCrossfadeTiming() {
        try {
            val currentPosition = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            
            if (duration > 0 && currentPosition > 0) {
                // Mettre à jour les timings selon la durée de la musique actuelle
                updateCrossfadeTimingForCurrentTrack()
                
                val timeRemaining = duration - currentPosition
                
                // DÉSACTIVÉ : Le crossfade est maintenant géré par SharedViewModel
                // Cela évite les conflits entre les deux systèmes
                
                /*
                // PHASE 1: Préchargement à 1m30s avant la fin
                if (!preloadScheduled && timeRemaining <= PRELOAD_START_TIME_MS) {
                    val nextIndex = currentMediaItemIndex + 1
                    if (nextIndex < mediaItemCount) {
                        val nextTrack = getMediaItemAt(nextIndex)
                        if (nextTrack != null) {
                            Logger.d(TAG, "🎧 DJ CROSSFADE - DÉBUT PRÉCHARGEMENT! Temps restant: ${timeRemaining}ms - Prochaine piste: ${nextTrack.metadata.title}")
                            scheduleCrossfade(nextTrack)
                            preloadScheduled = true
                        }
                    }
                }
                
                // PHASE 2: Transition (crossfade) à 15s avant la fin
                if (crossfadeScheduled && !isCrossfading && timeRemaining <= CROSSFADE_START_TIME_MS) {
                    Logger.d(TAG, "🎧 DJ CROSSFADE - DÉBUT TRANSITION! Temps restant: ${timeRemaining}ms")
                    startTrueDJCrossfade(nextMediaItemForCrossfade!!, CROSSFADE_DURATION_MS)
                    crossfadeScheduled = false
                    nextMediaItemForCrossfade = null
                }
                */
            }
        } catch (e: Exception) {
            Logger.e(TAG, "🎧 DJ CROSSFADE - Erreur vérification timing: ${e.message}")
        }
    }
    
    /**
     * Planifier le crossfade pour la prochaine chanson (phase de préchargement)
     */
    fun scheduleCrossfade(nextMediaItem: GenericMediaItem) {
        nextMediaItemForCrossfade = nextMediaItem
        crossfadeScheduled = true
        Logger.d(TAG, "🎧 DJ CROSSFADE - Crossfade planifié (préchargement) pour: ${nextMediaItem.metadata.title}")
    }
    
    override fun startTrueDJCrossfade(nextMediaItem: GenericMediaItem, crossfadeDurationMs: Long) {
        try {
            Logger.d(TAG, "🎧 DJ CROSSFADE - DÉMARRAGE MIXAGE DIRECT GARANTI!")
            Logger.d(TAG, "🎧 DJ CROSSFADE - Nouvelle musique: ${nextMediaItem.metadata.title}")
            Logger.d(TAG, "🎧 DJ CROSSFADE - Durée mixage: ${crossfadeDurationMs}ms")
            
            // Si un crossfade est déjà en cours, l'arrêter
            if (isCrossfading) {
                Logger.d(TAG, "🎧 DJ CROSSFADE - Arrêt du crossfade précédent")
                cleanupCrossfade()
            }
            
            // CRÉER un nouveau ExoPlayer pour la nouvelle musique
            Logger.d(TAG, "🎧 DJ CROSSFADE - Tentative lecture URI: ${nextMediaItem.uri}")
            
            // Configuration ultra-standard mais robuste
            val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            
            // Tenter d'utiliser la simple DefaultMediaSourceFactory sans fioritures complexes
            // Elle gère automatiquement File, Http, Asset, ContentContent, etc.
            crossfadePlayer = ExoPlayer.Builder(context, renderersFactory)
                .setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT, false)
                .build()
            
            crossfadePlayer?.playWhenReady = true // Force play immediately when ready    
            isCrossfading = true
            
            // CAPTURE DU VOLUME MASTER ACTUEL
            val masterVolume = exoPlayer.volume
            
            // Volume INITIAL à 10% du master comme demandé !
            crossfadePlayer?.volume = masterVolume * 0.1f
            crossfadePlayer?.playWhenReady = true // Force play immediately when ready
            
            Logger.d(TAG, "🎧 DJ CROSSFADE - NOUVEAU crossfade player créé (No Focus Steal)!")
            Logger.d(TAG, "🎧 DJ CROSSFADE - Master Volume: $masterVolume, Start Volume: ${masterVolume * 0.1f}")
            
            // Préparer la nouvelle musique
            val mediaItem = nextMediaItem.toMedia3MediaItem()
            crossfadePlayer?.setMediaItem(mediaItem)
            crossfadePlayer?.prepare()
            
            Logger.d(TAG, "🎧 DJ CROSSFADE - Media item préparé: ${nextMediaItem.metadata.title}")
            
            // Gestion des erreurs du crossfade player
            crossfadePlayer?.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Logger.e(TAG, "🎧 DJ CROSSFADE - ERREUR PLAYER SECONDAIRE: ${error.message}")
                    cleanupCrossfade()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && isCrossfading) {
                        Logger.d(TAG, "🎧 DJ CROSSFADE - Player prêt! Démarrage des deux musiques!")
                        
                        // DÉMARRER les deux musiques ensemble
                        crossfadePlayer?.play()
                        
                        // S'assurer encore du volume (10% du master)
                        crossfadePlayer?.volume = masterVolume * 0.1f
                        
                        Logger.d(TAG, "🎧 DJ CROSSFADE - ✅ NOUVELLE MUSIQUE DÉMARRÉE (volume=${masterVolume * 0.1f})")
                        Logger.d(TAG, "🎧 DJ CROSSFADE - ✅ LES DEUX MUSIQUES JOUENT ENSEMBLE!")
                        
                        // Démarrer l'animation de mixage
                        crossfadePlayer?.removeListener(this)
                        // On passe le volume cible pour l'animation
                        startCrossfadeAnimation(crossfadeDurationMs, masterVolume)
                    }
                }
            })
            
        } catch (e: Exception) {
            Logger.e(TAG, "🎧 DJ CROSSFADE - Erreur pendant mixage direct: ${e.message}")
            Logger.e(TAG, "🎧 DJ CROSSFADE - Stack: ${e.stackTraceToString()}")
            cleanupCrossfade()
        }
    }
    
    private fun startCrossfadeAnimation(durationMs: Long, targetVolume: Float) {
        try {
            Logger.d(TAG, "🎧 DJ CROSSFADE - DÉMARRAGE ANIMATION MIXAGE ${durationMs/1000}s vers volume $targetVolume!")
            
            // Utiliser un handler pour le crossfade progressif
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val steps = 100
            val stepDuration = durationMs / steps
            var currentStep = 0
            
            val crossfadeRunnable = object : Runnable {
                override fun run() {
                    if (currentStep >= steps || !isCrossfading) {
                        // Mixage terminé - les deux musiques ont joué ensemble!
                        Logger.d(TAG, "🎧 DJ CROSSFADE - Mixage terminé! Finalisation...")
                        completeCrossfade(targetVolume)
                        return
                    }
                    
                    val progress = currentStep.toFloat() / steps
                    
                    // ANCIENNE MUSIQUE : Reste à 100% la moitié du temps, puis fade out (Style DJ)
                    // Cela permet d'avoir la nouvelle musique "SUR" l'ancienne
                    val oldVolume = if (progress < 0.5f) {
                        targetVolume // Reste à fond les premières 50% du mix
                    } else {
                        // Descend de 100% à 0% sur la 2ème moitié
                        (targetVolume * (1.0f - progress) * 2).coerceAtLeast(0f)
                    }
                    exoPlayer.volume = oldVolume
                    
                    // NOUVELLE MUSIQUE : Volume monte de 10% Master à 100% Master (Régulier)
                    // Elle arrive progressivement "PAR DESSUS" l'ancienne
                    val newVolume = (targetVolume * 0.1f) + (progress * (targetVolume * 0.9f))
                    crossfadePlayer?.volume = newVolume.coerceAtMost(targetVolume)
                    
                    if (currentStep % 10 == 0) {
                       Logger.d(TAG, "🎧 DJ CROSSFADE - Étape $currentStep/100 - Ancienne: $oldVolume, Nouvelle: $newVolume")
                    }
                    
                    currentStep++
                    handler.postDelayed(this, stepDuration)
                }
            }
            
            handler.post(crossfadeRunnable)
            
        } catch (e: Exception) {
            Logger.e(TAG, "🎧 DJ CROSSFADE - Erreur dans l'animation: ${e.message}")
            Logger.e(TAG, "🎧 DJ CROSSFADE - Stack: ${e.stackTraceToString()}")
        }
    }
    
    private fun completeCrossfade(finalVolume: Float) {
        try {
            Logger.d(TAG, "🎧 DJ CROSSFADE - Mixage terminé! Passage au volume final: $finalVolume")
            
            if (crossfadePlayer != null) {
                Logger.d(TAG, "🎧 DJ CROSSFADE - 🚀 SWAP PLAYERS: Le Crossfade Player devient le Main Player (Zero Gap)!")
                
                val newMainPlayer = crossfadePlayer!!
                val oldPlayer = exoPlayer
                
                // --- SAUVEGARDE ET TRANSFERT DE LA PLAYLIST ---
                // Le nouveau player n'a que la chanson actuelle. On doit reconstruire toute la playlist autour.
                try {
                    val currentIndexInOld = oldPlayer.currentMediaItemIndex
                    // La nouvelle chanson correspond logiquement à la suivante dans la playlist actuelle
                    val targetIndex = currentIndexInOld + 1
                    val totalItems = oldPlayer.mediaItemCount
                    
                    Logger.d(TAG, "🎧 DJ CROSSFADE - Reconstruction playlist: Index cible=$targetIndex, Total=$totalItems")
                    
                    if (targetIndex < totalItems) {
                        // 1. Récupérer les items AVANT la chanson actuelle
                        if (targetIndex > 0) {
                             val beforeItems = List(targetIndex) { i -> oldPlayer.getMediaItemAt(i) }
                             newMainPlayer.addMediaItems(0, beforeItems)
                             Logger.d(TAG, "🎧 DJ CROSSFADE - Playlist: ${beforeItems.size} items ajoutés AVANT")
                        }
                        
                        // 2. Récupérer les items APRÈS la chanson actuelle
                        if (targetIndex + 1 < totalItems) {
                             val afterItems = List(totalItems - (targetIndex + 1)) { i -> oldPlayer.getMediaItemAt(targetIndex + 1 + i) }
                             newMainPlayer.addMediaItems(newMainPlayer.mediaItemCount, afterItems)
                             Logger.d(TAG, "🎧 DJ CROSSFADE - Playlist: ${afterItems.size} items ajoutés APRÈS")
                        }
                    }
                    
                    // Transfert des états
                    newMainPlayer.repeatMode = oldPlayer.repeatMode
                    newMainPlayer.shuffleModeEnabled = oldPlayer.shuffleModeEnabled
                    
                    // IMPORTANT: Copier l'instance de shuffle order si possible, ou laisser l'adapter gérer
                    // L'adapter utilise les listes 'shuffleOrder' qui sont dans CETTE classe, donc elles restent valides
                    // tant que la timeline du player correspond.
                    
                } catch (e: Exception) {
                    Logger.e(TAG, "🎧 DJ CROSSFADE - Erreur transfert playlist: ${e.message}")
                }
                
                // --- SWAP STANDARD ---
                
                // 3. Transition des listeners
                oldPlayer.removeListener(exoPlayerListener)
                newMainPlayer.addListener(exoPlayerListener)
                
                // 4. Arrêt propre de l'ancien
                oldPlayer.stop()
                oldPlayer.clearMediaItems()
                oldPlayer.release()
                
                // 5. PROMOTION DU PLAYER
                exoPlayer = newMainPlayer
                
                // NOTIFIER LE SERVICE QUE LE PLAYER A CHANGÉ !
                Logger.d(TAG, "🎧 DJ CROSSFADE - Notification au service du changement de player instance")
                onPlayerRefChanged?.invoke(exoPlayer as Any)
                
                // 6. Finalisation
                exoPlayer.volume = finalVolume
                
                // Reset des références temporaires
                crossfadePlayer = null
                isCrossfading = false
                
                Logger.d(TAG, "🎧 DJ CROSSFADE - ✅ SWAP TERMINÉ! La musique continue sans interruption.")
                notifyTimelineChanged("TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED")
                exoPlayerListener.onIsPlayingChanged(true)
                
                // FORCE UPDATE for UI and History
                val currentItem = exoPlayer.currentMediaItem
                if (currentItem != null) {
                    Logger.d(TAG, "🎧 DJ CROSSFADE - 🔄 Triggering manual MediaItemTransition for UI/History")
                    val genericItem = currentItem.toGenericMediaItem()
                    listeners.forEach { it.onMediaItemTransition(genericItem, PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_AUTO) }
                }
            }
            
        } catch (e: Exception) {
            Logger.e(TAG, "🎧 DJ CROSSFADE - Erreur completion mixage (Swap): ${e.message}")
            cleanupCrossfade()
        }
    }
    
    private fun cleanupCrossfade() {
        try {
            Logger.d(TAG, "🎧 DJ CROSSFADE - Cleaning up crossfade resources")
            
            // Arrêter et libérer le crossfade player
            crossfadePlayer?.stop()
            crossfadePlayer?.release()
            crossfadePlayer = null
            isCrossfading = false
            
            // S'assurer que le volume du player principal est restauré à 1.0
            exoPlayer.volume = 1f
            
            Logger.d(TAG, "🎧 DJ CROSSFADE - Crossfade cleanup completed!")
            
        } catch (e: Exception) {
            Logger.e(TAG, "🎧 DJ CROSSFADE - Error in cleanup: ${e.message}")
            // Forcer le nettoyage même en cas d'erreur
            try {
                crossfadePlayer?.release()
                crossfadePlayer = null
                isCrossfading = false
                exoPlayer.volume = 1f
            } catch (cleanupError: Exception) {
                Logger.e(TAG, "🎧 DJ CROSSFADE - Critical error in forced cleanup: ${cleanupError.message}")
            }
        }
    }
    
    /**
     * Vérifier si un crossfade est en cours
     */
    override fun isCrossfading(): Boolean = isCrossfading
}

// Extension functions for conversions between Media3 and Generic types

private fun GenericMediaItem.toMedia3MediaItem(): MediaItem {
    val builder =
        MediaItem
            .Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata.toMedia3MediaMetadata())

    uri?.let { builder.setUri(it) }
    customCacheKey?.let { builder.setCustomCacheKey(it) }

    return builder.build()
}

private fun MediaItem.toGenericMediaItem(): GenericMediaItem =
    GenericMediaItem(
        mediaId = mediaId,
        uri = localConfiguration?.uri.toString(),
        metadata = mediaMetadata.toGenericMediaMetadata(),
        customCacheKey = localConfiguration?.customCacheKey,
    )

private fun GenericMediaMetadata.toMedia3MediaMetadata(): MediaMetadata =
    MediaMetadata
        .Builder()
        .apply {
            title?.let { setTitle(it) }
            artist?.let { setArtist(it) }
            albumTitle?.let { setAlbumTitle(it) }
            artworkUri?.let { setArtworkUri(it.toUri()) }
            description?.let { setDescription(it) }
        }.build()

private fun MediaMetadata.toGenericMediaMetadata(): GenericMediaMetadata =
    GenericMediaMetadata(
        title = title?.toString(),
        artist = artist?.toString(),
        albumTitle = albumTitle?.toString(),
        artworkUri = artworkUri.toString(),
        description = description?.toString(),
    )

private fun GenericPlaybackParameters.toMedia3PlaybackParameters(): PlaybackParameters = PlaybackParameters(speed, pitch)

private fun PlaybackParameters.toGenericPlaybackParameters(): GenericPlaybackParameters = GenericPlaybackParameters(speed, pitch)

private fun Tracks.toGenericTracks(): GenericTracks {
    val genericGroups =
        groups.map { group ->
            GenericTracks.GenericTrackGroup(trackCount = group.length)
        }
    return GenericTracks(groups = genericGroups)
}