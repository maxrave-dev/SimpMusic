package com.maxrave.simpmusic.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.MediaSession
import com.maxrave.simpmusic.common.QUALITY
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.service.SimpleMediaSessionCallback
import com.maxrave.simpmusic.service.test.source.MusicSource
import com.maxrave.simpmusic.ui.MainActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCache

@Module
@InstallIn(SingletonComponent::class)
@UnstableApi
object MusicServiceModule {
    @Singleton
    @Provides
    fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider =
        StandaloneDatabaseProvider(context)

    @Singleton
    @Provides
    @PlayerCache
    fun providePlayerCache(@ApplicationContext context: Context, databaseProvider: DatabaseProvider): SimpleCache =
        SimpleCache(
            context.filesDir.resolve("exoplayer"),
            NoOpCacheEvictor(),
            databaseProvider
        )

    @Singleton
    @Provides
    @DownloadCache
    fun provideDownloadCache(@ApplicationContext context: Context, databaseProvider: DatabaseProvider): SimpleCache =
        SimpleCache(context.filesDir.resolve("download"), NoOpCacheEvictor(), databaseProvider)

    @Provides
    @Singleton
    fun provideAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

//    @Provides
//    @Singleton
//    @UnstableApi
//    fun provideDataSource(@ApplicationContext context: Context): DefaultMediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(
//        DefaultHttpDataSource.Factory()
//            .setAllowCrossProtocolRedirects(true)
//            .setUserAgent("Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
//            .setConnectTimeoutMs(5000)
//    )

    @Provides
    @Singleton
    @UnstableApi
    fun provideCacheDataSource(@DownloadCache downloadCache: SimpleCache, @PlayerCache playerCache: SimpleCache): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource.Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
                        .setConnectTimeoutMs(5000))
            )
            .setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)
    }

//    @Provides
//    @Singleton
//    @UnstableApi
//    fun provideMediaSourceFactory(
//        @ApplicationContext context: Context,
//        cacheDataSourceFactory: CacheDataSource.Factory
//    ): DefaultMediaSourceFactory =
//        DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory)

    @Provides
    @Singleton
    @UnstableApi
    fun provideResolvingDataSourceFactory(
        cacheDataSourceFactory: CacheDataSource.Factory,
        @DownloadCache downloadCache: SimpleCache, @PlayerCache playerCache: SimpleCache, mainRepository: MainRepository, dataStoreManager: DataStoreManager
    ): DataSource.Factory {
        return ResolvingDataSource.Factory(cacheDataSourceFactory) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val CHUNK_LENGTH = 512 * 1024L
            if (downloadCache.isCached(mediaId, dataSpec.position,  if (dataSpec.length >= 0) dataSpec.length else 1) || playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)) {
                return@Factory dataSpec
            }
            var dataSpecReturn: DataSpec? = null
            runBlocking {
                val itag = dataStoreManager.quality.first()

                mainRepository.getStream(mediaId, if (itag == QUALITY.items[0].toString()) QUALITY.itags[0] else QUALITY.itags[1]).collect {
                    if (it != null) {
                        dataSpecReturn = dataSpec.withUri(it.toUri())
                    }
                }
            }
            return@Factory dataSpecReturn!!
        }
    }

    @Provides
    @Singleton
    @UnstableApi
    fun provideMediaSourceFactory(
        dataSourceFactory: DataSource.Factory
    ): DefaultMediaSourceFactory =
        DefaultMediaSourceFactory(
            dataSourceFactory
        ) {
            arrayOf(MatroskaExtractor(), FragmentedMp4Extractor())
        }

    @Provides
    @Singleton
    @UnstableApi
    fun providePlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes,
        mediaSourceFactory: DefaultMediaSourceFactory
    ): ExoPlayer =
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .setSeekForwardIncrementMs(5000)
            .setSeekBackIncrementMs(5000)
            .setTrackSelector(DefaultTrackSelector(context))
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

    @Provides
    @Singleton
    fun provideMediaSessionCallback() : SimpleMediaSessionCallback = SimpleMediaSessionCallback()

    @Provides
    @Singleton
    fun provideMediaSession(
        @ApplicationContext context: Context,
        player: ExoPlayer,
        callback: SimpleMediaSessionCallback
    ): MediaSession =
        MediaSession.Builder(context, player)
            .setCallback(callback)
            .setSessionActivity(
                PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()


    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Provides
    @Singleton
    fun provideServiceHandler(
        player: ExoPlayer,
        dataStoreManager: DataStoreManager,
        mainRepository: MainRepository,
        @ApplicationContext context: Context,
        mediaSession: MediaSession,
        mediaSessionCallback: SimpleMediaSessionCallback
    ): SimpleMediaServiceHandler =
        SimpleMediaServiceHandler(
            player = player,
            dataStoreManager = dataStoreManager,
            mainRepository = mainRepository,
            context = context,
            mediaSession = mediaSession,
            mediaSessionCallback = mediaSessionCallback
        )

    @Provides
    @Singleton
    fun provideMusicSource(
        simpleMediaServiceHandler: SimpleMediaServiceHandler, dataStoreManager: DataStoreManager, mainRepository: MainRepository
    ): MusicSource =
        MusicSource(simpleMediaServiceHandler, dataStoreManager, mainRepository)
}