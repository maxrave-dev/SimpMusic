package com.maxrave.media3.di

import android.app.Activity
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
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
import com.maxrave.common.StreamHealthRegistry
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
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.net.Proxy
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

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
                    provideLoadControl(androidContext()),
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
    context: Context,
    cacheDataSourceFactory: CacheDataSource.Factory,
    okHttpClient: OkHttpClient,
    downloadCache: SimpleCache,
    playerCache: SimpleCache,
    dataStoreManager: DataStoreManager,
    streamRepository: StreamRepository,
    coroutineScope: CoroutineScope,
): DataSource.Factory {
    val isWatch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
    // Smaller chunks improve time-to-first-audio on WearOS (slower radios/CPU),
    // at the cost of more HTTP range requests.
    val chunkLength = 1 * 1024 * 1024L
    // Avoid spawning infinite background refreshes if ExoPlayer retries aggressively.
    val refreshThrottleMs = 15_000L
    val lastRefreshAtMs = ConcurrentHashMap<String, Long>()

    fun maybeKickRefresh(
        mediaId: String,
        id: String,
        isVideo: Boolean,
        existingFormat: com.maxrave.domain.data.entities.NewFormatEntity?,
    ) {
        val now = System.currentTimeMillis()
        val last = lastRefreshAtMs[mediaId]
        if (last != null && now - last < refreshThrottleMs) return
        lastRefreshAtMs[mediaId] = now

        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                // Mark cached URL as expired so the next resolve doesn't keep re-trying it.
                if (existingFormat != null) {
                    streamRepository.insertNewFormat(
                        existingFormat.copy(expiredTime = LocalDateTime.now().minusSeconds(10)),
                    )
                }
                // Trigger a fresh InnerTube player resolve; this will upsert a new_format row.
                streamRepository.getStream(dataStoreManager, id, isVideo).lastOrNull()
            }.onFailure {
                Logger.e("Stream", "Background refresh failed for $mediaId: ${it.message}")
            }
        }
    }

    // If we have a cached (signed) videoplayback URL, validate it quickly before handing it to ExoPlayer.
    // These URLs can become invalid when the network changes (IP binding), even if `expiresInSeconds`
    // suggests they are still valid. If invalid, we re-resolve to get a fresh URL.
    val preflightClient =
        okHttpClient
            .newBuilder()
            .connectTimeout(if (isWatch) 3 else 8, TimeUnit.SECONDS)
            .readTimeout(if (isWatch) 3 else 8, TimeUnit.SECONDS)
            .writeTimeout(if (isWatch) 3 else 8, TimeUnit.SECONDS)
            .callTimeout(if (isWatch) 4 else 10, TimeUnit.SECONDS)
            .build()
    val preflightDecisionCache = ConcurrentHashMap<String, Pair<Long, Boolean>>()
    fun rememberResolvedCandidate(
        mediaId: String,
        url: String?,
    ) {
        if (url.isNullOrBlank()) return
        val itag = url.toHttpUrlOrNull()?.queryParameter("itag")?.toIntOrNull()
        StreamHealthRegistry.rememberCandidate(mediaId, itag, url)
    }

    fun preflightIsPlayable(url: String): Boolean {
        if (StreamHealthRegistry.isUrlBlocked(url)) {
            Logger.w("Stream", "Preflight rejected previously bad URL")
            return false
        }

        val now = System.currentTimeMillis()
        preflightDecisionCache[url]?.let { cached ->
            if (cached.first > now) return cached.second
        }

        val decision =
            runCatching {
            val req =
                Request
                    .Builder()
                    .url(url)
                    .header("Accept-Encoding", "identity")
                    // Keep preflight tiny to avoid false 403s on larger ranges.
                    .header("Range", "bytes=0-0")
                    .build()
            preflightClient.newCall(req).execute().use { resp ->
                when (resp.code) {
                    200, 206 -> true
                    403, 404, 410 -> {
                        Logger.w("Stream", "Preflight rejected URL: code=${resp.code}")
                        StreamHealthRegistry.markUrlFailure(url)
                        false
                    }
                    else -> {
                        // Treat inconclusive server responses as usable, then rely on Exo retries.
                        Logger.w("Stream", "Preflight inconclusive: code=${resp.code}")
                        true
                    }
                }
            }
        }.getOrElse { t ->
            // A timeout here does not guarantee the stream URL is bad. If we reject on every timeout,
            // we can churn fresh URL resolves and create worse startup latency on flaky links.
            Logger.w("Stream", "Preflight skipped due to ${t.javaClass.simpleName}: ${t.message}")
            true
        }

        if (decision) {
            StreamHealthRegistry.markUrlHealthy(url)
        } else {
            StreamHealthRegistry.markUrlFailure(url)
        }
        preflightDecisionCache[url] = (now + if (decision) 45_000L else 10_000L) to decision
        return decision
    }
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
            try {
                val resolveTimeoutMs = if (isWatch) 45_000L else 25_000L
                // Keep URL resolution bounded. A very long timeout leaves the watch in "buffering"
                // with no actionable feedback when a stream is unplayable.
                withTimeout(resolveTimeoutMs) {
            val allowSignedUrlReuse = !isWatch
            if (mediaId.contains(MERGING_DATA_TYPE.VIDEO)) {
                val id = mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                if (allowSignedUrlReuse) {
                    streamRepository.getNewFormat(id).lastOrNull()?.let { fmt ->
                        val videoUrl = fmt.videoUrl
                        if (videoUrl != null && fmt.expiredTime > LocalDateTime.now()) {
                            rememberResolvedCandidate(mediaId, videoUrl)
                            Logger.d("Stream", videoUrl)
                            Logger.w("Stream", "Video from format")
                            if (preflightIsPlayable(videoUrl)) {
                                dataSpecReturn =
                                    dataSpec
                                        .withUri(videoUrl.toUri())
                                return@withTimeout
                            } else {
                                Logger.w("Stream", "Cached video URL rejected by preflight; refreshing")
                                runCatching {
                                    streamRepository.insertNewFormat(
                                        fmt.copy(expiredTime = LocalDateTime.now().minusSeconds(10)),
                                    )
                                }.onFailure {
                                    Logger.e("Stream", "Failed to expire cached video URL: ${it.message}")
                                }
                                maybeKickRefresh(mediaId = mediaId, id = id, isVideo = true, existingFormat = fmt)
                            }
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
                        rememberResolvedCandidate(mediaId, it)
                        Logger.d("Stream", it)
                        Logger.w("Stream", "Video")
                        if (preflightIsPlayable(it)) {
                            dataSpecReturn = dataSpec.withUri(it.toUri())
                        } else {
                            Logger.w("Stream", "Freshly resolved video URL failed preflight")
                            maybeKickRefresh(mediaId = mediaId, id = id, isVideo = true, existingFormat = null)
                        }
                    }
            } else {
                if (allowSignedUrlReuse) {
                    streamRepository.getNewFormat(mediaId).lastOrNull()?.let { fmt ->
                        val audioUrl = fmt.audioUrl
                        if (audioUrl != null && fmt.expiredTime > LocalDateTime.now()) {
                            rememberResolvedCandidate(mediaId, audioUrl)
                            Logger.d("Stream", audioUrl)
                            Logger.w("Stream", "Audio from format")
                            if (preflightIsPlayable(audioUrl)) {
                                dataSpecReturn =
                                    dataSpec
                                        .withUri(audioUrl.toUri())
                                return@withTimeout
                            } else {
                                Logger.w("Stream", "Cached audio URL rejected by preflight; refreshing")
                                runCatching {
                                    streamRepository.insertNewFormat(
                                        fmt.copy(expiredTime = LocalDateTime.now().minusSeconds(10)),
                                    )
                                }.onFailure {
                                    Logger.e("Stream", "Failed to expire cached audio URL: ${it.message}")
                                }
                                maybeKickRefresh(mediaId = mediaId, id = mediaId, isVideo = false, existingFormat = fmt)
                            }
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
                        rememberResolvedCandidate(mediaId, it)
                        Logger.d("Stream", it)
                        Logger.w("Stream", "Audio")
                        if (preflightIsPlayable(it)) {
                            dataSpecReturn = dataSpec.withUri(it.toUri())
                        } else {
                            Logger.w("Stream", "Freshly resolved audio URL failed preflight")
                            maybeKickRefresh(mediaId = mediaId, id = mediaId, isVideo = false, existingFormat = null)
                        }
                    }
            }
                }
            } catch (t: Throwable) {
                throw IOException("Failed to resolve stream URL for $mediaId", t)
            }
        }
        // If resolution failed, don't fall back to FileDataSource (which will try to open `kJQP7...` as a file).
        val scheme = dataSpecReturn.uri.scheme
        if (scheme != "http" && scheme != "https") {
            throw IOException("Failed to resolve stream URL for $mediaId")
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
    run {
        val okHttpClient = provideOkHttpClient(context, dataStoreManager.getJVMProxy())

        DefaultMediaSourceFactory(
            provideResolvingDataSourceFactory(
                context = context,
                provideCacheDataSource(
                    downloadCache,
                    playerCache,
                    context,
                    okHttpClient,
                ),
                okHttpClient,
                downloadCache,
                playerCache,
                dataStoreManager,
                streamRepository,
                coroutineScope,
            ),
            provideExtractorFactory(),
        )
    }

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
    okHttpClient: OkHttpClient,
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
                                okHttpClient,
                            ),
                        ),
                ),
        ).setCacheWriteDataSinkFactory(null)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

