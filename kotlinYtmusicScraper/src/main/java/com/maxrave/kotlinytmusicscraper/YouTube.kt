package com.maxrave.kotlinytmusicscraper

import android.util.Log
import com.maxrave.kotlinytmusicscraper.models.AccountInfo
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.kotlinytmusicscraper.models.Artist
import com.maxrave.kotlinytmusicscraper.models.ArtistItem
import com.maxrave.kotlinytmusicscraper.models.BrowseEndpoint
import com.maxrave.kotlinytmusicscraper.models.GridRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicCarouselShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.Run
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.TVHTML5
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.WEB_REMIX
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.kotlinytmusicscraper.models.getContinuation
import com.maxrave.kotlinytmusicscraper.models.lyrics.Lyrics
import com.maxrave.kotlinytmusicscraper.models.oddElements
import com.maxrave.kotlinytmusicscraper.models.response.AccountMenuResponse
import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import com.maxrave.kotlinytmusicscraper.models.response.GetQueueResponse
import com.maxrave.kotlinytmusicscraper.models.response.NextResponse
import com.maxrave.kotlinytmusicscraper.models.response.PipedResponse
import com.maxrave.kotlinytmusicscraper.models.response.PlayerResponse
import com.maxrave.kotlinytmusicscraper.models.response.SearchResponse
import com.maxrave.kotlinytmusicscraper.models.simpmusic.GithubResponse
import com.maxrave.kotlinytmusicscraper.models.splitBySeparator
import com.maxrave.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import com.maxrave.kotlinytmusicscraper.models.spotify.SpotifyResult
import com.maxrave.kotlinytmusicscraper.models.spotify.Token
import com.maxrave.kotlinytmusicscraper.pages.AlbumPage
import com.maxrave.kotlinytmusicscraper.pages.ArtistPage
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
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONArray
import java.net.Proxy

object YouTube {
    private val ytMusic = Ytmusic()

    var locale: YouTubeLocale
        get() = ytMusic.locale
        set(value) {
            ytMusic.locale = value
        }
    var visitorData: String
        get() = ytMusic.visitorData
        set(value) {
            ytMusic.visitorData = value
        }
    var cookie: String?
        get() = ytMusic.cookie
        set(value) {
            ytMusic.cookie = value
        }
    var proxy: Proxy?
        get() = ytMusic.proxy
        set(value) {
            ytMusic.proxy = value
        }

    suspend fun search(query: String, filter: SearchFilter): Result<SearchResult> = runCatching {
        val response = ytMusic.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
        SearchResult(
            items = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
                ?.musicShelfRenderer?.contents?.mapNotNull {
                    SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                }.orEmpty(),
            continuation = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
                ?.musicShelfRenderer?.continuations?.getContinuation()
        )
    }

    suspend fun searchContinuation(continuation: String): Result<SearchResult> = runCatching {
        val response = ytMusic.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
        SearchResult(
            items = response.continuationContents?.musicShelfContinuation?.contents
                ?.mapNotNull {
                    SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                }!!,
            continuation = response.continuationContents.musicShelfContinuation.continuations?.getContinuation()
        )
    }

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
    suspend fun customQuery(browseId: String, params: String? = null, continuation: String? = null, country: String? = null, setLogin: Boolean = true) = runCatching {
        ytMusic.browse(WEB_REMIX, browseId, params, continuation, country, setLogin).body<BrowseResponse>()
    }
    suspend fun nextCustom(videoId: String) = runCatching {
        ytMusic.nextCustom(WEB_REMIX, videoId).body<NextResponse>()
    }
    suspend fun getLyrics(songId: String) = runCatching {
        ytMusic.getLyrics(songId).body<Lyrics>()
    }
    suspend fun authentication() = runCatching {
        ytMusic.authorizationSpotify().body<Token>()
    }
    suspend fun getSongId(authorization: String, query: String) = runCatching {
        ytMusic.searchSongId(authorization, query).body<SpotifyResult>()
    }
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

    suspend fun player(videoId: String, pipedInstance: String, playlistId: String? = null): Result<PlayerResponse> = runCatching {
        val piped = ytMusic.pipedStreams(videoId, pipedInstance).body<PipedResponse>()
        val audioStreams = piped.audioStreams
        val playerResponse = ytMusic.player(ANDROID_MUSIC, videoId, playlistId).body<PlayerResponse>()
        Log.w("playerResponse", playerResponse.streamingData?.adaptiveFormats.toString())
        if (playerResponse.playabilityStatus.status == "OK") {
            return@runCatching playerResponse.copy(
                videoDetails = playerResponse.videoDetails?.copy(
                    authorAvatar = piped.uploaderAvatar?.replace(Regex("s48"), "s960"),
                    authorSubCount = piped.uploaderSubscriberCount,
                    description = piped.description,
                ),
                streamingData = playerResponse.streamingData?.copy(
                    adaptiveFormats = playerResponse.streamingData.adaptiveFormats.mapNotNull { adaptiveFormat ->
                        audioStreams.find { it.itag == adaptiveFormat.itag }?.let {
                            adaptiveFormat.copy(
                                mimeType = it.mimeType?: "",
                                bitrate = it.bitrate,
                            )
                        }
                    }
                )
            )
        }
        val safePlayerResponse = ytMusic.player(TVHTML5, videoId, playlistId).body<PlayerResponse>()
        Log.w("safePlayerResponse", safePlayerResponse.streamingData?.adaptiveFormats.toString())
        if (safePlayerResponse.playabilityStatus.status != "OK") {
            return@runCatching playerResponse.copy(
                videoDetails = safePlayerResponse.videoDetails?.copy(
                    authorAvatar = piped.uploaderAvatar?.replace(Regex("s48"), "s960"),
                    authorSubCount = piped.uploaderSubscriberCount,
                    description = piped.description,
                    ),
            )
        }
        safePlayerResponse.copy(
            streamingData = safePlayerResponse.streamingData?.copy(
                adaptiveFormats = safePlayerResponse.streamingData.adaptiveFormats.mapNotNull { adaptiveFormat ->
                    audioStreams.find { it.itag == adaptiveFormat.itag }?.let {
                        adaptiveFormat.copy(
                            url = it.url,
                        )
                    }
                }
            ),
            videoDetails = safePlayerResponse.videoDetails?.copy(
                authorAvatar = piped.uploaderAvatar?.replace(Regex("s48"), "s96"),
                authorSubCount = piped.uploaderSubscriberCount,
                description = piped.description,
                )
        )
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

    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
        }
    }

    const val MAX_GET_QUEUE_SIZE = 1000

    private const val VISITOR_DATA_PREFIX = "Cgt"

    const val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
}
