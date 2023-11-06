package com.maxrave.kotlinytmusicscraper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.maxrave.kotlinytmusicscraper.encoder.brotli
import com.maxrave.kotlinytmusicscraper.models.Context
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.kotlinytmusicscraper.models.body.AccountMenuBody
import com.maxrave.kotlinytmusicscraper.models.body.BrowseBody
import com.maxrave.kotlinytmusicscraper.models.body.CreatePlaylistBody
import com.maxrave.kotlinytmusicscraper.models.body.EditPlaylistBody
import com.maxrave.kotlinytmusicscraper.models.body.FormData
import com.maxrave.kotlinytmusicscraper.models.body.GetQueueBody
import com.maxrave.kotlinytmusicscraper.models.body.GetSearchSuggestionsBody
import com.maxrave.kotlinytmusicscraper.models.body.MusixmatchCredentialsBody
import com.maxrave.kotlinytmusicscraper.models.body.NextBody
import com.maxrave.kotlinytmusicscraper.models.body.PlayerBody
import com.maxrave.kotlinytmusicscraper.models.body.SearchBody
import com.maxrave.kotlinytmusicscraper.models.musixmatch.SearchMusixmatchResponse
import com.maxrave.kotlinytmusicscraper.utils.CustomRedirectConfig
import com.maxrave.kotlinytmusicscraper.utils.parseCookieString
import com.maxrave.kotlinytmusicscraper.utils.sha1
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.xml.xml
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import java.lang.reflect.Type
import java.net.Proxy
import java.util.*

class Ytmusic {
    private var httpClient = createClient()
    private var musixmatchClient = createMusixmatchClient()

    var locale = YouTubeLocale(
        gl = Locale.getDefault().country,
        hl = Locale.getDefault().toLanguageTag()
    )
    var visitorData: String = "Cgt6SUNYVzB2VkJDbyjGrrSmBg%3D%3D"
    var cookie: String? = null
        set(value) {
            field = value
            cookieMap = if (value == null) emptyMap() else parseCookieString(value)
        }
    private var cookieMap = emptyMap<String, String>()

    var musixMatchCookie: String? = null
        set(value) {
            field = value
        }

    var musixmatchUserToken: String? = null

