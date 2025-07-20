package com.maxrave.kotlinytmusicscraper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.util.Log
import android.webkit.CookieManager
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.liskovsoft.sharedutils.prefs.GlobalPreferences
import com.liskovsoft.youtubeapi.app.AppService
import com.liskovsoft.youtubeapi.service.internal.MediaServiceData
import com.maxrave.kotlinytmusicscraper.YouTube.Companion.DEFAULT_VISITOR_DATA
import com.maxrave.kotlinytmusicscraper.extension.toListFormat
import com.maxrave.kotlinytmusicscraper.models.AccountInfo
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.Artist
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint
import com.maxrave.kotlinytmusicscraper.models.GridRenderer
import com.maxrave.kotlinytmusicscraper.models.MediaType
import com.maxrave.kotlinytmusicscraper.models.MusicCarouselShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.PoToken
import com.maxrave.kotlinytmusicscraper.models.ReturnYouTubeDislikeResponse
import com.maxrave.kotlinytmusicscraper.models.Run
import com.maxrave.kotlinytmusicscraper.models.SearchSuggestions
import com.maxrave.kotlinytmusicscraper.models.SongInfo
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.YTItemType
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.TVHTML5
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.WEB
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.WEB_REMIX
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.kotlinytmusicscraper.models.getContinuation
import com.maxrave.kotlinytmusicscraper.models.oddElements
import com.maxrave.kotlinytmusicscraper.models.response.AccountMenuResponse
import com.maxrave.kotlinytmusicscraper.models.response.AddItemYouTubePlaylistResponse
import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import com.maxrave.kotlinytmusicscraper.models.response.CreatePlaylistResponse
import com.maxrave.kotlinytmusicscraper.models.response.DownloadProgress
import com.maxrave.kotlinytmusicscraper.models.response.GetQueueResponse
import com.maxrave.kotlinytmusicscraper.models.response.GetSearchSuggestionsResponse
import com.maxrave.kotlinytmusicscraper.models.response.LikeStatus
import com.maxrave.kotlinytmusicscraper.models.response.NextAndroidMusicResponse
import com.maxrave.kotlinytmusicscraper.models.response.NextResponse
import com.maxrave.kotlinytmusicscraper.models.response.PipedResponse
import com.maxrave.kotlinytmusicscraper.models.response.PlayerResponse
import com.maxrave.kotlinytmusicscraper.models.response.SearchResponse
import com.maxrave.kotlinytmusicscraper.models.response.toLikeStatus
import com.maxrave.kotlinytmusicscraper.models.simpmusic.GithubResponse
import com.maxrave.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import com.maxrave.kotlinytmusicscraper.models.youtube.GhostResponse
import com.maxrave.kotlinytmusicscraper.models.youtube.Transcript
import com.maxrave.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import com.maxrave.kotlinytmusicscraper.pages.AlbumPage
import com.maxrave.kotlinytmusicscraper.pages.ArtistPage
import com.maxrave.kotlinytmusicscraper.pages.ArtistSection
import com.maxrave.kotlinytmusicscraper.pages.BrowseResult
import com.maxrave.kotlinytmusicscraper.pages.ExplorePage
import com.maxrave.kotlinytmusicscraper.pages.MoodAndGenres
import com.maxrave.kotlinytmusicscraper.pages.NextPage
import com.maxrave.kotlinytmusicscraper.pages.NextResult
import com.maxrave.kotlinytmusicscraper.pages.PlaylistContinuationPage
import com.maxrave.kotlinytmusicscraper.pages.PlaylistPage
import com.maxrave.kotlinytmusicscraper.pages.RelatedPage
import com.maxrave.kotlinytmusicscraper.pages.SearchPage
import com.maxrave.kotlinytmusicscraper.pages.SearchResult
import com.maxrave.kotlinytmusicscraper.pages.SearchSuggestionPage
import com.maxrave.kotlinytmusicscraper.parser.fromPlaylistContinuationToTracks
import com.maxrave.kotlinytmusicscraper.parser.fromPlaylistToTrack
import com.maxrave.kotlinytmusicscraper.parser.fromPlaylistToTrackWithSetVideoId
import com.maxrave.kotlinytmusicscraper.parser.getContinuePlaylistContinuation
import com.maxrave.kotlinytmusicscraper.parser.getPlaylistContinuation
import com.maxrave.kotlinytmusicscraper.parser.getReloadParams
import com.maxrave.kotlinytmusicscraper.parser.getSuggestionSongItems
import com.maxrave.kotlinytmusicscraper.parser.hasReloadParams
import com.maxrave.kotlinytmusicscraper.utils.NewPipeUtils
import com.maxrave.kotlinytmusicscraper.utils.poTokenUtils.PoTokenGenerator
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.Proxy
import kotlin.random.Random

/**
 * Special thanks to [z-huang/InnerTune](https://github.com/z-huang/InnerTune)
 * This library is from [z-huang/InnerTune] and I just modified it to comply with SimpMusic
 *
 * Here is the object that can create all request to YouTube Music and Spotify in SimpMusic
 * Using YouTube Internal API
 * @author maxrave-dev
 */
