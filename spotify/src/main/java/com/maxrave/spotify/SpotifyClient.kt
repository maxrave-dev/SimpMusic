package com.maxrave.spotify

import com.maxrave.spotify.encoder.brotli
import com.maxrave.spotify.model.body.CanvasBody
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.protobuf.protobuf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import java.net.Proxy

class SpotifyClient {
    private var spotifyClient = createSpotifyClient()

    var proxy: Proxy? = null
        set(value) {
            field = value
            spotifyClient.close()
            spotifyClient = createSpotifyClient()
        }

    @OptIn(ExperimentalSerializationApi::class)
    private fun createSpotifyClient() =
        HttpClient(OkHttp) {
            expectSuccess = true
            followRedirects = false
            install(HttpCache)
            install(HttpSend) {
                maxSendCount = 100
            }
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(ContentNegotiation) {
                register(
                    ContentType.Text.Plain,
                    KotlinxSerializationConverter(
                        Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                            explicitNulls = false
                            encodeDefaults = true
                        },
                    ),
                )
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    },
                )
                protobuf(
                    ProtoBuf {
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
                url("https://api.spotify.com")
            }
            if (proxy != null) {
                engine {
                    proxy = this@SpotifyClient.proxy
                }
            }
        }

    suspend fun getSpotifyLyricsToken(spdc: String) =
        spotifyClient.get("https://open.spotify.com/get_access_token?reason=transport&productType=web_player") {
            userAgent(USER_AGENT)
            contentType(ContentType.Application.Json)
            header("Cookie", "sp_dc=$spdc")
        }

    suspend fun getSpotifyLyrics(
        token: String,
        trackId: String,
    ) = spotifyClient.get("https://spclient.wg.spotify.com/color-lyrics/v2/track/$trackId?format=json&vocalRemoval=false&market=from_token") {
        userAgent(USER_AGENT)
        contentType(ContentType.Application.Json)
        header("Authorization", "Bearer $token")
        header("App-platform", "WebPlayer")
    }

    suspend fun searchSpotifyTrack(
        q: String,
        authToken: String,
    ) = spotifyClient.get("https://api-partner.spotify.com/pathfinder/v1/query?operationName=searchTracks") {
        userAgent(USER_AGENT)
        contentType(ContentType.Application.Json)
        header("Authorization", "Bearer $authToken")
        header(
            HttpHeaders
                .AcceptEncoding,
            "gzip, deflate, br",
        )
        val variable =
            "{\"searchTerm\":\"${q}\",\"offset\":0,\"limit\":3,\"numberOfTopResults\":3,\"includeAudiobooks\":true,\"includePreReleases\":false}"
        val sha = "220d098228a4eaf216b39e8c147865244959c4cc6fd82d394d88afda0b710929"
        parameter(
            "variables",
            variable,
        )
        parameter(
            "extensions",
            "{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"${sha}\"}}",
        )
    }
    // {"searchTerm":"trình+hieuthuhai","offset":0,"limit":20,"numberOfTopResults":20,"includeAudiobooks":true,"includePreReleases":false}
    // {"persistedQuery":{"version":1,"sha256Hash":"e4ed1f91a2cc5415befedb85acf8671dc1a4bf3ca1a5b945a6386101a22e28a6"}}

    suspend fun getSpotifyCanvas(
        trackId: String,
        token: String,
    ) = spotifyClient.post("https://spclient.wg.spotify.com/canvaz-cache/v0/canvases") {
        headers {
            append(HttpHeaders.Accept, "application/protobuf")
            append(HttpHeaders.ContentType, "application/protobuf")
            append(
                HttpHeaders
                    .AcceptEncoding,
                "gzip, deflate, br",
            )
            append(HttpHeaders.Authorization, "Bearer $token")
            append(HttpHeaders.UserAgent, "Spotify/8.5.49 iOS/Version 13.3.1 (Build 17D50)")
        }
        setBody(
            CanvasBody(
                tracks =
                listOf(
                    CanvasBody.Track(
                        track_uri = "spotify:track:$trackId",
                    ),
                ),
            ),
        )
    }
    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36"
    }
}