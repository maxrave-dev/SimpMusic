package com.maxrave.media3.exoplayer

import android.annotation.SuppressLint
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.GenericMediaMetadata
import com.maxrave.domain.data.player.GenericPlaybackParameters
import com.maxrave.domain.data.player.GenericTracks
import com.maxrave.domain.data.player.PlayerConstants
import com.maxrave.domain.data.player.PlayerError
import com.maxrave.domain.mediaservice.player.MediaPlayerInterface
import com.maxrave.domain.mediaservice.player.MediaPlayerListener

/**
 * ExoPlayer implementation of MediaPlayerInterface
 * Handles all Media3-specific logic and conversions
 */
@SuppressLint("UnsafeOptInUsageError")
@OptIn(UnstableApi::class)
class ExoPlayerAdapter(
    private val exoPlayer: ExoPlayer,
) : MediaPlayerInterface {
    private val listeners = mutableListOf<MediaPlayerListener>()
    private val exoPlayerListener = ExoPlayerListenerImpl()

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
    }

    override fun addMediaItem(mediaItem: GenericMediaItem) {
        exoPlayer.addMediaItem(mediaItem.toMedia3MediaItem())
    }

    override fun addMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        exoPlayer.addMediaItem(index, mediaItem.toMedia3MediaItem())
    }

    override fun removeMediaItem(index: Int) = exoPlayer.removeMediaItem(index)

    override fun moveMediaItem(
        fromIndex: Int,
        toIndex: Int,
    ) = exoPlayer.moveMediaItem(fromIndex, toIndex)

    override fun clearMediaItems() = exoPlayer.clearMediaItems()

    override fun replaceMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    ) {
        exoPlayer.replaceMediaItem(index, mediaItem.toMedia3MediaItem())
    }

    override fun getMediaItemAt(index: Int): GenericMediaItem? = exoPlayer.getMediaItemAt(index)?.toGenericMediaItem()

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

    // Internal ExoPlayer listener that converts events to generic events
    private inner class ExoPlayerListenerImpl : Player.Listener {
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
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            listeners.forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled) }
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
}

// Extension functions for conversions between Media3 and Generic types

@UnstableApi
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

@UnstableApi
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