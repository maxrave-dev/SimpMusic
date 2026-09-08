package com.maxrave.simpmusic.service

import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.maxrave.media3.service.SimpleMediaService

/**
 * Keep media controls visible while paused by continuing to request notification updates
 * whenever there is an active queue item.
 */
class SimpMusicMediaService : SimpleMediaService() {
    override fun onUpdateNotification(
        session: MediaSession,
        startInForegroundRequired: Boolean,
    ) {
        val player = session.player
        val shouldKeepVisibleWhenPaused =
            player.currentMediaItem != null &&
                player.playbackState != Player.STATE_IDLE &&
                player.playbackState != Player.STATE_ENDED

        super.onUpdateNotification(
            session,
            startInForegroundRequired = startInForegroundRequired || shouldKeepVisibleWhenPaused,
        )
    }
}
