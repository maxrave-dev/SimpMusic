package com.maxrave.simpmusic.utils

import coil3.Canvas
import coil3.Image
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.disk.DiskCache
import coil3.intercept.Interceptor
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Path.Companion.toPath
import java.io.IOException
import java.net.SocketTimeoutException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame

class NetworkFirstInterceptorTest {
    @Test
    fun memoryHitWithin24HoursDoesNotCallTheNetwork() = withFixture(ttlMillis = DAY_MS) { fixture ->
        fixture.prime()
        fixture.client.nowMillis += DAY_MS - 1
        fixture.client.body = "cover:new"

        val result = assertIs<SuccessResult>(fixture.load())

        assertEquals("old", assertIs<TestImage>(result.image).label)
        assertEquals(DataSource.MEMORY_CACHE, result.dataSource)
        assertEquals(0, fixture.client.requests.size)
        assertEquals("cover:old", fixture.diskBody())
        fixture.assertNoTemporaryEntries()
    }

    @Test
    fun freshDiskCacheKeepsItsAgeAfterMemoryIsCleared() = withFixture(ttlMillis = DAY_MS) { fixture ->
        fixture.prime()
        fixture.loader.memoryCache?.clear()
        fixture.client.nowMillis += DAY_MS - 1
        fixture.client.body = "cover:new"

        val diskResult = assertIs<SuccessResult>(fixture.load())

        assertEquals("old", assertIs<TestImage>(diskResult.image).label)
        assertEquals(DataSource.DISK, diskResult.dataSource)
        assertEquals(0, fixture.client.requests.size)

        // A disk hit must warm RAM without resetting the original response's TTL.
        fixture.client.nowMillis += 1
        val refreshedResult = assertIs<SuccessResult>(fixture.load())
        assertEquals("new", assertIs<TestImage>(refreshedResult.image).label)
        assertEquals(DataSource.NETWORK, refreshedResult.dataSource)
        assertEquals(1, fixture.client.requests.size)
        fixture.assertNoTemporaryEntries()
    }

    @Test
    fun cacheExpiresAt24HoursAndSuccessfulRefreshStartsANewTtl() = withFixture(ttlMillis = DAY_MS) { fixture ->
        fixture.prime()
        fixture.client.nowMillis += DAY_MS
        fixture.client.body = "cover:new"

        val refreshedResult = assertIs<SuccessResult>(fixture.load())

        assertEquals("new", assertIs<TestImage>(refreshedResult.image).label)
        assertEquals(DataSource.NETWORK, refreshedResult.dataSource)
        assertEquals(1, fixture.client.requests.size)
        assertEquals("cover:new", fixture.diskBody())

        fixture.client.requests.clear()
        fixture.client.nowMillis += DAY_MS - 1
        val cachedResult = assertIs<SuccessResult>(fixture.load())
        assertEquals("new", assertIs<TestImage>(cachedResult.image).label)
        assertEquals(DataSource.MEMORY_CACHE, cachedResult.dataSource)
        assertEquals(0, fixture.client.requests.size)
        fixture.assertNoTemporaryEntries()
    }

    @Test
    fun failedRefreshForcesExpiredCacheWithoutExtendingItsTtl() = withFixture(ttlMillis = DAY_MS) { fixture ->
        fixture.prime()
        fixture.loader.memoryCache?.clear()
        fixture.client.nowMillis += DAY_MS
        fixture.client.failure = IOException("offline")

        val fallbackResult = assertIs<SuccessResult>(fixture.load())

        assertEquals("old", assertIs<TestImage>(fallbackResult.image).label)
        assertEquals(DataSource.DISK, fallbackResult.dataSource)
        assertEquals(1, fixture.client.requests.size)
        assertEquals("cover:old", fixture.diskBody())

        fixture.client.requests.clear()
        fixture.client.nowMillis += 1
        fixture.client.failure = null
        fixture.client.body = "cover:new"
        val refreshedResult = assertIs<SuccessResult>(fixture.load())
        assertEquals("new", assertIs<TestImage>(refreshedResult.image).label)
        assertEquals(DataSource.NETWORK, refreshedResult.dataSource)
        assertEquals(1, fixture.client.requests.size)
        fixture.assertNoTemporaryEntries()
    }

    @Test
    fun networkReplacesAnImageAlreadyInMemoryAndOnDisk() = withFixture { fixture ->
        fixture.prime()
        fixture.client.body = "cover:new"

        val result = assertIs<SuccessResult>(fixture.load())

        assertEquals("new", assertIs<TestImage>(result.image).label)
        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(1, fixture.client.requests.size)
        assertEquals(URL, result.diskCacheKey)
        assertEquals("cover:new", fixture.diskBody())
        assertEquals(fixture.request, result.request)
        fixture.assertNoTemporaryEntries()

        fixture.loader.memoryCache?.clear()
        fixture.client.failure = IOException("offline")
        assertEquals("new", assertIs<TestImage>(assertIs<SuccessResult>(fixture.load()).image).label)
    }

