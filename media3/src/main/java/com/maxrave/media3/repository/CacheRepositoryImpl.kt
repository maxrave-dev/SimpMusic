package com.maxrave.media3.repository

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import com.maxrave.common.Config
import com.maxrave.domain.repository.CacheRepository

@UnstableApi
internal class CacheRepositoryImpl(
    private val playerCache: SimpleCache,
    private val downloadCache: SimpleCache,
    private val canvasCache: SimpleCache,
) : CacheRepository {
    override suspend fun getCacheSize(cacheName: String): Long =
        when (cacheName) {
            Config.PLAYER_CACHE -> {
                playerCache.cacheSpace
            }
            Config.DOWNLOAD_CACHE -> {
                downloadCache.cacheSpace
            }
            Config.CANVAS_CACHE -> {
                canvasCache.cacheSpace
            }
            else -> 0L
        }

    override fun clearCache(cacheName: String) {
        when (cacheName) {
            Config.PLAYER_CACHE -> {
                playerCache.keys.forEach { key ->
                    playerCache.removeResource(key)
                }
            }
            Config.DOWNLOAD_CACHE -> {
                downloadCache.keys.forEach { key ->
                    downloadCache.removeResource(key)
                }
            }
            Config.CANVAS_CACHE -> {
                canvasCache.keys.forEach { key ->
                    canvasCache.removeResource(key)
                }
            }
            else -> { /* no-op */ }
        }
    }

    override suspend fun getAllCacheKeys(cacheName: String): List<String> =
        when (cacheName) {
            Config.PLAYER_CACHE -> {
                playerCache.keys.toList()
            }
            Config.DOWNLOAD_CACHE -> {
                downloadCache.keys.toList()
            }
            Config.CANVAS_CACHE -> {
                canvasCache.keys.toList()
            }
            else -> emptyList()
        }
}