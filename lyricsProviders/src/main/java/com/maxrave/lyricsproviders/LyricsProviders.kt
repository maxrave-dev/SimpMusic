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
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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
                defaultHostUrl = "https://apic.musixmatch.com"
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
                url("https://apic.musixmatch.com/ws/1.1/")
            }
            if (proxy != null) {
                engine {
                    proxy = this@LyricsProviders.proxy
                }
            }
        }

    suspend fun userGet() = lyricsClient.get("user.get") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            header(HttpHeaders.Cookie, musixmatchCookie ?: "")
        }
        parameter("app_id", "android-player-v1.0")
        parameter("part", "oauthtoken_list_full,payment_widget_link")
        parameter("usertoken", musixmatchUserToken ?: "")
        parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
        parameter("signature_protocol", "sha1")
    }

    suspend fun configGet(
        guid: String
    ) = lyricsClient.get("config.get") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            header(HttpHeaders.Cookie, musixmatchCookie ?: "")
        }
        parameter("app_id", "android-player-v1.0")
        parameter("timestamp", LocalDateTime.now().toString())
        parameter("lang", "en-US_US")
        parameter("guid", guid)
        parameter("usertoken", musixmatchUserToken ?: "")
        parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
        parameter("signature_protocol", "sha1")
    }

    suspend fun getMusixmatchUserToken() =
        lyricsClient.get("token.get?app_id=android-player-v1.0") {
            contentType(ContentType.Application.Json)
            headers {
                header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
                header(HttpHeaders.Accept, "*/*")
                header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
                header(HttpHeaders.Connection, "keep-alive")
                header(HttpHeaders.Cookie, musixmatchCookie ?: "")
            }

            parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
            parameter("signature_protocol", "sha1")
        }

    suspend fun postMusixmatchPostCredentials(
        email: String,
        password: String,
        userToken: String,
    ) = lyricsClient.post("https://apic.musixmatch.com/ws/1.1/credential.post") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
        }
        parameter("app_id", "android-player-v1.0")
        parameter("usertoken", userToken)
        parameter("format", "json")

        parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
        parameter("signature_protocol", "sha1")
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

    suspend fun macroSearch(
        q_artist: String,
        q_track: String,
        duration: Int,
        userToken: String,
        musixmatchCookie: String? = null,

    ) = lyricsClient.get("macro.subtitles.get") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            header(HttpHeaders.Cookie, musixmatchCookie ?: "")
        }
        parameters {
            parameter("tags", "scrobbling%2Cnotifications")
            parameter("f_subtitle_length_max_deviation", "1")
            parameter("f_subtitle_length", duration.toString())
            parameter("q_duration", duration.toString())

            parameter("subtitle_format", "lrc")
            parameter("page_size", "1")
            parameter("questions_id_list", "track_esync_action%2Ctrack_sync_action%2Ctrack_translation_action%2Clyrics_ai_mood_analysis_v3")
            parameter("optional_calls", "track.richsync%2Ccrowd.track.actions")
            parameter("q_artist", q_artist)
            parameter("q_track", q_track)
            parameter("usertoken", userToken)
            parameter("app_id", "android-player-v1.0")
            parameter("country", "us")
            parameter(
                "part",
                "lyrics_crowd%2Cuser%2Clyrics_vote%2Clyrics_poll%2Ctrack_lyrics_translation_status%2Clyrics_verified_by%2Clabels%2Ctrack_structure%2Ctrack_performer_tagging%2C"
            )
            parameter("scrobbling_package", "com.google.android.apps.youtube.music")
            parameter("language_iso_code", "1")
            parameter("format", "json")

            parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
            parameter("signature_protocol", "sha1")
        }
    }

    suspend fun searchMusixmatchTrackId(
        q: String,
        userToken: String,
    ) = lyricsClient.get("macro.search?app_id=android-player-v1.0&page_size=5&page=1&s_track_rating=desc&quorum_factor=1.0") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            header(HttpHeaders.Cookie, musixmatchCookie ?: "")
        }

        parameter("q", q)
        parameter("usertoken", userToken)

        parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
        parameter("signature_protocol", "sha1")
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
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            header(HttpHeaders.Cookie, musixmatchCookie ?: "")
        }

        parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
        parameter("signature_protocol", "sha1")
    }

    suspend fun macroSubtitles(
        usertoken: String,
        q_artist: String,
        q_track: String,
    ) = lyricsClient.get(
        "https://apic.musixmatch.com/ws/1.1/macro.subtitles.get?tags=scrobbling%2Cnotifications&f_subtitle_length_max_deviation=1&subtitle_format=lrc&page_size=1&questions_id_list=track_esync_action%2Ctrack_sync_action%2Ctrack_translation_action%2Clyrics_ai_mood_analysis_v3&optional_calls=track.richsync%2Ccrowd.track.actions&q_artist=${q_artist}&q_track=${q_track}&usertoken=${usertoken}&app_id=android-player-v1.0&country=us&part=lyrics_crowd%2Cuser%2Clyrics_vote%2Clyrics_poll%2Ctrack_lyrics_translation_status%2Clyrics_verified_by%2Clabels%2Ctrack_structure%2Ctrack_performer_tagging%2C&scrobbling_package=com.google.android.apps.youtube.music&language_iso_code=1&format=json"
    ) {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            header(HttpHeaders.Cookie, musixmatchCookie ?: "")
        }

        parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
        parameter("signature_protocol", "sha1")
    }

    suspend fun getMusixmatchLyrics(
        trackId: String,
        userToken: String,
    ) = lyricsClient.get("track.subtitle.get?app_id=android-player-v1.0&subtitle_format=id3") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            header(HttpHeaders.Cookie, musixmatchCookie ?: "")
        }

        parameter("usertoken", userToken)
        parameter("track_id", trackId)

        parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
        parameter("signature_protocol", "sha1")
    }

    suspend fun getMusixmatchLyricsByQ(
        track: SearchMusixmatchResponse.Message.Body.Track.TrackX,
        userToken: String,
    ) = lyricsClient.get("https://apic.musixmatch.com/ws/1.1/track.subtitles.get") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            header(HttpHeaders.Cookie, musixmatchCookie ?: "")
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

        parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
        parameter("signature_protocol", "sha1")
    }

    suspend fun getMusixmatchUnsyncedLyrics(
        trackId: String,
        userToken: String,
    ) = lyricsClient.get("track.lyrics.get?app_id=android-player-v1.0&subtitle_format=id3") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            header(HttpHeaders.Cookie, musixmatchCookie ?: "")
        }
        parameter("usertoken", userToken)
        parameter("track_id", trackId)

        parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
        parameter("signature_protocol", "sha1")
    }

    suspend fun searchLrclibLyrics(
        q_track: String,
        q_artist: String,
    ) = lyricsClient.get("https://lrclib.net/api/search") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
        }
        parameter("q", "$q_artist $q_track")

        parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
        parameter("signature_protocol", "sha1")
    }

    suspend fun getMusixmatchTranslateLyrics(
        trackId: String,
        userToken: String,
        language: String,
    ) = lyricsClient.get("https://apic.musixmatch.com/ws/1.1/crowd.track.translations.get") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "Dalvik/2.1.0 (Linux; U; Android 14; Phone Build/UP1A.231105.003.A1)")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            header(HttpHeaders.Cookie, musixmatchCookie ?: "")
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

        parameter("signature", getApiSignature(url.toString(), LocalDateTime.now()))
        parameter("signature_protocol", "sha1")
    }

    private fun getApiSignature(apiEndpoint: String, dateTime: LocalDateTime): String {
        val key = "IEJ5E8XFaHQvIQNfs7IC"
        val formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val data = apiEndpoint + formattedDate
        val hmacSha1 = Mac.getInstance("HmacSHA1")
        val secretKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA1")
        hmacSha1.init(secretKey)
        val signatureBytes = hmacSha1.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes)
    }
}