    @Test
    fun offlineFallsBackToMemoryWithoutAnotherNetworkAttempt() = withFixture { fixture ->
        fixture.prime()
        fixture.client.failure = IOException("offline")

        val result = assertIs<SuccessResult>(fixture.load())

        assertEquals("old", assertIs<TestImage>(result.image).label)
        assertEquals(DataSource.MEMORY_CACHE, result.dataSource)
        assertEquals(URL, result.diskCacheKey)
        assertEquals(1, fixture.client.requests.size)
        assertEquals("cover:old", fixture.diskBody())
        fixture.assertNoTemporaryEntries()
    }

    @Test
    fun httpErrorsDoNotOverwriteTheDiskFallback() = withFixture { fixture ->
        fixture.prime()
        for (code in listOf(403, 404, 500)) {
            fixture.loader.memoryCache?.clear()
            fixture.client.requests.clear()
            fixture.client.code = code
            fixture.client.body = "error:$code"

            val result = assertIs<SuccessResult>(fixture.load())

            assertEquals("old", assertIs<TestImage>(result.image).label)
            assertEquals(DataSource.DISK, result.dataSource)
            assertEquals(1, fixture.client.requests.size)
            assertEquals("cover:old", fixture.diskBody())
            fixture.assertNoTemporaryEntries()
        }
    }

    @Test
    fun undecodableSuccessfulResponseDoesNotOverwriteTheDiskFallback() = withFixture { fixture ->
        fixture.prime()
        fixture.loader.memoryCache?.clear()
        fixture.client.body = "not an image"

        val result = assertIs<SuccessResult>(fixture.load())

        assertEquals("old", assertIs<TestImage>(result.image).label)
        assertEquals(DataSource.DISK, result.dataSource)
        assertEquals(1, fixture.client.requests.size)
        assertEquals("cover:old", fixture.diskBody())
        fixture.assertNoTemporaryEntries()
    }

    @Test
    fun missingCacheReturnsTheOriginalNetworkFailure() = withFixture { fixture ->
        val failure = SocketTimeoutException("network timed out")
        fixture.client.failure = failure

        val result = assertIs<ErrorResult>(fixture.load())

        assertSame(failure, result.throwable)
        assertEquals(fixture.request, result.request)
        assertEquals(1, fixture.client.requests.size)
        fixture.assertNoTemporaryEntries(expectedEntries = 0)
    }

    @Test
    fun explicitCacheKeysAreKeptForNetworkAndMemoryFallback() = withFixture { fixture ->
        fixture.request = fixture.request.newBuilder()
            .diskCacheKey(URL + "BIGGER")
            .memoryCacheKey("header-cover")
            .build()
        fixture.prime()
        fixture.client.body = "cover:new"

        val networkResult = assertIs<SuccessResult>(fixture.load())

        assertEquals(URL + "BIGGER", networkResult.diskCacheKey)
        assertEquals("header-cover", networkResult.memoryCacheKey?.key)
        assertEquals("cover:new", fixture.diskBody(URL + "BIGGER"))
        fixture.client.failure = IOException("offline")

        val cachedResult = assertIs<SuccessResult>(fixture.load())

        assertEquals(DataSource.MEMORY_CACHE, cachedResult.dataSource)
        assertEquals("new", assertIs<TestImage>(cachedResult.image).label)
        assertEquals(URL + "BIGGER", cachedResult.diskCacheKey)
        fixture.assertNoTemporaryEntries()
    }

    @Test
    fun localFilesAreNotSentThroughTheNetworkFirstPath() = withFixture { fixture ->
        val file = fixture.directory.resolve("local.img").toFile()
        file.writeText("cover:local")
        val request = fixture.request.newBuilder().data(file.toURI().toString()).build()

        val result = assertIs<SuccessResult>(fixture.loader.execute(request))

        assertEquals("local", assertIs<TestImage>(result.image).label)
        assertEquals(0, fixture.client.requests.size)
    }

    @Test
    fun explicitlyCacheOnlyRequestsDoNotTryTheNetwork() = withFixture { fixture ->
        fixture.prime()
        val request = fixture.request.newBuilder()
            .networkCachePolicy(CachePolicy.DISABLED)
            .build()

        val result = assertIs<SuccessResult>(fixture.loader.execute(request))

        assertEquals("old", assertIs<TestImage>(result.image).label)
        assertEquals(0, fixture.client.requests.size)
    }

    @Test
    fun cancellationPropagatesInsteadOfFallingBack() = withFixture { fixture ->
        fixture.prime()
        fixture.loader.memoryCache?.clear()
        fixture.client.failure = CancellationException("request cancelled")

        assertFailsWith<CancellationException> { fixture.load() }

        assertEquals(1, fixture.client.requests.size)
        assertEquals("cover:old", fixture.diskBody())
        fixture.assertNoTemporaryEntries()
    }

