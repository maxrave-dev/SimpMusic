package com.maxrave.kotlinytmusicscraper

import android.util.Log
import com.google.gson.Gson
import com.maxrave.kotlinytmusicscraper.models.AccountInfo
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.Artist
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint
import com.maxrave.kotlinytmusicscraper.models.GridRenderer
import com.maxrave.kotlinytmusicscraper.models.LrclibObject
import com.maxrave.kotlinytmusicscraper.models.MediaType
import com.maxrave.kotlinytmusicscraper.models.MusicCarouselShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.ReturnYouTubeDislikeResponse
import com.maxrave.kotlinytmusicscraper.models.Run
import com.maxrave.kotlinytmusicscraper.models.SearchSuggestions
import com.maxrave.kotlinytmusicscraper.models.SongInfo
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.YTItemType
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.IOS
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.WEB
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
import com.maxrave.kotlinytmusicscraper.models.response.LikeStatus
import com.maxrave.kotlinytmusicscraper.models.response.NextAndroidMusicResponse
import com.maxrave.kotlinytmusicscraper.models.response.NextResponse
import com.maxrave.kotlinytmusicscraper.models.response.PipedResponse
import com.maxrave.kotlinytmusicscraper.models.response.PlayerResponse
import com.maxrave.kotlinytmusicscraper.models.response.SearchResponse
import com.maxrave.kotlinytmusicscraper.models.response.spotify.CanvasResponse
import com.maxrave.kotlinytmusicscraper.models.response.spotify.PersonalTokenResponse
import com.maxrave.kotlinytmusicscraper.models.response.spotify.SpotifyLyricsResponse
import com.maxrave.kotlinytmusicscraper.models.response.spotify.TokenResponse
import com.maxrave.kotlinytmusicscraper.models.response.toLikeStatus
import com.maxrave.kotlinytmusicscraper.models.simpmusic.GithubResponse
import com.maxrave.kotlinytmusicscraper.models.sponsorblock.SkipSegments
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
import okhttp3.Interceptor
import org.json.JSONArray
import java.io.File
import java.net.Proxy
import kotlin.math.abs
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
            ),
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

    private val listPipedInstances = listOf(
        "https://pipedapi.nosebs.ru",
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.tokhmi.xyz",
        "https://pipedapi.syncpundit.io",
        "https://pipedapi.leptons.xyz",
        "https://pipedapi.r4fo.com",
        "https://yapi.vyper.me",
        "https://pipedapi-libre.kavin.rocks"
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

    suspend fun albumSongs(
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
            // SongItem / SetVideoId
            val listPair = mutableListOf<Pair<SongItem, String>>()
            val response = ytMusic.playlist(playlistId).body<BrowseResponse>()
            listPair.addAll(
                response.fromPlaylistToTrackWithSetVideoId()
            )
            var continuation = response.getPlaylistContinuation()
            while (continuation != null) {
                val continuationResponse = ytMusic.browse(
                    client = WEB_REMIX,
                    setLogin = true,
                    params = null,
                    continuation = continuation
                ).body<BrowseResponse>()
                listPair.addAll(
                    continuationResponse.fromPlaylistToTrackWithSetVideoId()
                )
                continuation = continuationResponse.getContinuePlaylistContinuation()
            }

            return@runCatching listPair
        }

    suspend fun getSuggestionsTrackForPlaylist(playlistId: String): Result<Pair<String?, List<SongItem>?>?> =
        runCatching {
            val initialResponse = ytMusic.playlist(
                if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
            ).body<BrowseResponse>()
            var continuation = initialResponse.getPlaylistContinuation()
            println("YouTube: getSuggestionsTrackForPlaylist: $continuation")
            while (continuation != null) {
                val continuationResponse = ytMusic.browse(
                    client = WEB_REMIX,
                    setLogin = true,
                    params = "wAEB",
                    continuation = continuation
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
                response.fromPlaylistToTrack()
            )
            var continuation = response.getPlaylistContinuation()
            while (continuation != null) {
                val continuationResponse = ytMusic.browse(
                    client = WEB_REMIX,
                    setLogin = true,
                    params = null,
                    continuation = continuation
                ).body<BrowseResponse>()
                songs.addAll(
                    continuationResponse.fromPlaylistContinuationToTracks()
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
        browseId: String,
        params: String? = null,
        continuation: String? = null,
        country: String? = null,
        setLogin: Boolean = true,
    ) = runCatching {
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
    suspend fun nextCustom(videoId: String) =
        runCatching {
            ytMusic.nextCustom(WEB_REMIX, videoId).body<NextResponse>()
        }

    suspend fun getMusixmatchUserToken() =
        runCatching {
            ytMusic.getMusixmatchUserToken().body<UserTokenResponse>()
        }

    suspend fun postMusixmatchCredentials(
        email: String,
        password: String,
        userToken: String,
    ) = runCatching {
        val request = ytMusic.postMusixmatchPostCredentials(email, password, userToken)
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
                fromArrayListNull(setCookies)?.let {
                    musixMatchCookie = it
                }
            }
        }
//        Log.w("postMusixmatchCredentials cookie", musixMatchCookie.toString())
//        Log.w("postMusixmatchCredentials", response.toString())
        return@runCatching response
    }

    fun getMusixmatchCookie() = musixMatchCookie

    suspend fun searchMusixmatchTrackId(
        query: String,
        userToken: String,
    ) = runCatching {
//        val result = ytMusic.searchMusixmatchTrackId(query, userToken)
//        Log.w("Lyrics", "Search Track $query: " + result.bodyAsText())
//        Log.w("Lyrics", "Search Track $query: " + result.body<SearchMusixmatchResponse>().message.body.macro_result_list)
//        return@runCatching result.body<SearchMusixmatchResponse>(),
        ytMusic.searchMusixmatchTrackId(query, userToken).body<SearchMusixmatchResponse>()
    }

    suspend fun fixSearchMusixmatch(
        q_artist: String,
        q_track: String,
        q_duration: String,
        userToken: String,
    ) = runCatching {
        val rs = ytMusic.fixSearchMusixmatch(q_artist, q_track, q_duration, userToken).body<SearchMusixmatchResponse>()
        Log.w("Search Result", rs.toString())
        return@runCatching rs
    }

    suspend fun getMusixmatchLyrics(
        trackId: String,
        userToken: String,
    ) = runCatching {
        val response = ytMusic.getMusixmatchLyrics(trackId, userToken).body<MusixmatchLyricsReponse>()
        if (response.message.body.subtitle != null) {
            return@runCatching parseMusixmatchLyrics(response.message.body.subtitle.subtitle_body)
        } else {
            val unsyncedResponse = ytMusic.getMusixmatchUnsyncedLyrics(trackId, userToken).body<MusixmatchLyricsReponse>()
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
        val response = ytMusic.getMusixmatchLyricsByQ(track, userToken).body<MusixmatchLyricsResponseByQ>()

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
            val unsyncedResponse = ytMusic.getMusixmatchUnsyncedLyrics(track.track_id.toString(), userToken).body<MusixmatchLyricsReponse>()
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
        ytMusic
            .getMusixmatchTranslateLyrics(trackId, userToken, language)
            .body<MusixmatchTranslationLyricsResponse>()
    }

    suspend fun getYouTubeCaption(videoId: String) =
        runCatching {
            val ytWeb = ytMusic.player(WEB, videoId, null, null).body<YouTubeInitialPage>()
            ytMusic
                .getYouTubeCaption(
                    ytWeb.captions?.playerCaptionsTracklistRenderer?.captionTracks?.firstOrNull()?.baseUrl?.replace(
                        "&fmt=srv3",
                        "",
                    ) ?: "",
                ).body<Transcript>()
        }

    suspend fun getLrclibLyrics(
        q_track: String,
        q_artist: String,
        duration: Int?,
    ) = runCatching {
        val rs =
            ytMusic
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
                        ANDROID_MUSIC,
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

    suspend fun player(
        videoId: String,
        playlistId: String? = null,
    ): Result<Triple<String?, PlayerResponse, MediaType>> =
        runCatching {
            var error: String? = null
            val cpn =
                (1..16)
                    .map {
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"[
                            Random.Default.nextInt(
                                0,
                                64,
                            ),
                        ]
                    }.joinToString("")
            val playerResponse =
                ytMusic.player( if (cookie != null) ANDROID_MUSIC else IOS, videoId, playlistId, cpn).body<PlayerResponse>()
            println("Player Response " + playerResponse)
//        val ytScrapeInitial: YouTubeInitialPage = ytMusic.player(WEB, videoId, playlistId, cpn).body<YouTubeInitialPage>()
            println("Thumbnails " + playerResponse.videoDetails?.thumbnail)
            val firstThumb =
                playerResponse.videoDetails
                    ?.thumbnail
                    ?.thumbnails
                    ?.firstOrNull()
            val thumbnails =
                if (firstThumb?.height == firstThumb?.width && firstThumb != null) MediaType.Song else MediaType.Video
            val formatList = playerResponse.streamingData?.formats?.map { Pair(it.itag, it.isAudio) }
            println("Player Response " + formatList)
            val adaptiveFormatsList = playerResponse.streamingData?.adaptiveFormats?.map { Pair(it.itag, it.isAudio) }
            println("Player Response " + adaptiveFormatsList)

//        println( playerResponse.streamingData?.adaptiveFormats?.findLast { it.itag == 251 }?.mimeType.toString())
            if (playerResponse.playabilityStatus.status == "OK" && (formatList != null || adaptiveFormatsList != null)) {
                return@runCatching Triple(
                    cpn,
                    playerResponse.copy(
                        videoDetails = playerResponse.videoDetails?.copy(),
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
                            ),
                            thumbnails,
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        error = e.message
                        continue
                    }
                }
            }
            throw Exception(error ?: "Unknown error")
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

    suspend fun visitorData(): Result<String> =
        runCatching {
            Json
                .parseToJsonElement(ytMusic.getSwJsData().bodyAsText().substring(5))
                .jsonArray[0]
                .jsonArray[2]
                .jsonArray
                .first { (it as? JsonPrimitive)?.content?.startsWith(VISITOR_DATA_PREFIX) == true }
                .jsonPrimitive.content
        }

    suspend fun accountInfo(): Result<AccountInfo?> =
        runCatching {
            ytMusic
                .accountMenu(WEB_REMIX)
                .apply {
                    this.bodyAsText().let {
                        println(it)
                    }
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

    suspend fun getNotification() =
        runCatching {
            ytMusic.getNotification().bodyAsText()
        }

    /***
     * Spotify Implementation
     */
    suspend fun getClientToken() =
        runCatching {
            ytMusic.getSpotifyToken().body<TokenResponse>()
        }

    suspend fun getPersonalToken(spdc: String) =
        runCatching {
            ytMusic.getSpotifyLyricsToken(spdc).body<PersonalTokenResponse>()
        }

    suspend fun searchSpotifyTrack(
        query: String,
        token: String,
    ) = runCatching {
        ytMusic
            .searchSpotifyTrack(query, token)
            .body<com.maxrave.kotlinytmusicscraper.models.response.spotify.SearchResponse>()
    }

    suspend fun getSpotifyLyrics(
        trackId: String,
        token: String,
    ) = runCatching {
        ytMusic.getSpotifyLyrics(token, trackId).body<SpotifyLyricsResponse>()
    }

    suspend fun getSpotifyCanvas(
        trackId: String,
        token: String,
    ) = runCatching {
        ytMusic.getSpotifyCanvas(trackId, token).body<CanvasResponse>()
    }

    suspend fun addToLiked(mediaId: String) =
        runCatching {
            ytMusic.addToLiked(mediaId).status.value
        }

    suspend fun removeFromLiked(mediaId: String) =
        runCatching {
            ytMusic.removeFromLiked(mediaId).status.value
        }

    const val MAX_GET_QUEUE_SIZE = 1000

    private const val VISITOR_DATA_PREFIX = "Cgt"

    const val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
}