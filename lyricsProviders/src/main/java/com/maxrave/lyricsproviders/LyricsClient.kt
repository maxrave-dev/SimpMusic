package com.maxrave.lyricsproviders

import android.content.Context
import android.util.Log
import com.maxrave.lyricsproviders.models.response.LrclibObject
import com.maxrave.lyricsproviders.models.response.MusixmatchCredential
import com.maxrave.lyricsproviders.models.response.MusixmatchLyricsReponse
import com.maxrave.lyricsproviders.models.response.MusixmatchLyricsResponseByQ
import com.maxrave.lyricsproviders.models.response.MusixmatchTranslationLyricsResponse
import com.maxrave.lyricsproviders.models.response.SearchMusixmatchResponse
import com.maxrave.lyricsproviders.models.response.UserTokenResponse
import com.maxrave.lyricsproviders.parser.parseMusixmatchLyrics
import com.maxrave.lyricsproviders.parser.parseUnsyncedLyrics
import com.maxrave.lyricsproviders.utils.fromArrayListNull
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import kotlinx.serialization.json.Json
import kotlin.math.abs

class LyricsClient(
    private val context: Context,
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

    var musixmatchCookie: String?
        get() = lyricsProvider.musixmatchCookie
        set(value) {
            lyricsProvider.musixmatchCookie = value
        }

    var musixmatchUserToken: String?
        get() = lyricsProvider.musixmatchUserToken
        set(value) {
            lyricsProvider.musixmatchUserToken = value
        }

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

    suspend fun searchMusixmatchTrackId(
        query: String,
        userToken: String,
    ) = runCatching {
//        val result = lyricsProvider.searchMusixmatchTrackId(query, userToken)
//        Log.w("Lyrics", "Search Track $query: " + result.bodyAsText())
//        Log.w("Lyrics", "Search Track $query: " + result.body<SearchMusixmatchResponse>().message.body.macro_result_list)
//        return@runCatching result.body<SearchMusixmatchResponse>(),
        lyricsProvider.searchMusixmatchTrackId(query, userToken).body<SearchMusixmatchResponse>()
    }

    suspend fun fixSearchMusixmatch(
        q_artist: String,
        q_track: String,
        q_duration: String,
        userToken: String,
    ) = runCatching {
        val rs = lyricsProvider.fixSearchMusixmatch(q_artist, q_track, q_duration, userToken).body<SearchMusixmatchResponse>()
        Log.w("Search Result", rs.toString())
        return@runCatching rs
    }

    suspend fun getMusixmatchLyrics(
        trackId: String,
        userToken: String,
    ) = runCatching {
        val response = lyricsProvider.getMusixmatchLyrics(trackId, userToken).body<MusixmatchLyricsReponse>()
        if (response.message.body.subtitle != null) {
            return@runCatching parseMusixmatchLyrics(response.message.body.subtitle.subtitle_body)
        } else {
            val unsyncedResponse = lyricsProvider.getMusixmatchUnsyncedLyrics(trackId, userToken).body<MusixmatchLyricsReponse>()
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
        val response = lyricsProvider.getMusixmatchLyricsByQ(track, userToken).body<MusixmatchLyricsResponseByQ>()

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
            val unsyncedResponse = lyricsProvider.getMusixmatchUnsyncedLyrics(track.track_id.toString(), userToken).body<MusixmatchLyricsReponse>()
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
        lyricsProvider
            .getMusixmatchTranslateLyrics(trackId, userToken, language)
            .body<MusixmatchTranslationLyricsResponse>()
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
            if (syncedLyrics != null) {
                parseMusixmatchLyrics(syncedLyrics)
            } else if (plainLyrics != null) {
                parseUnsyncedLyrics(plainLyrics)
            } else {
                null
            }
        } else {
            null
        }
    }
}