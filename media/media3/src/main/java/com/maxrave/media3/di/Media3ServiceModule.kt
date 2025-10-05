package com.maxrave.media3.di

import android.app.Activity
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_MAX_BUFFER_MS
import androidx.media3.exoplayer.DefaultLoadControl.DEFAULT_MIN_BUFFER_MS
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.maxrave.common.Config.CANVAS_CACHE
import com.maxrave.common.Config.DOWNLOAD_CACHE
import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.common.Config.PLAYER_CACHE
import com.maxrave.common.Config.SERVICE_SCOPE
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.logger.Logger
import com.maxrave.media3.exoplayer.ExoPlayerAdapter
import com.maxrave.media3.repository.CacheRepositoryImpl
import com.maxrave.media3.service.SimpleMediaService
import com.maxrave.media3.service.callback.SimpleMediaSessionCallback
import com.maxrave.media3.service.download.DownloadUtils
import com.maxrave.media3.service.mediasourcefactory.MergingMediaSourceFactory
import com.maxrave.media3.utils.CoilBitmapLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.net.Proxy
import java.time.LocalDateTime

/**
 * Required repository first initialization
 */
@UnstableApi
private val mediaServiceModule =
    module {
        // Service
        // CoroutineScope for service
        single<CoroutineScope>(qualifier = named(SERVICE_SCOPE)) {
            CoroutineScope(Dispatchers.Main + SupervisorJob())
        }
        // Cache
        single<DatabaseProvider> {
            provideDatabaseProvider(androidContext())
        }
        // Player Cache
        single<SimpleCache>(qualifier = named(PLAYER_CACHE)) {
            provideSimpleCache(
                context = androidContext(),
                cacheName = "exoplayer",
                cacheSize = runBlocking { get<DataStoreManager>().maxSongCacheSize.first() },
                databaseProvider = get<DatabaseProvider>(),
            )
        }
        // Download Cache
        single<SimpleCache>(qualifier = named(DOWNLOAD_CACHE)) {
            provideSimpleCache(
                context = androidContext(),
                cacheName = "download",
                cacheSize = -1,
                databaseProvider = get<DatabaseProvider>(),
            )
        }
        // Spotify Canvas Cache
        single<SimpleCache>(qualifier = named(CANVAS_CACHE)) {
            provideSimpleCache(
                context = androidContext(),
                cacheName = "spotifyCanvas",
                cacheSize = -1,
                databaseProvider = get<DatabaseProvider>(),
            )
        }
        // DownloadUtils
        single<DownloadHandler> {
            DownloadUtils(
                context = androidContext(),
                playerCache = get(named(PLAYER_CACHE)),
                downloadCache = get(named(DOWNLOAD_CACHE)),
                dataStoreManager = get(),
                databaseProvider = get(),
                streamRepository = get(),
                songRepository = get(),
            )
        }

        // AudioAttributes
        single<AudioAttributes> {
            provideAudioAttributes()
        }

        single<MergingMediaSourceFactory> {
            provideMergingMediaSource(
                androidContext(),
                get(named(DOWNLOAD_CACHE)),
                get(named(PLAYER_CACHE)),
                get(),
                get(named(SERVICE_SCOPE)),
                get(),
            )
        }

        single<DefaultRenderersFactory> {
            provideRendererFactory(androidContext())
        }

        // ExoPlayer
        single<ExoPlayer>(createdAtStart = true, qualifier = named(MAIN_PLAYER)) {
            ExoPlayer
                .Builder(androidContext())
                .setAudioAttributes(get(), true)
                .setLoadControl(
                    provideLoadControl(),
                ).setWakeMode(C.WAKE_MODE_NETWORK)
                .setHandleAudioBecomingNoisy(true)
                .setSeekForwardIncrementMs(5000)
                .setSeekBackIncrementMs(5000)
                .setMediaSourceFactory(
                    get<MergingMediaSourceFactory>(),
                ).setRenderersFactory(
                    get<DefaultRenderersFactory>(),
                ).build()
                .also {
                    it.addAnalyticsListener(EventLogger())
                }
        }

        // CoilBitmapLoader
        single<CoilBitmapLoader> {
            provideCoilBitmapLoader(androidContext(), get(named(SERVICE_SCOPE)))
        }

        single<ExoPlayerAdapter> {
            ExoPlayerAdapter(get(named(MAIN_PLAYER)))
        }

        // MediaSession Callback for main player
        single<MediaLibrarySession.Callback> {
            SimpleMediaSessionCallback(
                androidContext(),
                get<CoroutineScope>(named(SERVICE_SCOPE)),
                get<MediaPlayerHandler>(),
                get<SearchRepository>(),
                get<SongRepository>(),
                get<LocalPlaylistRepository>(),
                get<PlaylistRepository>(),
                get<HomeRepository>(),
                get<StreamRepository>(),
            )
        }

        single<CacheRepository> {
            CacheRepositoryImpl(
                playerCache = get(named(PLAYER_CACHE)),
                downloadCache = get(named(DOWNLOAD_CACHE)),
                canvasCache = get(named(CANVAS_CACHE)),
            )
        }
    }

