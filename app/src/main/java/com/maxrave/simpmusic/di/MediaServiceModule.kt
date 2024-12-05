package com.maxrave.simpmusic.di

import android.content.Context
import android.util.Log
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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import com.maxrave.simpmusic.common.Config.CANVAS_CACHE
import com.maxrave.simpmusic.common.Config.DOWNLOAD_CACHE
import com.maxrave.simpmusic.common.Config.PLAYER_CACHE
import com.maxrave.simpmusic.common.Config.SERVICE_SCOPE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.service.SimpleMediaSessionCallback
import com.maxrave.simpmusic.service.test.CoilBitmapLoader
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.service.test.source.MergingMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

@UnstableApi
val mediaServiceModule =
    module {
        // Cache
        single<DatabaseProvider>(createdAtStart = true) {
            StandaloneDatabaseProvider(androidContext())
        }
        // Player Cache
        single<SimpleCache>(createdAtStart = true, qualifier = named(PLAYER_CACHE)) {
            SimpleCache(
                androidContext().filesDir.resolve("exoplayer"),
                when (val cacheSize = runBlocking { get<DataStoreManager>().maxSongCacheSize.first() }) {
                    -1 -> NoOpCacheEvictor()
                    else -> LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L)
                },
                get<DatabaseProvider>(),
            )
        }
        // Download Cache
        single<SimpleCache>(createdAtStart = true, qualifier = named(DOWNLOAD_CACHE)) {
            SimpleCache(
                androidContext().filesDir.resolve("download"),
                NoOpCacheEvictor(),
                get<DatabaseProvider>(),
            )
        }
        // Spotify Canvas Cache
        single<SimpleCache>(createdAtStart = true, qualifier = named(CANVAS_CACHE)) {
            SimpleCache(
                androidContext().filesDir.resolve("spotifyCanvas"),
                NoOpCacheEvictor(),
                get<DatabaseProvider>(),
            )
        }
        // MediaSession Callback for main player
        single(createdAtStart = true) {
            SimpleMediaSessionCallback(androidContext(), get<MainRepository>())
        }
        // DownloadUtils
        single(createdAtStart = true) {
            DownloadUtils(
                context = androidContext(),
                playerCache = get(named(PLAYER_CACHE)),
                downloadCache = get(named(DOWNLOAD_CACHE)),
                mainRepository = get(),
                databaseProvider = get(),
            )
        }

        // Service
        // CoroutineScope for service
        single<CoroutineScope>(createdAtStart = true, qualifier = named(SERVICE_SCOPE)) {
            CoroutineScope(Dispatchers.Main + SupervisorJob())
        }

        // AudioAttributes
        single<AudioAttributes>(createdAtStart = true) {
            AudioAttributes
                .Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
        }

        // ExoPlayer
        single<ExoPlayer>(createdAtStart = true) {
            ExoPlayer
                .Builder(androidContext())
                .setAudioAttributes(get(), true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setHandleAudioBecomingNoisy(true)
                .setSeekForwardIncrementMs(5000)
                .setSeekBackIncrementMs(5000)
                .setMediaSourceFactory(
                    provideMergingMediaSource(
                        androidContext(),
                        get(named(DOWNLOAD_CACHE)),
                        get(named(PLAYER_CACHE)),
                        get(),
                        get(named(SERVICE_SCOPE)),
                        get(),
                    ),
                ).setRenderersFactory(provideRendererFactory(androidContext()))
                .build().also {
                    it.addAnalyticsListener(EventLogger())
                }
        }
        // CoilBitmapLoader
        single<CoilBitmapLoader>(createdAtStart = true) {
            provideCoilBitmapLoader(androidContext(), get(named(SERVICE_SCOPE)))
        }

        // MediaSessionCallback
        single<SimpleMediaSessionCallback>(createdAtStart = true) {
            SimpleMediaSessionCallback(
                androidContext(),
                get(),
            )
        }
        // MediaServiceHandler
        single<SimpleMediaServiceHandler>(createdAtStart = true) {
            SimpleMediaServiceHandler(
                player = get(),
                dataStoreManager = get(),
                mainRepository = get(),
                mediaSessionCallback = get(),
                context = androidContext(),
                coroutineScope = get(named(SERVICE_SCOPE)),
            )
        }
    }

