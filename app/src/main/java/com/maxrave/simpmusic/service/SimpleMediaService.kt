package com.maxrave.simpmusic.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config.MAIN_PLAYER
import com.maxrave.simpmusic.common.MEDIA_NOTIFICATION
import com.maxrave.simpmusic.service.test.CoilBitmapLoader
import com.maxrave.simpmusic.ui.MainActivity
import com.maxrave.simpmusic.ui.widget.BasicWidget
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

@UnstableApi
class SimpleMediaService :
    MediaLibraryService(),
    KoinComponent {
    private val player: ExoPlayer by inject(named(MAIN_PLAYER))
    private val coilBitmapLoader: CoilBitmapLoader by inject()

    private var mediaSession: MediaLibrarySession? = null

    private val simpleMediaSessionCallback: SimpleMediaSessionCallback by inject()

    private val simpleMediaServiceHandler: SimpleMediaServiceHandler by inject()

    private val binder = MusicBinder()

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        Log.w("Service", "Simple Media Service Created")
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { MEDIA_NOTIFICATION.NOTIFICATION_ID },
                MEDIA_NOTIFICATION.NOTIFICATION_CHANNEL_ID,
                R.string.notification_channel_name,
            ).apply {
                setSmallIcon(R.drawable.mono)
            },
        )

        mediaSession =
            provideMediaLibrarySession(
                this,
                this,
                player,
                simpleMediaSessionCallback,
            )

        // Set late init set notification layout
        simpleMediaServiceHandler.setNotificationLayout = { listCommand ->
            mediaSession?.setCustomLayout(listCommand)
        }
        val sessionToken = SessionToken(this, ComponentName(this, SimpleMediaService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())
    }

    @UnstableApi
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.w("Service", "Simple Media Service Received Action: ${intent?.action}")
        if (intent != null && intent.action != null) {
            when (intent.action) {
                BasicWidget.ACTION_TOGGLE_PAUSE -> {
                    if (player.isPlaying) player.pause() else player.play()
                }

                BasicWidget.ACTION_SKIP -> {
                    simpleMediaServiceHandler.resetCrossfade()
                    player.seekToNext()
                }

                BasicWidget.ACTION_REWIND -> {
                    simpleMediaServiceHandler.resetCrossfade()
                    player.seekToPrevious()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    @UnstableApi
    override fun onUpdateNotification(
        session: MediaSession,
        startInForegroundRequired: Boolean,
    ) {
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    @UnstableApi
    private fun release() {
        runBlocking {
            simpleMediaServiceHandler.release()
            mediaSession?.run {
                player.stop()
                this.release()
                player.release()
                mediaSession = null
            }
        }
    }

    @UnstableApi
    override fun onDestroy() {
        super.onDestroy()
        Log.w("Service", "Simple Media Service Destroyed")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        simpleMediaServiceHandler.mayBeSaveRecentSong()
    }

    @UnstableApi
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (simpleMediaServiceHandler.shouldReleaseOnTaskRemoved()) {
            release()
        }
        super.onTaskRemoved(rootIntent)
    }

    inner class MusicBinder : Binder() {
        val service: SimpleMediaService
            get() = this@SimpleMediaService
    }

    override fun onBind(intent: Intent?): IBinder = super.onBind(intent) ?: binder

    // Can't inject by Koin because it depend on service
    @UnstableApi
    private fun provideMediaLibrarySession(
        context: Context,
        service: MediaLibraryService,
        player: ExoPlayer,
        callback: SimpleMediaSessionCallback,
    ): MediaLibrarySession =
        MediaLibrarySession
            .Builder(
                service,
                player,
                callback,
            ).setSessionActivity(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            ).setBitmapLoader(coilBitmapLoader)
            .build()
}