@UnstableApi
private fun provideResolvingDataSourceFactory(
    cacheDataSourceFactory: CacheDataSource.Factory,
    downloadCache: SimpleCache,
    playerCache: SimpleCache,
    dataStoreManager: DataStoreManager,
    streamRepository: StreamRepository,
    coroutineScope: CoroutineScope,
): DataSource.Factory {
    val chunkLength = 10 * 512 * 1024L
    return ResolvingDataSource.Factory(cacheDataSourceFactory) { dataSpec ->
        val mediaId = dataSpec.key ?: error("No media id")
        Logger.w("Stream", mediaId)
        Logger.w("Stream", mediaId.startsWith(MERGING_DATA_TYPE.VIDEO).toString())
        val length = if (dataSpec.length >= 0) dataSpec.length else 1
        if (downloadCache.isCached(
                mediaId,
                dataSpec.position,
                length,
            )
        ) {
            coroutineScope.launch(Dispatchers.IO) {
                streamRepository.updateFormat(
                    if (mediaId.contains(MERGING_DATA_TYPE.VIDEO)) {
                        mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                    } else {
                        mediaId
                    },
                )
            }
            Logger.w("Stream", "Downloaded $mediaId")
            return@Factory dataSpec
        }
        if (playerCache.isCached(mediaId, dataSpec.position, chunkLength)) {
            coroutineScope.launch(Dispatchers.IO) {
                streamRepository.updateFormat(
                    if (mediaId.contains(MERGING_DATA_TYPE.VIDEO)) {
                        mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                    } else {
                        mediaId
                    },
                )
            }
            Logger.w("Stream", "Cached $mediaId")
            return@Factory dataSpec
        }
        var dataSpecReturn: DataSpec = dataSpec
        runBlocking(Dispatchers.IO) {
            if (mediaId.contains(MERGING_DATA_TYPE.VIDEO)) {
                val id = mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                streamRepository.getNewFormat(id).lastOrNull()?.let {
                    val videoUrl = it.videoUrl
                    if (videoUrl != null && it.expiredTime > LocalDateTime.now()) {
                        Logger.d("Stream", videoUrl)
                        Logger.w("Stream", "Video from format")
                        val is403Url = streamRepository.is403Url(videoUrl).firstOrNull() != false
                        Logger.d("Stream", "is 403 $is403Url")
                        if (!is403Url) {
                            dataSpecReturn = dataSpec.withUri(videoUrl.toUri()).subrange(dataSpec.uriPositionOffset, chunkLength)
                            return@runBlocking
                        }
                    }
                }
                streamRepository
                    .getStream(
                        dataStoreManager,
                        id,
                        true,
                    ).lastOrNull()
                    ?.let {
                        Logger.d("Stream", it)
                        Logger.w("Stream", "Video")
                        dataSpecReturn = dataSpec.withUri(it.toUri()).subrange(dataSpec.uriPositionOffset, chunkLength)
                    }
            } else {
                streamRepository.getNewFormat(mediaId).lastOrNull()?.let {
                    val audioUrl = it.audioUrl
                    if (audioUrl != null && it.expiredTime > LocalDateTime.now()) {
                        Logger.d("Stream", audioUrl)
                        Logger.w("Stream", "Audio from format")
                        val is403Url = streamRepository.is403Url(audioUrl).firstOrNull() != false
                        Logger.d("Stream", "is 403 $is403Url")
                        if (!is403Url) {
                            dataSpecReturn = dataSpec.withUri(audioUrl.toUri()).subrange(dataSpec.uriPositionOffset, chunkLength)
                            return@runBlocking
                        }
                    }
                }
                streamRepository
                    .getStream(
                        dataStoreManager,
                        mediaId,
                        isVideo = false,
                    ).lastOrNull()
                    ?.let {
                        Logger.d("Stream", it)
                        Logger.w("Stream", "Audio")
                        dataSpecReturn = dataSpec.withUri(it.toUri()).subrange(dataSpec.uriPositionOffset, chunkLength)
                    }
            }
        }
        return@Factory dataSpecReturn
    }
}

@UnstableApi
private fun provideExtractorFactory(): ExtractorsFactory =
    ExtractorsFactory {
        arrayOf(
            MatroskaExtractor(
                DefaultSubtitleParserFactory(),
            ),
            FragmentedMp4Extractor(
                DefaultSubtitleParserFactory(),
            ),
            Mp4Extractor(
                DefaultSubtitleParserFactory(),
            ),
        )
    }

