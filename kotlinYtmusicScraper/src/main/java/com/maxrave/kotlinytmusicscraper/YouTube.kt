package com.maxrave.kotlinytmusicscraper

import android.util.Log
import com.google.gson.Gson
import com.maxrave.kotlinytmusicscraper.models.AccountInfo
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.Artist
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint
import com.maxrave.kotlinytmusicscraper.models.GridRenderer
import com.maxrave.kotlinytmusicscraper.models.MediaType
import com.maxrave.kotlinytmusicscraper.models.MusicCarouselShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.Run
import com.maxrave.kotlinytmusicscraper.models.SearchSuggestions
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.TVHTML5
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.WEB_REMIX
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.kotlinytmusicscraper.models.getContinuation
import com.maxrave.kotlinytmusicscraper.models.musixmatch.MusixmatchCredential
import com.maxrave.kotlinytmusicscraper.models.musixmatch.MusixmatchLyricsReponse
import com.maxrave.kotlinytmusicscraper.models.musixmatch.MusixmatchLyricsResponseByQ
import com.maxrave.kotlinytmusicscraper.models.musixmatch.MusixmatchTranslationLyricsResponse
import com.maxrave.kotlinytmusicscraper.models.musixmatch.SearchMusixmatchResponse
import com.maxrave.kotlinytmusicscraper.models.musixmatch.UserTokenResponse
import com.maxrave.kotlinytmusicscraper.models.oddElements
import com.maxrave.kotlinytmusicscraper.models.response.AccountMenuResponse
import com.maxrave.kotlinytmusicscraper.models.response.AddItemYouTubePlaylistResponse
import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import com.maxrave.kotlinytmusicscraper.models.response.CreatePlaylistResponse
import com.maxrave.kotlinytmusicscraper.models.response.GetQueueResponse
import com.maxrave.kotlinytmusicscraper.models.response.GetSearchSuggestionsResponse
import com.maxrave.kotlinytmusicscraper.models.response.NextResponse
import com.maxrave.kotlinytmusicscraper.models.response.PipedResponse
import com.maxrave.kotlinytmusicscraper.models.response.PlayerResponse
import com.maxrave.kotlinytmusicscraper.models.response.SearchResponse
import com.maxrave.kotlinytmusicscraper.models.simpmusic.GithubResponse
import com.maxrave.kotlinytmusicscraper.models.splitBySeparator
import com.maxrave.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import com.maxrave.kotlinytmusicscraper.models.youtube.Transcript
import com.maxrave.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import com.maxrave.kotlinytmusicscraper.models.youtube.data.YouTubeDataPage
import com.maxrave.kotlinytmusicscraper.pages.AlbumPage
import com.maxrave.kotlinytmusicscraper.pages.ArtistPage
import com.maxrave.kotlinytmusicscraper.pages.ArtistSection
import com.maxrave.kotlinytmusicscraper.pages.BrowseResult
import com.maxrave.kotlinytmusicscraper.pages.ExplorePage
import com.maxrave.kotlinytmusicscraper.pages.MoodAndGenres
import com.maxrave.kotlinytmusicscraper.pages.NewReleaseAlbumPage
import com.maxrave.kotlinytmusicscraper.pages.NextPage
import com.maxrave.kotlinytmusicscraper.pages.NextResult
import com.maxrave.kotlinytmusicscraper.pages.PlaylistContinuationPage
import com.maxrave.kotlinytmusicscraper.pages.PlaylistPage
import com.maxrave.kotlinytmusicscraper.pages.RelatedPage
import com.maxrave.kotlinytmusicscraper.pages.SearchPage
import com.maxrave.kotlinytmusicscraper.pages.SearchResult
import com.maxrave.kotlinytmusicscraper.pages.SearchSuggestionPage
import com.maxrave.kotlinytmusicscraper.parser.parseMusixmatchLyrics
import com.maxrave.kotlinytmusicscraper.parser.parseUnsyncedLyrics
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import java.net.Proxy
import kotlin.random.Random

private fun List<PipedResponse.AudioStream>.toListFormat(): List<PlayerResponse.StreamingData.Format> {
    val list = mutableListOf<PlayerResponse.StreamingData.Format>()
    this.forEach {
        list.add(
            PlayerResponse.StreamingData.Format(
                itag = it.itag,
                url = it.url,
                mimeType = it.mimeType ?: "",
                bitrate = it.bitrate,
                width = it.width,
                height = it.height,
                contentLength = it.contentLength.toLong(),
                quality = it.quality,
                fps = it.fps,
                qualityLabel = "",
                averageBitrate = it.bitrate,
                audioQuality = it.quality,
                approxDurationMs = "",
                audioSampleRate = 0,
                audioChannels = 0,
                loudnessDb = 0.0,
                lastModified = 0,
            )
        )
    }

    return list
}

