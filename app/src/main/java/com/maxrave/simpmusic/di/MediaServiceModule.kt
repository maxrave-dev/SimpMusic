package com.maxrave.simpmusic.di

import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.maxrave.simpmusic.common.Config.CANVAS_CACHE
import com.maxrave.simpmusic.common.Config.DOWNLOAD_CACHE
import com.maxrave.simpmusic.common.Config.PLAYER_CACHE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.service.SimpleMediaSessionCallback
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    }