@UnstableApi
private fun provideResolvingDataSourceFactory(
    cacheDataSourceFactory: CacheDataSource.Factory,
    downloadCache: SimpleCache,
    playerCache: SimpleCache,
    mainRepository: MainRepository,
    coroutineScope: CoroutineScope,
): DataSource.Factory {
    val CHUNK_LENGTH = 512 * 1024L
    return ResolvingDataSource.Factory(cacheDataSourceFactory) { dataSpec ->
        val mediaId = dataSpec.key ?: error("No media id")
        Log.w("Stream", mediaId)
        Log.w("Stream", mediaId.startsWith(MergingMediaSourceFactory.isVideo).toString())
        val length = if (dataSpec.length >= 0) dataSpec.length else 1
        if (downloadCache.isCached(
                mediaId,
                dataSpec.position,
                length,
            )) {
            coroutineScope.launch(Dispatchers.IO) {
                mainRepository.updateFormat(
                    if (mediaId.contains(MergingMediaSourceFactory.isVideo)) {
                        mediaId.removePrefix(MergingMediaSourceFactory.isVideo)
                    } else {
                        mediaId
                    },
                )
            }
            Log.w("Stream", "Downloaded $mediaId")
            return@Factory dataSpec
        }
        if (playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)) {
            coroutineScope.launch(Dispatchers.IO) {
                mainRepository.updateFormat(
                    if (mediaId.contains(MergingMediaSourceFactory.isVideo)) {
                        mediaId.removePrefix(MergingMediaSourceFactory.isVideo)
                    } else {
                        mediaId
                    },
                )
            }
            Log.w("Stream", "Cached $mediaId")
            return@Factory dataSpec
        }
        var dataSpecReturn: DataSpec = dataSpec
        runBlocking(Dispatchers.IO) {
            if (mediaId.contains(MergingMediaSourceFactory.isVideo)) {
                val id = mediaId.removePrefix(MergingMediaSourceFactory.isVideo)
                mainRepository
                    .getStream(
                        id,
                        true,
                    ).singleOrNull()?.let {
                        dataSpecReturn = dataSpec.withUri(it.toUri()).subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
                    }
            } else {
                mainRepository
                    .getStream(
                        mediaId,
                        isVideo = false,
                    ).singleOrNull()
                    ?.let {
                        dataSpecReturn = dataSpec.withUri(it.toUri()).subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
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
            androidx.media3.extractor.mp4.Mp4Extractor(
                DefaultSubtitleParserFactory(),
            ),
        )
    }

@UnstableApi
private fun provideMediaSourceFactory(
    context: Context,
    downloadCache: SimpleCache,
    playerCache: SimpleCache,
    mainRepository: MainRepository,
    coroutineScope: CoroutineScope,
): DefaultMediaSourceFactory =
    DefaultMediaSourceFactory(
        provideResolvingDataSourceFactory(
            provideCacheDataSource(downloadCache, playerCache, context),
            downloadCache,
            playerCache,
            mainRepository,
            coroutineScope,
        ),
        provideExtractorFactory(),
    )

@OptIn(UnstableApi::class)
private fun provideMergingMediaSource(
    context: Context,
    downloadCache: SimpleCache,
    playerCache: SimpleCache,
    mainRepository: MainRepository,
    coroutineScope: CoroutineScope,
    dataStoreManager: DataStoreManager,
): MergingMediaSourceFactory =
    MergingMediaSourceFactory(
        provideMediaSourceFactory(
            context,
            downloadCache,
            playerCache,
            mainRepository,
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
private fun provideCoilBitmapLoader(
    context: Context,
    coroutineScope: CoroutineScope,
): CoilBitmapLoader = CoilBitmapLoader(context, coroutineScope)

@UnstableApi
private fun provideCacheDataSource(
    downloadCache: SimpleCache,
    playerCache: SimpleCache,
    context: Context
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
                                OkHttpClient.Builder()
                                    .build()
                            )
                        )
                ),
        ).setCacheWriteDataSinkFactory(null)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)