/**
 * Special thanks to [z-huang/InnerTune](https://github.com/z-huang/InnerTune)
 * This library is from [z-huang/InnerTune] and I just modified it to comply with SimpMusic
 *
 * Here is the object that can create all request to YouTube Music and Spotify in SimpMusic
 * Using YouTube Internal API, Spotify Web API and Spotify Internal API for get lyrics
 * @author maxrave-dev
 */
object YouTube {
    private val ytMusic = Ytmusic()

    /**
     * Set the locale and language for YouTube Music
     */
    var locale: YouTubeLocale
        get() = ytMusic.locale
        set(value) {
            ytMusic.locale = value
        }
    /**
     * Set custom visitorData for client (default is @see [DEFAULT_VISITOR_DATA])
     */
    var visitorData: String
        get() = ytMusic.visitorData
        set(value) {
            ytMusic.visitorData = value
        }

    /**
     * Set cookie and authentication header for client (for log in option)
     */
    var cookie: String?
        get() = ytMusic.cookie
        set(value) {
            ytMusic.cookie = value
        }

    var musixMatchCookie: String?
        get() = ytMusic.musixMatchCookie
        set(value) {
            ytMusic.musixMatchCookie = value
        }

    var musixmatchUserToken: String?
        get() = ytMusic.musixmatchUserToken
        set(value) {
            ytMusic.musixmatchUserToken = value
        }

    /**
     * Set the proxy for client
     */
    var proxy: Proxy?
        get() = ytMusic.proxy
        set(value) {
            ytMusic.proxy = value
        }

    /**
     * Search for a song, album, artist, playlist, etc.
     * @param query the search query
     * @param filter the search filter (see in [SearchFilter])
     * @return a [Result]<[SearchResult]> object
     */
    suspend fun search(query: String, filter: SearchFilter): Result<SearchResult> = runCatching {
        val response = ytMusic.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
        SearchResult(
            items = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
                ?.musicShelfRenderer?.contents?.mapNotNull {
                    SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                }.orEmpty(),
            listPodcast = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
                ?.musicShelfRenderer?.contents?.mapNotNull {
                    SearchPage.toPodcast(it.musicResponsiveListItemRenderer)
                }.orEmpty(),
            continuation = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
                ?.musicShelfRenderer?.continuations?.getContinuation()
        )
    }

    /**
     * Every search request response a limited data. Use this function to get the next data
     * @param continuation continuation token from [SearchResult.continuation]
     * @return a [Result]<[SearchResult]> object
     */
    suspend fun searchContinuation(continuation: String): Result<SearchResult> = runCatching {
        val response = ytMusic.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
        SearchResult(
            items = response.continuationContents?.musicShelfContinuation?.contents
                ?.mapNotNull {
                    SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                }!!,
            listPodcast = response.continuationContents.musicShelfContinuation.contents
                .mapNotNull {
                    SearchPage.toPodcast(it.musicResponsiveListItemRenderer)
                }.orEmpty(),
            continuation = response.continuationContents.musicShelfContinuation.continuations?.getContinuation()
        )
    }

