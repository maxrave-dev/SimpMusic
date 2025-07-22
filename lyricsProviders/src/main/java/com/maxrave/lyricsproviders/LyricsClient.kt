package com.maxrave.lyricsproviders

import android.content.Context
import android.util.Log
import com.maxrave.lyricsproviders.models.lyrics.Lyrics
import com.maxrave.lyricsproviders.models.response.LrclibObject
import com.maxrave.lyricsproviders.models.response.MacroSearchResponse
import com.maxrave.lyricsproviders.models.response.MusixmatchCredential
import com.maxrave.lyricsproviders.models.response.MusixmatchLyricsResponse
import com.maxrave.lyricsproviders.models.response.MusixmatchLyricsResponseByQ
import com.maxrave.lyricsproviders.models.response.MusixmatchTranslationLyricsResponse
import com.maxrave.lyricsproviders.models.response.SearchMusixmatchResponse
import com.maxrave.lyricsproviders.models.response.UserTokenResponse
import com.maxrave.lyricsproviders.parser.parseMusixmatchLyrics
import com.maxrave.lyricsproviders.parser.parseUnsyncedLyrics
import com.maxrave.lyricsproviders.utils.CaptchaException
import com.maxrave.lyricsproviders.utils.fromArrayListNull
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LyricsClient(
    context: Context,
) {
    private val commonJson = commonJson()
    private val lyricsProvider = LyricsProviders(context, commonJson)

    private fun commonJson() =
        Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

    @OptIn(ExperimentalUuidApi::class)
    var musixmatchCookie: String?
        get() = lyricsProvider.musixmatchCookie
        set(value) {
            lyricsProvider.musixmatchCookie = value
            value?.let {
                if (it.isNotEmpty()) {
                    val guid = it.substringAfter("x-mxm-token-guid=").substringBefore(";")
                    this.guid = guid.ifEmpty { Uuid.random().toString() }
                }
            }
        }

    var musixmatchUserToken: String?
        get() = lyricsProvider.musixmatchUserToken
        set(value) {
            lyricsProvider.musixmatchUserToken = value
        }

    @OptIn(ExperimentalUuidApi::class)
    var guid: String = Uuid.random().toString()

    fun removeProxy() {
        lyricsProvider.proxy = null
    }

    /**
     * Set the proxy for client
     */
    fun setProxy(
        isHttp: Boolean,
        host: String,
        port: Int,
    ) {
        runCatching {
            if (isHttp) ProxyBuilder.http("$host:$port") else ProxyBuilder.socks(host, port)
        }.onSuccess {
            lyricsProvider.proxy = it
        }.onFailure {
            it.printStackTrace()
        }
    }

    suspend fun getMusixmatchUserToken() =
        runCatching {
            lyricsProvider.getMusixmatchUserToken().body<UserTokenResponse>()
        }

    suspend fun userGet() =
        runCatching {
            lyricsProvider.userGet().bodyAsText()
        }

    suspend fun configGet() =
        runCatching {
            lyricsProvider.configGet(guid).bodyAsText()
        }

    suspend fun postMusixmatchCredentials(
        email: String,
        password: String,
        userToken: String,
    ) = runCatching {
        val request = lyricsProvider.postMusixmatchPostCredentials(email, password, userToken)
        val response = request.body<MusixmatchCredential>()
        if (response.message.body
                .get(0)
                .credential.error == null &&
            response.message.body
                .get(0)
                .credential.account != null
        ) {
            val setCookies = request.headers.getAll("Set-Cookie")
//            Log.w("postMusixmatchCredentials", setCookies.toString())
            if (!setCookies.isNullOrEmpty()) {
                fromArrayListNull(setCookies, commonJson)?.let {
                    musixmatchCookie = it
                }
            }
        }
//        Log.w("postMusixmatchCredentials cookie", musixmatchCookie.toString())
//        Log.w("postMusixmatchCredentials", response.toString())
        return@runCatching response
    }

    fun getmusixmatchCookie() = musixmatchCookie

    suspend fun macroSearch(
        q_track: String,
        q_artist: String,
        duration: Int,
        userToken: String,
    ) = runCatching {
        val rs =
            lyricsProvider
                .macroSearch(
                    userToken = userToken,
                    q_track = q_track,
                    q_artist = q_artist,
                    duration = duration,
                ).bodyWithoutCaptcha<MacroSearchResponse>()
        val trackId =
            rs.message
                ?.body
                ?.macroCalls
                ?.matcherTrackGet
                ?.message
                ?.body
                ?.track
                ?.trackId ?: return@runCatching null
        val subtitleBody =
            rs.message.body.macroCalls.trackSubtitlesGet
                ?.message
                ?.body
                ?.subtitleList
                ?.firstOrNull()
                ?.subtitle
                ?.subtitleBody
        val lyricsBody =
            rs.message.body.macroCalls.trackLyricsGet
                ?.message
                ?.body
                ?.lyrics
                ?.lyricsBody
        if (!subtitleBody.isNullOrEmpty()) {
            return@runCatching trackId to
                parseMusixmatchLyrics(
                    subtitleBody,
                )
        } else if (!lyricsBody.isNullOrEmpty()) {
            return@runCatching trackId to parseUnsyncedLyrics(lyricsBody)
        } else {
            null
        }
    }

    suspend fun searchMusixmatchTrackId(
        query: String,
        userToken: String,
    ) = runCatching {
        delay(500)
        lyricsProvider.searchMusixmatchTrackId(query, userToken).bodyWithoutCaptcha<SearchMusixmatchResponse>()
    }

    suspend fun macroSubtitle(
        q_artist: String,
        q_track: String,
        userToken: String,
    ): Result<Pair<Int, Lyrics>?> =
        runCatching {
            delay(500)
            val response =
                lyricsProvider
                    .macroSubtitles(
                        usertoken = userToken,
                        q_track = q_track,
                        q_artist = q_artist,
                    ).bodyWithoutCaptcha<MusixmatchLyricsResponse>()
            Log.w("Macro Subtitle Result", response.toString())
            val trackId =
                response.message.body.macro_calls
                    ?.trackGet
                    ?.message
                    ?.body
                    ?.track
                    ?.track_id ?: return@runCatching null
            val subtitleBody =
                response.message.body.macro_calls.trackSubtitlesGet
                    ?.message
                    ?.body
                    ?.subtitle_list
                    ?.firstOrNull()
                    ?.subtitle_body
            val lyricsBody =
                response.message.body.macro_calls.trackLyricsGet
                    ?.message
                    ?.body
                    ?.lyrics
                    ?.lyrics_body
            if (!subtitleBody.isNullOrEmpty()) {
                return@runCatching trackId to
                    parseMusixmatchLyrics(
                        subtitleBody,
                    )
            } else if (!lyricsBody.isNullOrEmpty()) {
                return@runCatching trackId to parseUnsyncedLyrics(lyricsBody)
            } else {
                null
            }
        }

    suspend fun fixSearchMusixmatch(
        q_artist: String,
        q_track: String,
        q_duration: String,
        userToken: String,
    ) = runCatching {
        delay(500)
        val rs = lyricsProvider.fixSearchMusixmatch(q_artist, q_track, q_duration, userToken).bodyWithoutCaptcha<SearchMusixmatchResponse>()
        Log.w("Search Result", rs.toString())
        return@runCatching rs
    }

    suspend fun getMusixmatchLyrics(
        trackId: String,
        userToken: String,
    ) = runCatching {
        delay(500)
        val response = lyricsProvider.getMusixmatchLyrics(trackId, userToken).bodyWithoutCaptcha<MusixmatchLyricsResponse>()
        if (response.message.body.subtitle != null) {
            return@runCatching parseMusixmatchLyrics(response.message.body.subtitle.subtitle_body)
        } else {
            val unsyncedResponse = lyricsProvider.getMusixmatchUnsyncedLyrics(trackId, userToken).bodyWithoutCaptcha<MusixmatchLyricsResponse>()
            if (unsyncedResponse.message.body.lyrics != null && unsyncedResponse.message.body.lyrics.lyrics_body != "") {
                return@runCatching parseUnsyncedLyrics(unsyncedResponse.message.body.lyrics.lyrics_body)
            } else {
                null
            }
        }
    }

    suspend fun getMusixmatchLyricsByQ(
        track: SearchMusixmatchResponse.Message.Body.Track.TrackX,
        userToken: String,
    ) = runCatching {
        val response = lyricsProvider.getMusixmatchLyricsByQ(track, userToken).bodyWithoutCaptcha<MusixmatchLyricsResponseByQ>()
        if (!response.message.body.subtitle_list
                .isNullOrEmpty() &&
            response.message.body.subtitle_list
                .firstOrNull()
                ?.subtitle
                ?.subtitle_body != null
        ) {
            return@runCatching parseMusixmatchLyrics(
                response.message.body.subtitle_list
                    .firstOrNull()
                    ?.subtitle
                    ?.subtitle_body!!,
            )
        } else {
            val unsyncedResponse = lyricsProvider.getMusixmatchUnsyncedLyrics(track.track_id.toString(), userToken).body<MusixmatchLyricsResponse>()
            if (unsyncedResponse.message.body.lyrics != null && unsyncedResponse.message.body.lyrics.lyrics_body != "") {
                return@runCatching parseUnsyncedLyrics(unsyncedResponse.message.body.lyrics.lyrics_body)
            } else {
                null
            }
        }
    }

    suspend fun getMusixmatchTranslateLyrics(
        trackId: String,
        userToken: String,
        language: String,
    ) = runCatching {
        delay(500)
        lyricsProvider
            .getMusixmatchTranslateLyrics(trackId, userToken, language)
            .bodyWithoutCaptcha<MusixmatchTranslationLyricsResponse>()
    }

    suspend fun getLrclibLyrics(
        q_track: String,
        q_artist: String,
        duration: Int?,
    ) = runCatching {
        val rs =
            lyricsProvider
                .searchLrclibLyrics(
                    q_track = q_track,
                    q_artist = q_artist,
                ).body<List<LrclibObject>>()
        val lrclibObject: LrclibObject? =
            if (duration != null) {
                rs.find { abs(it.duration.toInt() - duration) <= 10 }
            } else {
                rs.firstOrNull()
            }
        if (lrclibObject != null) {
            val syncedLyrics = lrclibObject.syncedLyrics
            val plainLyrics = lrclibObject.plainLyrics
            if (!syncedLyrics.isNullOrEmpty()) {
                parseMusixmatchLyrics(syncedLyrics)
            } else if (!plainLyrics.isNullOrEmpty()) {
                parseUnsyncedLyrics(plainLyrics)
            } else {
                null
            }
        } else {
            null
        }
    }

    private suspend inline fun <reified T> HttpResponse.bodyWithoutCaptcha() =
        try {
            this.body<T>()
        } catch (e: Exception) {
            e.printStackTrace()
            val text = this.bodyAsText()
            if (text.contains("captcha")) {
                throw CaptchaException()
            } else {
                Log.e("LyricsClient", "Error: $text")
                throw Exception("Error: $text")
            }
        }
}