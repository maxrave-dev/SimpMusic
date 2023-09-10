package com.maxrave.simpmusic.service

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.maxrave.simpmusic.common.MEDIA_CUSTOM_COMMAND

class SimpleMediaSessionCallback: MediaSession.Callback {
    var toggleLike: () -> Unit = {}

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        val sessionCommands =
            connectionResult.availableSessionCommands
                .buildUpon()
                // Add custom commands
                .add(SessionCommand(MEDIA_CUSTOM_COMMAND.LIKE, Bundle()))
                .add(SessionCommand(MEDIA_CUSTOM_COMMAND.REPEAT, Bundle()))
                .build()
        return MediaSession.ConnectionResult.accept(
            sessionCommands, connectionResult.availablePlayerCommands
        )
    }

    @UnstableApi
    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            MEDIA_CUSTOM_COMMAND.LIKE -> {
                toggleLike()
            }
            MEDIA_CUSTOM_COMMAND.REPEAT -> {
                session.player.repeatMode = when (session.player.repeatMode) {
                    ExoPlayer.REPEAT_MODE_OFF -> ExoPlayer.REPEAT_MODE_ONE
                    ExoPlayer.REPEAT_MODE_ONE -> ExoPlayer.REPEAT_MODE_ALL
                    ExoPlayer.REPEAT_MODE_ALL -> ExoPlayer.REPEAT_MODE_OFF
                    else -> ExoPlayer.REPEAT_MODE_OFF
                }
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }
}