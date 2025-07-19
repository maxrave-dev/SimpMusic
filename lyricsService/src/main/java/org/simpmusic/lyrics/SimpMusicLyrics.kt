package org.simpmusic.lyrics

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Cache
import org.simpmusic.lyrics.encoder.brotli
import org.simpmusic.lyrics.models.request.LyricsBody
import org.simpmusic.lyrics.models.request.TranslatedLyricsBody
import org.simpmusic.lyrics.models.request.VoteBody
import java.net.Proxy

class SimpMusicLyrics(
    private val context: Context,
) {
    private var httpClient = createClient()
    var proxy: Proxy? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    private fun cachePath() = context.cacheDir.resolve("lyrics_cache")

    private val baseUrl = "https://api-lyrics.simpmusic.org/v1/"

    private fun createClient() =
        HttpClient(OkHttp) {
            expectSuccess = true
            followRedirects = true
            engine {
                config {
                    cache(
                        Cache(cachePath(), 50L * 1024 * 1024),
                    )
                }
            }
            install(HttpCache)
            install(HttpSend) {
                maxSendCount = 100
            }
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    },
                )
            }
            install(ContentEncoding) {
                brotli(1.0F)
                gzip(0.9F)
                deflate(0.8F)
            }
            defaultRequest {
                url("https://api-lyrics.simpmusic.org/v1")
            }
            if (proxy != null) {
                engine {
                    proxy = this@SimpMusicLyrics.proxy
                }
            }
        }

    private fun HttpRequestBuilder.buildDefaultHeaders(
        timestamp: String? = null,
        hmac: String? = null,
    ) {
        headers {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, "SimpMusicLyrics/1.0")
            header(HttpHeaders.ContentType, "application/json")
            timestamp?.let {
                header("X-Timestamp", it)
            }
            hmac?.let {
                header("X-HMAC", it)
            }
        }
    }

    suspend fun findLyricsByVideoId(videoId: String) =
        httpClient.get(baseUrl + videoId) {
            buildDefaultHeaders()
        }

    suspend fun findTranslatedLyrics(
        videoId: String,
        language: String,
    ) = httpClient.get(baseUrl + "translated/$videoId/$language") {
        buildDefaultHeaders()
    }

    suspend fun insertLyrics(
        lyricsBody: LyricsBody,
        hmacTimestamp: Pair<String, String>,
    ) = httpClient.post {
        buildDefaultHeaders(
            timestamp = hmacTimestamp.second,
            hmac = hmacTimestamp.first,
        )
        setBody(lyricsBody)
    }

    suspend fun insertTranslatedLyrics(
        translatedLyricsBody: TranslatedLyricsBody,
        hmacTimestamp: Pair<String, String>,
    ) = httpClient.post(baseUrl + "translated") {
        buildDefaultHeaders(
            timestamp = hmacTimestamp.second,
            hmac = hmacTimestamp.first,
        )
        setBody(translatedLyricsBody)
    }

    suspend fun voteLyrics(
        id: String,
        upvote: Boolean,
        hmacTimestamp: Pair<String, String>,
    ) = httpClient.post(baseUrl + "vote") {
        buildDefaultHeaders(
            timestamp = hmacTimestamp.second,
            hmac = hmacTimestamp.first,
        )
        setBody(
            VoteBody(
                id = id,
                vote = if (upvote) 1 else 0, // 1 for upvote, 0 for downvote
            ),
        )
    }

    suspend fun voteTranslatedLyrics(
        id: String,
        upvote: Boolean,
        hmacTimestamp: Pair<String, String>,
    ) = httpClient.post(baseUrl + "translated/vote") {
        buildDefaultHeaders(
            timestamp = hmacTimestamp.second,
            hmac = hmacTimestamp.first,
        )
        setBody(
            VoteBody(
                id = id,
                vote = if (upvote) 1 else 0, // 1 for upvote, 0 for downvote
            ),
        )
    }
}