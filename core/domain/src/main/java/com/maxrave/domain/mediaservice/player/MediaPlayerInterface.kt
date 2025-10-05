package com.maxrave.domain.mediaservice.player

import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.GenericPlaybackParameters

/**
 * Abstract interface for media player implementations
 */
interface MediaPlayerInterface {
    // Playback control
    fun play()

    fun pause()

    fun stop()

    fun seekTo(positionMs: Long)

    fun seekTo(
        mediaItemIndex: Int,
        positionMs: Long,
    )

    fun seekBack()

    fun seekForward()

    fun seekToNext()

    fun seekToPrevious()

    fun prepare()

    // Media item management
    fun setMediaItem(mediaItem: GenericMediaItem)

    fun addMediaItem(mediaItem: GenericMediaItem)

    fun addMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    )

    fun removeMediaItem(index: Int)

    fun moveMediaItem(
        fromIndex: Int,
        toIndex: Int,
    )

    fun clearMediaItems()

    fun replaceMediaItem(
        index: Int,
        mediaItem: GenericMediaItem,
    )

    fun getMediaItemAt(index: Int): GenericMediaItem?

    // Playback state properties
    val isPlaying: Boolean
    val currentPosition: Long
    val duration: Long
    val bufferedPosition: Long
    val bufferedPercentage: Int
    val currentMediaItem: GenericMediaItem?
    val currentMediaItemIndex: Int
    val mediaItemCount: Int
    val contentPosition: Long
    val playbackState: Int

    // Navigation
    fun hasNextMediaItem(): Boolean

    fun hasPreviousMediaItem(): Boolean

    // Playback modes
    var shuffleModeEnabled: Boolean
    var repeatMode: Int
    var playWhenReady: Boolean
    var playbackParameters: GenericPlaybackParameters

    // Audio settings
    val audioSessionId: Int
    var volume: Float
    var skipSilenceEnabled: Boolean

    // Listener management
    fun addListener(listener: MediaPlayerListener)

    fun removeListener(listener: MediaPlayerListener)

    // Release resources
    fun release()
}