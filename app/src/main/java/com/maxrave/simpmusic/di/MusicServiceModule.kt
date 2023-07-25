package com.maxrave.simpmusic.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.maxrave.simpmusic.service.SimpleMediaNotificationManager
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.service.test.source.MusicSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.components.SingletonComponent
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
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
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

    @Provides
    @Singleton
    @UnstableApi
    fun provideMediaSourceFactory(
        @ApplicationContext context: Context,
        cacheDataSourceFactory: CacheDataSource.Factory
    ): DefaultMediaSourceFactory =
        DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory)

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
            .setHandleAudioBecomingNoisy(true)
            .setTrackSelector(DefaultTrackSelector(context))
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context,
        player: ExoPlayer
    ): SimpleMediaNotificationManager =
        SimpleMediaNotificationManager(
            context = context,
            player = player
        )

    @Provides
    @Singleton
    fun provideMediaSession(
        @ApplicationContext context: Context,
        player: ExoPlayer
    ): MediaSession =
        MediaSession.Builder(context, player)
            .build()

//    @Provides
//    @Singleton
//    fun provideMediaLibrarySession(@ApplicationContext context: Context): MediaLibrarySession =
//        MediaLibrarySession.Builder().build()

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Provides
    @Singleton
    fun provideServiceHandler(
        player: ExoPlayer,
        @ApplicationContext context: Context
    ): SimpleMediaServiceHandler =
        SimpleMediaServiceHandler(
            player = player
        )

    @Provides
    @Singleton
    fun provideMusicSource(
        @ApplicationContext context: Context,
        simpleMediaServiceHandler: SimpleMediaServiceHandler
    ): MusicSource =
        MusicSource(context, simpleMediaServiceHandler)
}