    var proxy: Proxy? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }
    @OptIn(ExperimentalSerializationApi::class)
    private fun createMusixmatchClient() = HttpClient(OkHttp) {
        expectSuccess = true
        followRedirects = false

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
                ContentType.Text.Plain, KotlinxSerializationConverter(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    }
                )
            )
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
        }
        install(ContentEncoding) {
            brotli(1.0F)
            gzip(0.9F)
            deflate(0.8F)
        }
        defaultRequest {
            url("https://apic-desktop.musixmatch.com/ws/1.1/")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient() = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
            xml(
                format = XML {
                    xmlDeclMode = XmlDeclMode.Charset
                    autoPolymorphic = true
                },
                contentType = ContentType.Text.Xml
            )
        }

        install(ContentEncoding) {
            brotli(1.0F)
            gzip(0.9F)
            deflate(0.8F)
        }

        if (proxy != null) {
            engine {
                proxy = this@Ytmusic.proxy
            }
        }

        defaultRequest {
            url("https://music.youtube.com/youtubei/v1/")
        }
    }

    internal fun HttpRequestBuilder.mask(value: String = "*") =
        header("X-Goog-FieldMask", value)

    private fun HttpRequestBuilder.ytClient(client: YouTubeClient, setLogin: Boolean = false) {
        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientName)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("x-origin", "https://music.youtube.com")
            append("X-Goog-Visitor-Id", visitorData)
            if (client.referer != null) {
                append("Referer", client.referer)
            }
            if (setLogin) {
                cookie?.let { cookie ->
                    append("cookie", cookie)
                    if ("SAPISID" !in cookieMap) return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val sapisidHash =
                        sha1("$currentTime ${cookieMap["SAPISID"]} https://music.youtube.com")
                    append("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
        }
        userAgent(client.userAgent)
        parameter("key", client.api_key)
        parameter("prettyPrint", false)
    }

    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ) = httpClient.post("search") {
        ytClient(client, true)
        setBody(
            SearchBody(
                context = client.toContext(locale, visitorData),
                query = query,
                params = params
            )
        )
        parameter("continuation", continuation)
        parameter("ctoken", continuation)
    }

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        cpn: String?,
    ) = httpClient.post("player") {
        ytClient(client, setLogin = true)
        setBody(
            PlayerBody(
                context = client.toContext(locale, visitorData).let {
                    if (client == YouTubeClient.TVHTML5) {
                        it.copy(
                            thirdParty = Context.ThirdParty(
                                embedUrl = "https://www.youtube.com/watch?v=${videoId}"
                            )
                        )
                    } else it
                },
                videoId = videoId,
                playlistId = playlistId,
                cpn = cpn
            )
        )
    }

    suspend fun pipedStreams(videoId: String, pipedInstance: String) =
        httpClient.get("https://${pipedInstance}/streams/${videoId}") {
            contentType(ContentType.Application.Json)
        }

    suspend fun getSuggestQuery(query: String) =
        httpClient.get("http://suggestqueries.google.com/complete/search") {
            contentType(ContentType.Application.Json)
            parameter("client", "firefox")
            parameter("ds", "yt")
            parameter("q", query)
        }

    fun fromString(value: String?): List<String>? {
        val listType: Type = object : TypeToken<ArrayList<String?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    suspend fun getMusixmatchUserToken() = musixmatchClient.get("token.get?app_id=android-player-v1.0") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixMatchCookie != null) {
                val listCookies = fromString(musixMatchCookie)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie = listCookies.joinToString(separator = "; ") { eachCookie ->
                        eachCookie
                    }
                    header(HttpHeaders.Cookie, appendCookie)
                }
            }
        }
    }

    suspend fun postMusixmatchPostCredentials(email: String, password: String, userToken: String) = musixmatchClient.post("https://apic.musixmatch.com/ws/1.1/credential.post") {
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
                            password = password
                        )
                    )
                )
            )
        )
    }

    suspend fun searchMusixmatchTrackId(q: String, userToken: String) = musixmatchClient.get("track.search?app_id=android-player-v1.0&page_size=5&page=1&s_track_rating=desc&quorum_factor=1.0") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixMatchCookie != null) {
                val listCookies = fromString(musixMatchCookie)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie = listCookies.joinToString(separator = "; ") { eachCookie ->
                        eachCookie
                    }
                    header(HttpHeaders.Cookie, appendCookie)
                }
            }
        }

        parameter("q", q)
        parameter("usertoken", userToken)
    }

    suspend fun getMusixmatchLyrics(trackId: String, userToken: String) = musixmatchClient.get("track.subtitle.get?app_id=android-player-v1.0&subtitle_format=id3") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixMatchCookie != null) {
                val listCookies = fromString(musixMatchCookie)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie = listCookies.joinToString(separator = "; ") { eachCookie ->
                        eachCookie
                    }
                    header(HttpHeaders.Cookie, appendCookie)
                }
            }
        }

        parameter("usertoken", userToken)
        parameter("track_id", trackId)
    }
    suspend fun getMusixmatchLyricsByQ(track: SearchMusixmatchResponse.Message.Body.Track.TrackX, userToken: String) = musixmatchClient.get("https://apic.musixmatch.com/ws/1.1/track.subtitles.get") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixMatchCookie != null) {
                val listCookies = fromString(musixMatchCookie)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie = listCookies.joinToString(separator = "; ") { eachCookie ->
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
        parameter("part", "lyrics_crowd%2Cuser%2Clyrics_vote%2Clyrics_poll%2Ctrack_lyrics_translation_status%2Clyrics_verified_by%2Clabels%2Ctrack_structure%2Ctrack_performer_tagging%2C")
        parameter("language_iso_code", "1")
        parameter("format", "json")
        parameter("q_duration", track.track_length)
    }
    suspend fun getMusixmatchUnsyncedLyrics(trackId: String, userToken: String) = musixmatchClient.get("track.lyrics.get?app_id=android-player-v1.0&subtitle_format=id3") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixMatchCookie != null) {
                val listCookies = fromString(musixMatchCookie)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie = listCookies.joinToString(separator = "; ") { eachCookie ->
                        eachCookie
                    }
                    header(HttpHeaders.Cookie, appendCookie)
                }
            }
        }
        parameter("usertoken", userToken)
        parameter("track_id", trackId)
    }

    suspend fun getMusixmatchTranslateLyrics(trackId: String, userToken: String, language: String) = musixmatchClient.get("https://apic.musixmatch.com/ws/1.1/crowd.track.translations.get") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixMatchCookie != null) {
                val listCookies = fromString(musixMatchCookie)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie = listCookies.joinToString(separator = "; ") { eachCookie ->
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

    suspend fun getYouTubeCaption(url: String) = httpClient.get(url) {
        contentType(ContentType.Text.Xml)
        headers {
            append(HttpHeaders.Accept, "text/xml; charset=UTF-8")
        }
    }

    suspend fun createYouTubePlaylist(title: String, listVideoId: List<String>?) =
        httpClient.post("playlist/create") {
            ytClient(YouTubeClient.WEB_REMIX, setLogin = true)
            setBody(
                CreatePlaylistBody(
                    context = YouTubeClient.WEB_REMIX.toContext(locale, visitorData),
                    title = title,
                    videoIds = listVideoId
                )
            )
        }

    suspend fun editYouTubePlaylist(playlistId: String, title: String? = null) =
        httpClient.post("browse/edit_playlist") {
            ytClient(YouTubeClient.WEB_REMIX, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = YouTubeClient.WEB_REMIX.toContext(locale, visitorData),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        EditPlaylistBody.Action(
                            action = "ACTION_SET_PLAYLIST_NAME",
                            playlistName = title ?: ""
                        )
                    )
                )
            )
        }

    suspend fun addItemYouTubePlaylist(playlistId: String, videoId: String) =
        httpClient.post("browse/edit_playlist") {
            ytClient(YouTubeClient.WEB_REMIX, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = YouTubeClient.WEB_REMIX.toContext(locale, visitorData),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        EditPlaylistBody.Action(
                            playlistName = null,
                            action = "ACTION_ADD_VIDEO",
                            addedVideoId = videoId
                        )
                    )
                )
            )
        }
    suspend fun removeItemYouTubePlaylist(playlistId: String, videoId: String, setVideoId: String) =
        httpClient.post("browse/edit_playlist") {
            ytClient(YouTubeClient.WEB_REMIX, setLogin = true)
            setBody(
                EditPlaylistBody(
                    context = YouTubeClient.WEB_REMIX.toContext(locale, visitorData),
                    playlistId = playlistId.removePrefix("VL"),
                    actions = listOf(
                        EditPlaylistBody.Action(
                            playlistName = null,
                            action = "ACTION_REMOVE_VIDEO",
                            removedVideoId = videoId,
                            setVideoId = setVideoId
                        )
                    )
                )
            )
        }

    /***
     * SponsorBlock testing
     * @author maxrave-dev
     */

    suspend fun getSkipSegments(videoId: String) =
        httpClient.get("https://sponsor.ajay.app/api/skipSegments/") {
            contentType(ContentType.Application.Json)
            parameter("videoID", videoId)
            parameter("category", "sponsor")
            parameter("category", "selfpromo")
            parameter("category", "interaction")
            parameter("category", "intro")
            parameter("category", "outro")
            parameter("category", "preview")
            parameter("category", "music_offtopic")
            parameter("category", "poi_highlight")
            parameter("category", "filler")
            parameter("service", "YouTube")
        }

    suspend fun checkForUpdate() = httpClient.get("https://api.github.com/repos/maxrave-dev/SimpMusic/releases/latest") {
        contentType(ContentType.Application.Json)
    }

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        countryCode: String? = null,
        setLogin: Boolean = false,
    ) = httpClient.post("browse") {
        ytClient(client, if (setLogin) true else cookie != "" && cookie != null)

        if (countryCode != null) {
            setBody(
                BrowseBody(
                    context = client.toContext(locale, visitorData),
                    browseId = browseId,
                    params = params,
                    formData = FormData(listOf(countryCode))
                )
            )
        } else {
            setBody(
                BrowseBody(
                    context = client.toContext(locale, visitorData),
                    browseId = browseId,
                    params = params
                )
            )

        }
        parameter("alt", "json")
        if (continuation != null) {
            parameter("ctoken", continuation)
            parameter("continuation", continuation)
            parameter("type", "next")
        }
    }

    suspend fun nextCustom(client: YouTubeClient, videoId: String) = httpClient.post("next") {
        ytClient(client, setLogin = false)
        setBody(
            BrowseBody(
                context = client.toContext(locale, visitorData),
                browseId = null,
                params = "wAEB",
                enablePersistentPlaylistPanel = true,
                isAudioOnly = true,
                tunerSettingValue = "AUTOMIX_SETTING_NORMAL",
                playlistId = "RDAMVM$videoId",
                watchEndpointMusicSupportedConfigs = WatchEndpoint.WatchEndpointMusicSupportedConfigs(
                    WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig(
                        musicVideoType = "MUSIC_VIDEO_TYPE_ATV",
                    )
                )
            )
        )
        parameter("alt", "json")
    }

    suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ) = httpClient.post("next") {
        ytClient(client, setLogin = true)
        setBody(
            NextBody(
                context = client.toContext(locale, visitorData),
                videoId = videoId,
                playlistId = playlistId,
                playlistSetVideoId = playlistSetVideoId,
                index = index,
                params = params,
                continuation = continuation
            )
        )
    }

    suspend fun getSearchSuggestions(
        client: YouTubeClient,
        input: String,
    ) = httpClient.post("music/get_search_suggestions") {
        ytClient(client)
        setBody(
            GetSearchSuggestionsBody(
                context = client.toContext(locale, visitorData),
                input = input
            )
        )
    }

    suspend fun getQueue(
        client: YouTubeClient,
        videoIds: List<String>?,
        playlistId: String?,
    ) = httpClient.post("music/get_queue") {
        ytClient(client)
        setBody(
            GetQueueBody(
                context = client.toContext(locale, visitorData),
                videoIds = videoIds,
                playlistId = playlistId
            )
        )
    }

    suspend fun getSwJsData() = httpClient.get("https://music.youtube.com/sw.js_data")

    suspend fun accountMenu(client: YouTubeClient) = httpClient.post("account/account_menu") {
        ytClient(client)
        setBody(AccountMenuBody(client.toContext(locale, visitorData)))
    }

    suspend fun scrapeYouTube(
        videoId: String
    ) = httpClient.get("https://www.youtube.com/watch?v=$videoId") {
        headers {
            append(HttpHeaders.AcceptLanguage, locale.hl)
            append(HttpHeaders.ContentLanguage, locale.gl)
        }
    }

    suspend fun initPlayback(url: String, cpn: String, customParams: Map<String, String>? = null, playlistId: String?)
    = httpClient.get(url) {
        ytClient(YouTubeClient.ANDROID_MUSIC, true)
        parameter("ver", "2")
        parameter("c", "ANDROID_MUSIC")
        parameter("cpn", cpn)
        customParams?.forEach { (key, value) ->
            parameter(key, value)
        }
        if (playlistId != null) {
            parameter("list", playlistId)
            parameter("referrer", "https://music.youtube.com/playlist?list=$playlistId")
        }
    }

    suspend fun atr(url: String, cpn: String, customParams: Map<String, String>? = null, playlistId: String?) = httpClient.post(url) {
        ytClient(YouTubeClient.ANDROID_MUSIC, true)
        parameter("c", "ANDROID_MUSIC")
        parameter("cpn", cpn)
        customParams?.forEach { (key, value) ->
            parameter(key, value)
        }
        if (playlistId != null) {
            parameter("list", playlistId)
            parameter("referrer", "https://music.youtube.com/playlist?list=$playlistId")
        }
    }


}