    /**
     * Get the album page data from YouTube Music
     * @param browseId the album browseId
     * @param withSongs if true, the function will get the songs data too
     * @return a [Result]<[AlbumPage]> object
     */
    suspend fun album(browseId: String, withSongs: Boolean = true): Result<AlbumPage> = runCatching {
        val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
        val playlistId = response.microformat?.microformatDataRenderer?.urlCanonical?.substringAfterLast('=')!!
        AlbumPage(
            album = AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = response.header?.musicDetailHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                artists = response.header.musicDetailHeaderRenderer.subtitle.runs?.splitBySeparator()?.getOrNull(1)?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                }!!,
                year = response.header.musicDetailHeaderRenderer.subtitle.runs.lastOrNull()?.text?.toIntOrNull(),
                thumbnail = response.header.musicDetailHeaderRenderer.thumbnail.croppedSquareThumbnailRenderer?.getThumbnailUrl()!!
            ),
            songs = if (withSongs) albumSongs(playlistId).getOrThrow() else emptyList(),
            description = getDescriptionAlbum(response.header.musicDetailHeaderRenderer.description?.runs),
            duration = response.header.musicDetailHeaderRenderer.secondSubtitle.runs?.get(2)?.text ?: "",
            thumbnails = response.header.musicDetailHeaderRenderer.thumbnail.croppedSquareThumbnailRenderer.thumbnail,
        )
    }
    private fun getDescriptionAlbum(runs: List<Run>?): String {
        var description = ""
        if (!runs.isNullOrEmpty()) {
            for (run in runs) {
                description += run.text
            }
        }
        Log.d("description", description)
        return description
    }

    suspend fun albumSongs(playlistId: String): Result<List<SongItem>> = runCatching {
        val response = ytMusic.browse(WEB_REMIX, "VL$playlistId").body<BrowseResponse>()
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            ?.musicPlaylistShelfRenderer?.contents
            ?.mapNotNull {
                AlbumPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
            }!!
    }

    suspend fun testArtist(browseId: String): Result<ArrayList<ArtistSection>> = runCatching {
        val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
        val artistSections = arrayListOf<ArtistSection>()
        val content = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents
        if (content != null) {
            for (i in 0 until content.size) {
                ArtistPage.fromSectionListRendererContent(content.get(i))
                    ?.let { artistSections.add(it) }
                println("Section $i checking \n artistSection ${artistSections.lastOrNull()}")
            }
        }
        return@runCatching artistSections
    }

    /**
     * Get the artist page data from YouTube Music
     * @param browseId the artist browseId
     * @return a [Result]<[ArtistPage]> object
     */
    suspend fun artist(browseId: String): Result<ArtistPage> = runCatching {
        val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
        ArtistPage(
            artist = ArtistItem(
                id = browseId,
                title = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
                    ?: response.header?.musicVisualHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                thumbnail = response.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                    ?: response.header?.musicVisualHeaderRenderer?.foregroundThumbnail?.musicThumbnailRenderer?.getThumbnailUrl()!!,
                shuffleEndpoint = response.header?.musicImmersiveHeaderRenderer?.playButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint,
                radioEndpoint = response.header?.musicImmersiveHeaderRenderer?.startRadioButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint
            ),
            sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!,
            description = response.header?.musicImmersiveHeaderRenderer?.description?.runs?.firstOrNull()?.text,
            subscribers = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.longSubscriberCountText?.runs?.get(0)?.text,
            view = response.contents.singleColumnBrowseResultsRenderer.tabs[0].tabRenderer.content?.sectionListRenderer?.contents?.lastOrNull()?.musicDescriptionShelfRenderer?.subheader?.runs?.firstOrNull()?.text
        )
    }

    /**
     * Get the playlist page data from YouTube Music
     * @param playlistId the playlistId
     * @return a [Result]<[PlaylistPage]> object
     */
    suspend fun playlist(playlistId: String): Result<PlaylistPage> = runCatching {
        val response = ytMusic.browse(
            client = WEB_REMIX,
            browseId = "VL$playlistId",
            setLogin = true
        ).body<BrowseResponse>()
        val header = response.header?.musicDetailHeaderRenderer ?: response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicDetailHeaderRenderer!!
        PlaylistPage(
            playlist = PlaylistItem(
                id = playlistId,
                title = header.title.runs?.firstOrNull()?.text!!,
                author = header.subtitle.runs?.getOrNull(2)?.let {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                },
                songCountText = header.secondSubtitle.runs?.firstOrNull()?.text,
                thumbnail = header.thumbnail.croppedSquareThumbnailRenderer?.getThumbnailUrl()!!,
                playEndpoint = null,
                shuffleEndpoint = header.menu.menuRenderer.topLevelButtons?.firstOrNull()?.buttonRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!,
                radioEndpoint = header.menu.menuRenderer.items.find {
                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!
            ),
            songs = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicPlaylistShelfRenderer?.contents?.mapNotNull {
                    PlaylistPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                }!!,
            songsContinuation = response.contents.singleColumnBrowseResultsRenderer.tabs.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?.musicPlaylistShelfRenderer?.continuations?.getContinuation(),
            continuation = response.contents.singleColumnBrowseResultsRenderer.tabs.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.continuations?.getContinuation()
        )
    }

    suspend fun playlistContinuation(continuation: String) = runCatching {
        val response = ytMusic.browse(
            client = WEB_REMIX,
            continuation = continuation,
            setLogin = true
        ).body<BrowseResponse>()
        PlaylistContinuationPage(
            songs = response.continuationContents?.musicPlaylistShelfContinuation?.contents?.mapNotNull {
                PlaylistPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
            }!!,
            continuation = response.continuationContents.musicPlaylistShelfContinuation.continuations?.getContinuation()
        )
    }

    /**
     * Execute a custom POST request to YouTube Music
     * In SimpMusic, I use this function to parsing Home, Playlist, Album data instead using [album], [playlist], [artist] function
     * @param browseId the browseId (such as "FEmusic_home", "VL$playlistId", etc.)
     * @param params the params
     * @param continuation the continuation token
     * @param country the country code
     * @param setLogin if true, the function will set the cookie and authentication header
     * @return a [Result]<[BrowseResponse]> object
     */
    suspend fun customQuery(browseId: String, params: String? = null, continuation: String? = null, country: String? = null, setLogin: Boolean = true) = runCatching {
        ytMusic.browse(WEB_REMIX, browseId, params, continuation, country, setLogin).body<BrowseResponse>()
    }

    fun fromArrayListNull(list: List<String?>?): String? {
        val gson = Gson()
        return gson.toJson(list)
    }

    /**
     * Get the related data of a song from YouTube Music
     * @param videoId the videoId of song
     * @return a [Result]<[NextResponse]> object
     */
    suspend fun nextCustom(videoId: String) = runCatching {
        ytMusic.nextCustom(WEB_REMIX, videoId).body<NextResponse>()
    }

    suspend fun getMusixmatchUserToken() = runCatching {
        ytMusic.getMusixmatchUserToken().body<UserTokenResponse>()
    }
    suspend fun postMusixmatchCredentials(email: String, password: String, userToken: String) = runCatching {
        val request = ytMusic.postMusixmatchPostCredentials(email, password, userToken)
        val response = request.body<MusixmatchCredential>()
        if (response.message.body.get(0).credential.error == null && response.message.body.get(0).credential.account != null) {
            val setCookies = request.headers.getAll("Set-Cookie")
            Log.w("postMusixmatchCredentials", setCookies.toString())
            if (!setCookies.isNullOrEmpty()) {
                fromArrayListNull(setCookies)?.let {
                    musixMatchCookie = it
                }
            }
        }
        Log.w("postMusixmatchCredentials cookie", musixMatchCookie.toString())
        Log.w("postMusixmatchCredentials", response.toString())
        return@runCatching response
    }
    fun getMusixmatchCookie() = musixMatchCookie
    suspend fun searchMusixmatchTrackId(query: String, userToken: String) = runCatching {
        ytMusic.searchMusixmatchTrackId(query, userToken).body<SearchMusixmatchResponse>()
    }
    suspend fun getMusixmatchLyrics(trackId: String, userToken: String) = runCatching {
        val response = ytMusic.getMusixmatchLyrics(trackId, userToken).body<MusixmatchLyricsReponse>()
        if (response.message.body.subtitle != null) {
            return@runCatching parseMusixmatchLyrics(response.message.body.subtitle.subtitle_body)
        }
        else {
            val unsyncedResponse = ytMusic.getMusixmatchUnsyncedLyrics(trackId, userToken).body<MusixmatchLyricsReponse>()
            if (unsyncedResponse.message.body.lyrics != null && unsyncedResponse.message.body.lyrics.lyrics_body != "") {
                return@runCatching parseUnsyncedLyrics(unsyncedResponse.message.body.lyrics.lyrics_body)
            }
            else {
                null
            }
        }
    }
    suspend fun getMusixmatchLyricsByQ(track: SearchMusixmatchResponse.Message.Body.Track.TrackX, userToken: String) = runCatching {
        val response = ytMusic.getMusixmatchLyricsByQ(track, userToken).body<MusixmatchLyricsResponseByQ>()

        if (!response.message.body.subtitle_list.isNullOrEmpty() && response.message.body.subtitle_list.firstOrNull()?.subtitle?.subtitle_body != null) {
            return@runCatching parseMusixmatchLyrics(response.message.body.subtitle_list.firstOrNull()?.subtitle?.subtitle_body!!)
        }
        else {
            val unsyncedResponse = ytMusic.getMusixmatchUnsyncedLyrics(track.track_id.toString(), userToken).body<MusixmatchLyricsReponse>()
            if (unsyncedResponse.message.body.lyrics != null && unsyncedResponse.message.body.lyrics.lyrics_body != "") {
                return@runCatching parseUnsyncedLyrics(unsyncedResponse.message.body.lyrics.lyrics_body)
            }
            else {
                null
            }
        }
    }
    suspend fun getMusixmatchTranslateLyrics(trackId: String, userToken: String, language: String) = runCatching {
        ytMusic.getMusixmatchTranslateLyrics(trackId, userToken, language).body<MusixmatchTranslationLyricsResponse>()
    }

    suspend fun getYouTubeCaption(url: String) = runCatching {
        ytMusic.getYouTubeCaption(url).body<Transcript>()
    }

    /**
     * Get the suggest query from Google
     * @param query the search query
     * @return a [Result]<[ArrayList]<[String]>> object
     */
    suspend fun getSuggestQuery(query: String) = runCatching {
        val listSuggest: ArrayList<String> = arrayListOf()
        ytMusic.getSuggestQuery(query).body<String>().let { array ->
                JSONArray(array).let { jsonArray ->
                    val data = jsonArray.get(1)
                    if (data is JSONArray) {
                        for (i in 0 until data.length()) {
                            listSuggest.add(data.getString(i))
                        }
                    }
            }
        }
        return@runCatching listSuggest
    }

    /**
     * Get Skip Segments from SponsorBlock
     * @param videoId the videoId of song
     * @return a [Result]<[List]<[SkipSegments]>> object
     */
    suspend fun getSkipSegments(videoId: String) = runCatching {
        ytMusic.getSkipSegments(videoId).body<List<SkipSegments>>()
    }

    suspend fun checkForUpdate() = runCatching {
        ytMusic.checkForUpdate().body<GithubResponse>()
    }

    suspend fun explore(): Result<ExplorePage> = runCatching {
        val response = ytMusic.browse(WEB_REMIX, browseId = "FEmusic_explore").body<BrowseResponse>()
        ExplorePage(
            newReleaseAlbums = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.find {
                it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_new_releases_albums"
            }?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer).orEmpty(),
            moodAndGenres = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.find {
                it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_moods_and_genres"
            }?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicNavigationButtonRenderer }
                ?.mapNotNull(MoodAndGenres.Companion::fromMusicNavigationButtonRenderer)
                .orEmpty()
        )
    }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> = runCatching {
        val response = ytMusic.browse(WEB_REMIX, browseId = "FEmusic_moods_and_genres").body<BrowseResponse>()
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents!!
            .mapNotNull(MoodAndGenres.Companion::fromSectionListRendererContent)
    }

    suspend fun browse(browseId: String, params: String?): Result<BrowseResult> = runCatching {
        val response = ytMusic.browse(WEB_REMIX, browseId = browseId, params = params).body<BrowseResponse>()
        BrowseResult(
            title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text,
            items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { content ->
                when {
                    content.gridRenderer != null -> {
                        BrowseResult.Item(
                            title = content.gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text,
                            items = content.gridRenderer.items
                                .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer)
                        )
                    }

                    content.musicCarouselShelfRenderer != null -> {
                        BrowseResult.Item(
                            title = content.musicCarouselShelfRenderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text,
                            items = content.musicCarouselShelfRenderer.contents
                                .mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer)
                        )
                    }

                    else -> null
                }
            }.orEmpty()
        )
    }
    suspend fun getFullMetadata(videoId: String): Result<YouTubeInitialPage> = runCatching {
        val ytScrape = ytMusic.scrapeYouTube(videoId).body<String>()
        var response = ""
        val ksoupHtmlParser = KsoupHtmlParser(
            object : KsoupHtmlHandler {
                override fun onText(text: String) {
                    super.onText(text)
                    if (text.contains("var ytInitialPlayerResponse")) {
                        val temp = text.replace("var ytInitialPlayerResponse = ", "").dropLast(1)
                        Log.d("Scrape", "Temp $temp")
                        response = temp.trimIndent()
                    }
                }
            }
        )
        ksoupHtmlParser.write(ytScrape)
        ksoupHtmlParser.end()
        val json = Json { ignoreUnknownKeys = true }
        return@runCatching json.decodeFromString<YouTubeInitialPage>(response)
    }

    suspend fun player(
        videoId: String,
        playlistId: String? = null
    ): Result<Triple<String, PlayerResponse, MediaType>> = runCatching {
        val ytScrape = ytMusic.scrapeYouTube(videoId).body<String>()
        var response = ""
        var data = ""
        val ksoupHtmlParser = KsoupHtmlParser(
            object : KsoupHtmlHandler {
                override fun onText(text: String) {
                    super.onText(text)
                    if (text.contains("var ytInitialPlayerResponse")) {
                        val temp = text.replace("var ytInitialPlayerResponse = ", "").dropLast(1)
//                        println("Scrape Temp $temp")
                        response = temp.trimIndent()
                    }
                    else if (text.contains("var ytInitialData")) {
                        val temp = text.replace("var ytInitialData = ", "").dropLast(1)
//                        println("Scrape Temp $temp")
                        data = temp.trimIndent()
//                        println(data)
                    }
                }
            }
        )
        ksoupHtmlParser.write(ytScrape)
        ksoupHtmlParser.end()
        val json = Json { ignoreUnknownKeys = true }
//        println(data)
        var ytScrapeData: YouTubeDataPage? = null
        var ytScrapeInitial: YouTubeInitialPage? = null
        runCatching {
            json.decodeFromString<YouTubeDataPage>(data)
        }.onSuccess {
            ytScrapeData = it
        }.onFailure {
            it.printStackTrace()
            ytScrapeData = null
        }
        runCatching {
            json.decodeFromString<YouTubeInitialPage>(response)
        }.onSuccess {
            ytScrapeInitial = it
        }.onFailure {
            it.printStackTrace()
            ytScrapeInitial = null
        }

        val cpn = (1..16).map {
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"[Random.Default.nextInt(
                0,
                64
            )]
        }.joinToString("")
        val playerResponse =
            ytMusic.player(ANDROID_MUSIC, videoId, playlistId, cpn).body<PlayerResponse>()
        println("Thumbnails " + playerResponse.videoDetails?.thumbnail)
        val firstThumb = playerResponse.videoDetails?.thumbnail?.thumbnails?.firstOrNull()
        val thumbnails =
            if (firstThumb?.height == firstThumb?.width && firstThumb != null) MediaType.Song else MediaType.Video
        println("Player Response " + playerResponse.streamingData)