class YouTube(
    private val context: Context,
) {
    private val ytMusic = Ytmusic()
    private var newPipeUtils = NewPipeUtils()
    private val mAppService = AppService.instance()
    private val poTokenGenerator =
        PoTokenGenerator(
            context = context,
        )

    var cachePath: File?
        get() = ytMusic.cachePath
        set(value) {
            ytMusic.cachePath = value
        }

    var cacheControlInterceptor: Interceptor?
        get() = ytMusic.cacheControlInterceptor
        set(value) {
            ytMusic.cacheControlInterceptor = value
        }

    var forceCacheInterceptor: Interceptor?
        get() = ytMusic.forceCacheInterceptor
        set(value) {
            ytMusic.forceCacheInterceptor = value
        }

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
    var visitorData: String?
        get() = ytMusic.visitorData
        set(value) {
            ytMusic.visitorData = value
        }

    var dataSyncId: String?
        get() = ytMusic.dataSyncId
        set(value) {
            ytMusic.dataSyncId = value
        }

    /**
     * Set cookie and authentication header for client (for log in option)
     */
    var cookie: String?
        get() = ytMusic.cookie
        set(value) {
            CookieManager.getInstance().setCookie("https://www.youtube.com", value)
            ytMusic.cookie = value
        }

    /**
     * Json deserializer for PO token request
     */
    private val poTokenJsonDeserializer =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            coerceInputValues = true
            useArrayPolymorphism = true
        }

    private fun String.getPoToken(): String? =
        this
            .replace("[", "")
            .replace("]", "")
            .split(",")
            .findLast { it.contains("\"") }
            ?.replace("\"", "")

    private var poTokenObject: Pair<String?, Long> = Pair(null, 0)

    /**
     * Remove proxy for client
     */
    fun removeProxy() {
        ytMusic.proxy = null
        newPipeUtils = NewPipeUtils()
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
            ytMusic.proxy = it
            newPipeUtils = NewPipeUtils(it)
        }.onFailure {
            it.printStackTrace()
        }
    }

    private val listPipedInstances =
        listOf(
            "https://pipedapi.nosebs.ru",
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.tokhmi.xyz",
            "https://pipedapi.syncpundit.io",
            "https://pipedapi.leptons.xyz",
            "https://pipedapi.r4fo.com",
            "https://yapi.vyper.me",
            "https://pipedapi-libre.kavin.rocks",
        )

    /**
     * Search for a song, album, artist, playlist, etc.
     * @param query the search query
     * @param filter the search filter (see in [SearchFilter])
     * @return a [Result]<[SearchResult]> object
     */
    suspend fun search(
        query: String,
        filter: SearchFilter,
    ): Result<SearchResult> =
        runCatching {
            val response = ytMusic.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
            SearchResult(
                items =
                    response.contents
                        ?.tabbedSearchResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                        }.orEmpty(),
                listPodcast =
                    response.contents
                        ?.tabbedSearchResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            SearchPage.toPodcast(it.musicResponsiveListItemRenderer)
                        }.orEmpty(),
                continuation =
                    response.contents
                        ?.tabbedSearchResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicShelfRenderer
                        ?.continuations
                        ?.getContinuation(),
            )
        }

    /**
     * Every search request response a limited data. Use this function to get the next data
     * @param continuation continuation token from [SearchResult.continuation]
     * @return a [Result]<[SearchResult]> object
     */
    suspend fun searchContinuation(continuation: String): Result<SearchResult> =
        runCatching {
            val response = ytMusic.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
            SearchResult(
                items =
                    response.continuationContents
                        ?.musicShelfContinuation
                        ?.contents
                        ?.mapNotNull {
                            SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                        }!!,
                listPodcast =
                    response.continuationContents.musicShelfContinuation.contents
                        .mapNotNull {
                            SearchPage.toPodcast(it.musicResponsiveListItemRenderer)
                        }.orEmpty(),
                continuation =
                    response.continuationContents.musicShelfContinuation.continuations
                        ?.getContinuation(),
            )
        }

    /**
     * Get the album page data from YouTube Music
     * @param browseId the album browseId
     * @param withSongs if true, the function will get the songs data too
     * @return a [Result]<[AlbumPage]> object
     */
    suspend fun album(
        browseId: String,
        withSongs: Boolean = true,
    ): Result<AlbumPage> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            val playlistId =
                response.microformat
                    ?.microformatDataRenderer
                    ?.urlCanonical
                    ?.substringAfterLast('=')!!
            val albumItem =
                AlbumItem(
                    browseId = browseId,
                    playlistId = playlistId,
                    title =
                        response.contents
                            ?.twoColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: "",
                    artists =
                        response.contents
                            ?.twoColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.straplineTextOne
                            ?.runs
                            ?.oddElements()
                            ?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            }!!,
                    year =
                        response.contents.twoColumnBrowseResultsRenderer.tabs
                            .firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.subtitle
                            ?.runs
                            ?.lastOrNull()
                            ?.text
                            ?.toIntOrNull(),
                    thumbnail =
                        response.contents.twoColumnBrowseResultsRenderer.tabs
                            .firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.thumbnail
                            ?.musicThumbnailRenderer
                            ?.getThumbnailUrl()!!,
                )
            AlbumPage(
                album = albumItem,
                songs =
                    if (withSongs) {
                        albumSongs(
                            response.contents
                                .twoColumnBrowseResultsRenderer
                                .secondaryContents
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicShelfRenderer
                                ?.contents,
                            albumItem,
                        ).getOrThrow()
                    } else {
                        emptyList()
                    },
                description =
                    getDescriptionAlbum(
                        response.contents.twoColumnBrowseResultsRenderer.tabs
                            .firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.description
                            ?.musicDescriptionShelfRenderer
                            ?.description
                            ?.runs,
                    ),
                duration =
                    response.contents.twoColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicResponsiveHeaderRenderer
                        ?.secondSubtitle
                        ?.runs
                        ?.get(2)
                        ?.text ?: "",
                thumbnails =
                    response.contents.twoColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicResponsiveHeaderRenderer
                        ?.thumbnail
                        ?.musicThumbnailRenderer
                        ?.thumbnail,
                otherVersion =
                    response.contents
                        .twoColumnBrowseResultsRenderer
                        .secondaryContents
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicCarouselShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            AlbumPage.fromMusicTwoRowItemRenderer(
                                it.musicTwoRowItemRenderer,
                            )
                        } ?: emptyList(),
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

    private fun albumSongs(
        content: List<MusicShelfRenderer.Content>?,
        album: AlbumItem,
    ): Result<List<SongItem>> =
        runCatching {
            if (content == null) {
                return@runCatching emptyList()
            } else {
                return@runCatching content.mapNotNull {
                    AlbumPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer, album)
                }
            }
        }

    suspend fun testArtist(browseId: String): Result<ArrayList<ArtistSection>> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            val artistSections = arrayListOf<ArtistSection>()
            val content =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
            if (content != null) {
                for (i in 0 until content.size) {
                    ArtistPage
                        .fromSectionListRendererContent(content.get(i))
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
    suspend fun artist(browseId: String): Result<ArtistPage> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            ArtistPage(
                artist =
                    ArtistItem(
                        id = browseId,
                        title =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?: response.header
                                    ?.musicVisualHeaderRenderer
                                    ?.title
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text!!,
                        thumbnail =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.thumbnail
                                ?.musicThumbnailRenderer
                                ?.getThumbnailUrl()
                                ?: response.header
                                    ?.musicVisualHeaderRenderer
                                    ?.foregroundThumbnail
                                    ?.musicThumbnailRenderer
                                    ?.getThumbnailUrl()!!,
                        shuffleEndpoint =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.playButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchEndpoint,
                        radioEndpoint =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.startRadioButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchEndpoint,
                    ),
                sections =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!,
                description =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.description
                        ?.runs
                        ?.firstOrNull()
                        ?.text,
                subscribers =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.subscriptionButton
                        ?.subscribeButtonRenderer
                        ?.longSubscriberCountText
                        ?.runs
                        ?.get(
                            0,
                        )?.text,
                view =
                    response.contents.singleColumnBrowseResultsRenderer.tabs[0]
                        .tabRenderer.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicDescriptionShelfRenderer
                        ?.subheader
                        ?.runs
                        ?.firstOrNull()
                        ?.text,
            )
        }

    suspend fun getYouTubePlaylistFullTracksWithSetVideoId(playlistId: String): Result<List<Pair<SongItem, String>>> =
        runCatching {
            val plId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
            // SongItem / SetVideoId
            val listPair = mutableListOf<Pair<SongItem, String>>()
            val response = ytMusic.playlist(plId).body<BrowseResponse>()
            listPair.addAll(
                response.fromPlaylistToTrackWithSetVideoId(),
            )
            var continuation = response.getPlaylistContinuation()
            while (continuation != null) {
                val continuationResponse =
                    ytMusic
                        .browse(
                            client = WEB_REMIX,
                            setLogin = true,
                            params = null,
                            continuation = continuation,
                        ).body<BrowseResponse>()
                listPair.addAll(
                    continuationResponse.fromPlaylistToTrackWithSetVideoId(),
                )
                continuation = continuationResponse.getContinuePlaylistContinuation()
            }

            return@runCatching listPair
        }

    suspend fun getSuggestionsTrackForPlaylist(playlistId: String): Result<Pair<String?, List<SongItem>?>?> =
        runCatching {
            val initialResponse =
                ytMusic
                    .playlist(
                        if (playlistId.startsWith("VL")) playlistId else "VL$playlistId",
                    ).body<BrowseResponse>()
            var continuation = initialResponse.getPlaylistContinuation()
            println("YouTube: getSuggestionsTrackForPlaylist: $continuation")
            while (continuation != null) {
                val continuationResponse =
                    ytMusic
                        .browse(
                            client = WEB_REMIX,
                            setLogin = true,
                            params = "wAEB",
                            continuation = continuation,
                        ).body<BrowseResponse>()
                println("YouTube: getSuggestionsTrackForPlaylist: ${continuationResponse.getReloadParams()}")
                if (continuationResponse.hasReloadParams()) {
                    return@runCatching Pair(continuationResponse.getReloadParams(), continuationResponse.getSuggestionSongItems())
                } else {
                    continuation = continuationResponse.getContinuePlaylistContinuation()
                }
            }
            return@runCatching null
        }

    suspend fun getPlaylistFullTracks(playlistId: String): Result<List<SongItem>> =
        runCatching {
            val songs = mutableListOf<SongItem>()
            val response = ytMusic.playlist(playlistId).body<BrowseResponse>()
            songs.addAll(
                response.fromPlaylistToTrack(),
            )
            var continuation = response.getPlaylistContinuation()
            while (continuation != null) {
                val continuationResponse =
                    ytMusic
                        .browse(
                            client = WEB_REMIX,
                            setLogin = true,
                            params = null,
                            continuation = continuation,
                        ).body<BrowseResponse>()
                songs.addAll(
                    continuationResponse.fromPlaylistContinuationToTracks(),
                )
                continuation = continuationResponse.getContinuePlaylistContinuation()
            }
            return@runCatching songs
        }

    /**
     * Get the playlist page data from YouTube Music
     * @param playlistId the playlistId
     * @return a [Result]<[PlaylistPage]> object
     */
    suspend fun playlist(playlistId: String): Result<PlaylistPage> =
        runCatching {
            val response =
                ytMusic
                    .browse(
                        client = WEB_REMIX,
                        browseId = "VL$playlistId",
                        setLogin = true,
                    ).body<BrowseResponse>()
            val header =
                response.header?.musicDetailHeaderRenderer
                    ?: response.header
                        ?.musicEditablePlaylistDetailHeaderRenderer
                        ?.header
                        ?.musicDetailHeaderRenderer!!
            PlaylistPage(
                playlist =
                    PlaylistItem(
                        id = playlistId,
                        title =
                            header.title.runs
                                ?.firstOrNull()
                                ?.text!!,
                        author =
                            header.subtitle.runs?.getOrNull(2)?.let {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            },
                        songCountText =
                            header.secondSubtitle.runs
                                ?.firstOrNull()
                                ?.text,
                        thumbnail = header.thumbnail.croppedSquareThumbnailRenderer?.getThumbnailUrl()!!,
                        playEndpoint = null,
                        shuffleEndpoint =
                            header.menu.menuRenderer.topLevelButtons
                                ?.firstOrNull()
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint!!,
                        radioEndpoint =
                            header.menu.menuRenderer.items
                                .find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint!!,
                    ),
                songs =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicPlaylistShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            PlaylistPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                        }!!,
                songsContinuation =
                    response.contents.singleColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicPlaylistShelfRenderer
                        ?.continuations
                        ?.getContinuation(),
                continuation =
                    response.contents.singleColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.continuations
                        ?.getContinuation(),
            )
        }

    suspend fun playlistContinuation(continuation: String) =
        runCatching {
            val response =
                ytMusic
                    .browse(
                        client = WEB_REMIX,
                        continuation = continuation,
                        setLogin = true,
                    ).body<BrowseResponse>()
            PlaylistContinuationPage(
                songs =
                    response.continuationContents?.musicPlaylistShelfContinuation?.contents?.mapNotNull {
                        PlaylistPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                    }!!,
                continuation =
                    response.continuationContents.musicPlaylistShelfContinuation.continuations
                        ?.getContinuation(),
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
    suspend fun customQuery(
        browseId: String?,
        params: String? = null,
        continuation: String? = null,
        country: String? = null,
        setLogin: Boolean = true,
    ) = runCatching {
        ytMusic.browse(WEB_REMIX, browseId, params, continuation, country, setLogin).body<BrowseResponse>()
    }

    /**
     * Get the related data of a song from YouTube Music
     * @param videoId the videoId of song
     * @return a [Result]<[NextResponse]> object
     */
    suspend fun nextCustom(videoId: String) =
        runCatching {
            ytMusic.nextCustom(WEB_REMIX, videoId).body<NextResponse>()
        }

    /**
     * Get the suggest query from Google
     * @param query the search query
     * @return a [Result]<[ArrayList]<[String]>> object
     */
    suspend fun getSuggestQuery(query: String) =
        runCatching {
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
    suspend fun getSkipSegments(videoId: String) =
        runCatching {
            ytMusic.getSkipSegments(videoId).body<List<SkipSegments>>()
        }

    suspend fun checkForUpdate() =
        runCatching {
            ytMusic.checkForUpdate().body<GithubResponse>()
        }

    suspend fun newRelease(): Result<ExplorePage> =
        runCatching {
            val response =
                ytMusic.browse(WEB_REMIX, browseId = "FEmusic_new_releases").body<BrowseResponse>()
            println(response)
//        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items
//            ?.mapNotNull { it.musicTwoRowItemRenderer }
//            ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
//            .orEmpty()
            ExplorePage(
                released =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.gridRenderer
                        ?.items
                        ?.mapNotNull { it.musicTwoRowItemRenderer }
                        ?.mapNotNull(RelatedPage::fromMusicTwoRowItemRenderer)
                        .orEmpty()
                        .mapNotNull {
                            if (it.type == YTItemType.PLAYLIST) it as? PlaylistItem else null
                        },
                musicVideo =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicCarouselShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            it.musicTwoRowItemRenderer
                        }?.mapNotNull(
                            ArtistPage::fromMusicTwoRowItemRenderer,
                        ).orEmpty()
                        .mapNotNull {
                            if (it.type == YTItemType.VIDEO) it as? VideoItem else null
                        },
            )
        }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId = "FEmusic_moods_and_genres").body<BrowseResponse>()
            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents!!
                .mapNotNull(MoodAndGenres.Companion::fromSectionListRendererContent)
        }

    suspend fun browse(
        browseId: String,
        params: String?,
    ): Result<BrowseResult> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId = browseId, params = params).body<BrowseResponse>()
            BrowseResult(
                title =
                    response.header
                        ?.musicHeaderRenderer
                        ?.title
                        ?.runs
                        ?.firstOrNull()
                        ?.text,
                items =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.mapNotNull { content ->
                            when {
                                content.gridRenderer != null -> {
                                    BrowseResult.Item(
                                        title =
                                            content.gridRenderer.header
                                                ?.gridHeaderRenderer
                                                ?.title
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.text,
                                        items =
                                            content.gridRenderer.items
                                                .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer),
                                    )
                                }

                                content.musicCarouselShelfRenderer != null -> {
                                    BrowseResult.Item(
                                        title =
                                            content.musicCarouselShelfRenderer.header
                                                ?.musicCarouselShelfBasicHeaderRenderer
                                                ?.title
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.text,
                                        items =
                                            content.musicCarouselShelfRenderer.contents
                                                .mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer),
                                    )
                                }

                                else -> null
                            }
                        }.orEmpty(),
            )
        }

    suspend fun getFullMetadata(videoId: String): Result<YouTubeInitialPage> =
        runCatching {
            val ytScrape = ytMusic.scrapeYouTube(videoId).body<String>()
            var response = ""
            val ksoupHtmlParser =
                KsoupHtmlParser(
                    object : KsoupHtmlHandler {
                        override fun onText(text: String) {
                            super.onText(text)
                            if (text.contains("var ytInitialPlayerResponse")) {
                                val temp = text.replace("var ytInitialPlayerResponse = ", "").dropLast(1)
                                Log.d("Scrape", "Temp $temp")
                                response = temp.trimIndent()
                            }
                        }
                    },
                )
            ksoupHtmlParser.write(ytScrape)
            ksoupHtmlParser.end()
            val json = Json { ignoreUnknownKeys = true }
            return@runCatching json.decodeFromString<YouTubeInitialPage>(response)
        }

    suspend fun getLikedInfo(videoId: String): Result<LikeStatus> =
        runCatching {
            val response =
                ytMusic
                    .next(
                        WEB_REMIX,
                        videoId,
                        null,
                        null,
                        null,
                        null,
                        null,
                    ).body<NextAndroidMusicResponse>()
            val likeStatus =
                response.playerOverlays
                    ?.playerOverlayRenderer
                    ?.actions
                    ?.find { it.likeButtonRenderer != null }
                    ?.likeButtonRenderer
                    ?.likeStatus
                    ?.toLikeStatus()
            Log.w("YouTube", "Like Status ${response.playerOverlays}")
            return@runCatching likeStatus ?: LikeStatus.INDIFFERENT
        }

    suspend fun getSongInfo(videoId: String): Result<SongInfo> =
        runCatching {
            val ytNext = ytMusic.next(WEB, videoId, null, null, null, null, null).body<NextResponse>()
            val videoSecondary =
                ytNext.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoSecondaryInfoRenderer != null
                    }?.videoSecondaryInfoRenderer
            val videoPrimary =
                ytNext.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoPrimaryInfoRenderer != null
                    }?.videoPrimaryInfoRenderer
            val returnYouTubeDislikeResponse =
                ytMusic.returnYouTubeDislike(videoId).body<ReturnYouTubeDislikeResponse>()
            return@runCatching SongInfo(
                videoId = videoId,
                author =
                    videoSecondary?.owner?.videoOwnerRenderer?.title?.runs?.firstOrNull()?.text?.replace(
                        Regex(" - Topic| - Chủ đề|"),
                        "",
                    ),
                authorId =
                    videoSecondary
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseId,
                authorThumbnail =
                    videoSecondary
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.thumbnail
                        ?.thumbnails
                        ?.find {
                            it.height == 48
                        }?.url
                        ?.replace("s48", "s960"),
                description = videoSecondary?.attributedDescription?.content,
                subscribers =
                    videoSecondary
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.subscriberCountText
                        ?.simpleText,
                uploadDate = videoPrimary?.dateText?.simpleText,
                viewCount = returnYouTubeDislikeResponse.viewCount,
                like = returnYouTubeDislikeResponse.likes,
                dislike = returnYouTubeDislikeResponse.dislikes,
            )
            // Get author thumbnails, subscribers, description, like count
        }

    private suspend fun getVisitorData(
        videoId: String,
        playlistId: String?,
    ): Triple<String, String, PlayerResponse.PlaybackTracking?> {
        try {
            val pId = if (playlistId?.startsWith("VL") == true) playlistId.removeRange(0..1) else playlistId
            val ghostRequest = ytMusic.ghostRequest(videoId, pId)
            val cookie =
                "PREF=hl=en&tz=UTC; SOCS=CAI; ${ghostRequest.headers
                    .getAll("set-cookie")
                    ?.map {
                        it.split(";").first()
                    }?.filter {
                        it.lastOrNull() != '='
                    }?.joinToString("; ")}"
            var response = ""
            var data = ""
            val ksoupHtmlParser =
                KsoupHtmlParser(
                    object : KsoupHtmlHandler {
                        override fun onText(text: String) {
                            super.onText(text)
                            if (text.contains("var ytInitialPlayerResponse")) {
                                val temp = text.replace("var ytInitialPlayerResponse = ", "").split(";var").firstOrNull()
                                temp?.let {
                                    response = it.trimIndent()
                                }
                            } else if (text.contains("var ytInitialData = ")) {
                                val temp = text.replace("var ytInitialData = ", "").dropLast(1)
                                temp.let {
                                    data = it.trimIndent()
                                }
                            }
                        }
                    },
                )
            ksoupHtmlParser.write(ghostRequest.bodyAsText())
            ksoupHtmlParser.end()
            val ytInitialData = poTokenJsonDeserializer.decodeFromString<GhostResponse>(data)
            val ytInitialPlayerResponse = poTokenJsonDeserializer.decodeFromString<GhostResponse>(response)
            val playbackTracking = ytInitialPlayerResponse.playbackTracking
            val loggedIn =
                ytInitialData.responseContext.serviceTrackingParams
                    ?.find { it.service == "GFEEDBACK" }
                    ?.params
                    ?.find { it.key == "logged_in" }
                    ?.value == "1"
            println("Logged In $loggedIn")
            val visitorData =
                ytInitialPlayerResponse.responseContext.serviceTrackingParams
                    ?.find { it.service == "GFEEDBACK" }
                    ?.params
                    ?.find { it.key == "visitor_data" }
                    ?.value
                    ?: ytInitialData.responseContext.webResponseContextExtensionData
                        ?.ytConfigData
                        ?.visitorData
            println("Visitor Data $visitorData")
            println("New Cookie $cookie")
            println("Playback Tracking $playbackTracking")
            return Triple(cookie, visitorData ?: this@YouTube.visitorData ?: "", playbackTracking)
        } catch (e: Exception) {
            e.printStackTrace()
            return Triple("", "", null)
        }
    }

    suspend fun player(
        videoId: String,
        playlistId: String? = null,
    ): Result<Triple<String?, PlayerResponse, MediaType>> =
        runCatching {
            val cpn =
                (1..16)
                    .map {
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"[
                            Random.nextInt(
                                0,
                                64,
                            ),
                        ]
                    }.joinToString("")
