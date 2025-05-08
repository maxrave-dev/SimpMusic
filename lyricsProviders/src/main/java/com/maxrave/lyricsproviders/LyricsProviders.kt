package com.maxrave.lyricsproviders

import android.content.Context
import com.maxrave.lyricsproviders.encoder.brotli
import com.maxrave.lyricsproviders.models.body.MusixmatchCredentialsBody
import com.maxrave.lyricsproviders.models.response.SearchMusixmatchResponse
import com.maxrave.lyricsproviders.utils.CustomRedirectConfig
import com.maxrave.lyricsproviders.utils.fromString
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
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Cache
import java.net.Proxy

class LyricsProviders(
    private val context: Context,
    private val commonJson: Json,
) {
    private var lyricsClient = createClient()

    var musixmatchCookie: String? = null
        set(value) {
            field = value
        }

    var musixmatchUserToken: String? = null

    var proxy: Proxy? = null
        set(value) {
            field = value
            lyricsClient.close()
            lyricsClient = createClient()
        }

    private fun cachePath() = context.cacheDir.resolve("http_cache")

    private fun createClient() =
        HttpClient(OkHttp) {
            expectSuccess = true
            followRedirects = false
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
            install(CustomRedirectConfig) {
                checkHttpMethod = false
                allowHttpsDowngrade = true
                defaultHostUrl = "https://apic-desktop.musixmatch.com"
            }
            install(ContentNegotiation) {
                register(
                    ContentType.Text.Plain,
                    KotlinxSerializationConverter(
                        commonJson,
                    ),
                )
                json(
                    commonJson,
                )
            }
            install(ContentEncoding) {
                brotli(1.0F)
                gzip(0.9F)
                deflate(0.8F)
            }
            defaultRequest {
                url("https://apic-desktop.musixmatch.com/ws/1.1/")
            }
            if (proxy != null) {
                engine {
                    proxy = this@LyricsProviders.proxy
                }
            }
        }

    suspend fun getMusixmatchUserToken() =
        lyricsClient.get("token.get?app_id=android-player-v1.0") {
            contentType(ContentType.Application.Json)
            headers {
                header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
                header(HttpHeaders.Accept, "*/*")
                header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
                header(HttpHeaders.Connection, "keep-alive")
                if (musixmatchCookie != null) {
                    val listCookies = fromString(musixmatchCookie, commonJson)
                    if (!listCookies.isNullOrEmpty()) {
                        val appendCookie =
                            listCookies.joinToString(separator = "; ") { eachCookie ->
                                eachCookie
                            }
                        header(HttpHeaders.Cookie, appendCookie)
                    }
                }
            }
        }

    suspend fun postMusixmatchPostCredentials(
        email: String,
        password: String,
        userToken: String,
    ) = lyricsClient.post("https://apic.musixmatch.com/ws/1.1/credential.post") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
        }
        parameter("app_id", "android-player-v1.0")
        parameter("usertoken", userToken)
        parameter("format", "json")
        setBody(
            MusixmatchCredentialsBody(
                listOf(
                    MusixmatchCredentialsBody.Credential(
                        MusixmatchCredentialsBody.Credential.CredentialData(
                            email = email,
                            password = password,
                        ),
                    ),
                ),
            ),
        )
    }

    suspend fun searchMusixmatchTrackId(
        q: String,
        userToken: String,
    ) = lyricsClient.get("macro.search?app_id=android-player-v1.0&page_size=5&page=1&s_track_rating=desc&quorum_factor=1.0") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixmatchCookie != null) {
                val listCookies = fromString(musixmatchCookie, commonJson)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie =
                        listCookies.joinToString(separator = "; ") { eachCookie ->
                            eachCookie
                        }
                    header(HttpHeaders.Cookie, appendCookie)
                }
            }
        }

        parameter("q", q)
        parameter("usertoken", userToken)
    }

    suspend fun fixSearchMusixmatch(
        q_artist: String,
        q_track: String,
        q_duration: String,
        userToken: String,
    ) = lyricsClient.get(
        "matcher.track.get?tags=scrobbling%2Cnotifications&subtitle_format=dfxp&page_size=5&questions_id_list=track_esync_action%2Ctrack_sync_action%2Ctrack_translation_action%2Clyrics_ai_mood_analysis_v3&optional_calls=track.richsync%2Ccrowd.track.actions&app_id=android-player-v1.0&country=us&part=lyrics_crowd%2Cuser%2Clyrics_vote%2Clyrics_poll%2Ctrack_lyrics_translation_status%2Clyrics_verified_by%2Clabels%2Ctrack_structure%2Ctrack_performer_tagging%2C&scrobbling_package=com.google.android.apps.youtube.music&language_iso_code=1&format=json",
    ) {
        contentType(ContentType.Application.Json)
        parameter("usertoken", userToken)
//            q_artist=culture+code,+james+roche+&+karra&q_track=make+me+move+(james+roche+remix)
        parameter("q_artist", q_artist)
        parameter("q_track", q_track)
        parameter("q_duration", q_duration)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixmatchCookie != null) {
                val listCookies = fromString(musixmatchCookie, commonJson)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie =
                        listCookies.joinToString(separator = "; ") { eachCookie ->
                            eachCookie
                        }
                    header(HttpHeaders.Cookie, appendCookie)
                }
            }
        }
    }

    suspend fun getMusixmatchLyrics(
        trackId: String,
        userToken: String,
    ) = lyricsClient.get("track.subtitle.get?app_id=android-player-v1.0&subtitle_format=id3") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixmatchCookie != null) {
                val listCookies = fromString(musixmatchCookie, commonJson)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie =
                        listCookies.joinToString(separator = "; ") { eachCookie ->
                            eachCookie
                        }
                    header(HttpHeaders.Cookie, appendCookie)
                }
            }
        }

        parameter("usertoken", userToken)
        parameter("track_id", trackId)
    }

    suspend fun getMusixmatchLyricsByQ(
        track: SearchMusixmatchResponse.Message.Body.Track.TrackX,
        userToken: String,
    ) = lyricsClient.get("https://apic.musixmatch.com/ws/1.1/track.subtitles.get") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixmatchCookie != null) {
                val listCookies = fromString(musixmatchCookie, commonJson)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie =
                        listCookies.joinToString(separator = "; ") { eachCookie ->
                            eachCookie
                        }
                    header(HttpHeaders.Cookie, appendCookie)
                }
            }
        }

        parameter("usertoken", userToken)
        parameter("track_id", track.track_id)
        parameter("f_subtitle_length_max_deviation", "1")
        parameter("page_size", "1")
        parameter("questions_id_list", "track_esync_action%2Ctrack_sync_action%2Ctrack_translation_action%2Clyrics_ai_mood_analysis_v3")
        parameter("optional_calls", "track.richsync%2Ccrowd.track.actions")
        parameter("q_artist", track.artist_name)
        parameter("q_track", track.track_name)
        parameter("app_id", "android-player-v1.0")
        parameter(
            "part",
            "lyrics_crowd%2Cuser%2Clyrics_vote%2Clyrics_poll%2Ctrack_lyrics_translation_status%2Clyrics_verified_by%2Clabels%2Ctrack_structure%2Ctrack_performer_tagging%2C",
        )
        parameter("language_iso_code", "1")
        parameter("format", "json")
        parameter("q_duration", track.track_length)
    }

    suspend fun getMusixmatchUnsyncedLyrics(
        trackId: String,
        userToken: String,
    ) = lyricsClient.get("track.lyrics.get?app_id=android-player-v1.0&subtitle_format=id3") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixmatchCookie != null) {
                val listCookies = fromString(musixmatchCookie, commonJson)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie =
                        listCookies.joinToString(separator = "; ") { eachCookie ->
                            eachCookie
                        }
                    header(HttpHeaders.Cookie, appendCookie)
                }
            }
        }
        parameter("usertoken", userToken)
        parameter("track_id", trackId)
    }

    suspend fun searchLrclibLyrics(
        q_track: String,
        q_artist: String,
    ) = lyricsClient.get("https://lrclib.net/api/search") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
        }
        parameter("q", "$q_artist $q_track")
    }

    suspend fun getMusixmatchTranslateLyrics(
        trackId: String,
        userToken: String,
        language: String,
    ) = lyricsClient.get("https://apic.musixmatch.com/ws/1.1/crowd.track.translations.get") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixmatchCookie != null) {
                val listCookies = fromString(musixmatchCookie, commonJson)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie =
                        listCookies.joinToString(separator = "; ") { eachCookie ->
                            eachCookie
                        }
                    header(HttpHeaders.Cookie, appendCookie)
                }
            }
        }
        parameters {
            parameter("translation_fields_set", "minimal")
            parameter("track_id", trackId)
            parameter("selected_language", language)
            parameter("comment_format", "text")
            parameter("part", "user")
            parameter("format", "json")
            parameter("usertoken", userToken)
            parameter("app_id", "android-player-v1.0")
            parameter("tags", "playing")
        }
    }
}