@UnstableApi
private fun provideMediaSourceFactory(
    context: Context,
    downloadCache: SimpleCache,
    playerCache: SimpleCache,
    streamRepository: StreamRepository,
    dataStoreManager: DataStoreManager,
    coroutineScope: CoroutineScope,
): DefaultMediaSourceFactory =
    DefaultMediaSourceFactory(
        provideResolvingDataSourceFactory(
            provideCacheDataSource(
                downloadCache,
                playerCache,
                context,
                dataStoreManager.getJVMProxy(),
            ),
            downloadCache,
            playerCache,
            dataStoreManager,
            streamRepository,
            coroutineScope,
        ),
        provideExtractorFactory(),
    )

@OptIn(UnstableApi::class)
private fun provideMergingMediaSource(
    context: Context,
    downloadCache: SimpleCache,
    playerCache: SimpleCache,
    streamRepository: StreamRepository,
    coroutineScope: CoroutineScope,
    dataStoreManager: DataStoreManager,
): MergingMediaSourceFactory =
    MergingMediaSourceFactory(
        provideMediaSourceFactory(
            context,
            downloadCache,
            playerCache,
            streamRepository,
            dataStoreManager,
            coroutineScope,
        ),
        dataStoreManager,
    )

@UnstableApi
private fun provideRendererFactory(context: Context): DefaultRenderersFactory =
    object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink =
            DefaultAudioSink
                .Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        emptyArray(),
                        SilenceSkippingAudioProcessor(
                            2_000_000,
                            (20_000 / 2_000_000).toFloat(),
                            2_000_000,
                            0,
                            256,
                        ),
                        SonicAudioProcessor(),
                    ),
                ).build()
    }

@UnstableApi
private fun provideCacheDataSource(
    downloadCache: SimpleCache,
    playerCache: SimpleCache,
    context: Context,
    proxy: Proxy? = null,
): CacheDataSource.Factory =
    CacheDataSource
        .Factory()
        .setCache(downloadCache)
        .setUpstreamDataSourceFactory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    DefaultDataSource
                        .Factory(
                            context,
                            OkHttpDataSource.Factory(
                                OkHttpClient
                                    .Builder()
                                    .proxy(
                                        proxy,
                                    ).addInterceptor(
                                        HttpLoggingInterceptor()
                                            .apply {
                                                level = HttpLoggingInterceptor.Level.HEADERS
                                            },
                                    ).build(),
                            ),
                        ),
                ),
        ).setCacheWriteDataSinkFactory(null)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

@UnstableApi
private fun provideLoadControl(): LoadControl =
    DefaultLoadControl
        .Builder()
        .setBufferDurationsMs(
            DEFAULT_MIN_BUFFER_MS * 4,
            DEFAULT_MAX_BUFFER_MS * 4,
            // bufferForPlaybackMs=
            0,
            // bufferForPlaybackAfterRebufferMs=
            0,
        ).build()

@UnstableApi
private fun provideAudioAttributes(): AudioAttributes =
    AudioAttributes
        .Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

@UnstableApi
private fun provideDatabaseProvider(context: Context) = StandaloneDatabaseProvider(context)

@UnstableApi
private fun provideSimpleCache(
    context: Context,
    cacheName: String,
    cacheSize: Int = -1,
    databaseProvider: DatabaseProvider,
) = SimpleCache(
    context.filesDir.resolve(cacheName),
    when (cacheSize) {
        -1 -> NoOpCacheEvictor()
        else -> LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L)
    },
    databaseProvider,
)

@UnstableApi
private fun provideCoilBitmapLoader(
    context: Context,
    coroutineScope: CoroutineScope,
): CoilBitmapLoader = CoilBitmapLoader(context, coroutineScope)

@OptIn(UnstableApi::class)
fun loadMediaService() {
    loadKoinModules(mediaServiceModule)
}

@OptIn(UnstableApi::class)
fun startService(
    context: Context,
    serviceConnection: ServiceConnection,
) {
    val intent = Intent(context, SimpleMediaService::class.java)
    context.startService(intent)
    context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    Logger.d("Service", "Service started")
}

@OptIn(UnstableApi::class)
fun stopService(context: Context) {
    context.stopService(Intent(context, SimpleMediaService::class.java))
}

@OptIn(UnstableApi::class)
fun setServiceActivitySession(
    context: Context,
    cls: Class<out Activity>,
    musicService: IBinder?,
) {
    (musicService as? SimpleMediaService.MusicBinder)?.setActivitySession(context, cls)
}