//        println( playerResponse.streamingData?.adaptiveFormats?.findLast { it.itag == 251 }?.mimeType.toString())
        if (playerResponse.playabilityStatus.status == "OK") {
            return@runCatching Triple(
                cpn, playerResponse.copy(
                    videoDetails = playerResponse.videoDetails?.copy(
//                    authorAvatar = piped.uploaderAvatar?.replace(Regex("s48"), "s960"),
                        author = ytScrapeInitial?.videoDetails?.author
                            ?: playerResponse.videoDetails.author,
                        authorAvatar = ytScrapeData?.contents?.twoColumnWatchNextResults?.results?.results?.content?.findLast { it?.videoSecondaryInfoRenderer != null }?.videoSecondaryInfoRenderer?.owner?.videoOwnerRenderer?.thumbnail?.thumbnails?.get(
                            0
                        )?.url?.replace(Regex("s48"), "s960"),
//                    authorSubCount = piped.uploaderSubscriberCount,
                        authorSubCount = ytScrapeData?.contents?.twoColumnWatchNextResults?.results?.results?.content?.findLast { it?.videoSecondaryInfoRenderer != null }?.videoSecondaryInfoRenderer?.owner?.videoOwnerRenderer?.subscriberCountText?.simpleText
                            ?: "0",
                        description = ytScrapeInitial?.videoDetails?.shortDescription,
                    ),
                    captions = ytScrapeInitial?.captions,
                ), thumbnails
            )
        }
        val safePlayerResponse =
            ytMusic.player(TVHTML5, videoId, playlistId, cpn).body<PlayerResponse>()
        println("Safe Player Response " + safePlayerResponse.playabilityStatus.status)
        if (safePlayerResponse.playabilityStatus.status == "OK" && safePlayerResponse.streamingData != null) {
            return@runCatching Triple(
                cpn, safePlayerResponse.copy(
                    videoDetails = safePlayerResponse.videoDetails?.copy(
//                    authorAvatar = piped.uploaderAvatar?.replace(Regex("s48"), "s960"),
                        author = ytScrapeInitial?.videoDetails?.author
                            ?: safePlayerResponse.videoDetails.author,
                        authorAvatar = ytScrapeData?.contents?.twoColumnWatchNextResults?.results?.results?.content?.findLast { it?.videoSecondaryInfoRenderer != null }?.videoSecondaryInfoRenderer?.owner?.videoOwnerRenderer?.thumbnail?.thumbnails?.get(
                            0
                        )?.url?.replace(Regex("s48"), "s960"),
//                    authorSubCount = piped.uploaderSubscriberCount,
                        authorSubCount = ytScrapeData?.contents?.twoColumnWatchNextResults?.results?.results?.content?.findLast { it?.videoSecondaryInfoRenderer != null }?.videoSecondaryInfoRenderer?.owner?.videoOwnerRenderer?.subscriberCountText?.simpleText
                            ?: "0",
                        description = ytScrapeInitial?.videoDetails?.shortDescription,
                    ),
                    captions = ytScrapeInitial?.captions,
                ), thumbnails
            )
        }
        else {
            val piped = ytMusic.pipedStreams(videoId, "pipedapi.kavin.rocks").body<PipedResponse>()
            Log.w("use Piped?", piped.audioStreams.toString())
            val audioStreams = piped.audioStreams
            val videoStreams = piped.videoStreams
            val stream = audioStreams + videoStreams
            return@runCatching Triple(
                cpn, safePlayerResponse.copy(
                    streamingData = PlayerResponse.StreamingData(
                        formats = stream.toListFormat(),
                        adaptiveFormats = stream.toListFormat(),
                        expiresInSeconds = 0
                    ),
                    videoDetails = safePlayerResponse.videoDetails?.copy(
//                    authorAvatar = piped.uploaderAvatar?.replace(Regex("s48"), "s960"),
                        author = ytScrapeInitial?.videoDetails?.author
                            ?: safePlayerResponse.videoDetails.author,
                        authorAvatar = ytScrapeData?.contents?.twoColumnWatchNextResults?.results?.results?.content?.findLast { it?.videoSecondaryInfoRenderer != null }?.videoSecondaryInfoRenderer?.owner?.videoOwnerRenderer?.thumbnail?.thumbnails?.get(
                            0
                        )?.url?.replace(Regex("s48"), "s960"),
//                    authorSubCount = piped.uploaderSubscriberCount,
                        authorSubCount = ytScrapeData?.contents?.twoColumnWatchNextResults?.results?.results?.content?.findLast { it?.videoSecondaryInfoRenderer != null }?.videoSecondaryInfoRenderer?.owner?.videoOwnerRenderer?.subscriberCountText?.simpleText
                            ?: "0",
                        description = ytScrapeInitial?.videoDetails?.shortDescription,
                    ),
                    captions = ytScrapeInitial?.captions,
                ), thumbnails
            )
        }
    }
    suspend fun updateWatchTime(watchtimeUrl: String, watchtimeList: ArrayList<Float>, cpn: String, playlistId: String?): Result<Int> =
        runCatching {
            val et = watchtimeList.takeLast(2).joinToString(",")
            val watchtime = watchtimeList.dropLast(1).takeLast(2).joinToString(",")
            ytMusic.initPlayback(watchtimeUrl, cpn, mapOf("st" to watchtime, "et" to et), playlistId).status.value.let { status ->
                if (status == 204) {
                    println("watchtime done")
                }
                return@runCatching status
            }
        }
    suspend fun updateWatchTimeFull(watchtimeUrl: String, cpn: String, playlistId: String?): Result<Int> =
        runCatching {
            val regex = Regex("len=([^&]+)")
            val length = regex.find(watchtimeUrl)?.groupValues?.firstOrNull()?.drop(4) ?: "0"
            println(length)
            ytMusic.initPlayback(watchtimeUrl, cpn, mapOf("st" to length, "et" to length), playlistId).status.value.let { status ->
                if (status == 204) {
                    println("watchtime full done")
                }
                return@runCatching status
            }
        }

    /**
     * @return [Pair<Int, Float>]
     * Int: status code
     * Float: second watchtime
     * First watchtime is 5.54
     */
    suspend fun initPlayback(playbackUrl: String, atrUrl: String, watchtimeUrl: String, cpn: String, playlistId: String?): Result<Pair<Int, Float>> {
        println("playbackUrl $playbackUrl")
        println("atrUrl $atrUrl")
        println("watchtimeUrl $watchtimeUrl")
        return runCatching {
            ytMusic.initPlayback(playbackUrl, cpn, null, playlistId).status.value.let { status ->
                if (status == 204) {
                    println("playback done")
                    ytMusic.initPlayback(watchtimeUrl, cpn, mapOf("st" to "0", "et" to "5.54"), playlistId).status.value.let { firstWatchTime ->
                        if (firstWatchTime == 204) {
                            println("first watchtime done")
                            delay(5000)
                            ytMusic.atr(atrUrl, cpn, null, playlistId).status.value.let { atr ->
                                if (atr == 204) {
                                    println("atr done")
                                    delay(500)
                                    val secondWatchTime = (Math.round(Random.nextFloat()*100.0)/100.0).toFloat() + 12f
                                    ytMusic.initPlayback(watchtimeUrl, cpn, mapOf<String, String>("st" to "0,5.54", "et" to "5.54,$secondWatchTime"), playlistId).status.value.let { watchtime ->
                                        if (watchtime == 204) {
                                            println("watchtime done")
                                            return@runCatching Pair(watchtime, secondWatchTime)
                                        }
                                        else {
                                            return@runCatching Pair(watchtime, secondWatchTime)
                                        }
                                    }
                                }
                                else {
                                    return@runCatching Pair(atr, 0f)
                                }
                            }
                        }
                        else {
                            return@runCatching Pair(firstWatchTime, 0f)
                        }
                    }
                }
                else {
                    return@runCatching Pair(status, 0f)
                }
            }
        }
    }

    suspend fun next(endpoint: WatchEndpoint, continuation: String? = null): Result<NextResult> = runCatching {
        val response = ytMusic.next(WEB_REMIX, endpoint.videoId, endpoint.playlistId, endpoint.playlistSetVideoId, endpoint.index, endpoint.params, continuation).body<NextResponse>()
        val playlistPanelRenderer = response.continuationContents?.playlistPanelContinuation
            ?: response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs[0].tabRenderer.content?.musicQueueRenderer?.content?.playlistPanelRenderer!!
        // load automix items
        playlistPanelRenderer.contents.lastOrNull()?.automixPreviewVideoRenderer?.content?.automixPlaylistVideoRenderer?.navigationEndpoint?.watchPlaylistEndpoint?.let { watchPlaylistEndpoint ->
            return@runCatching next(watchPlaylistEndpoint).getOrThrow().let { result ->
                result.copy(
                    title = playlistPanelRenderer.title,
                    items = playlistPanelRenderer.contents.mapNotNull {
                        it.playlistPanelVideoRenderer?.let { renderer ->
                            NextPage.fromPlaylistPanelVideoRenderer(renderer)
                        }
                    } + result.items,
                    lyricsEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(1)?.tabRenderer?.endpoint?.browseEndpoint,
                    relatedEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(2)?.tabRenderer?.endpoint?.browseEndpoint,
                    currentIndex = playlistPanelRenderer.currentIndex,
                    endpoint = watchPlaylistEndpoint
                )
            }
        }
        NextResult(
            title = playlistPanelRenderer.title,
            items = playlistPanelRenderer.contents.mapNotNull {
                it.playlistPanelVideoRenderer?.let(NextPage::fromPlaylistPanelVideoRenderer)
            },
            currentIndex = playlistPanelRenderer.currentIndex,
            lyricsEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(1)?.tabRenderer?.endpoint?.browseEndpoint,
            relatedEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(2)?.tabRenderer?.endpoint?.browseEndpoint,
            continuation = playlistPanelRenderer.continuations?.getContinuation(),
            endpoint = endpoint
        )
    }

    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> = runCatching {
        val response = ytMusic.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
        response.contents?.sectionListRenderer?.contents?.firstOrNull()?.musicDescriptionShelfRenderer?.description?.runs?.firstOrNull()?.text
    }

    suspend fun queue(videoIds: List<String>? = null, playlistId: String? = null): Result<List<SongItem>> = runCatching {
        if (videoIds != null) {
            assert(videoIds.size <= MAX_GET_QUEUE_SIZE) // Max video limit
        }
        ytMusic.getQueue(WEB_REMIX, videoIds, playlistId).body<GetQueueResponse>().queueDatas
            .mapNotNull {
                it.content.playlistPanelVideoRenderer?.let { renderer ->
                    NextPage.fromPlaylistPanelVideoRenderer(renderer)
                }
            }
    }

    suspend fun visitorData(): Result<String> = runCatching {
        Json.parseToJsonElement(ytMusic.getSwJsData().bodyAsText().substring(5))
            .jsonArray[0]
            .jsonArray[2]
            .jsonArray.first { (it as? JsonPrimitive)?.content?.startsWith(VISITOR_DATA_PREFIX) == true }
            .jsonPrimitive.content
    }

    suspend fun accountInfo(): Result<AccountInfo?> = runCatching {
        ytMusic.accountMenu(WEB_REMIX).body<AccountMenuResponse>().actions[0].openPopupAction.popup.multiPageMenuRenderer.header?.activeAccountHeaderRenderer?.toAccountInfo()
    }

    suspend fun pipeStream(videoId: String, pipedInstance: String) = runCatching {
        ytMusic.pipedStreams(videoId, pipedInstance).body<PipedResponse>()
    }

    suspend fun getLibraryPlaylists() = runCatching {
        ytMusic.browse(WEB_REMIX, "FEmusic_liked_playlists", setLogin = true).body<BrowseResponse>()
    }

    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
            val FILTER_PODCAST = SearchFilter("EgWKAQJQAWoIEBAQERADEBU%3D")
        }
    }

    suspend fun getYTMusicSearchSuggestions(query: String) = runCatching {
        val response = ytMusic.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>()
        SearchSuggestions(
            queries = response.contents?.getOrNull(0)?.searchSuggestionsSectionRenderer?.contents?.mapNotNull { content ->
                content.searchSuggestionRenderer?.suggestion?.runs?.joinToString(separator = "") { it.text }
            }.orEmpty(),
            recommendedItems = response.contents?.getOrNull(1)?.searchSuggestionsSectionRenderer?.contents?.mapNotNull {
                it.musicResponsiveListItemRenderer?.let { renderer ->
                    SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)
                }
            }.orEmpty()
        )
    }

    suspend fun scrapeYouTube(videoId: String) = runCatching {
        ytMusic.scrapeYouTube(videoId).body<String>()
    }

    suspend fun removeItemYouTubePlaylist(playlistId: String, videoId: String, setVideoId: String) = runCatching {
        ytMusic.removeItemYouTubePlaylist(playlistId, videoId, setVideoId).status.value
    }

    suspend fun addPlaylistItem(playlistId: String, videoId: String) = runCatching {
        ytMusic.addItemYouTubePlaylist(playlistId, videoId).body<AddItemYouTubePlaylistResponse>()
    }

    suspend fun editPlaylist(playlistId: String, title: String) = runCatching {
        ytMusic.editYouTubePlaylist(playlistId, title).status.value
    }

    suspend fun createPlaylist(title: String, listVideoId: List<String>?) = runCatching {
        ytMusic.createYouTubePlaylist(title, listVideoId).body<CreatePlaylistResponse>()
    }

    const val MAX_GET_QUEUE_SIZE = 1000

    private const val VISITOR_DATA_PREFIX = "Cgt"

    const val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
}
