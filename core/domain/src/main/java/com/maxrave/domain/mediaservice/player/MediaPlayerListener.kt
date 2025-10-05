package com.maxrave.domain.mediaservice.player

import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.GenericTracks
import com.maxrave.domain.data.player.PlayerError

/**
 * Listener interface for media player events
 */
interface MediaPlayerListener {
    fun onPlaybackStateChanged(playbackState: Int)

    fun onIsPlayingChanged(isPlaying: Boolean)

    fun onMediaItemTransition(
        mediaItem: GenericMediaItem?,
        reason: Int,
    )

    fun onTracksChanged(tracks: GenericTracks)

    fun onPlayerError(error: PlayerError)

    fun shouldOpenOrCloseEqualizerIntent(shouldOpen: Boolean)

    fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean)

    fun onRepeatModeChanged(repeatMode: Int)

    fun onIsLoadingChanged(isLoading: Boolean)
}