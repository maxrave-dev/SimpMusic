package com.maxrave.simpmusic.service


import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.MEDIA_NOTIFICATION
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.di.DownloadCache
import com.maxrave.simpmusic.di.PlayerCache
import com.maxrave.simpmusic.service.test.CoilBitmapLoader
import com.maxrave.simpmusic.service.test.source.MergingMediaSourceFactory
import com.maxrave.simpmusic.ui.MainActivity
import com.maxrave.simpmusic.ui.widget.BasicWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


@AndroidEntryPoint
@UnstableApi
class SimpleMediaService : MediaLibraryService() {

    lateinit var player: ExoPlayer

    lateinit var mediaSession: MediaLibrarySession

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    @Inject
    lateinit var mainRepository: MainRepository

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    @Inject
    lateinit var simpleMediaSessionCallback: SimpleMediaSessionCallback

    lateinit var simpleMediaServiceHandler: SimpleMediaServiceHandler

    private val serviceCoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
                R.string.notification_channel_name
            )
                .apply {
                    setSmallIcon(R.drawable.mono)
                }
        )
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(provideAudioAttributes(), true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .setSeekForwardIncrementMs(5000)
            .setSeekBackIncrementMs(5000)
            .setMediaSourceFactory(provideMergingMediaSource())
            .setRenderersFactory(provideRendererFactory(this))
            .build()

        mediaSession = provideMediaLibrarySession(
            this,
            this,
            player,
            simpleMediaSessionCallback
        )
        val sessionToken = SessionToken(this, ComponentName(this, SimpleMediaService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())
        simpleMediaServiceHandler = SimpleMediaServiceHandler(
            player = player,
            mediaSession = mediaSession,
            mediaSessionCallback = simpleMediaSessionCallback,
            dataStoreManager = dataStoreManager,
            mainRepository = mainRepository,
            coroutineScope = serviceCoroutineScope,
            context = application.applicationContext
        )
    }

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action != null) {
            when (intent.action) {
                BasicWidget.ACTION_TOGGLE_PAUSE -> {
                    if (player.isPlaying) player.pause() else player.play()
                }

                BasicWidget.ACTION_SKIP -> {
                    player.seekToNext()
                }

                BasicWidget.ACTION_REWIND -> {
                    player.seekToPrevious()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSession

    @UnstableApi
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)

    }

    @UnstableApi
    private fun release() {
        mediaSession.run {
            release()
            if (player.playbackState != Player.STATE_IDLE) {
                player.release()
            }
        }
    }

    @UnstableApi
    override fun onDestroy() {
        super.onDestroy()
        serviceCoroutineScope.cancel()
        release()
        Log.d("SimpleMediaService", "onDestroy: ")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        simpleMediaServiceHandler.mayBeSaveRecentSong()
    }

    @UnstableApi
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
//        serviceCoroutineScope.cancel()
//        Log.d("SimpleMediaService", "onTaskRemoved: ")
//        release()
//        stopSelf()

    }

    inner class MusicBinder : Binder() {
        val service: SimpleMediaService
            get() = this@SimpleMediaService
    }

    override fun onBind(intent: Intent?): IBinder =
        super.onBind(intent) ?: binder

    private fun provideAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

//    @Provides
//    @Singleton
//    @UnstableApi
//    fun provideDataSource( context: Context): DefaultMediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(
//        DefaultHttpDataSource.Factory()
//            .setAllowCrossProtocolRedirects(true)
//            .setUserAgent("Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
//            .setConnectTimeoutMs(5000)
//    )

    @UnstableApi
    fun provideCacheDataSource(
        @DownloadCache downloadCache: SimpleCache,
        @PlayerCache playerCache: SimpleCache
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource.Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(
                        DefaultHttpDataSource.Factory()
                            .setAllowCrossProtocolRedirects(true)
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
                            .setConnectTimeoutMs(5000)
                    )
            )
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

//    @Provides
//    @Singleton
//    @UnstableApi
//    fun provideMediaSourceFactory(
//         context: Context,
//        cacheDataSourceFactory: CacheDataSource.Factory
//    ): DefaultMediaSourceFactory =
//        DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory)


    @UnstableApi
    fun provideResolvingDataSourceFactory(
        cacheDataSourceFactory: CacheDataSource.Factory,
        @DownloadCache downloadCache: SimpleCache,
        @PlayerCache playerCache: SimpleCache,
        mainRepository: MainRepository
    ): DataSource.Factory {
        return ResolvingDataSource.Factory(cacheDataSourceFactory) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            Log.w("Stream", mediaId)
            Log.w("Stream", mediaId.startsWith(MergingMediaSourceFactory.isVideo).toString())
            val CHUNK_LENGTH = 512 * 1024L
            if (downloadCache.isCached(
                    mediaId,
                    dataSpec.position,
                    if (dataSpec.length >= 0) dataSpec.length else 1
                ) || playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)
            ) {
                return@Factory dataSpec
            }
            var dataSpecReturn: DataSpec = dataSpec
            runBlocking(Dispatchers.IO) {
                if (mediaId.contains(MergingMediaSourceFactory.isVideo)) {
                    val id = mediaId.removePrefix(MergingMediaSourceFactory.isVideo)
                    mainRepository.getStream(
                        id, true
                    ).cancellable().collect {
                        if (it != null) {
                            dataSpecReturn = dataSpec.withUri(it.toUri())
                        }
                    }
                }
                else {
                    mainRepository.getStream(
                        mediaId, isVideo = false
                    ).cancellable().collect {
                        if (it != null) {
                            dataSpecReturn = dataSpec.withUri(it.toUri())
                        }
                    }
                }
            }
            return@Factory dataSpecReturn
        }
    }

    @UnstableApi
    fun provideExtractorFactory(): ExtractorsFactory = ExtractorsFactory {
        arrayOf(
            MatroskaExtractor(),
            FragmentedMp4Extractor(),
            androidx.media3.extractor.mp4.Mp4Extractor(),
        )
    }

    @UnstableApi
    fun provideMediaSourceFactory(): DefaultMediaSourceFactory =
        DefaultMediaSourceFactory(
            provideResolvingDataSourceFactory(
                provideCacheDataSource(downloadCache, playerCache),
                downloadCache,
                playerCache,
                mainRepository
            ),
            provideExtractorFactory()
        )

    private fun provideMergingMediaSource(): MergingMediaSourceFactory = MergingMediaSourceFactory(
        provideMediaSourceFactory(),
        dataStoreManager
    )

    @UnstableApi
    fun provideRendererFactory(context: Context): DefaultRenderersFactory =
        object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessorChain(
                        DefaultAudioSink.DefaultAudioProcessorChain(
                            emptyArray(),
                            SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                            SonicAudioProcessor()
                        )
                    )
                    .build()
            }
        }

    @UnstableApi
    fun provideCoilBitmapLoader(context: Context): CoilBitmapLoader = CoilBitmapLoader(context)


    @UnstableApi
    fun provideMediaLibrarySession(
        context: Context,
        service: MediaLibraryService,
        player: ExoPlayer,
        callback: SimpleMediaSessionCallback
    ): MediaLibrarySession = MediaLibrarySession.Builder(
        service, player, callback
    )
        .setSessionActivity(
            PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setBitmapLoader(provideCoilBitmapLoader(context))
        .build()

    @UnstableApi
    fun provideMediaSession(
        context: Context,
        player: ExoPlayer,
        callback: SimpleMediaSessionCallback
    ): MediaSession =
        MediaSession.Builder(context, player)
            .setCallback(callback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setBitmapLoader(provideCoilBitmapLoader(context))
            .build()

}