    @Test
    fun cancellationAfterDecodingDoesNotLeaveADeadStagingKeyInMemory() = withFixture { fixture ->
        fixture.prime()
        fixture.client.body = "cover:new"
        fixture.client.cancelAfterDecode = true

        assertFailsWith<CancellationException> { fixture.load() }

        assertEquals("cover:old", fixture.diskBody())
        fixture.assertNoTemporaryEntries()
        fixture.client.cancelAfterDecode = false
        fixture.client.failure = IOException("offline")
        val result = assertIs<SuccessResult>(fixture.load())
        assertEquals("old", assertIs<TestImage>(result.image).label)
    }

    private fun withFixture(
        ttlMillis: Long = 0L,
        block: suspend (Fixture) -> Unit,
    ) = runBlocking {
        val fixture = Fixture(ttlMillis)
        try {
            block(fixture)
        } finally {
            fixture.loader.shutdown()
            fixture.diskCache.shutdown()
            fixture.directory.toFile().deleteRecursively()
        }
    }

    private class Fixture(ttlMillis: Long) {
        val directory = Files.createTempDirectory("coil-network-first-test")
        val diskCache = DiskCache.Builder()
            .directory(directory.resolve("cache").toString().toPath())
            .maxSizeBytes(1024L * 1024)
            .build()
        val client = TestNetworkClient()
        lateinit var loader: ImageLoader
            private set
        var request = ImageRequest.Builder(PlatformContext.INSTANCE)
            .data(URL)
            .size(1, 1)
            .build()

        init {
            loader = ImageLoader.Builder(PlatformContext.INSTANCE)
                .mainCoroutineContext(Dispatchers.Unconfined)
                .diskCache(diskCache)
                .components {
                    add(NetworkFirstInterceptor(ttlMillis = ttlMillis, nowMillis = { client.nowMillis }) { loader })
                    add(Interceptor { chain ->
                        val result = chain.proceed()
                        if (client.cancelAfterDecode && result is SuccessResult && result.dataSource == DataSource.NETWORK) {
                            throw CancellationException("cancelled after decoding")
                        }
                        result
                    })
                    add(NetworkFetcher.Factory(networkClient = { client }))
                    add(Decoder.Factory { result, _, _ ->
                        Decoder {
                            val body = result.source.source().readUtf8()
                            if (!body.startsWith("cover:")) throw IOException("Image could not be decoded")
                            DecodeResult(TestImage(body.removePrefix("cover:")), isSampled = false)
                        }
                    })
                }
                .build()
        }

        suspend fun load() = loader.execute(request)

        suspend fun prime() {
            assertIs<SuccessResult>(load())
            client.requests.clear()
        }

        fun diskBody(key: String = URL): String? = diskCache.openSnapshot(key)?.use { snapshot ->
            diskCache.fileSystem.read(snapshot.data) { readUtf8() }
        }

        fun assertNoTemporaryEntries(expectedEntries: Int = 1) {
            val entryFiles = diskCache.fileSystem.list(diskCache.directory)
                .filter { it.name.endsWith(".0") || it.name.endsWith(".1") }
            assertEquals(expectedEntries * 2, entryFiles.size)
            loader.memoryCache?.keys?.forEach { key ->
                val value = loader.memoryCache?.get(key)
                assertFalse(value?.extras?.values?.any { it.toString().startsWith("coil-network-first:") } == true)
            }
        }
    }

    private class TestNetworkClient : NetworkClient {
        val requests = mutableListOf<NetworkRequest>()
        var code = 200
        var body = "cover:old"
        var failure: Exception? = null
        var cancelAfterDecode = false
        var nowMillis = 1_800_000_000_000L

        override suspend fun <T> executeRequest(
            request: NetworkRequest,
            block: suspend (NetworkResponse) -> T,
        ): T {
            // Match OkHttp's only-if-cached behaviour without a socket or an HTTP cache.
            if (request.headers["Cache-Control"]?.contains("only-if-cached") == true) {
                return block(NetworkResponse(code = 504))
            }
            requests += request
            failure?.let { throw it }
            val response = NetworkResponse(
                code = code,
                requestMillis = nowMillis,
                responseMillis = nowMillis,
                headers = NetworkHeaders.Builder().set("Content-Type", "image/test").build(),
                body = NetworkResponseBody(Buffer().writeUtf8(body)),
            )
            try {
                return block(response)
            } finally {
                response.body?.close()
            }
        }
    }

    private class TestImage(val label: String) : Image {
        override val size = 4L
        override val width = 1
        override val height = 1
        override val shareable = true

        override fun draw(canvas: Canvas) = error("These cache tests do not draw images")
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val URL = "https://example.test/cover.img"
    }
}
