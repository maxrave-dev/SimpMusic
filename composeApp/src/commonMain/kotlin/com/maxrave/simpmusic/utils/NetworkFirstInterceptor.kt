package com.maxrave.simpmusic.utils

import coil3.ImageLoader
import coil3.annotation.InternalCoilApi
import coil3.disk.DiskCache
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.network.CacheNetworkResponse
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.maxrave.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * HTTP(S) images use cache for 24 hours, then try the network with stale-cache fallback.
 * Expiry never deletes an image, and a failed refresh never renews its TTL.
 *
 * Coil writes network responses to disk before decoding them, including some HTTP failures.
 * Stage each response under a temporary key in the SAME disk cache and promote it only after
 * decoding succeeds. This keeps a 404 or an undecodable 200 from replacing the offline fallback.
 */
class NetworkFirstInterceptor(
    private val ttlMillis: Long = 24L * 60 * 60 * 1000,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val imageLoader: () -> ImageLoader,
) : Interceptor {
    init {
        require(ttlMillis >= 0) { "Image cache TTL must not be negative" }
    }

    private class RequestLock {
        val mutex = Mutex()
        var users = 0
    }

    private val requestLocks = mutableMapOf<String, RequestLock>()
    private val registryMutex = Mutex()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val url = request.data.toString()
        if ((!url.startsWith("https://", ignoreCase = true) && !url.startsWith("http://", ignoreCase = true)) ||
            !request.networkCachePolicy.readEnabled
        ) {
            return chain.proceed()
        }

        val diskKey = request.diskCacheKey ?: url
        // Serialize only requests for the same disk entry so promotion and RAM updates agree.
        // Remove the lock after its last user; browsing new URLs must not grow a permanent map.
        val requestLock = registryMutex.withLock {
            requestLocks.getOrPut(diskKey) { RequestLock() }.also { it.users++ }
        }
        try {
            return requestLock.mutex.withLock {
                loadWithTtl(chain, request, diskKey)
            }
        } finally {
            withContext(NonCancellable) {
                registryMutex.withLock {
                    if (--requestLock.users == 0) requestLocks.remove(diskKey)
                }
            }
        }
    }

    private suspend fun loadWithTtl(
        chain: Interceptor.Chain,
        request: ImageRequest,
        diskKey: String,
    ): ImageResult {
        val loader = imageLoader()
        val diskCache = loader.diskCache
        val cacheRequest = request.newBuilder()
            .memoryCachePolicy(
                when {
                    !request.memoryCachePolicy.readEnabled -> CachePolicy.DISABLED
                    request.memoryCachePolicy.writeEnabled -> CachePolicy.ENABLED
                    else -> CachePolicy.READ_ONLY
                },
            )
            .diskCachePolicy(if (request.diskCachePolicy.readEnabled) CachePolicy.READ_ONLY else CachePolicy.DISABLED)
            .networkCachePolicy(CachePolicy.DISABLED)
            .build()
        val cachedResult = chain.withRequest(cacheRequest).proceed()
        if (cachedResult is SuccessResult) {
            val cachedValue = cachedResult.memoryCacheKey?.let { loader.memoryCache?.get(it) }
            val cachedAtMillis =
                (cachedValue?.takeIf { it.image === cachedResult.image }?.extras?.get(CACHED_AT_KEY) as? Long)
                    ?: readCachedAt(diskCache, cachedResult.diskCacheKey ?: diskKey)
            if (cachedAtMillis != null) {
                // Warming RAM from disk must carry the original age, not start a new TTL.
                updateMemoryEntry(loader.memoryCache, cachedResult, cachedAtMillis = cachedAtMillis)
            }
            if (isFresh(cachedAtMillis)) return cachedResult.copy(request = request)
        }

        // Keep a successfully decoded stale image alive while refreshing. A failed refresh can
        // return it even if another request evicts its disk entry in the meantime.
        val temporaryKey = "coil-network-first:${UUID.randomUUID()}"
        val networkRequest = request.newBuilder()
            .memoryCachePolicy(if (request.memoryCachePolicy.writeEnabled) CachePolicy.WRITE_ONLY else CachePolicy.DISABLED)
            .diskCachePolicy(if (request.diskCachePolicy.writeEnabled) CachePolicy.WRITE_ONLY else CachePolicy.DISABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCacheKey(temporaryKey)
            .build()

        try {
            val networkResult = chain.withRequest(networkRequest).proceed()
            if (networkResult is SuccessResult) {
                val cachedAtMillis =
                    readCachedAt(diskCache, networkResult.diskCacheKey ?: temporaryKey)?.takeIf { it > 0 }
                        ?: nowMillis()
                val savedDiskKey = if (networkResult.diskCacheKey == temporaryKey) {
                    if (diskCache != null && promote(diskCache, temporaryKey, diskKey)) diskKey else null
                } else {
                    networkResult.diskCacheKey
                }
                updateMemoryEntry(loader.memoryCache, networkResult, temporaryKey, savedDiskKey, cachedAtMillis)
                return networkResult.copy(request = request, diskCacheKey = savedDiskKey)
            }

            // Force the stale cache on failure without changing its age. Retry cache-only if it
            // was initially missing, in case another producer filled it during the network attempt.
            val fallbackResult = if (cachedResult is SuccessResult) cachedResult else chain.withRequest(cacheRequest).proceed()
            return if (fallbackResult is SuccessResult) {
                fallbackResult.copy(request = request)
            } else {
                // Keep the real network error, not the cache-only miss (usually HTTP 504).
                (networkResult as ErrorResult).copy(request = request)
            }
        } catch (cancelled: CancellationException) {
            // The engine may have written RAM just before cancellation prevented it returning a
            // result. Drop only entries still pointing at this request's soon-to-be-deleted file.
            val memoryCache = loader.memoryCache
            if (memoryCache != null) {
                memoryCache.keys.forEach { key ->
                    if (memoryCache[key]?.extras?.values?.any { it == temporaryKey } == true) {
                        memoryCache.remove(key)
                    }
                }
            }
            throw cancelled
        } finally {
            // Cancellation must propagate, but it must not leave completed staging entries behind.
            withContext(NonCancellable + Dispatchers.IO) {
                try {
                    diskCache?.remove(temporaryKey)
                } catch (error: Exception) {
                    Logger.w("Coil", "Could not remove temporary image cache entry (${error::class.simpleName})")
                }
            }
        }
    }

    private fun isFresh(cachedAtMillis: Long?): Boolean {
        if (cachedAtMillis == null || cachedAtMillis <= 0) return false
        val now = nowMillis()
        return now >= cachedAtMillis && now - cachedAtMillis < ttlMillis
    }

    @OptIn(InternalCoilApi::class)
    private suspend fun readCachedAt(diskCache: DiskCache?, key: String): Long? = withContext(Dispatchers.IO) {
        if (diskCache == null) return@withContext null
        try {
            diskCache.openSnapshot(key)?.use { snapshot ->
                diskCache.fileSystem.read(snapshot.metadata) { CacheNetworkResponse.readFrom(this).responseMillis }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            // Unknown/older metadata is stale, not grounds to delete its still-usable image.
            null
        }
    }

    private suspend fun promote(
        diskCache: DiskCache,
        temporaryKey: String,
        diskKey: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val snapshot = diskCache.openSnapshot(temporaryKey) ?: return@withContext false
            snapshot.use {
                val editor = diskCache.openEditor(diskKey) ?: return@withContext false
                try {
                    // Copy encoded bytes, not a recompressed bitmap: format and quality stay intact.
                    diskCache.fileSystem.copy(snapshot.metadata, editor.metadata)
                    diskCache.fileSystem.copy(snapshot.data, editor.data)
                    coroutineContext.ensureActive()
                    editor.commit()
                    true
                } catch (error: Exception) {
                    runCatching { editor.abort() }
                    throw error
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            // Cache write failure must not hide an image that was successfully downloaded.
            Logger.w("Coil", "Could not update image cache (${error::class.simpleName})")
            false
        }
    }

    private fun updateMemoryEntry(
        memoryCache: MemoryCache?,
        result: SuccessResult,
        temporaryKey: String? = null,
        savedDiskKey: String? = result.diskCacheKey,
        cachedAtMillis: Long? = null,
    ) {
        val memoryKey = result.memoryCacheKey ?: return
        val cache = memoryCache ?: return
        val value = cache[memoryKey] ?: return
        if (value.image !== result.image) return

        // Coil keeps the source disk key in value extras. Replace only our unique temporary value,
        // without depending on Coil's private extra names or altering sampling/size information.
        val extras = value.extras.toMutableMap()
        if (temporaryKey != null) {
            value.extras.forEach { (key, extra) ->
                if (extra == temporaryKey) {
                    if (savedDiskKey != null) extras[key] = savedDiskKey else extras.remove(key)
                }
            }
        }
        if (cachedAtMillis != null) extras[CACHED_AT_KEY] = cachedAtMillis
        cache[memoryKey] = MemoryCache.Value(value.image, extras)
    }

    private companion object {
        const val CACHED_AT_KEY = "simpmusic#image_response_millis"
    }
}