//            val sessionId = dataSyncId ?: visitorData ?: visitorData() ?: getVisitorData(videoId, null).second
//            // If logged in, use dataSyncId else use visitorData
//            val (webPlayerPot, webStreamingPot) =
//                getWebClientPoTokenOrNull(videoId, sessionId)?.let {
//                    Pair(it.playerRequestPoToken, it.streamingDataPoToken)
//                } ?: Pair(null, null).also {
//                    Log.w("YouTube", "[$videoId] No po token")
//                }
            var webPlayerPot = ""
            try {
                if (GlobalPreferences.sInstance == null) {
                    GlobalPreferences.instance(context)
                }
                val mediaServiceData = MediaServiceData.instance()
                mediaServiceData.visitorCookie = cookie
                mAppService.resetClientPlaybackNonce()
                mAppService.clientPlaybackNonce?.let {
                    println("Client playback nonce $it")
                }
                mAppService.refreshCacheIfNeeded()
                mAppService.refreshPoTokenIfNeeded()
                webPlayerPot = mAppService.sessionPoToken
                println("YouTube poToken $webPlayerPot")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val sigTimestamp =
                try {
                    mAppService.signatureTimestamp?.toInt()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            val listUrlSig = mutableListOf<String>()
            var decodedSigResponse: PlayerResponse? = null
            val listClients = listOf(WEB_REMIX, TVHTML5)
            var sigResponse: PlayerResponse? = null
            var currentClient = listClients.first()
            for (client in listClients) {
                listUrlSig.removeAll(listUrlSig)
                decodedSigResponse = null
                println("YouTube Client $client")
                val tempRes =
                    ytMusic
                        .player(
                            client,
                            videoId,
                            playlistId,
                            cpn,
                            signatureTimestamp = sigTimestamp,
                            poToken = webPlayerPot,
                        ).body<PlayerResponse>()
                println("YouTube TempRes ${tempRes.playabilityStatus}")
                if (tempRes.playabilityStatus.status != "OK") {
                    continue
                } else {
                    sigResponse = tempRes
                    currentClient = client
                }
                decodedSigResponse =
                    sigResponse.copy(
                        streamingData =
                            sigResponse.streamingData?.copy(
                                formats =
                                    sigResponse.streamingData.formats?.map { format ->
                                        format.copy(
                                            url =
                                                format.signatureCipher?.let { decodeSignatureCipher(it) }?.let { url ->
                                                    if (webPlayerPot.isNotEmpty() && currentClient.clientName.contains("WEB")) {
                                                        "$url&pot=$webPlayerPot"
                                                    } else {
                                                        url
                                                    }
                                                },
                                        )
                                    },
                                adaptiveFormats =
                                    sigResponse.streamingData.adaptiveFormats.map { adaptiveFormats ->
                                        adaptiveFormats.copy(
                                            url =
                                                adaptiveFormats.signatureCipher?.let { decodeSignatureCipher(it) }?.let { url ->
                                                    if (webPlayerPot.isNotEmpty() && currentClient.clientName.contains("WEB")) {
                                                        "$url&pot=$webPlayerPot"
                                                    } else {
                                                        url
                                                    }
                                                },
                                        )
                                    },
                            ),
                    )
                listUrlSig.addAll(
                    (
                        decodedSigResponse
                            .streamingData
                            ?.adaptiveFormats
                            ?.mapNotNull { it.url }
                            ?.toMutableList() ?: mutableListOf()
                    ).apply {
                        decodedSigResponse
                            .streamingData
                            ?.formats
                            ?.mapNotNull { it.url }
                            ?.let { addAll(it) }
                    },
                )
                println("YouTube URL ${decodedSigResponse.streamingData?.formats?.mapNotNull { it.url }}")
                val listFormat =
                    (
                        decodedSigResponse
                            .streamingData
                            ?.formats
                            ?.mapNotNull { Pair(it.itag, it.url) }
                            ?.toMutableList() ?: mutableListOf()
                    ).apply {
                        addAll(
                            decodedSigResponse.streamingData?.adaptiveFormats?.map {
                                Pair(it.itag, it.url)
                            } ?: emptyList(),
                        )
                    }
                listFormat.forEach {
                    println("YouTube Format ${it.first} ${it.second}")
                }
                if (listUrlSig.isNotEmpty() && !is403Url(listUrlSig.first())) {
                    break
                } else {
                    listUrlSig.clear()
                    decodedSigResponse =
                        sigResponse.copy(
                            streamingData =
                                sigResponse.streamingData?.copy(
                                    formats =
                                        sigResponse.streamingData.formats?.map { format ->
                                            format.copy(
                                                url =
                                                    newPipeUtils.getStreamUrl(format, videoId)?.let { url ->
                                                        if (webPlayerPot.isNotEmpty() && currentClient.clientName.contains("WEB")) {
                                                            "$url&pot=$webPlayerPot"
                                                        } else {
                                                            url
                                                        }
                                                    },
                                            )
                                        },
                                    adaptiveFormats =
                                        sigResponse.streamingData.adaptiveFormats.map { adaptiveFormats ->
                                            adaptiveFormats.copy(
                                                url =
                                                    newPipeUtils.getStreamUrl(adaptiveFormats, videoId)?.let { url ->
                                                        if (webPlayerPot.isNotEmpty() && currentClient.clientName.contains("WEB")) {
                                                            "$url&pot=$webPlayerPot"
                                                        } else {
                                                            url
                                                        }
                                                    },
                                            )
                                        },
                                ),
                        )
                    listUrlSig.addAll(
                        (
                            decodedSigResponse
                                .streamingData
                                ?.adaptiveFormats
                                ?.mapNotNull { it.url }
                                ?.toMutableList() ?: mutableListOf()
                        ).apply {
                            decodedSigResponse
                                .streamingData
                                ?.formats
                                ?.mapNotNull { it.url }
                                ?.let { addAll(it) }
                        },
                    )
                    if (listUrlSig.isNotEmpty() && !is403Url(listUrlSig.first())) {
                        break
                    }
                }
            }
            if (listUrlSig.isEmpty() || decodedSigResponse == null) {
                val (tempCookie, visitorData, playbackTracking) = getVisitorData(videoId, playlistId)
                val now = System.currentTimeMillis()
                val poToken =
                    if (now < poTokenObject.second) {
                        println("Use saved PoToken")
                        poTokenObject.first
                    } else {
                        ytMusic
                            .createPoTokenChallenge()
                            .bodyAsText()
                            .let { challenge ->
                                val listChallenge = poTokenJsonDeserializer.decodeFromString<List<String?>>(challenge)
                                listChallenge.filterIsInstance<String>().firstOrNull()
                            }?.let { poTokenChallenge ->
                                ytMusic.generatePoToken(poTokenChallenge).bodyAsText().getPoToken().also { poToken ->
                                    if (poToken != null) {
                                        poTokenObject = Pair(poToken, now + 3600)
                                    }
                                }
                            }
                    }
                println("PoToken $poToken")
                val playerResponse = ytMusic.noLogInPlayer(videoId, tempCookie, visitorData, poToken ?: "").body<PlayerResponse>()
                println("Player Response $playerResponse")
                println("Thumbnails " + playerResponse.videoDetails?.thumbnail)
                println("Player Response status: ${playerResponse.playabilityStatus.status}")
                val firstThumb =
                    playerResponse.videoDetails
                        ?.thumbnail
                        ?.thumbnails
                        ?.firstOrNull()
                val thumbnails =
                    if (firstThumb?.height == firstThumb?.width && firstThumb != null) MediaType.Song else MediaType.Video
                val formatList = playerResponse.streamingData?.formats?.map { Pair(it.itag, it.isAudio) }
                println("Player Response formatList $formatList")
                val adaptiveFormatsList = playerResponse.streamingData?.adaptiveFormats?.map { Pair(it.itag, it.isAudio) }
                println("Player Response adaptiveFormat $adaptiveFormatsList")

                if (playerResponse.playabilityStatus.status == "OK" && (formatList != null || adaptiveFormatsList != null)) {
                    return@runCatching Triple(
                        cpn,
                        playerResponse.copy(
                            videoDetails = playerResponse.videoDetails?.copy(),
                            playbackTracking = playbackTracking ?: playerResponse.playbackTracking,
                        ),
                        thumbnails,
                    )
                } else {
                    for (instance in listPipedInstances) {
                        try {
                            val piped = ytMusic.pipedStreams(videoId, instance).body<PipedResponse>()
                            val audioStreams = piped.audioStreams
                            val videoStreams = piped.videoStreams
                            val stream = audioStreams + videoStreams
                            return@runCatching Triple(
                                null,
                                playerResponse.copy(
                                    streamingData =
                                        PlayerResponse.StreamingData(
                                            formats = stream.toListFormat(),
                                            adaptiveFormats = stream.toListFormat(),
                                            expiresInSeconds = 0,
                                        ),
                                    videoDetails = playerResponse.videoDetails?.copy(),
                                    playbackTracking = playbackTracking ?: playerResponse.playbackTracking,
                                ),
                                thumbnails,
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            continue
                        }
                    }
                }
                throw Exception(playerResponse.playabilityStatus.status)
            } else {
                val firstThumb =
                    decodedSigResponse.videoDetails
                        ?.thumbnail
                        ?.thumbnails
                        ?.firstOrNull()
                val thumbnails =
                    if (firstThumb?.height == firstThumb?.width && firstThumb != null) MediaType.Song else MediaType.Video
                return@runCatching Triple(
                    cpn,
                    decodedSigResponse.copy(
                        videoDetails = decodedSigResponse.videoDetails?.copy(),
                        playbackTracking = decodedSigResponse.playbackTracking,
                    ),
                    thumbnails,
                )
            }
        }

    private fun decodeSignatureCipher(signatureCipher: String): String? =
        try {
            val params = parseQueryString(signatureCipher)
            val cipher = params["s"] ?: throw Exception("Could not parse cipher signature")
            val signatureParam = params["sp"] ?: throw Exception("Could not parse cipher signature parameter")
            val url = params["url"]?.let { URLBuilder(it) } ?: throw Exception("Could not parse cipher url")
//            url.parameters[signatureParam] = YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscatedSignature)
            print("URL $url")
            val nSigParam = url.parameters["n"] ?: throw Exception("Could not parse cipher signature parameter")
//            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url.toString())
            val decodedCipher = mAppService.decipher(cipher)
            val fixedThrottling = mAppService.fixThrottling(nSigParam)
            val newUrl = URLBuilder(url.toString())
            newUrl.parameters["n"] = fixedThrottling
            newUrl.parameters[signatureParam] = decodedCipher
            newUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    /**
     * Wrapper around the [PoTokenGenerator.getWebClientPoToken] function which reports exceptions
     */
    private fun getWebClientPoTokenOrNull(
        videoId: String,
        sessionId: String?,
        proxy: Proxy? = ytMusic.proxy,
    ): PoToken? {
        if (sessionId == null) {
            Log.d("YouTube", "[$videoId] Session identifier is null")
            return null
        }
        try {
            return poTokenGenerator.getWebClientPoToken(videoId, sessionId, proxy)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun updateWatchTime(
        watchtimeUrl: String,
        watchtimeList: ArrayList<Float>,
        cpn: String,
        playlistId: String?,
    ): Result<Int> =
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

    suspend fun updateWatchTimeFull(
        watchtimeUrl: String,
        cpn: String,
        playlistId: String?,
    ): Result<Int> =
        runCatching {
            val regex = Regex("len=([^&]+)")
            val length =
                regex
                    .find(watchtimeUrl)
                    ?.groupValues
                    ?.firstOrNull()
                    ?.drop(4) ?: "0"
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
    suspend fun initPlayback(
        playbackUrl: String,
        atrUrl: String,
        watchtimeUrl: String,
        cpn: String,
        playlistId: String?,
    ): Result<Pair<Int, Float>> {
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
                                    val secondWatchTime = (Math.round(Random.nextFloat() * 100.0) / 100.0).toFloat() + 12f
                                    ytMusic
                                        .initPlayback(
                                            watchtimeUrl,
                                            cpn,
                                            mapOf<String, String>("st" to "0,5.54", "et" to "5.54,$secondWatchTime"),
                                            playlistId,
                                        ).status.value
                                        .let { watchtime ->
                                            if (watchtime == 204) {
                                                println("watchtime done")
                                                return@runCatching Pair(watchtime, secondWatchTime)
                                            } else {
                                                return@runCatching Pair(watchtime, secondWatchTime)
                                            }
                                        }
                                } else {
                                    return@runCatching Pair(atr, 0f)
                                }
                            }
                        } else {
                            return@runCatching Pair(firstWatchTime, 0f)
                        }
                    }
                } else {
                    return@runCatching Pair(status, 0f)
                }
            }
        }
    }

    suspend fun nextYouTubePlaylists(continuation: String): Result<Pair<List<MusicTwoRowItemRenderer>, String?>> =
        runCatching {
            val response =
                ytMusic
                    .next(
                        WEB_REMIX,
                        null,
                        null,
                        null,
                        null,
                        null,
                        continuation,
                    ).body<BrowseResponse>()
            Pair(
                response
                    .continuationContents
                    ?.gridContinuation
                    ?.items ?: emptyList(),
                response.continuationContents
                    ?.gridContinuation
                    ?.continuations
                    ?.getContinuation(),
            )
        }

    suspend fun next(
        endpoint: WatchEndpoint,
        continuation: String? = null,
    ): Result<NextResult> =
        runCatching {
            val response =
                ytMusic
                    .next(
                        WEB_REMIX,
                        endpoint.videoId,
                        endpoint.playlistId,
                        endpoint.playlistSetVideoId,
                        endpoint.index,
                        endpoint.params,
                        continuation,
                    ).body<NextResponse>()
            Log.w("YouTube", response.toString())
            val playlistPanelRenderer =
                response.continuationContents?.playlistPanelContinuation
                    ?: response.contents.singleColumnMusicWatchNextResultsRenderer
                        ?.tabbedRenderer
                        ?.watchNextTabbedResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.musicQueueRenderer
                        ?.content
                        ?.playlistPanelRenderer
            if (playlistPanelRenderer != null) {
                // load automix items
                if (playlistPanelRenderer.contents
                        .lastOrNull()
                        ?.automixPreviewVideoRenderer
                        ?.content
                        ?.automixPlaylistVideoRenderer
                        ?.navigationEndpoint
                        ?.watchPlaylistEndpoint !=
                    null
                ) {
                    return@runCatching next(
                        playlistPanelRenderer.contents
                            .lastOrNull()
                            ?.automixPreviewVideoRenderer
                            ?.content
                            ?.automixPlaylistVideoRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint!!,
                    ).getOrThrow()
                        .let { result ->
                            result.copy(
                                title = playlistPanelRenderer.title,
                                items =
                                    playlistPanelRenderer.contents.mapNotNull {
                                        it.playlistPanelVideoRenderer?.let { renderer ->
                                            NextPage.fromPlaylistPanelVideoRenderer(renderer)
                                        }
                                    } + result.items,
                                lyricsEndpoint =
                                    response.contents.singleColumnMusicWatchNextResultsRenderer
                                        ?.tabbedRenderer
                                        ?.watchNextTabbedResultsRenderer
                                        ?.tabs
                                        ?.getOrNull(
                                            1,
                                        )?.tabRenderer
                                        ?.endpoint
                                        ?.browseEndpoint,
                                relatedEndpoint =
                                    response.contents.singleColumnMusicWatchNextResultsRenderer
                                        ?.tabbedRenderer
                                        ?.watchNextTabbedResultsRenderer
                                        ?.tabs
                                        ?.getOrNull(
                                            2,
                                        )?.tabRenderer
                                        ?.endpoint
                                        ?.browseEndpoint,
                                currentIndex = playlistPanelRenderer.currentIndex,
                                endpoint =
                                    playlistPanelRenderer.contents
                                        .lastOrNull()
                                        ?.automixPreviewVideoRenderer
                                        ?.content
                                        ?.automixPlaylistVideoRenderer
                                        ?.navigationEndpoint
                                        ?.watchPlaylistEndpoint!!,
                            )
                        }
                }
//        else if (playlistPanelRenderer.contents.firstOrNull()?.playlistPanelVideoRenderer?.navigationEndpoint?.watchPlaylistEndpoint != null) {
//
//        }
                return@runCatching NextResult(
                    title = playlistPanelRenderer.title,
                    items =
                        playlistPanelRenderer.contents.mapNotNull {
                            it.playlistPanelVideoRenderer?.let(NextPage::fromPlaylistPanelVideoRenderer)
                        },
                    currentIndex = playlistPanelRenderer.currentIndex,
                    lyricsEndpoint =
                        response.contents.singleColumnMusicWatchNextResultsRenderer
                            ?.tabbedRenderer
                            ?.watchNextTabbedResultsRenderer
                            ?.tabs
                            ?.getOrNull(
                                1,
                            )?.tabRenderer
                            ?.endpoint
                            ?.browseEndpoint,
                    relatedEndpoint =
                        response.contents.singleColumnMusicWatchNextResultsRenderer
                            ?.tabbedRenderer
                            ?.watchNextTabbedResultsRenderer
                            ?.tabs
                            ?.getOrNull(
                                2,
                            )?.tabRenderer
                            ?.endpoint
                            ?.browseEndpoint,
                    continuation = playlistPanelRenderer.continuations?.getContinuation(),
                    endpoint = endpoint,
                )
            } else {
                Log.e("YouTube", response.toString())
                val musicPlaylistShelfContinuation = response.continuationContents?.musicPlaylistShelfContinuation!!
                return@runCatching NextResult(
                    items =
                        musicPlaylistShelfContinuation.contents.mapNotNull {
                            it.musicResponsiveListItemRenderer?.let { renderer ->
                                NextPage.fromMusicResponsiveListItemRenderer(renderer)
                            }
                        },
                    continuation =
                        musicPlaylistShelfContinuation.continuations
                            ?.firstOrNull()
                            ?.nextContinuationData
                            ?.continuation,
                    endpoint =
                        WatchEndpoint(
                            videoId = null,
                            playlistId = null,
                            playlistSetVideoId = null,
                            params = null,
                            index = null,
                            watchEndpointMusicSupportedConfigs = null,
                        ),
                )
            }
        }

    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
            response.contents
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.musicDescriptionShelfRenderer
                ?.description
                ?.runs
                ?.firstOrNull()
                ?.text
        }

    suspend fun queue(
        videoIds: List<String>? = null,
        playlistId: String? = null,
    ): Result<List<SongItem>> =
        runCatching {
            if (videoIds != null) {
                assert(videoIds.size <= MAX_GET_QUEUE_SIZE) // Max video limit
            }
            ytMusic
                .getQueue(WEB_REMIX, videoIds, playlistId)
                .body<GetQueueResponse>()
                .queueDatas
                .mapNotNull {
                    it.content.playlistPanelVideoRenderer?.let { renderer ->
                        NextPage.fromPlaylistPanelVideoRenderer(renderer)
                    }
                }
        }

    suspend fun visitorData(): String? =
        try {
            Json
                .parseToJsonElement(ytMusic.getSwJsData().bodyAsText().substring(5))
                .jsonArray[0]
                .jsonArray[2]
                .jsonArray
                .first { (it as? JsonPrimitive)?.content?.startsWith(VISITOR_DATA_PREFIX) == true }
                .jsonPrimitive.content
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    suspend fun accountInfo(customCookie: String? = null): Result<AccountInfo?> =
        runCatching {
            ytMusic
                .accountMenu(customCookie, WEB_REMIX)
                .apply {
                    println(this.bodyAsText())
                }.body<AccountMenuResponse>()
                .actions[0]
                .openPopupAction.popup.multiPageMenuRenderer.header
                ?.activeAccountHeaderRenderer
                ?.toAccountInfo()
        }

    suspend fun pipeStream(
        videoId: String,
        pipedInstance: String,
    ) = runCatching {
        ytMusic.pipedStreams(videoId, pipedInstance).body<PipedResponse>()
    }

    suspend fun getLibraryPlaylists() =
        runCatching {
            ytMusic.browse(WEB_REMIX, "FEmusic_liked_playlists", setLogin = true).body<BrowseResponse>()
        }

    @JvmInline
    value class SearchFilter(
        val value: String,
    ) {
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

    suspend fun getYTMusicSearchSuggestions(query: String) =
        runCatching {
            val response = ytMusic.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>()
            SearchSuggestions(
                queries =
                    response.contents
                        ?.getOrNull(0)
                        ?.searchSuggestionsSectionRenderer
                        ?.contents
                        ?.mapNotNull { content ->
                            content.searchSuggestionRenderer
                                ?.suggestion
                                ?.runs
                                ?.joinToString(separator = "") { it.text }
                        }.orEmpty(),
                recommendedItems =
                    response.contents
                        ?.getOrNull(1)
                        ?.searchSuggestionsSectionRenderer
                        ?.contents
                        ?.mapNotNull {
                            it.musicResponsiveListItemRenderer?.let { renderer ->
                                SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)
                            }
                        }.orEmpty(),
            )
        }

    suspend fun getYouTubeCaption(
        videoId: String,
        preferLang: String,
    ) = runCatching {
        val ytWeb = ytMusic.player(WEB, videoId, null, null).body<YouTubeInitialPage>()
        val baseCaption =
            ytMusic
                .getYouTubeCaption(
                    ytWeb.captions?.playerCaptionsTracklistRenderer?.captionTracks?.firstOrNull()?.baseUrl?.replace(
                        "&fmt=srv3",
                        "",
                    ) ?: "",
                ).body<Transcript>()
        val translateCaption =
            try {
                ytMusic
                    .getYouTubeCaption(
                        "${ytWeb.captions?.playerCaptionsTracklistRenderer?.captionTracks?.firstOrNull()?.baseUrl?.replace(
                            "&fmt=srv3",
                            "",
                        )}&tlang=$preferLang",
                    ).body<Transcript>()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        return@runCatching baseCaption to translateCaption
    }

    suspend fun scrapeYouTube(videoId: String) =
        runCatching {
            ytMusic.scrapeYouTube(videoId).body<String>()
        }

    suspend fun removeItemYouTubePlaylist(
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = runCatching {
        ytMusic.removeItemYouTubePlaylist(playlistId, videoId, setVideoId).status.value
    }

    suspend fun addPlaylistItem(
        playlistId: String,
        videoId: String,
    ) = runCatching {
        ytMusic.addItemYouTubePlaylist(playlistId, videoId).body<AddItemYouTubePlaylistResponse>()
    }

    suspend fun editPlaylist(
        playlistId: String,
        title: String,
    ) = runCatching {
        ytMusic.editYouTubePlaylist(playlistId, title).status.value
    }

    suspend fun createPlaylist(
        title: String,
        listVideoId: List<String>?,
    ) = runCatching {
        ytMusic.createYouTubePlaylist(title, listVideoId).body<CreatePlaylistResponse>()
    }

    suspend fun addToLiked(mediaId: String) =
        runCatching {
            ytMusic.addToLiked(mediaId).status.value
        }

    suspend fun removeFromLiked(mediaId: String) =
        runCatching {
            ytMusic.removeFromLiked(mediaId).status.value
        }

    fun download(
        track: SongItem,
        filePath: String,
        thumbnail: Bitmap,
        videoId: String,
        isVideo: Boolean = false,
    ): Flow<DownloadProgress> =
        channelFlow {
            val byteArrayOutputStream = ByteArrayOutputStream()
            thumbnail.compress(JPEG, 100, byteArrayOutputStream)
            val bytesArray = byteArrayOutputStream.toByteArray()
            try {
                val fileOutputStream = FileOutputStream("$filePath.jpg")
                fileOutputStream.write(bytesArray)
                fileOutputStream.close()
                println("Thumbnail saved to $filePath.jpg")
            } catch (e: java.lang.Exception) {
                throw RuntimeException(e)
            }
            // Video if videoId is not null
            trySend(DownloadProgress(0.00001f))
            player(videoId = videoId)
                .onSuccess { playerResponse ->
                    val audioFormat =
                        listOf(
                            playerResponse.second.streamingData
                                ?.formats
                                ?.filter { it.isAudio }
                                ?.maxByOrNull { it.bitrate },
                            playerResponse.second.streamingData
                                ?.adaptiveFormats
                                ?.filter { it.isAudio }
                                ?.maxByOrNull { it.bitrate },
                        ).maxByOrNull { it?.bitrate ?: 0 }
                    val videoFormat =
                        listOf(
                            playerResponse.second.streamingData
                                ?.formats
                                ?.filter { !it.isAudio }
                                ?.maxByOrNull { it.bitrate },
                            playerResponse.second.streamingData
                                ?.adaptiveFormats
                                ?.filter { !it.isAudio }
                                ?.maxByOrNull { it.bitrate },
                        ).maxByOrNull { it?.bitrate ?: 0 }
                    println("Audio Format $audioFormat")
                    println("Video Format $videoFormat")
                    val audioUrl = audioFormat?.url ?: return@channelFlow
                    val videoUrl = videoFormat?.url ?: return@channelFlow
                    if (isVideo) {
                        runCatching {
                            val downloadAudioJob = ytMusic.download(audioUrl, ("$filePath.webm"))
                            val downloadVideoJob = ytMusic.download(videoUrl, ("$filePath.mp4"))
                            combine(downloadVideoJob, downloadAudioJob) { videoProgress, audioProgress ->
                                Pair(videoProgress, audioProgress)
                            }.collectLatest { (videoProgress, audioProgress) ->
                                if (!videoProgress.first || !audioProgress.first) {
                                    trySend(
                                        DownloadProgress(
                                            videoDownloadProgress = videoProgress.second,
                                            audioDownloadProgress = audioProgress.second,
                                            downloadSpeed = if (videoProgress.third != 0) videoProgress.third else audioProgress.third,
                                        ),
                                    )
                                } else {
                                    trySend(DownloadProgress.MERGING)
                                    val command =
                                        listOf(
                                            "-i",
                                            ("$filePath.mp4"),
                                            "-i",
                                            ("$filePath.webm"),
                                            "-c:v",
                                            "copy",
                                            "-c:a",
                                            "aac",
                                            "-map",
                                            "0:v:0",
                                            "-map",
                                            "1:a:0",
                                            "-shortest",
                                            "$filePath-SimpMusic.mp4",
                                        ).joinToString(" ")

                                    if (FileSystem.SYSTEM.exists("$filePath-SimpMusic.mp4".toPath())) {
                                        FileSystem.SYSTEM.delete("$filePath-SimpMusic.mp4".toPath())
                                    }

                                    val session =
                                        FFmpegKit.execute(
                                            command,
                                        )
                                    if (ReturnCode.isSuccess(session.returnCode)) {
                                        // SUCCESS
                                        println("Command succeeded ${session.state}, ${session.returnCode}")
                                        try {
                                            FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                                            FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                                            FileSystem.SYSTEM.delete("$filePath.mp4".toPath())
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                        trySend(DownloadProgress.VIDEO_DONE)
                                    } else if (ReturnCode.isCancel(session.returnCode)) {
                                        // CANCEL
                                        println("Command cancelled ${session.state}, ${session.returnCode}")
                                        try {
                                            FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                                            FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                                            FileSystem.SYSTEM.delete("$filePath.mp4".toPath())
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                        trySend(DownloadProgress.failed(session.failStackTrace))
                                    } else {
                                        // FAILURE
                                        println("Command failed ${session.state}, ${session.returnCode}, ${session.failStackTrace}")
                                        try {
                                            FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                                            FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                                            FileSystem.SYSTEM.delete("$filePath.mp4".toPath())
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                        trySend(DownloadProgress.failed(session.failStackTrace))
                                    }
                                }
                            }
                        }.onSuccess {
                            println("Download Video Success")
                        }.onFailure {
                            it.printStackTrace()
                            trySend(DownloadProgress.failed(it.message ?: "Download failed"))
                        }
                    } else {
                        // Song if url is not null
                        runCatching {
                            ytMusic
                                .download(audioUrl, ("$filePath.webm"))
                                .collect { downloadProgress ->
                                    if (!downloadProgress.first) {
                                        trySend(DownloadProgress(audioDownloadProgress = downloadProgress.second))
                                    } else {
                                        trySend(DownloadProgress(audioDownloadProgress = 1f, isDone = true))
                                    }
                                }
                        }.onSuccess {
                            println("Download only Audio Success")
                            // Convert to mp3
                            val command =
                                listOf(
                                    "-i",
                                    ("$filePath.webm"),
                                    "-q:a 0",
                                    "$filePath.mp3",
                                ).joinToString(" ")

                            try {
                                if (FileSystem.SYSTEM.exists("$filePath.mp3".toPath())) {
                                    FileSystem.SYSTEM.delete("$filePath.mp3".toPath())
                                }
                                if (FileSystem.SYSTEM.exists("$filePath-simpmusic.mp3".toPath())) {
                                    FileSystem.SYSTEM.delete("$filePath-simpmusic.mp3".toPath())
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                            val session =
                                FFmpegKit.execute(
                                    command,
                                )
                            if (ReturnCode.isSuccess(session.returnCode)) {
                                // SUCCESS
                                println("Command succeeded ${session.state}, ${session.returnCode}")
                                try {
                                    FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            } else if (ReturnCode.isCancel(session.returnCode)) {
                                // CANCEL
                                println("Command cancelled ${session.state}, ${session.returnCode}")
                                try {
                                    FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                                    FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                                trySend(DownloadProgress.failed("Error"))
                            } else {
                                // FAILURE
                                println("Command failed ${session.state}, ${session.returnCode}, ${session.failStackTrace}")
                                try {
                                    FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                                    FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                                trySend(DownloadProgress.failed("Error"))
                            }

                            val commandInject =
                                listOf(
                                    "-i",
                                    "$filePath.mp3",
                                    "-i $filePath.jpg",
                                    "-map 0:a",
                                    "-map 1:v",
                                    "-c copy",
                                    "-id3v2_version 3",
                                    "-metadata",
                                    "title=\"${track.title}\"",
                                    "-metadata",
                                    "artist=\"${track.artists.joinToString(", ") { it.name }}\"",
                                    "-metadata",
                                    "album=\"${track.album?.name ?: track.title}\"",
                                    "-disposition:v:0 attached_pic",
                                    "$filePath-simpmusic.mp3",
                                ).joinToString(" ")
                            val sessionInject =
                                FFmpegKit.execute(
                                    commandInject,
                                )
                            if (ReturnCode.isSuccess(sessionInject.returnCode)) {
                                // SUCCESS
                                println("Command succeeded ${sessionInject.state}, ${sessionInject.returnCode}")
                                try {
                                    FileSystem.SYSTEM.delete("$filePath.mp3".toPath())
                                    FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                                    FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                                trySend(DownloadProgress.AUDIO_DONE)
                            } else if (ReturnCode.isCancel(sessionInject.returnCode)) {
                                // CANCEL
                                println("Command cancelled ${sessionInject.state}, ${sessionInject.returnCode}")
                                try {
                                    FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                                    FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                                    FileSystem.SYSTEM.delete("$filePath-simpmusic.mp3".toPath())
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                                trySend(DownloadProgress.failed("Error"))
                            } else {
                                // FAILURE
                                println("Command failed ${sessionInject.state}, ${sessionInject.returnCode}, ${sessionInject.failStackTrace}")
                                try {
                                    FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                                    FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                                    FileSystem.SYSTEM.delete("$filePath-simpmusic.mp3".toPath())
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                                trySend(DownloadProgress.failed("Error"))
                            }
                        }.onFailure { e ->
                            e.printStackTrace()
                            trySend(DownloadProgress.failed(e.message ?: "Download failed"))
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                    println("Player Response is null")
                    trySend(DownloadProgress.failed(it.message ?: "Player response is null"))
                }
        }.flowOn(Dispatchers.IO)

    suspend fun is403Url(url: String) = ytMusic.is403Url(url)

    companion object {
        const val MAX_GET_QUEUE_SIZE = 1000

        private const val VISITOR_DATA_PREFIX = "Cgt"

        const val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
    }
}