package com.maxrave.simpmusic.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.service.SimpleMediaSessionCallback
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
    fun providePlayerCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
        dataStoreManager: DataStoreManager
    ): SimpleCache =
        SimpleCache(
            context.filesDir.resolve("exoplayer"),
            when (val cacheSize = runBlocking { dataStoreManager.maxSongCacheSize.first() }) {
                -1 -> NoOpCacheEvictor()
                else -> LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L)
            },
            databaseProvider
        )

    @Singleton
    @Provides
    @DownloadCache
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider
    ): SimpleCache =
        SimpleCache(context.filesDir.resolve("download"), NoOpCacheEvictor(), databaseProvider)


    @Provides
    @Singleton
    fun provideMediaSessionCallback(
        @ApplicationContext context: Context,
        mainRepository: MainRepository
    ): SimpleMediaSessionCallback = SimpleMediaSessionCallback(context, mainRepository)
}