package com.maxrave.simpmusic.service


import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.MEDIA_NOTIFICATION.NOTIFICATION_CHANNEL_ID
import com.maxrave.simpmusic.common.MEDIA_NOTIFICATION.NOTIFICATION_ID
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SimpleMediaService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var mediaSession: MediaSession

//    @Inject
//    lateinit var notificationManager: SimpleMediaNotificationManager

    @Inject
    lateinit var context: Application

    @Inject
    lateinit var simpleMediaServiceHandler: SimpleMediaServiceHandler

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this, { NOTIFICATION_ID }, NOTIFICATION_CHANNEL_ID, R.string.notification_channel_name)
                .apply {
                    setSmallIcon(R.drawable.logo_simpmusic_01_removebg_preview)
                }
        )
        val sessionToken = SessionToken(this, ComponentName(this, SimpleMediaService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())
        return super.onStartCommand(intent, flags, startId)
    }

    @UnstableApi
    override fun onDestroy() {
        super.onDestroy()
        simpleMediaServiceHandler.mayBeSaveRecentSong()
        simpleMediaServiceHandler.mayBeSavePlaybackState()
        mediaSession.run {
            release()
            if (player.playbackState != Player.STATE_IDLE) {
                player.seekTo(0)
                player.playWhenReady = false
                player.stop()
            }
        }
        simpleMediaServiceHandler.release()
        Log.d("SimpleMediaService", "onDestroy: ")
    }

    @UnstableApi
    override fun onTaskRemoved(rootIntent: Intent?) {
        simpleMediaServiceHandler.mayBeSaveRecentSong()
        simpleMediaServiceHandler.mayBeSavePlaybackState()
        mediaSession.run {
            release()
            if (player.playbackState != Player.STATE_IDLE) {
                player.seekTo(0)
                player.playWhenReady = false
                player.stop()
            }
        }
        simpleMediaServiceHandler.release()
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    @UnstableApi
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)

    }

}