@UnstableApi
private fun provideOkHttpClient(
    context: Context,
    proxy: Proxy? = null,
): OkHttpClient {
    val isWatch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
    data class YouTubeHttpHeaders(
        val userAgent: String,
        val origin: String? = null,
        val referer: String? = null,
    )

    fun headersForClient(clientId: String?): YouTubeHttpHeaders {
        return when (clientId?.uppercase()) {
            "IOS" ->
                YouTubeHttpHeaders(
                    userAgent = "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)",
                    origin = "https://music.youtube.com",
                    referer = "https://music.youtube.com/",
                )

            "ANDROID", "ANDROID_MUSIC" ->
                YouTubeHttpHeaders(
                    userAgent =
                        "com.google.android.apps.youtube.music/7.03.53 (Linux; U; Android 14; en_US; Pixel 7 Pro Build/UQ1A.240205.002)",
                )

            "ANDROID_VR" ->
                YouTubeHttpHeaders(
                    userAgent =
                        "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; en_US; Quest 3) gzip",
                )

            "TVHTML5" ->
                YouTubeHttpHeaders(
                    userAgent =
                        "Mozilla/5.0 (SMART-TV; Linux; Tizen 6.0) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/3.0 TV Safari/537.36",
                )

            "WEB", "WEB_REMIX", "MWEB" ->
                YouTubeHttpHeaders(
                    userAgent =
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    origin = "https://music.youtube.com",
                    referer = "https://music.youtube.com/",
                )

            else ->
                YouTubeHttpHeaders(
                    userAgent =
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    origin = "https://music.youtube.com",
                    referer = "https://music.youtube.com/",
                )
        }
    }

    return OkHttpClient
        .Builder()
        // Wireless/Bluetooth tethering can be spiky; avoid aggressive timeouts.
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        // Many YouTube `googlevideo.com/videoplayback` URLs are tied to the client UA (e.g. `c=IOS`).
        // Without a matching UA, playback can 403.
        .addInterceptor { chain ->
            val source = chain.request()
            val ytClient = source.url.queryParameter("c")
            val headers = headersForClient(ytClient)
            val builder =
                source
                    .newBuilder()
                    // Some servers reject this header (it is meant for Icecast/Shoutcast-style metadata).
                    // ExoPlayer may add it automatically; strip it for YouTube googlevideo requests.
                    .removeHeader("Icy-MetaData")
                    .removeHeader("Origin")
                    .removeHeader("Referer")
                    .header("User-Agent", headers.userAgent)
            headers.origin?.let { builder.header("Origin", it) }
            headers.referer?.let { builder.header("Referer", it) }
            val req =
                builder.build()
            val resp = chain.proceed(req)
            if (req.url.host.contains("googlevideo.com", ignoreCase = true) && resp.code == 403) {
                Logger.w(
                    "Stream",
                    "Runtime 403 host=${req.url.host} c=${req.url.queryParameter("c")} itag=${req.url.queryParameter("itag")}",
                )
                StreamHealthRegistry.markUrlFailure(req.url.toString())
            }
            resp
        }.apply {
            if (proxy != null) proxy(proxy)
            // OkHttp header logging on WearOS is expensive; keep it quiet.
            if (!isWatch) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS },
                )
            }
        }.build()
}

@UnstableApi
private fun provideLoadControl(context: Context): LoadControl {
    val isWatch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
    return if (isWatch) {
        DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(
                30_000,
                120_000,
                // Keep a small startup buffer to reduce audio micro-stutters on watches.
                2_500,
                5_000,
            ).build()
    } else {
        DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(
                DEFAULT_MIN_BUFFER_MS * 4,
                DEFAULT_MAX_BUFFER_MS * 4,
                0,
                0,
            ).build()
    }
}

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
    // On modern Android (and on some WearOS builds), starting a background service can throw
    // BackgroundServiceStartNotAllowedException even from Activity lifecycles under certain
    // conditions (e.g. OS "freezing"/offload). Binding with BIND_AUTO_CREATE is sufficient to
    // create the service for in-app playback control, so avoid crashing if startService is blocked.
    runCatching {
        context.startService(intent)
    }.onFailure {
        Logger.w("Service", "startService blocked: ${it.javaClass.simpleName}: ${it.message}")
    }

    runCatching {
        context.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }.onFailure {
        Logger.e("Service", "bindService failed: ${it.javaClass.simpleName}: ${it.message}")
    }
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
