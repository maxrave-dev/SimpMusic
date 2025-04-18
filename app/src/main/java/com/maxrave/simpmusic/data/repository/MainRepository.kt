package com.maxrave.simpmusic.data.repository

import android.content.Context
import android.util.Log
import androidx.media3.common.util.UnstableApi
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.extension.verifyYouTubePlaylistId
import com.maxrave.kotlinytmusicscraper.models.AccountInfo
import com.maxrave.kotlinytmusicscraper.models.MediaType
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.SearchSuggestions
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.kotlinytmusicscraper.models.response.DownloadProgress
import com.maxrave.kotlinytmusicscraper.models.response.LikeStatus
import com.maxrave.kotlinytmusicscraper.models.response.SearchResponse
import com.maxrave.kotlinytmusicscraper.models.simpmusic.GithubResponse
import com.maxrave.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import com.maxrave.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import com.maxrave.kotlinytmusicscraper.pages.BrowseResult
import com.maxrave.kotlinytmusicscraper.pages.NextPage
import com.maxrave.kotlinytmusicscraper.pages.PlaylistPage
import com.maxrave.kotlinytmusicscraper.pages.SearchPage
import com.maxrave.kotlinytmusicscraper.parser.getPlaylistContinuation
import com.maxrave.kotlinytmusicscraper.parser.getPlaylistRadioEndpoint
import com.maxrave.kotlinytmusicscraper.parser.getPlaylistShuffleEndpoint
import com.maxrave.lyricsproviders.LyricsClient
import com.maxrave.lyricsproviders.models.response.MusixmatchCredential
import com.maxrave.lyricsproviders.models.response.MusixmatchTranslationLyricsResponse
import com.maxrave.lyricsproviders.models.response.SearchMusixmatchResponse
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.QUALITY
import com.maxrave.simpmusic.common.VIDEO_QUALITY
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.LocalDataSource
import com.maxrave.simpmusic.data.db.MusicDatabase
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.FollowedArtistSingleAndAlbum
import com.maxrave.simpmusic.data.db.entities.GoogleAccountEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.LyricsEntity
import com.maxrave.simpmusic.data.db.entities.NewFormatEntity
import com.maxrave.simpmusic.data.db.entities.NotificationEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.QueueEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SetVideoIdEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.db.entities.SongInfoEntity
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.artist.ArtistBrowse
import com.maxrave.simpmusic.data.model.browse.playlist.Author
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.model.explore.mood.Genre
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.explore.mood.MoodsMoment
import com.maxrave.simpmusic.data.model.explore.mood.genre.GenreObject
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.MoodsMomentObject
import com.maxrave.simpmusic.data.model.home.HomeItem
import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.data.model.podcast.PodcastBrowse
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.data.parser.parseAlbumData
import com.maxrave.simpmusic.data.parser.parseArtistData
import com.maxrave.simpmusic.data.parser.parseChart
import com.maxrave.simpmusic.data.parser.parseGenreObject
import com.maxrave.simpmusic.data.parser.parseLibraryPlaylist
import com.maxrave.simpmusic.data.parser.parseMixedContent
import com.maxrave.simpmusic.data.parser.parseMoodsMomentObject
import com.maxrave.simpmusic.data.parser.parseNewRelease
import com.maxrave.simpmusic.data.parser.parsePlaylistData
import com.maxrave.simpmusic.data.parser.parsePodcast
import com.maxrave.simpmusic.data.parser.parsePodcastContinueData
import com.maxrave.simpmusic.data.parser.parsePodcastData
import com.maxrave.simpmusic.data.parser.parseSetVideoId
import com.maxrave.simpmusic.data.parser.search.parseSearchAlbum
import com.maxrave.simpmusic.data.parser.search.parseSearchArtist
import com.maxrave.simpmusic.data.parser.search.parseSearchPlaylist
import com.maxrave.simpmusic.data.parser.search.parseSearchSong
import com.maxrave.simpmusic.data.parser.search.parseSearchVideo
import com.maxrave.simpmusic.data.parser.toListThumbnail
import com.maxrave.simpmusic.data.type.PlaylistType
import com.maxrave.simpmusic.data.type.RecentlyType
import com.maxrave.simpmusic.extension.bestMatchingIndex
import com.maxrave.simpmusic.extension.isNetworkAvailable
import com.maxrave.simpmusic.extension.toListTrack
import com.maxrave.simpmusic.extension.toLyrics
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.test.source.MergingMediaSourceFactory
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.FilterState
import com.maxrave.spotify.Spotify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.time.LocalDateTime
import kotlin.collections.firstOrNull
import kotlin.math.abs

class MainRepository(
    private val localDataSource: LocalDataSource,
    private val dataStoreManager: DataStoreManager,
    private val youTube: YouTube,
    private val spotify: Spotify,
    private val lyricsClient: LyricsClient,
    private val database: MusicDatabase,
    private val context: Context,
) {
    var init = false

    fun initYouTube(scope: CoroutineScope) {
        if (init) return
        init = true
        youTube.cacheControlInterceptor =
            object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val originalResponse = chain.proceed(chain.request())
                    if (isNetworkAvailable(context)) {
                        val maxAge = 60 // read from cache for 1 minute
                        return originalResponse
                            .newBuilder()
                            .header("Cache-Control", "public, max-age=$maxAge")
                            .build()
                    } else {
                        val maxStale = 60 * 60 * 24 * 28 // tolerate 4-weeks stale
                        return originalResponse
                            .newBuilder()
                            .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                            .build()
                    }
                }
            }
        youTube.forceCacheInterceptor =
            Interceptor { chain ->
                val builder: Request.Builder = chain.request().newBuilder()
                if (!isNetworkAvailable(context)) {
                    builder.cacheControl(CacheControl.FORCE_CACHE)
                }
                chain.proceed(builder.build())
            }
        youTube.cachePath = File(context.cacheDir, "http-cache")
        scope.launch {
            val resetSpotifyToken =
                launch {
                    dataStoreManager.setSpotifyClientToken("")
                    dataStoreManager.setSpotifyPersonalToken("")
                    dataStoreManager.setSpotifyClientTokenExpires(System.currentTimeMillis())
                    dataStoreManager.setSpotifyPersonalTokenExpires(System.currentTimeMillis())
                }
            val localeJob =
                launch {
                    combine(dataStoreManager.location, dataStoreManager.language) { location, language ->
                        Pair(location, language)
                    }.collectLatest { (location, language) ->
                        youTube.locale =
                            YouTubeLocale(
                                location,
                                try {
                                    language.substring(0..1)
                                } catch (e: Exception) {
                                    "en"
                                },
                            )
                    }
                }
            val ytCookieJob =
                launch {
                    dataStoreManager.cookie.distinctUntilChanged().collectLatest { cookie ->
                        if (cookie.isNotEmpty()) {
                            youTube.cookie = cookie
                            youTube.visitorData()?.let {
                                youTube.visitorData = it
                            }
                        } else {
                            youTube.cookie = null
                        }
                    }
                }
            val musixmatchCookieJob =
                launch {
                    dataStoreManager.musixmatchCookie.collectLatest { cookie ->
                        lyricsClient.musixmatchCookie = cookie
                    }
                }
            val usingProxy =
                launch {
                    combine(
                        dataStoreManager.usingProxy,
                        dataStoreManager.proxyType,
                        dataStoreManager.proxyHost,
                        dataStoreManager.proxyPort,
                    ) { usingProxy, proxyType, proxyHost, proxyPort ->
                        Pair(usingProxy == DataStoreManager.TRUE, Triple(proxyType, proxyHost, proxyPort))
                    }.collectLatest { (usingProxy, data) ->
                        if (usingProxy) {
                            withContext(Dispatchers.IO) {
                                youTube.setProxy(
                                    data.first == DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP,
                                    data.second,
                                    data.third,
                                )
                                spotify.setProxy(
                                    data.first == DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP,
                                    data.second,
                                    data.third,
                                )
                                lyricsClient.setProxy(
                                    data.first == DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP,
                                    data.second,
                                    data.third,
                                )
                            }
                        } else {
                            youTube.removeProxy()
                            spotify.removeProxy()
                            lyricsClient.removeProxy()
                        }
                    }
                }
            val dataSyncIdJob =
                launch {
                    dataStoreManager.dataSyncId.collectLatest { dataSyncId ->
                        youTube.dataSyncId = dataSyncId
                    }
                }
            val visitorDataJob =
                launch {
                    dataStoreManager.visitorData.collectLatest { visitorData ->
                        youTube.visitorData = visitorData
                    }
                }

            localeJob.join()
            ytCookieJob.join()
            musixmatchCookieJob.join()
            usingProxy.join()
            dataSyncIdJob.join()
            visitorDataJob.join()
            resetSpotifyToken.join()
        }
    }

    fun getMusixmatchCookie() = lyricsClient.musixmatchCookie

    fun getYouTubeCookie() = youTube.cookie

    // Database
    fun closeDatabase() {
        if (database.isOpen) {
            database.close()
        }
    }

    fun getDatabasePath() = database.openHelper.writableDatabase.path

    fun databaseDaoCheckpoint() = localDataSource.checkpoint()

    fun getSearchHistory(): Flow<List<SearchHistory>> =
        flow {
            emit(localDataSource.getSearchHistory())
        }.flowOn(Dispatchers.IO)

    suspend fun insertSearchHistory(searchHistory: SearchHistory) = withContext(Dispatchers.IO) { localDataSource.insertSearchHistory(searchHistory) }

    suspend fun deleteSearchHistory() =
        withContext(Dispatchers.IO) {
            localDataSource.deleteSearchHistory()
        }

    fun getAllSongs(): Flow<List<SongEntity>> =
        flow {
            emit(localDataSource.getAllSongs())
        }.flowOn(Dispatchers.IO)

    suspend fun setInLibrary(
        videoId: String,
        inLibrary: LocalDateTime,
    ) = withContext(Dispatchers.IO) { localDataSource.setInLibrary(videoId, inLibrary) }

    fun getSongsByListVideoId(listVideoId: List<String>): Flow<List<SongEntity>> =
        flow {
            emit(
                localDataSource.getSongByListVideoIdFull(listVideoId),
            )
        }.flowOn(Dispatchers.IO)

    fun getSongsByListVideoIdOffset(
        listVideoId: List<String>,
        offset: Int,
    ): Flow<List<SongEntity>> =
        flow<List<SongEntity>> {
            emit(localDataSource.getSongByListVideoId(listVideoId, offset))
        }.flowOn(Dispatchers.IO)

    fun getDownloadedSongs(): Flow<List<SongEntity>?> = flow { emit(localDataSource.getDownloadedSongs()) }.flowOn(Dispatchers.IO)

    fun getDownloadedSongsAsFlow(offset: Int) = localDataSource.getDownloadedSongsAsFlow(offset)

    fun getDownloadingSongs(): Flow<List<SongEntity>?> = flow { emit(localDataSource.getDownloadingSongs()) }.flowOn(Dispatchers.IO)

    fun getPreparingSongs(): Flow<List<SongEntity>> =
        flow {
            emit(localDataSource.getPreparingSongs())
        }.flowOn(Dispatchers.IO)

    fun getDownloadedVideoIdListFromListVideoIdAsFlow(listVideoId: List<String>) =
        localDataSource.getDownloadedVideoIdListFromListVideoIdAsFlow(listVideoId)

    fun getLikedSongs(): Flow<List<SongEntity>> = localDataSource.getLikedSongs()

    fun getLibrarySongs(): Flow<List<SongEntity>> =
        flow {
            emit(localDataSource.getLibrarySongs())
        }.flowOn(Dispatchers.IO)

    fun getCanvasSong(max: Int): Flow<List<SongEntity>> =
        flow {
            emit(localDataSource.getCanvasSong(max))
        }.flowOn(Dispatchers.IO)

    fun getSongById(id: String): Flow<SongEntity?> =
        flow {
            emit(localDataSource.getSong(id))
        }.flowOn(Dispatchers.IO)

    fun getSongAsFlow(id: String) = localDataSource.getSongAsFlow(id)

    fun insertSong(songEntity: SongEntity): Flow<Long> = flow<Long> { emit(localDataSource.insertSong(songEntity)) }.flowOn(Dispatchers.IO)

    fun updateThumbnailsSongEntity(
        thumbnail: String,
        videoId: String,
    ): Flow<Int> = flow { emit(localDataSource.updateThumbnailsSongEntity(thumbnail, videoId)) }.flowOn(Dispatchers.IO)

    suspend fun updateListenCount(videoId: String) =
        withContext(Dispatchers.IO) {
            localDataSource.updateListenCount(videoId)
        }

    suspend fun updateCanvasUrl(
        videoId: String,
        canvasUrl: String,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateCanvasUrl(videoId, canvasUrl)
    }

    suspend fun updateLikeStatus(
        videoId: String,
        likeStatus: Int,
    ) = withContext(Dispatchers.Main) { localDataSource.updateLiked(likeStatus, videoId) }

    fun updateSongInLibrary(
        inLibrary: LocalDateTime,
        videoId: String,
    ): Flow<Int> = flow { emit(localDataSource.updateSongInLibrary(inLibrary, videoId)) }

    suspend fun updateDurationSeconds(
        durationSeconds: Int,
        videoId: String,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updateDurationSeconds(
            durationSeconds,
            videoId,
        )
    }

    fun getMostPlayedSongs(): Flow<List<SongEntity>> = localDataSource.getMostPlayedSongs()

    suspend fun updateDownloadState(
        videoId: String,
        downloadState: Int,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updateDownloadState(
            downloadState,
            videoId,
        )
    }

    fun getAllArtists(): Flow<List<ArtistEntity>> =
        flow {
            emit(localDataSource.getAllArtists())
        }.flowOn(Dispatchers.IO)

    fun getArtistById(id: String): Flow<ArtistEntity> =
        flow {
            emit(localDataSource.getArtist(id))
        }.flowOn(Dispatchers.IO)

    suspend fun insertArtist(artistEntity: ArtistEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertArtist(artistEntity)
        }

    suspend fun updateArtistImage(
        channelId: String,
        thumbnail: String,
    ) = withContext(
        Dispatchers.Main,
    ) {
        localDataSource.updateArtistImage(
            channelId,
            thumbnail,
        )
    }

    suspend fun updateFollowedStatus(
        channelId: String,
        followedStatus: Int,
    ) = withContext(
        Dispatchers.Main,
    ) { localDataSource.updateFollowed(followedStatus, channelId) }

    fun getFollowedArtists(): Flow<List<ArtistEntity>> = localDataSource.getFollowedArtists()

    suspend fun updateArtistInLibrary(
        inLibrary: LocalDateTime,
        channelId: String,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updateArtistInLibrary(
            inLibrary,
            channelId,
        )
    }

    fun getAllAlbums(): Flow<List<AlbumEntity>> =
        flow {
            emit(localDataSource.getAllAlbums())
        }.flowOn(Dispatchers.IO)

    fun getAlbum(id: String): Flow<AlbumEntity?> =
        flow {
            emit(localDataSource.getAlbum(id))
        }.flowOn(Dispatchers.IO)

    fun getAlbumAsFlow(id: String) = localDataSource.getAlbumAsFlow(id)

    fun getLikedAlbums(): Flow<List<AlbumEntity>> =
        flow {
            emit(localDataSource.getLikedAlbums())
        }.flowOn(Dispatchers.IO)

    fun insertAlbum(albumEntity: AlbumEntity) =
        flow {
            emit(localDataSource.insertAlbum(albumEntity))
        }.flowOn(Dispatchers.IO)

    suspend fun updateAlbumLiked(
        albumId: String,
        likeStatus: Int,
    ) = withContext(Dispatchers.Main) { localDataSource.updateAlbumLiked(likeStatus, albumId) }

    suspend fun updateAlbumInLibrary(
        inLibrary: LocalDateTime,
        albumId: String,
    ) = withContext(
        Dispatchers.Main,
    ) { localDataSource.updateAlbumInLibrary(inLibrary, albumId) }

    suspend fun updateAlbumDownloadState(
        albumId: String,
        downloadState: Int,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updateAlbumDownloadState(
            downloadState,
            albumId,
        )
    }

    suspend fun getAllPlaylists(): Flow<List<PlaylistEntity>> =
        flow {
            emit(localDataSource.getAllPlaylists())
        }.flowOn(Dispatchers.IO)

    suspend fun getPlaylist(id: String): Flow<PlaylistEntity?> =
        flow {
            emit(localDataSource.getPlaylist(id))
        }.flowOn(Dispatchers.IO)

    suspend fun getLikedPlaylists(): Flow<List<PlaylistEntity>> = flow { emit(localDataSource.getLikedPlaylists()) }.flowOn(Dispatchers.IO)

    suspend fun insertPlaylist(playlistEntity: PlaylistEntity) = withContext(Dispatchers.IO) { localDataSource.insertPlaylist(playlistEntity) }

    suspend fun insertAndReplacePlaylist(playlistEntity: PlaylistEntity) =
        withContext(Dispatchers.IO) {
            val oldPlaylist = getPlaylist(playlistEntity.id).firstOrNull()
            if (oldPlaylist != null) {
                localDataSource.insertAndReplacePlaylist(
                    playlistEntity.copy(
                        downloadState = oldPlaylist.downloadState,
                        liked = oldPlaylist.liked,
                    ),
                )
            } else {
                localDataSource.insertAndReplacePlaylist(playlistEntity)
            }
        }

    suspend fun insertRadioPlaylist(playlistEntity: PlaylistEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertRadioPlaylist(playlistEntity) }

    suspend fun updatePlaylistLiked(
        playlistId: String,
        likeStatus: Int,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updatePlaylistLiked(
            likeStatus,
            playlistId,
        )
    }

    suspend fun updatePlaylistInLibrary(
        inLibrary: LocalDateTime,
        playlistId: String,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updatePlaylistInLibrary(
            inLibrary,
            playlistId,
        )
    }

    suspend fun updatePlaylistDownloadState(
        playlistId: String,
        downloadState: Int,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updatePlaylistDownloadState(
            downloadState,
            playlistId,
        )
    }

    suspend fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>> = flow { emit(localDataSource.getAllLocalPlaylists()) }.flowOn(Dispatchers.IO)

    suspend fun getLocalPlaylist(id: Long): Flow<LocalPlaylistEntity?> = flow { emit(localDataSource.getLocalPlaylist(id)) }.flowOn(Dispatchers.IO)

    suspend fun insertLocalPlaylist(localPlaylistEntity: LocalPlaylistEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertLocalPlaylist(localPlaylistEntity) }

    suspend fun deleteLocalPlaylist(id: Long) =
        withContext(Dispatchers.IO) {
            localDataSource.deleteLocalPlaylist(id)
        }

    suspend fun updateLocalPlaylistTitle(
        title: String,
        id: Long,
    ) = withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistTitle(title, id) }

    suspend fun updateLocalPlaylistThumbnail(
        thumbnail: String,
        id: Long,
    ) = withContext(
        Dispatchers.IO,
    ) { localDataSource.updateLocalPlaylistThumbnail(thumbnail, id) }

    suspend fun updateLocalPlaylistTracks(
        tracks: List<String>,
        id: Long,
    ) = withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistTracks(tracks, id) }

    suspend fun updateLocalPlaylistDownloadState(
        downloadState: Int,
        id: Long,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistDownloadState(
            downloadState,
            id,
        )
    }

    suspend fun updateLocalPlaylistInLibrary(
        inLibrary: LocalDateTime,
        id: Long,
    ) = withContext(
        Dispatchers.IO,
    ) { localDataSource.updateLocalPlaylistInLibrary(inLibrary, id) }

    fun getDownloadedLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
        flow { emit(localDataSource.getDownloadedLocalPlaylists()) }.flowOn(Dispatchers.IO)

    fun getAllRecentData(): Flow<List<RecentlyType>> =
        flow {
            emit(localDataSource.getAllRecentData())
        }.flowOn(Dispatchers.IO)

    suspend fun getAllDownloadedPlaylist(): Flow<List<PlaylistType>> =
        flow { emit(localDataSource.getAllDownloadedPlaylist()) }.flowOn(Dispatchers.IO)

    suspend fun getRecentSong(
        limit: Int,
        offset: Int,
    ) = localDataSource.getRecentSongs(limit, offset)

    suspend fun getSavedLyrics(videoId: String): Flow<LyricsEntity?> = flow { emit(localDataSource.getSavedLyrics(videoId)) }.flowOn(Dispatchers.IO)

    suspend fun insertLyrics(lyricsEntity: LyricsEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertLyrics(lyricsEntity)
        }

    suspend fun insertNewFormat(newFormat: NewFormatEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertNewFormat(newFormat)
        }

    suspend fun getNewFormat(videoId: String): Flow<NewFormatEntity?> = flow { emit(localDataSource.getNewFormat(videoId)) }.flowOn(Dispatchers.Main)

    suspend fun getFormatFlow(videoId: String) = localDataSource.getNewFormatAsFlow(videoId)

    suspend fun insertSongInfo(songInfo: SongInfoEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertSongInfo(songInfo)
        }

    suspend fun getSongInfoEntity(videoId: String): Flow<SongInfoEntity?> =
        flow { emit(localDataSource.getSongInfo(videoId)) }.flowOn(Dispatchers.Main)

    suspend fun recoverQueue(temp: List<Track>) {
        val queueEntity = QueueEntity(listTrack = temp)
        withContext(Dispatchers.IO) { localDataSource.recoverQueue(queueEntity) }
    }

    suspend fun removeQueue() {
        withContext(Dispatchers.IO) { localDataSource.deleteQueue() }
    }

    suspend fun getSavedQueue(): Flow<List<QueueEntity>?> =
        flow {
            emit(localDataSource.getQueue())
        }.flowOn(Dispatchers.IO)

    suspend fun getLocalPlaylistByYoutubePlaylistId(playlistId: String): Flow<LocalPlaylistEntity?> =
        flow { emit(localDataSource.getLocalPlaylistByYoutubePlaylistId(playlistId)) }.flowOn(
            Dispatchers.IO,
        )

    private suspend fun insertSetVideoId(setVideoId: SetVideoIdEntity) = withContext(Dispatchers.IO) { localDataSource.insertSetVideoId(setVideoId) }

    suspend fun getSetVideoId(videoId: String): Flow<SetVideoIdEntity?> = flow { emit(localDataSource.getSetVideoId(videoId)) }.flowOn(Dispatchers.IO)

    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) =
        withContext(Dispatchers.IO) {
            localDataSource.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }

    suspend fun getPlaylistPairSong(playlistId: Long): Flow<List<PairSongLocalPlaylist>?> =
        flow<List<PairSongLocalPlaylist>?> {
            emit(localDataSource.getPlaylistPairSong(playlistId))
        }.flowOn(Dispatchers.IO)

    suspend fun getPlaylistPairSongByListPosition(
        playlistId: Long,
        listPosition: List<Int>,
    ): Flow<List<PairSongLocalPlaylist>?> =
        flow {
            emit(localDataSource.getPlaylistPairSongByListPosition(playlistId, listPosition))
        }.flowOn(Dispatchers.IO)

    suspend fun getPlaylistPairSongByOffset(
        playlistId: Long,
        offset: Int,
        filterState: FilterState,
        totalCount: Int,
    ): Flow<List<PairSongLocalPlaylist>?> =
        flow {
            emit(localDataSource.getPlaylistPairSongByOffset(playlistId, offset, filterState, totalCount))
        }.flowOn(Dispatchers.IO)

    suspend fun deletePairSongLocalPlaylist(
        playlistId: Long,
        videoId: String,
    ) = withContext(Dispatchers.IO) {
        localDataSource.deletePairSongLocalPlaylist(playlistId, videoId)
    }

    suspend fun updateLocalPlaylistYouTubePlaylistId(
        id: Long,
        ytId: String?,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistYouTubePlaylistId(id, ytId)
    }

    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(
        id: Long,
        syncState: Int,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)
    }

    suspend fun insertGoogleAccount(googleAccountEntity: GoogleAccountEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertGoogleAccount(googleAccountEntity)
        }

    suspend fun getGoogleAccounts(): Flow<List<GoogleAccountEntity>?> =
        flow<List<GoogleAccountEntity>?> { emit(localDataSource.getGoogleAccounts()) }.flowOn(
            Dispatchers.IO,
        )

    suspend fun getUsedGoogleAccount(): Flow<GoogleAccountEntity?> =
        flow<GoogleAccountEntity?> { emit(localDataSource.getUsedGoogleAccount()) }.flowOn(
            Dispatchers.IO,
        )

    suspend fun deleteGoogleAccount(email: String) =
        withContext(Dispatchers.IO) {
            localDataSource.deleteGoogleAccount(email)
        }

    suspend fun updateGoogleAccountUsed(
        email: String,
        isUsed: Boolean,
    ): Flow<Int> = flow { emit(localDataSource.updateGoogleAccountUsed(email, isUsed)) }.flowOn(Dispatchers.IO)

    suspend fun insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum: FollowedArtistSingleAndAlbum) =
        withContext(Dispatchers.IO) {
            localDataSource.insertFollowedArtistSingleAndAlbum(followedArtistSingleAndAlbum)
        }

    suspend fun deleteFollowedArtistSingleAndAlbum(channelId: String) =
        withContext(Dispatchers.IO) {
            localDataSource.deleteFollowedArtistSingleAndAlbum(channelId)
        }

    suspend fun getAllFollowedArtistSingleAndAlbums(): Flow<List<FollowedArtistSingleAndAlbum>?> =
        flow {
            emit(localDataSource.getAllFollowedArtistSingleAndAlbums())
        }.flowOn(Dispatchers.IO)

    suspend fun getFollowedArtistSingleAndAlbum(channelId: String): Flow<FollowedArtistSingleAndAlbum?> =
        flow {
            emit(localDataSource.getFollowedArtistSingleAndAlbum(channelId))
        }.flowOn(Dispatchers.IO)

    suspend fun insertNotification(notificationEntity: NotificationEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertNotification(notificationEntity)
        }

    suspend fun getAllNotifications(): Flow<List<NotificationEntity>?> =
        flow {
            emit(localDataSource.getAllNotification())
        }.flowOn(Dispatchers.IO)

    suspend fun deleteNotification(id: Long) =
        withContext(Dispatchers.IO) {
            localDataSource.deleteNotification(id)
        }

    suspend fun getAccountInfo() =
        flow<AccountInfo?> {
            youTube
                .accountInfo()
                .onSuccess { accountInfo ->
                    emit(accountInfo)
                }.onFailure {
                    it.printStackTrace()
                    emit(null)
                }
        }.flowOn(Dispatchers.IO)

    suspend fun getHomeData(params: String? = null): Flow<Resource<ArrayList<HomeItem>>> =
        flow {
            runCatching {
                val limit = dataStoreManager.homeLimit.first()
                youTube
                    .customQuery(browseId = "FEmusic_home", params = params)
                    .onSuccess { result ->
                        val list: ArrayList<HomeItem> = arrayListOf()
                        if (result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicCarouselShelfRenderer
                                ?.header
                                ?.musicCarouselShelfBasicHeaderRenderer
                                ?.strapline
                                ?.runs
                                ?.get(
                                    0,
                                )?.text != null
                        ) {
                            val accountName =
                                result.contents
                                    ?.singleColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(
                                        0,
                                    )?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(
                                        0,
                                    )?.musicCarouselShelfRenderer
                                    ?.header
                                    ?.musicCarouselShelfBasicHeaderRenderer
                                    ?.strapline
                                    ?.runs
                                    ?.get(
                                        0,
                                    )?.text ?: ""
                            val accountThumbUrl =
                                result.contents
                                    ?.singleColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(
                                        0,
                                    )?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(
                                        0,
                                    )?.musicCarouselShelfRenderer
                                    ?.header
                                    ?.musicCarouselShelfBasicHeaderRenderer
                                    ?.thumbnail
                                    ?.musicThumbnailRenderer
                                    ?.thumbnail
                                    ?.thumbnails
                                    ?.get(
                                        0,
                                    )?.url
                                    ?.replace("s88", "s352") ?: ""
                            if (accountName != "" && accountThumbUrl != "") {
                                dataStoreManager.putString("AccountName", accountName)
                                dataStoreManager.putString("AccountThumbUrl", accountThumbUrl)
                            }
                        }
                        var continueParam =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.continuations
                                ?.get(
                                    0,
                                )?.nextContinuationData
                                ?.continuation
                        val data =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                        list.addAll(parseMixedContent(data, context))
                        var count = 0
                        while (count < limit && continueParam != null) {
                            youTube
                                .customQuery(browseId = "", continuation = continueParam)
                                .onSuccess { response ->
                                    continueParam =
                                        response.continuationContents
                                            ?.sectionListContinuation
                                            ?.continuations
                                            ?.get(
                                                0,
                                            )?.nextContinuationData
                                            ?.continuation
                                    Log.d("Repository", "continueParam: $continueParam")
                                    val dataContinue =
                                        response.continuationContents?.sectionListContinuation?.contents
                                    list.addAll(parseMixedContent(dataContinue, context))
                                    count++
                                    Log.d("Repository", "count: $count")
                                }.onFailure {
                                    Log.e("Repository", "Error: ${it.message}")
                                    count++
                                }
                        }
                        Log.d("Repository", "List size: ${list.size}")
                        emit(Resource.Success<ArrayList<HomeItem>>(list))
                    }.onFailure { error ->
                        emit(Resource.Error<ArrayList<HomeItem>>(error.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getNewRelease(): Flow<Resource<ArrayList<HomeItem>>> =
        flow {
            youTube
                .newRelease()
                .onSuccess { result ->
                    emit(Resource.Success<ArrayList<HomeItem>>(parseNewRelease(result, context)))
                }.onFailure { error ->
                    emit(Resource.Error<ArrayList<HomeItem>>(error.message.toString()))
                }
        }.flowOn(Dispatchers.IO)

    suspend fun getChartData(countryCode: String = "KR"): Flow<Resource<Chart>> =
        flow {
            runCatching {
                youTube
                    .customQuery("FEmusic_charts", country = countryCode)
                    .onSuccess { result ->
                        val data =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                        val chart = parseChart(data)
                        if (chart != null) {
                            emit(Resource.Success<Chart>(chart))
                        } else {
                            emit(Resource.Error<Chart>("Error"))
                        }
                    }.onFailure { error ->
                        emit(Resource.Error<Chart>(error.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getMoodAndMomentsData(): Flow<Resource<Mood>> =
        flow {
            runCatching {
                youTube
                    .moodAndGenres()
                    .onSuccess { result ->
                        val listMoodMoments: ArrayList<MoodsMoment> = arrayListOf()
                        val listGenre: ArrayList<Genre> = arrayListOf()
                        result[0].let { moodsmoment ->
                            for (item in moodsmoment.items) {
                                listMoodMoments.add(
                                    MoodsMoment(
                                        params = item.endpoint.params ?: "",
                                        title = item.title,
                                    ),
                                )
                            }
                        }
                        result[1].let { genres ->
                            for (item in genres.items) {
                                listGenre.add(
                                    Genre(
                                        params = item.endpoint.params ?: "",
                                        title = item.title,
                                    ),
                                )
                            }
                        }
                        emit(Resource.Success<Mood>(Mood(listGenre, listMoodMoments)))
                    }.onFailure { e ->
                        emit(Resource.Error<Mood>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getMoodData(params: String): Flow<Resource<MoodsMomentObject>> =
        flow {
            runCatching {
                youTube
                    .customQuery(
                        browseId = "FEmusic_moods_and_genres_category",
                        params = params,
                    ).onSuccess { result ->
                        val data = parseMoodsMomentObject(result)
                        if (data != null) {
                            emit(Resource.Success<MoodsMomentObject>(data))
                        } else {
                            emit(Resource.Error<MoodsMomentObject>("Error"))
                        }
                    }.onFailure { e ->
                        emit(Resource.Error<MoodsMomentObject>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getGenreData(params: String): Flow<Resource<GenreObject>> =
        flow {
            kotlin.runCatching {
                youTube
                    .customQuery(
                        browseId = "FEmusic_moods_and_genres_category",
                        params = params,
                    ).onSuccess { result ->
                        val data = parseGenreObject(result)
                        if (data != null) {
                            emit(Resource.Success<GenreObject>(data))
                        } else {
                            emit(Resource.Error<GenreObject>("Error"))
                        }
                    }.onFailure { e ->
                        emit(Resource.Error<GenreObject>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    fun getContinueTrack(
        playlistId: String,
        continuation: String,
        fromPlaylist: Boolean = false,
    ): Flow<Pair<ArrayList<Track>?, String?>> =
        flow {
            runCatching {
                var newContinuation: String? = null
                newContinuation = null
                Log.d("getContinueTrack", "playlistId: $playlistId")
                Log.d("getContinueTrack", "continuation: $continuation")
                if (!fromPlaylist) {
                    youTube
                        .next(
                            if (playlistId.startsWith("RRDAMVM")) {
                                WatchEndpoint(videoId = playlistId.removePrefix("RRDAMVM"))
                            } else {
                                WatchEndpoint(playlistId = playlistId)
                            },
                            continuation = continuation,
                        ).onSuccess { next ->
                            val data: ArrayList<SongItem> = arrayListOf()
                            data.addAll(next.items)
                            newContinuation = next.continuation
                            emit(Pair(data.toListTrack(), newContinuation))
                        }.onFailure { exception ->
                            exception.printStackTrace()
                            emit(Pair(null, null))
                        }
                } else {
                    youTube
                        .customQuery(
                            browseId = null,
                            continuation = continuation,
                            setLogin = true,
                        ).onSuccess { values ->
                            Log.d("getPlaylistData", "continue: $continuation")
                            Log.d(
                                "getPlaylistData",
                                "values: ${values.onResponseReceivedActions}",
                            )
                            val dataMore: List<SongItem> =
                                values.onResponseReceivedActions
                                    ?.firstOrNull()
                                    ?.appendContinuationItemsAction
                                    ?.continuationItems
                                    ?.apply {
                                        Log.w("getContinueTrack", "dataMore: ${this.size}")
                                    }?.mapNotNull {
                                        NextPage.fromMusicResponsiveListItemRenderer(
                                            it.musicResponsiveListItemRenderer ?: return@mapNotNull null,
                                        )
                                    } ?: emptyList()
                            newContinuation =
                                values.getPlaylistContinuation()
                            emit(
                                Pair<ArrayList<Track>?, String?>(
                                    dataMore.toListTrack(),
                                    newContinuation,
                                ),
                            )
                        }.onFailure {
                            Log.e("getContinueTrack", "Error: ${it.message}")
                            emit(Pair(null, null))
                        }
                }
            }
        }

    suspend fun getRadio(
        radioId: String,
        originalTrack: SongEntity? = null,
        artist: ArtistEntity? = null,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>> =
        if (radioId.startsWith("RDAT")) {
            getRDATRadioData(radioId)
        } else {
            flow {
                runCatching {
                    youTube
                        .next(endpoint = WatchEndpoint(playlistId = radioId))
                        .onSuccess { next ->
                            Log.w("Radio", "Title: ${next.title}")
                            val data: ArrayList<SongItem> = arrayListOf()
                            data.addAll(next.items)
                            var continuation = next.continuation
                            Log.w("Radio", "data: ${data.size}")
                            var count = 0
                            while (continuation != null && count < 3) {
                                youTube
                                    .next(
                                        endpoint = WatchEndpoint(playlistId = radioId),
                                        continuation = continuation,
                                    ).onSuccess { nextContinue ->
                                        data.addAll(nextContinue.items)
                                        continuation = nextContinue.continuation
                                        if (data.size >= 50) {
                                            count = 3
                                        }
                                        Log.w("Radio", "data: ${data.size}")
                                        count++
                                    }.onFailure {
                                        count = 3
                                    }
                            }
                            val listTrackResult = data.toListTrack()
                            if (originalTrack != null) {
                                listTrackResult.add(0, originalTrack.toTrack())
                            }
                            Log.w("Repository", "data: ${data.size}")
                            val playlistBrowse =
                                PlaylistBrowse(
                                    author = Author(id = "", name = "YouTube Music"),
                                    description =
                                        context.getString(
                                            R.string.auto_created_by_youtube_music,
                                        ),
                                    duration = "",
                                    durationSeconds = 0,
                                    id = radioId,
                                    privacy = "PRIVATE",
                                    thumbnails =
                                        listOf(
                                            Thumbnail(
                                                544,
                                                originalTrack?.thumbnails ?: artist?.thumbnails ?: "",
                                                544,
                                            ),
                                        ),
                                    title = "${originalTrack?.title ?: artist?.name} ${
                                        context.getString(
                                            R.string.radio,
                                        )
                                    }",
                                    trackCount = listTrackResult.size,
                                    tracks = listTrackResult,
                                    year = LocalDateTime.now().year.toString(),
                                )
                            Log.w("Repository", "playlistBrowse: $playlistBrowse")
                            emit(Resource.Success<Pair<PlaylistBrowse, String?>>(Pair(playlistBrowse, continuation)))
                        }.onFailure { exception ->
                            exception.printStackTrace()
                            emit(Resource.Error<Pair<PlaylistBrowse, String?>>(exception.message.toString()))
                        }
                }
            }.flowOn(Dispatchers.IO)
        }

    suspend fun reloadSuggestionPlaylist(reloadParams: String): Flow<Pair<String?, ArrayList<Track>?>?> =
        flow {
            runCatching {
                youTube
                    .customQuery(browseId = "", continuation = reloadParams, setLogin = true)
                    .onSuccess { values ->
                        val data = values.continuationContents?.musicShelfContinuation?.contents
                        val dataResult:
                            ArrayList<SearchResponse.ContinuationContents.MusicShelfContinuation.Content> =
                            arrayListOf()
                        if (!data.isNullOrEmpty()) {
                            dataResult.addAll(data)
                        }
                        val reloadParamsNew =
                            values.continuationContents
                                ?.musicShelfContinuation
                                ?.continuations
                                ?.get(
                                    0,
                                )?.reloadContinuationData
                                ?.continuation
                        if (dataResult.isNotEmpty()) {
                            val listTrack: ArrayList<Track> = arrayListOf()
                            dataResult.forEach {
                                listTrack.add(
                                    (
                                        SearchPage.toYTItem(
                                            it.musicResponsiveListItemRenderer,
                                        ) as SongItem
                                    ).toTrack(),
                                )
                            }
                            emit(Pair(reloadParamsNew, listTrack))
                        } else {
                            emit(null)
                        }
                    }.onFailure { exception ->
                        exception.printStackTrace()
                        emit(null)
                    }
            }
        }

    suspend fun getSuggestionPlaylist(ytPlaylistId: String): Flow<Pair<String?, ArrayList<Track>?>?> =
        flow {
            runCatching {
                var id = ""
                if (!ytPlaylistId.startsWith("VL")) {
                    id += "VL$ytPlaylistId"
                } else {
                    id += ytPlaylistId
                }
                youTube
                    .customQuery(browseId = id, setLogin = true)
                    .onSuccess { result ->
                        println(result)
                        var continueParam =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicPlaylistShelfRenderer
                                ?.continuations
                                ?.get(
                                    0,
                                )?.nextContinuationData
                                ?.continuation
                                ?: result.contents
                                    ?.singleColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(
                                        0,
                                    )?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.continuations
                                    ?.get(
                                        0,
                                    )?.nextContinuationData
                                    ?.continuation
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.secondaryContents
                                    ?.sectionListRenderer
                                    ?.continuations
                                    ?.firstOrNull()
                                    ?.nextContinuationData
                                    ?.continuation
                        val dataResult: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                        var reloadParams: String? = null
                        println("continueParam: $continueParam")
                        while (continueParam != null) {
                            youTube
                                .customQuery(
                                    browseId = "",
                                    continuation = continueParam,
                                    setLogin = true,
                                ).onSuccess { values ->
                                    val data =
                                        values.continuationContents
                                            ?.sectionListContinuation
                                            ?.contents
                                            ?.get(
                                                0,
                                            )?.musicShelfRenderer
                                            ?.contents
                                    println("data: $data")
                                    if (!data.isNullOrEmpty()) {
                                        dataResult.addAll(data)
                                    }
                                    reloadParams =
                                        values.continuationContents
                                            ?.sectionListContinuation
                                            ?.contents
                                            ?.get(
                                                0,
                                            )?.musicShelfRenderer
                                            ?.continuations
                                            ?.get(
                                                0,
                                            )?.reloadContinuationData
                                            ?.continuation
                                    continueParam =
                                        values.continuationContents
                                            ?.musicPlaylistShelfContinuation
                                            ?.continuations
                                            ?.get(
                                                0,
                                            )?.nextContinuationData
                                            ?.continuation
                                    println("reloadParams: $reloadParams")
                                    println("continueParam: $continueParam")
                                }.onFailure {
                                    Log.e("Repository", "Error: ${it.message}")
                                    continueParam = null
                                }
                        }
                        println("dataResult: ${dataResult.size}")
                        if (dataResult.isNotEmpty()) {
                            val listTrack: ArrayList<Track> = arrayListOf()
                            dataResult.forEach {
                                listTrack.add(
                                    (
                                        PlaylistPage.fromMusicResponsiveListItemRenderer(
                                            it.musicResponsiveListItemRenderer,
                                        ) as SongItem
                                    ).toTrack(),
                                )
                            }
                            println("listTrack: $listTrack")
                            emit(Pair(reloadParams, listTrack))
                        } else {
                            emit(null)
                        }
                    }.onFailure { exception ->
                        exception.printStackTrace()
                        emit(null)
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getPodcastData(podcastId: String): Flow<Resource<PodcastBrowse>> =
        flow {
            runCatching {
                youTube
                    .customQuery(browseId = podcastId)
                    .onSuccess { result ->
                        val listEpisode = arrayListOf<PodcastBrowse.EpisodeItem>()
                        val thumbnail =
                            result.background
                                ?.musicThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?.toListThumbnail()
                        val title =
                            result.contents
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
                                ?.text
                        val author =
                            result.contents
                                ?.twoColumnBrowseResultsRenderer
                                ?.tabs
                                ?.firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                                ?.let {
                                    Artist(
                                        id =
                                            it.straplineTextOne
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.navigationEndpoint
                                                ?.browseEndpoint
                                                ?.browseId,
                                        name =
                                            it.straplineTextOne
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.text ?: "",
                                    )
                                }
                        val authorThumbnail =
                            result.contents
                                ?.twoColumnBrowseResultsRenderer
                                ?.tabs
                                ?.firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                                ?.let {
                                    it.straplineThumbnail
                                        ?.musicThumbnailRenderer
                                        ?.thumbnail
                                        ?.thumbnails
                                        ?.lastOrNull()
                                        ?.url
                                }
                        val description =
                            result.contents
                                ?.twoColumnBrowseResultsRenderer
                                ?.tabs
                                ?.firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                                ?.description
                                ?.musicDescriptionShelfRenderer
                                ?.description
                                ?.runs
                                ?.map {
                                    it.text
                                }?.joinToString("")
                        val data =
                            result.contents
                                ?.twoColumnBrowseResultsRenderer
                                ?.secondaryContents
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicShelfRenderer
                                ?.contents
                        parsePodcastData(data, author).let {
                            listEpisode.addAll(it)
                        }
                        var continueParam =
                            result.contents
                                ?.twoColumnBrowseResultsRenderer
                                ?.secondaryContents
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicShelfRenderer
                                ?.continuations
                                ?.firstOrNull()
                                ?.nextContinuationData
                                ?.continuation
                        while (continueParam != null) {
                            youTube
                                .customQuery(continuation = continueParam, browseId = "")
                                .onSuccess { continueData ->
                                    parsePodcastContinueData(
                                        continueData.continuationContents?.musicShelfContinuation?.contents,
                                        author,
                                    ).let {
                                        listEpisode.addAll(it)
                                    }
                                    continueParam =
                                        continueData.continuationContents
                                            ?.musicShelfContinuation
                                            ?.continuations
                                            ?.firstOrNull()
                                            ?.nextContinuationData
                                            ?.continuation
                                }.onFailure {
                                    it.printStackTrace()
                                    continueParam = null
                                }
                        }
                        if (author != null) {
                            emit(
                                Resource.Success<PodcastBrowse>(
                                    PodcastBrowse(
                                        title = title ?: "",
                                        author = author,
                                        authorThumbnail = authorThumbnail,
                                        thumbnail = thumbnail ?: emptyList<Thumbnail>(),
                                        description = description,
                                        listEpisode = listEpisode,
                                    ),
                                ),
                            )
                        } else {
                            emit(Resource.Error<PodcastBrowse>("Error"))
                        }
                    }.onFailure { error ->
                        Log.w("Podcast", "Error: ${error.message}")
                        emit(Resource.Error<PodcastBrowse>(error.message.toString()))
                    }
            }
        }

    suspend fun getRDATRadioData(radioId: String): Flow<Resource<Pair<PlaylistBrowse, String?>>> =
        flow<Resource<Pair<PlaylistBrowse, String?>>> {
            runCatching {
                val id =
                    if (radioId.startsWith("VL")) {
                        radioId
                    } else {
                        "VL$radioId"
                    }
                youTube
                    .customQuery(browseId = id, setLogin = true)
                    .onSuccess { result ->
                        val listContent: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicPlaylistShelfRenderer
                                ?.contents
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.secondaryContents
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicPlaylistShelfRenderer
                                    ?.contents
                        if (data != null) {
                            Log.d("Data", "data: $data")
                            Log.d("Data", "data size: ${data.size}")
                            listContent.addAll(data)
                        }
                        val header =
                            result.header?.musicDetailHeaderRenderer
                                ?: result.header?.musicEditablePlaylistDetailHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicResponsiveHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicEditablePlaylistDetailHeaderRenderer
                                    ?.header
                                    ?.musicResponsiveHeaderRenderer
                        Log.d("Header", "header: $header")
                        val finalContinueParam =
                            result.getPlaylistContinuation()
                        Log.d("Repository", "playlist data: ${listContent.size}")
                        Log.d("Repository", "continueParam: $finalContinueParam")
//                        else {
//                            var listTrack = playlistBrowse.tracks.toMutableList()
                        Log.d("Repository", "playlist final data: ${listContent.size}")
                        if (finalContinueParam != null) {
                            parsePlaylistData(header, listContent, radioId, context)?.let { playlist ->
                                emit(
                                    Resource.Success(
                                        Pair(
                                            playlist.copy(
                                                author = Author("", "YouTube Music"),
                                            ),
                                            finalContinueParam,
                                        ),
                                    ),
                                )
                            } ?: emit(Resource.Error("Can't parse data"))
                        } else {
                            emit(Resource.Error("Continue param is null"))
                        }
                    }.onFailure { e ->
                        Log.e("Playlist Data", e.message ?: "Error")
                        emit(Resource.Error(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    fun getFullPlaylistData(playlistId: String): Flow<Resource<PlaylistBrowse>> =
        flow {
            runCatching {
                var id = ""
                id +=
                    if (!playlistId.startsWith("VL")) {
                        "VL$playlistId"
                    } else {
                        playlistId
                    }
                Log.d("getPlaylistData", "playlist id: $id")
                youTube
                    .customQuery(browseId = id, setLogin = true)
                    .onSuccess { result ->
                        val listContent: ArrayList<Track> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicPlaylistShelfRenderer
                                ?.contents
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.secondaryContents
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicPlaylistShelfRenderer
                                    ?.contents
                        if (data != null) {
                            Log.d("getPlaylistData", "data: $data")
                            Log.d("getPlaylistData", "data size: ${data.size}")
                        }
                        val header =
                            result.header?.musicDetailHeaderRenderer
                                ?: result.header?.musicEditablePlaylistDetailHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicResponsiveHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicEditablePlaylistDetailHeaderRenderer
                                    ?.header
                                    ?.musicResponsiveHeaderRenderer
                        Log.d("getPlaylistData", "header: $header")
                        var continueParam =
                            result.getPlaylistContinuation()
                        var count = 0
                        Log.d("getPlaylistData", "playlist data: ${listContent.size}")
                        Log.d("getPlaylistData", "continueParam: $continueParam")
//                        else {
//                            var listTrack = playlistBrowse.tracks.toMutableList()
                        while (continueParam != null) {
                            youTube
                                .customQuery(
                                    browseId = null,
                                    continuation = continueParam,
                                    setLogin = true,
                                ).onSuccess { values ->
                                    Log.d("getPlaylistData", "continue: $continueParam")
                                    Log.d(
                                        "getPlaylistData",
                                        "values: ${values.onResponseReceivedActions}",
                                    )
                                    val dataMore: List<SongItem> =
                                        values.onResponseReceivedActions
                                            ?.firstOrNull()
                                            ?.appendContinuationItemsAction
                                            ?.continuationItems
                                            ?.apply {
                                                Log.w("getPlaylistData", "dataMore: ${this.size}")
                                            }?.mapNotNull {
                                                NextPage.fromMusicResponsiveListItemRenderer(
                                                    it.musicResponsiveListItemRenderer ?: return@mapNotNull null,
                                                )
                                            } ?: emptyList()
                                    listContent.addAll(dataMore.map { it.toTrack() })
                                    continueParam =
                                        values.getPlaylistContinuation()
                                    count++
                                }.onFailure {
                                    Log.e("getPlaylistData", "Error: ${it.message}")
                                    continueParam = null
                                    count++
                                }
                        }
                        Log.d("getPlaylistData", "playlist final data: ${listContent.size}")
                        parsePlaylistData(header, data ?: emptyList(), playlistId, context)?.let { playlist ->
                            emit(
                                Resource.Success<PlaylistBrowse>(
                                    playlist.copy(
                                        tracks =
                                            playlist.tracks.toMutableList().apply {
                                                addAll(listContent)
                                            },
                                        trackCount = (playlist.trackCount + listContent.size),
                                    ),
                                ),
                            )
                        } ?: emit(Resource.Error<PlaylistBrowse>("Error"))
                    }.onFailure { e ->
                        Log.e("getPlaylistData", e.message ?: "Error")
                        emit(Resource.Error<PlaylistBrowse>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    fun getPlaylistData(playlistId: String): Flow<Resource<Pair<PlaylistBrowse, String?>>> =
        flow {
            runCatching {
                var id = ""
                id +=
                    if (!playlistId.startsWith("VL")) {
                        "VL$playlistId"
                    } else {
                        playlistId
                    }
                Log.d("getPlaylistData", "playlist id: $id")
                youTube
                    .customQuery(browseId = id, setLogin = true)
                    .onSuccess { result ->
                        val listContent: ArrayList<Track> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicPlaylistShelfRenderer
                                ?.contents
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.secondaryContents
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicPlaylistShelfRenderer
                                    ?.contents
                        if (data != null) {
                            Log.d("getPlaylistData", "data: $data")
                            Log.d("getPlaylistData", "data size: ${data.size}")
                        }
                        val header =
                            result.header?.musicDetailHeaderRenderer
                                ?: result.header?.musicEditablePlaylistDetailHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicResponsiveHeaderRenderer
                                ?: result.contents
                                    ?.twoColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.get(0)
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.get(0)
                                    ?.musicEditablePlaylistDetailHeaderRenderer
                                    ?.header
                                    ?.musicResponsiveHeaderRenderer
                        Log.d("getPlaylistData", "header: $header")
                        val continueParam =
                            result.getPlaylistContinuation()
                        val radioEndpoint =
                            result.getPlaylistRadioEndpoint()
                        val shuffleEndpoint =
                            result.getPlaylistShuffleEndpoint()
                        Log.d("getPlaylistData", "Endpoint: $radioEndpoint $shuffleEndpoint")
                        parsePlaylistData(header, data ?: emptyList(), playlistId, context)?.let { playlist ->
                            emit(
                                Resource.Success<Pair<PlaylistBrowse, String?>>(
                                    Pair(
                                        playlist.copy(
                                            tracks =
                                                playlist.tracks.toMutableList().apply {
                                                    addAll(listContent)
                                                },
                                            trackCount = (playlist.trackCount + listContent.size),
                                            shuffleEndpoint = shuffleEndpoint,
                                            radioEndpoint = radioEndpoint,
                                        ),
                                        continueParam,
                                    ),
                                ),
                            )
                        } ?: emit(
                            Resource.Error<
                                Pair<PlaylistBrowse, String?>,
                            >("Error"),
                        )
                    }.onFailure { e ->
                        Log.e("getPlaylistData", e.message ?: "Error")
                        emit(
                            Resource.Error<
                                Pair<PlaylistBrowse, String?>,
                            >(e.message.toString()),
                        )
                    }
            }
        }.flowOn(Dispatchers.IO)

    fun getAlbumData(browseId: String): Flow<Resource<AlbumBrowse>> =
        flow {
            runCatching {
                youTube
                    .album(browseId, withSongs = true)
                    .onSuccess { result ->
                        emit(Resource.Success<AlbumBrowse>(parseAlbumData(result)))
                    }.onFailure { e ->
                        Log.d("Album", "Error: ${e.message}")
                        emit(Resource.Error<AlbumBrowse>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    fun getAlbumMore(
        browseId: String,
        params: String,
    ): Flow<BrowseResult?> =
        flow {
            runCatching {
                youTube
                    .browse(browseId = browseId, params = params)
                    .onSuccess { result ->
                        Log.w("Album More", "result: $result")
                        emit(result)
                    }.onFailure {
                        it.printStackTrace()
                        emit(null)
                    }
            }
        }.flowOn(Dispatchers.IO)

    fun getArtistData(channelId: String): Flow<Resource<ArtistBrowse>> =
        flow {
            runCatching {
                youTube
                    .artist(channelId)
                    .onSuccess { result ->
                        emit(Resource.Success<ArtistBrowse>(parseArtistData(result)))
                    }.onFailure { e ->
                        Log.d("Artist", "Error: ${e.message}")
                        emit(Resource.Error<ArtistBrowse>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    fun getSearchDataSong(query: String): Flow<Resource<ArrayList<SongsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_SONG)
                    .onSuccess { result ->
                        val listSongs: ArrayList<SongsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchSong(result).let { list ->
                            listSongs.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchSong(values).let { list ->
                                        listSongs.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Log.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }

                        emit(Resource.Success<ArrayList<SongsResult>>(listSongs))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<SongsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getSearchDataVideo(query: String): Flow<Resource<ArrayList<VideosResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_VIDEO)
                    .onSuccess { result ->
                        val listSongs: ArrayList<VideosResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchVideo(result).let { list ->
                            listSongs.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchVideo(values).let { list ->
                                        listSongs.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Log.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }

                        emit(Resource.Success<ArrayList<VideosResult>>(listSongs))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<VideosResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getSearchDataPodcast(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_PODCAST)
                    .onSuccess { result ->
                        println(query)
                        val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        Log.w("Podcast", "result: $result")
                        parsePodcast(result.listPodcast).let { list ->
                            listPlaylist.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parsePodcast(values.listPodcast).let { list ->
                                        listPlaylist.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Log.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getSearchDataFeaturedPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    .onSuccess { result ->
                        val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchPlaylist(result).let { list ->
                            listPlaylist.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchPlaylist(values).let { list ->
                                        listPlaylist.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Log.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getSearchDataArtist(query: String): Flow<Resource<ArrayList<ArtistsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_ARTIST)
                    .onSuccess { result ->
                        val listArtist: ArrayList<ArtistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchArtist(result).let { list ->
                            listArtist.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchArtist(values).let { list ->
                                        listArtist.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Log.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<ArtistsResult>>(listArtist))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<ArtistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getSearchDataAlbum(query: String): Flow<Resource<ArrayList<AlbumsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_ALBUM)
                    .onSuccess { result ->
                        val listAlbum: ArrayList<AlbumsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchAlbum(result).let { list ->
                            listAlbum.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchAlbum(values).let { list ->
                                        listAlbum.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Log.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<AlbumsResult>>(listAlbum))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<AlbumsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getSearchDataPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                    .onSuccess { result ->
                        val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        parseSearchPlaylist(result).let { list ->
                            listPlaylist.addAll(list)
                        }
                        var count = 0
                        while (count < 2 && countinueParam != null) {
                            youTube
                                .searchContinuation(countinueParam)
                                .onSuccess { values ->
                                    parseSearchPlaylist(values).let { list ->
                                        listPlaylist.addAll(list)
                                    }
                                    count++
                                    countinueParam = values.continuation
                                }.onFailure {
                                    Log.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                    }.onFailure { e ->
                        Log.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getSuggestQuery(query: String): Flow<Resource<SearchSuggestions>> =
        flow {
            runCatching {
//            youTube.getSuggestQuery(query).onSuccess {
//                emit(Resource.Success<ArrayList<String>>(it))
//            }.onFailure { e ->
//                Log.d("Suggest", "Error: ${e.message}")
//                emit(Resource.Error<ArrayList<String>>(e.message.toString()))
//            }
                youTube
                    .getYTMusicSearchSuggestions(query)
                    .onSuccess {
                        emit(Resource.Success(it))
                    }.onFailure { e ->
                        Log.d("Suggest", "Error: ${e.message}")
                        emit(Resource.Error<SearchSuggestions>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getRadioArtist(endpoint: WatchEndpoint): Flow<Resource<Pair<List<Track>, String?>>> =
        flow {
            runCatching {
                youTube
                    .next(endpoint)
                    .onSuccess { next ->
                        emit(Resource.Success(Pair(next.items.toListTrack(), next.continuation)))
                    }.onFailure {
                        it.printStackTrace()
                        emit(Resource.Error(it.message ?: it.localizedMessage ?: "Error"))
                    }
            }
        }

    suspend fun getRelatedData(videoId: String): Flow<Resource<Pair<ArrayList<Track>, String?>>> =
        flow {
            runCatching {
                youTube
                    .next(WatchEndpoint(videoId = videoId))
                    .onSuccess { next ->
                        val data: ArrayList<SongItem> = arrayListOf()
                        data.addAll(next.items.filter { it.id != videoId }.toSet())
                        val nextContinuation = next.continuation
//                            if (nextContinuation != null) {
//                                Log.w("Queue", "nextContinuation: $nextContinuation")
//                                Queue.setContinuation("RDAMVM$videoId", nextContinuation)
//                            } else {
//                                Log.w("Related", "nextContinuation: null")
//                                Queue.setContinuation("RDAMVM$videoId", null)
//                            }
                        emit(Resource.Success<Pair<ArrayList<Track>, String?>>(Pair(data.toListTrack(), nextContinuation)))
                    }.onFailure { exception ->
                        exception.printStackTrace()
                        emit(Resource.Error<Pair<ArrayList<Track>, String?>>(exception.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getYouTubeCaption(videoId: String): Flow<Resource<Lyrics>> =
        flow {
            runCatching {
                youTube
                    .getYouTubeCaption(videoId)
                    .onSuccess { lyrics ->
                        Log.w("Lyrics", "lyrics: ${lyrics.toLyrics()}")
                        emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
                    }.onFailure { e ->
                        Log.d("Lyrics", "Error: ${e.message}")
                        emit(Resource.Error<Lyrics>(e.message.toString()))
                    }
            }
        }

    suspend fun getCanvas(
        videoId: String,
        duration: Int,
    ): Flow<com.maxrave.spotify.model.response.spotify.CanvasResponse?> =
        flow {
            runCatching {
                getSongById(videoId).first().let { song ->
                    val q =
                        "${song?.title} ${song?.artistName?.firstOrNull() ?: ""}"
                            .replace(
                                Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                                " ",
                            ).replace(
                                Regex("( và | & | и | e | und |, |和| dan)"),
                                " ",
                            ).replace("  ", " ")
                            .replace(Regex("([()])"), "")
                            .replace(".", " ")
                            .replace("  ", " ")
                    var spotifyPersonalToken = ""
                    var spotifyClientToken = ""
                    if (dataStoreManager.spotifyPersonalToken
                            .first()
                            .isNotEmpty() &&
                        dataStoreManager.spotifyClientToken.first().isNotEmpty() &&
                        dataStoreManager.spotifyPersonalTokenExpires.first() > System.currentTimeMillis() &&
                        dataStoreManager.spotifyPersonalTokenExpires.first() != 0L &&
                        dataStoreManager.spotifyClientTokenExpires.first() > System.currentTimeMillis() &&
                        dataStoreManager.spotifyClientTokenExpires.first() != 0L
                    ) {
                        spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                        spotifyClientToken = dataStoreManager.spotifyClientToken.first()
                        Log.d("Canvas", "spotifyPersonalToken: $spotifyPersonalToken")
                        Log.d("Canvas", "spotifyClientToken: $spotifyClientToken")
                    } else if (dataStoreManager.spdc.first().isNotEmpty()) {
                        spotify
                            .getClientToken()
                            .onSuccess {
                                Log.d("Canvas", "clientToken: ${it.grantedToken.token}")
                                dataStoreManager.setSpotifyClientTokenExpires(
                                    (it.grantedToken.expiresAfterSeconds * 1000L),
                                )
                                dataStoreManager.setSpotifyClientToken(it.grantedToken.token)
                                spotifyClientToken = it.grantedToken.token
                            }.onFailure {
                                it.printStackTrace()
                                emit(null)
                            }
                        spotify
                            .getPersonalTokenWithTotp(dataStoreManager.spdc.first())
                            .onSuccess {
                                spotifyPersonalToken = it.accessToken
                                dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                                dataStoreManager.setSpotifyPersonalTokenExpires(
                                    it.accessTokenExpirationTimestampMs,
                                )
                                Log.d("Canvas", "spotifyPersonalToken: $spotifyPersonalToken")
                            }.onFailure {
                                it.printStackTrace()
                                emit(null)
                            }
                    }
                    if (spotifyPersonalToken.isNotEmpty() && spotifyClientToken.isNotEmpty()) {
                        val authToken = spotifyPersonalToken
                        spotify
                            .searchSpotifyTrack(q, authToken, spotifyClientToken)
                            .onSuccess { searchResponse ->
                                Log.w("Canvas", "searchSpotifyResponse: $searchResponse")
                                val track =
                                    if (duration != 0) {
                                        searchResponse.data?.searchV2?.tracksV2?.items?.find {
                                            abs(
                                                (
                                                    (
                                                        (
                                                            it.item
                                                                ?.data
                                                                ?.duration
                                                                ?.totalMilliseconds ?: (0 / 1000)
                                                        ) - duration
                                                    )
                                                ),
                                            ) < 1
                                        }
                                            ?: searchResponse.data
                                                ?.searchV2
                                                ?.tracksV2
                                                ?.items
                                                ?.firstOrNull()
                                    } else {
                                        searchResponse.data
                                            ?.searchV2
                                            ?.tracksV2
                                            ?.items
                                            ?.firstOrNull()
                                    }
                                if (track != null) {
                                    Log.w("Canvas", "track: $track")
                                    spotify
                                        .getSpotifyCanvas(
                                            track.item?.data?.id ?: "",
                                            spotifyPersonalToken,
                                            spotifyClientToken,
                                        ).onSuccess {
                                            Log.w("Canvas", "canvas: $it")
                                            emit(it)
                                        }.onFailure {
                                            it.printStackTrace()
                                            emit(null)
                                        }
                                } else {
                                    emit(null)
                                }
                            }.onFailure { throwable ->
                                throwable.printStackTrace()
                                emit(null)
                            }
                    } else {
                        emit(null)
                    }
                }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getSpotifyLyrics(
        query: String,
        duration: Int?,
    ): Flow<Resource<Lyrics>> =
        flow {
            runCatching {
                val q =
                    query
                        .replace(
                            Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                            " ",
                        ).replace(
                            Regex("( và | & | и | e | und |, |和| dan)"),
                            " ",
                        ).replace("  ", " ")
                        .replace(Regex("([()])"), "")
                        .replace(".", " ")
                        .replace("  ", " ")
                Log.d("Lyrics", "query: $q")
                var spotifyPersonalToken = ""
                var spotifyClientToken = ""
                if (dataStoreManager.spotifyPersonalToken
                        .first()
                        .isNotEmpty() &&
                    dataStoreManager.spotifyPersonalTokenExpires.first() > System.currentTimeMillis() &&
                    dataStoreManager.spotifyPersonalTokenExpires.first() != 0L &&
                    dataStoreManager.spotifyClientTokenExpires.first() > System.currentTimeMillis() &&
                    dataStoreManager.spotifyClientTokenExpires.first() != 0L
                ) {
                    spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                    spotifyClientToken = dataStoreManager.spotifyClientToken.first()
                    Log.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
                    Log.d("Lyrics", "spotifyClientToken: $spotifyClientToken")
                } else if (dataStoreManager.spdc.first().isNotEmpty()) {
                    runBlocking {
                        spotify
                            .getClientToken()
                            .onSuccess {
                                Log.d("Canvas", "clientToken: ${it.grantedToken.token}")
                                dataStoreManager.setSpotifyClientTokenExpires(
                                    (it.grantedToken.expiresAfterSeconds * 1000L),
                                )
                                dataStoreManager.setSpotifyClientToken(it.grantedToken.token)
                                spotifyClientToken = it.grantedToken.token
                            }.onFailure {
                                it.printStackTrace()
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                    }
                    runBlocking {
                        spotify
                            .getPersonalTokenWithTotp(dataStoreManager.spdc.first())
                            .onSuccess {
                                spotifyPersonalToken = it.accessToken
                                dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                                dataStoreManager.setSpotifyPersonalTokenExpires(
                                    it.accessTokenExpirationTimestampMs,
                                )
                                Log.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
                            }.onFailure {
                                it.printStackTrace()
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                    }
                }
                if (spotifyPersonalToken.isNotEmpty() && spotifyClientToken.isNotEmpty()) {
                    val authToken = spotifyPersonalToken
                    Log.d("Lyrics", "authToken: $authToken")
                    spotify
                        .searchSpotifyTrack(q, authToken, spotifyClientToken)
                        .onSuccess { searchResponse ->
                            val track =
                                if (duration != 0 && duration != null) {
                                    searchResponse.data?.searchV2?.tracksV2?.items?.find {
                                        abs(
                                            (
                                                (
                                                    (
                                                        it.item
                                                            ?.data
                                                            ?.duration
                                                            ?.totalMilliseconds ?: (0 / 1000)
                                                    ) - duration
                                                )
                                            ),
                                        ) < 1
                                    }
                                        ?: searchResponse.data
                                            ?.searchV2
                                            ?.tracksV2
                                            ?.items
                                            ?.firstOrNull()
                                } else {
                                    searchResponse.data
                                        ?.searchV2
                                        ?.tracksV2
                                        ?.items
                                        ?.firstOrNull()
                                }
                            Log.d("Lyrics", "track: $track")
                            if (track != null) {
                                spotify
                                    .getSpotifyLyrics(track.item?.data?.id ?: "", spotifyPersonalToken, spotifyClientToken)
                                    .onSuccess {
                                        emit(Resource.Success<Lyrics>(it.toLyrics()))
                                    }.onFailure {
                                        it.printStackTrace()
                                        emit(Resource.Error<Lyrics>("Not found"))
                                    }
                            } else {
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                        }.onFailure { throwable ->
                            throwable.printStackTrace()
                            emit(Resource.Error<Lyrics>("Not found"))
                        }
                }
            }
        }

    fun getLrclibLyricsData(
        sartist: String,
        strack: String,
        duration: Int? = null,
    ): Flow<Resource<Lyrics>> =
        flow {
            val qartist =
                sartist
                    .replace(
                        Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                        " ",
                    ).replace(
                        Regex("( và | & | и | e | und |, |和| dan)"),
                        " ",
                    ).replace("  ", " ")
                    .replace(Regex("([()])"), "")
                    .replace(".", " ")
            val qtrack =
                strack
                    .replace(
                        Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                        " ",
                    ).replace(
                        Regex("( và | & | и | e | und |, |和| dan)"),
                        " ",
                    ).replace("  ", " ")
                    .replace(Regex("([()])"), "")
                    .replace(".", " ")
            lyricsClient
                .getLrclibLyrics(qtrack, qartist, duration)
                .onSuccess {
                    it?.let { emit(Resource.Success<Lyrics>(it.toLyrics())) }
                }.onFailure {
                    it.printStackTrace()
                    emit(Resource.Error<Lyrics>("Not found"))
                }
        }.flowOn(Dispatchers.IO)

    fun getLyricsData(
        sartist: String,
        strack: String,
        durationInt: Int? = null,
    ): Flow<Pair<String, Resource<Lyrics>>> =
        flow {
            runCatching {
                val tag = "Lyrics"
//            val q = query.replace(Regex("\\([^)]*?(feat.|ft.|cùng với|con)[^)]*?\\)"), "")
//                .replace("  ", " ")
                val qartist =
                    sartist
                        .replace(
                            Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                            " ",
                        ).replace(
                            Regex("( và | & | и | e | und |, |和| dan)"),
                            " ",
                        ).replace("  ", " ")
                        .replace(Regex("([()])"), "")
                        .replace(".", " ")
                val qtrack =
                    strack
                        .replace(
                            Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                            " ",
                        ).replace(
                            Regex("( và | & | и | e | und |, |和| dan)"),
                            " ",
                        ).replace("  ", " ")
                        .replace(Regex("([()])"), "")
                        .replace(".", " ")
                val q = "$qtrack $qartist"
                Log.d(tag, "query: $q")
                var musixMatchUserToken = lyricsClient.musixmatchUserToken
                if (musixMatchUserToken == null) {
                    lyricsClient
                        .getMusixmatchUserToken()
                        .onSuccess { usertoken ->
                            lyricsClient.musixmatchUserToken = usertoken.message.body.user_token
                            Log.d(tag, "musixMatchUserToken: ${usertoken.message.body.user_token}")
                            musixMatchUserToken = usertoken.message.body.user_token
                        }.onFailure { throwable ->
                            Log.e(tag, throwable.message.toString())
                            emit(Pair("", Resource.Error<Lyrics>("Not found")))
                        }
                }
                lyricsClient
                    .searchMusixmatchTrackId(q, musixMatchUserToken!!)
                    .onSuccess { searchResult ->
                        Log.d(
                            tag,
                            "searchResult: ${
                                searchResult.message.body.track_list?.map {
                                    it.track.track_name + " " + it.track.artist_name
                                }
                            }",
                        )
                        val trackList =
                            if (!searchResult.message.body.track_list
                                    .isNullOrEmpty()
                            ) {
                                searchResult.message.body.track_list
                            } else {
                                searchResult.message.body.macro_result_list
                                    ?.track_list
                            }
                        if (!trackList.isNullOrEmpty()) {
                            Log.d(
                                tag,
                                "trackList: ${
                                    trackList.map {
                                        it.track.track_name + " " + it.track.artist_name
                                    }
                                }",
                            )
                            val list = arrayListOf<String>()
                            for (i in trackList) {
                                list.add(i.track.track_name + " " + i.track.artist_name)
                            }
                            var id = ""
                            var track: SearchMusixmatchResponse.Message.Body.Track.TrackX? = null
                            Log.d(tag, "duration: $durationInt")
                            val bestMatchingIndex = bestMatchingIndex(q, list)
                            if (bestMatchingIndex != null) {
                                if (durationInt != null && durationInt != 0) {
                                    val trackLengthList = arrayListOf<Int>()
                                    for (i in trackList) {
                                        trackLengthList.add(i.track.track_length)
                                    }
                                    val closestIndex =
                                        trackLengthList.minByOrNull {
                                            abs(
                                                it - durationInt,
                                            )
                                        }
                                    if (closestIndex != null &&
                                        abs(
                                            closestIndex - durationInt,
                                        ) < 2
                                    ) {
                                        id +=
                                            trackList
                                                .find {
                                                    it.track.track_length == closestIndex
                                                }?.track
                                                ?.track_id
                                                .toString()
                                        track =
                                            trackList.find { it.track.track_length == closestIndex }?.track
                                    }
                                    if (id == "" &&
                                        list.get(bestMatchingIndex).contains(
                                            trackList
                                                .get(
                                                    bestMatchingIndex,
                                                ).track.track_name,
                                        ) &&
                                        q.contains(
                                            trackList
                                                .get(
                                                    bestMatchingIndex,
                                                ).track.track_name,
                                        )
                                    ) {
                                        Log.w(
                                            "Lyrics",
                                            "item: ${
                                                trackList.get(
                                                    bestMatchingIndex,
                                                ).track.track_name
                                            }",
                                        )
                                        id +=
                                            trackList
                                                .get(
                                                    bestMatchingIndex,
                                                ).track.track_id
                                                .toString()
                                        track =
                                            trackList
                                                .get(
                                                    bestMatchingIndex,
                                                ).track
                                    }
                                } else if (list
                                        .get(bestMatchingIndex)
                                        .contains(
                                            trackList
                                                .get(
                                                    bestMatchingIndex,
                                                ).track.track_name,
                                        ) &&
                                    q.contains(
                                        trackList
                                            .get(
                                                bestMatchingIndex,
                                            ).track.track_name,
                                    )
                                ) {
                                    Log.w(
                                        "Lyrics",
                                        "item: ${
                                            trackList.get(
                                                bestMatchingIndex,
                                            ).track.track_name
                                        }",
                                    )
                                    id +=
                                        trackList
                                            .get(
                                                bestMatchingIndex,
                                            ).track.track_id
                                            .toString()
                                    track =
                                        trackList
                                            .get(
                                                bestMatchingIndex,
                                            ).track
                                }
                            }
                            Log.d(tag, "id: $id")
                            Log.w(
                                tag,
                                "item lyrics $track",
                            )
                            if (id != "" && track != null) {
                                delay(1000)
                                lyricsClient
                                    .getMusixmatchLyricsByQ(track, musixMatchUserToken!!)
                                    .onSuccess {
                                        if (it != null) {
                                            emit(
                                                Pair(
                                                    id,
                                                    Resource.Success<Lyrics>(it.toLyrics()),
                                                ),
                                            )
                                        } else {
                                            Log.w("Lyrics", "Error: Lỗi getLyrics $it")
                                            emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                        }
                                    }.onFailure { throwable ->
                                        throwable.printStackTrace()
                                        lyricsClient
                                            .getLrclibLyrics(qtrack, qartist, durationInt)
                                            .onSuccess {
                                                it?.let { emit(Pair(id, Resource.Success<Lyrics>(it.toLyrics()))) }
                                            }.onFailure {
                                                it.printStackTrace()
                                                emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                            }
                                    }
                            } else {
                                lyricsClient
                                    .fixSearchMusixmatch(
                                        q_artist = qartist,
                                        q_track = qtrack,
                                        q_duration = (durationInt ?: 0).toString(),
                                        userToken = musixMatchUserToken!!,
                                    ).onSuccess {
                                        val trackX = it.message.body.track
                                        Log.w(tag, "Fix Search Musixmatch: $trackX")
                                        if (trackX != null && (abs(trackX.track_length - (durationInt ?: 0)) <= 10)) {
                                            delay(1000)
                                            lyricsClient
                                                .getMusixmatchLyricsByQ(trackX, musixMatchUserToken!!)
                                                .onSuccess {
                                                    Log.w(tag, "Item lyrics ${it?.lyrics?.syncType}")
                                                    if (it != null) {
                                                        emit(
                                                            Pair(
                                                                trackX.track_id.toString(),
                                                                Resource.Success<Lyrics>(it.toLyrics()),
                                                            ),
                                                        )
                                                    } else {
                                                        Log.w("Lyrics", "Error: Lỗi getLyrics $it")
                                                        lyricsClient.getLrclibLyrics(qtrack, qartist, durationInt)
                                                        emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                                    }
                                                }.onFailure {
                                                    it.printStackTrace()
                                                    emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                                }
                                        } else {
                                            lyricsClient
                                                .getLrclibLyrics(qtrack, qartist, durationInt)
                                                .onSuccess {
                                                    it?.let { emit(Pair(trackX?.track_id.toString(), Resource.Success<Lyrics>(it.toLyrics()))) }
                                                }.onFailure {
                                                    it.printStackTrace()
                                                    emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                                }
                                        }
                                    }.onFailure {
                                        Log.e(tag, "Fix musixmatch search" + it.message.toString())
                                        lyricsClient
                                            .getLrclibLyrics(qtrack, qartist, durationInt)
                                            .onSuccess {
                                                Log.w(tag, "Liblrc Item lyrics ${it?.lyrics?.syncType}")
                                                it?.let { emit(Pair(id, Resource.Success<Lyrics>(it.toLyrics()))) }
                                            }.onFailure {
                                                Log.e(tag, "Liblrc Error: ${it.message}")
                                                emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                                            }
                                    }
                            }
                        } else {
                            emit(Pair("", Resource.Error<Lyrics>("Not found")))
                        }
                    }.onFailure { throwable ->
                        throwable.printStackTrace()
                        emit(Pair("", Resource.Error<Lyrics>("Not found")))
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getTranslateLyrics(id: String): Flow<MusixmatchTranslationLyricsResponse?> =
        flow {
            runCatching {
                lyricsClient.musixmatchUserToken?.let {
                    lyricsClient
                        .getMusixmatchTranslateLyrics(
                            id,
                            it,
                            dataStoreManager.translationLanguage.first(),
                        ).onSuccess { lyrics ->
                            Log.w("Translate Lyrics", "lyrics: $lyrics")
                            emit(lyrics)
                        }.onFailure {
                            it.printStackTrace()
                            emit(null)
                        }
                }
            }
        }.flowOn(Dispatchers.IO)

    @UnstableApi
    suspend fun getSongInfo(videoId: String): Flow<SongInfoEntity?> =
        flow {
            runCatching {
                val id =
                    if (videoId.contains(MergingMediaSourceFactory.isVideo)) {
                        videoId.removePrefix(MergingMediaSourceFactory.isVideo)
                    } else {
                        videoId
                    }
                youTube
                    .getSongInfo(id)
                    .onSuccess { songInfo ->
                        val song =
                            SongInfoEntity(
                                videoId = songInfo.videoId,
                                author = songInfo.author,
                                authorId = songInfo.authorId,
                                authorThumbnail = songInfo.authorThumbnail,
                                description = songInfo.description,
                                uploadDate = songInfo.uploadDate,
                                subscribers = songInfo.subscribers,
                                viewCount = songInfo.viewCount,
                                like = songInfo.like,
                                dislike = songInfo.dislike,
                            )
                        emit(song)
                        insertSongInfo(
                            song,
                        )
                    }.onFailure {
                        it.printStackTrace()
                        emit(getSongInfoEntity(videoId).firstOrNull())
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun getLikeStatus(videoId: String): Flow<Boolean> =
        flow {
            runCatching {
                youTube
                    .getLikedInfo(videoId)
                    .onSuccess {
                        if (it == LikeStatus.LIKE) emit(true) else emit(false)
                    }.onFailure {
                        it.printStackTrace()
                        emit(false)
                    }
            }
        }

    @UnstableApi
    suspend fun updateFormat(videoId: String) {
        localDataSource.getNewFormat(videoId)?.let { oldFormat ->
            Log.w("Stream", "oldFormatExpired: ${oldFormat.expiredTime}")
            Log.w("Stream", "now: ${LocalDateTime.now()}")
            Log.w("Stream", "isExpired: ${oldFormat.expiredTime.isBefore(LocalDateTime.now())}")
            if (oldFormat.expiredTime.isBefore(LocalDateTime.now())) {
                youTube
                    .player(videoId)
                    .onSuccess { triple ->
                        val response = triple.second
                        localDataSource.updateNewFormat(
                            oldFormat.copy(
                                expiredTime = LocalDateTime.now().plusSeconds(response.streamingData?.expiresInSeconds?.toLong() ?: 0L),
                                playbackTrackingVideostatsPlaybackUrl =
                                    response.playbackTracking?.videostatsPlaybackUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                playbackTrackingAtrUrl =
                                    response.playbackTracking?.atrUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                playbackTrackingVideostatsWatchtimeUrl =
                                    response.playbackTracking?.videostatsWatchtimeUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                cpn = triple.first,
                            ),
                        )
                        Log.w("UpdateFormat", "Updated format for $videoId")
                    }.onFailure { throwable ->
                        Log.e("UpdateFormat", "Error: ${throwable.message}")
                    }
            }
        }
    }

    @UnstableApi
    suspend fun getStream(
        videoId: String,
        isVideo: Boolean,
    ): Flow<String?> =
        flow {
            // 134, 136, 137
            youTube
                .player(videoId)
                .onSuccess { data ->
                    val itag = QUALITY.itags.getOrNull(QUALITY.items.indexOf(dataStoreManager.quality.first()))
                    val videoItag =
                        VIDEO_QUALITY.itags.getOrNull(
                            VIDEO_QUALITY.items.indexOf(dataStoreManager.videoQuality.first()),
                        )
                            ?: 134
                    val response = data.second
                    if (data.third == MediaType.Song) {
                        Log.w(
                            "Stream",
                            "response: is SONG",
                        )
                    } else {
                        Log.w("Stream", "response: is VIDEO")
                    }
                    Log.w(
                        "Stream",
                        response.streamingData
                            ?.formats
                            ?.map { it.itag }
                            .toString() + " " +
                            response.streamingData
                                ?.adaptiveFormats
                                ?.map { it.itag }
                                .toString(),
                    )

                    Log.w("Stream", "Get stream for video $isVideo")
                    var format =
                        if (isVideo) {
                            response.streamingData?.formats?.find { it.itag == videoItag }
                                ?: response.streamingData?.adaptiveFormats?.find { it.itag == videoItag }
                                ?: response.streamingData?.formats?.find { it.itag == 136 }
                                ?: response.streamingData?.adaptiveFormats?.find { it.itag == 136 }
                                ?: response.streamingData?.formats?.find { it.itag == 134 }
                                ?: response.streamingData?.adaptiveFormats?.find { it.itag == 134 }
                        } else {
                            if (response.streamingData?.adaptiveFormats?.find { it.itag == 141 } != null) {
                                response.streamingData?.adaptiveFormats?.find { it.itag == 141 }
                            } else {
                                response.streamingData?.adaptiveFormats?.find { it.itag == itag }
                            }
                        }
                    if (format == null) {
                        format = response.streamingData?.adaptiveFormats?.lastOrNull() ?: response.streamingData?.formats?.lastOrNull()
                    }
                    val superFormat =
                        response.streamingData?.adaptiveFormats?.find {
                            it.quality == "AUDIO_QUALITY_HIGH"
                        }
                    Log.w("Stream", "Super format: $superFormat")
                    Log.w("Stream", "format: $format")
                    Log.d("Stream", "expireInSeconds ${response.streamingData?.expiresInSeconds}")
                    Log.w("Stream", "expired at ${LocalDateTime.now().plusSeconds(response.streamingData?.expiresInSeconds?.toLong() ?: 0L)}")
                    runBlocking {
                        insertNewFormat(
                            NewFormatEntity(
                                videoId = if (VIDEO_QUALITY.itags.contains(format?.itag)) "${MergingMediaSourceFactory.isVideo}$videoId" else videoId,
                                itag = format?.itag ?: itag ?: 141,
                                mimeType =
                                    Regex("""([^;]+);\s*codecs=["']([^"']+)["']""")
                                        .find(
                                            format?.mimeType ?: "",
                                        )?.groupValues
                                        ?.getOrNull(1) ?: format?.mimeType ?: "",
                                codecs =
                                    Regex("""([^;]+);\s*codecs=["']([^"']+)["']""")
                                        .find(
                                            format?.mimeType ?: "",
                                        )?.groupValues
                                        ?.getOrNull(2) ?: format?.mimeType ?: "",
                                bitrate = format?.bitrate,
                                sampleRate = format?.audioSampleRate,
                                contentLength = format?.contentLength,
                                loudnessDb =
                                    response.playerConfig
                                        ?.audioConfig
                                        ?.loudnessDb
                                        ?.toFloat(),
                                lengthSeconds = response.videoDetails?.lengthSeconds?.toInt(),
                                playbackTrackingVideostatsPlaybackUrl =
                                    response.playbackTracking?.videostatsPlaybackUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                playbackTrackingAtrUrl =
                                    response.playbackTracking?.atrUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                playbackTrackingVideostatsWatchtimeUrl =
                                    response.playbackTracking?.videostatsWatchtimeUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                cpn = data.first,
                                expiredTime = LocalDateTime.now().plusSeconds(response.streamingData?.expiresInSeconds?.toLong() ?: 0L),
                            ),
                        )
                    }
                    if (data.first != null) {
                        emit(format?.url?.plus("&cpn=${data.first}&range=0-${format.contentLength ?: 10000000}"))
                    } else {
                        emit(format?.url?.plus("&range=0-${format.contentLength ?: 10000000}"))
                    }
//                insertFormat(
//                    FormatEntity(
//                        videoId = videoId,
//                        itag = format?.itag ?: itag,
//                        mimeType = format?.mimeType,
//                        bitrate = format?.bitrate?.toLong(),
//                        contentLength = format?.contentLength,
//                        lastModified = format?.lastModified,
//                        loudnessDb = response.playerConfig?.audioConfig?.loudnessDb?.toFloat(),
//                        uploader = response.videoDetails?.author?.replace(Regex(" - Topic| - Chủ đề|"), ""),
//                        uploaderId = response.videoDetails?.channelId,
//                        uploaderThumbnail = response.videoDetails?.authorAvatar,
//                        uploaderSubCount = response.videoDetails?.authorSubCount,
//                        description = response.videoDetails?.description,
//                        youtubeCaptionsUrl = response.captions?.playerCaptionsTracklistRenderer?.captionTracks?.get(
//                            0
//                        )?.baseUrl?.replace("&fmt=srv3", ""),
//                        lengthSeconds = response.videoDetails?.lengthSeconds?.toInt(),
//                    )
//                )
                }.onFailure {
                    it.printStackTrace()
                    Log.e("Stream", "Error: ${it.message}")
                    emit(null)
                }
        }.flowOn(Dispatchers.IO)

    suspend fun getLibraryPlaylist(): Flow<ArrayList<PlaylistsResult>?> =
        flow {
            youTube
                .getLibraryPlaylists()
                .onSuccess { data ->
                    val input =
                        data.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.get(
                                0,
                            )?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.get(
                                0,
                            )?.gridRenderer
                            ?.items
                            ?: null
                    if (input != null) {
                        Log.w("Library", "input: ${input.size}")
                        val list = parseLibraryPlaylist(input)
                        emit(list)
                    } else {
                        emit(null)
                    }
                }.onFailure { e ->
                    Log.e("Library", "Error: ${e.message}")
                    e.printStackTrace()
                    emit(null)
                }
        }.flowOn(Dispatchers.IO)

    suspend fun initPlayback(
        playback: String,
        atr: String,
        watchTime: String,
        cpn: String,
        playlistId: String?,
    ): Flow<Pair<Int, Float>> =
        flow {
            youTube
                .initPlayback(playback, atr, watchTime, cpn, playlistId)
                .onSuccess { response ->
                    emit(response)
                }.onFailure {
                    Log.e("InitPlayback", "Error: ${it.message}")
                    emit(Pair(0, 0f))
                }
        }.flowOn(Dispatchers.IO)

    suspend fun getSkipSegments(videoId: String): Flow<List<SkipSegments>?> =
        flow {
            youTube
                .getSkipSegments(videoId)
                .onSuccess {
                    emit(it)
                }.onFailure {
                    emit(null)
                }
        }.flowOn(Dispatchers.IO)

    fun getFullMetadata(videoId: String): Flow<YouTubeInitialPage?> =
        flow {
            Log.w("getFullMetadata", "videoId: $videoId")
            youTube
                .getFullMetadata(videoId)
                .onSuccess {
                    emit(it)
                }.onFailure {
                    Log.e("getFullMetadata", "Error: ${it.message}")
                    emit(null)
                }
        }.flowOn(Dispatchers.IO)

    fun checkForUpdate(): Flow<GithubResponse?> =
        flow {
            youTube
                .checkForUpdate()
                .onSuccess {
                    emit(it)
                }.onFailure {
                    emit(null)
                }
        }

    suspend fun getYouTubeSetVideoId(youtubePlaylistId: String): Flow<ArrayList<SetVideoIdEntity>?> =
        flow {
            runCatching {
                var id = ""
                if (!youtubePlaylistId.startsWith("VL")) {
                    id += "VL$youtubePlaylistId"
                } else {
                    id += youtubePlaylistId
                }
                Log.d("Repository", "playlist id: $id")
                youTube
                    .customQuery(browseId = id, setLogin = true)
                    .onSuccess { result ->
                        val listContent: ArrayList<SongItem> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? =
                            result.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.get(
                                    0,
                                )?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.get(
                                    0,
                                )?.musicPlaylistShelfRenderer
                                ?.contents
                        var continueParam =
                            result.getPlaylistContinuation()
                        var count = 0
                        Log.d("Repository", "playlist data: ${listContent.size}")
                        Log.d("Repository", "continueParam: $continueParam")
                        while (continueParam != null) {
                            youTube
                                .customQuery(
                                    browseId = "",
                                    continuation = continueParam,
                                    setLogin = true,
                                ).onSuccess { values ->
                                    Log.d("getPlaylistData", "continue: $continueParam")
                                    Log.d(
                                        "getPlaylistData",
                                        "values: ${values.onResponseReceivedActions}",
                                    )
                                    val dataMore: List<SongItem> =
                                        values.onResponseReceivedActions
                                            ?.firstOrNull()
                                            ?.appendContinuationItemsAction
                                            ?.continuationItems
                                            ?.apply {
                                                Log.w("getPlaylistData", "dataMore: ${this.size}")
                                            }?.mapNotNull {
                                                NextPage.fromMusicResponsiveListItemRenderer(
                                                    it.musicResponsiveListItemRenderer ?: return@mapNotNull null,
                                                )
                                            } ?: emptyList()
                                    listContent.addAll(dataMore)
                                    continueParam =
                                        values.getPlaylistContinuation()
                                    count++
                                }.onFailure {
                                    Log.e("Continue", "Error: ${it.message}")
                                    continueParam = null
                                    count++
                                }
                        }
                        Log.d("Repository", "playlist final data: ${listContent.size}")
                        parseSetVideoId(youtubePlaylistId, data ?: emptyList()).let { playlist ->
                            playlist.forEach { item ->
                                insertSetVideoId(item)
                            }
                            listContent.forEach { item ->
                                insertSetVideoId(
                                    SetVideoIdEntity(
                                        videoId = item.id,
                                        setVideoId = item.setVideoId,
                                        youtubePlaylistId = youtubePlaylistId,
                                    ),
                                )
                            }
                            emit(playlist)
                        }
                    }.onFailure { e ->
                        e.printStackTrace()
                        emit(null)
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun addYouTubePlaylistItem(
        youtubePlaylistId: String,
        videoId: String,
    ) = flow {
        runCatching {
            youTube
                .addPlaylistItem(youtubePlaylistId.verifyYouTubePlaylistId(), videoId)
                .onSuccess {
                    if (it.playlistEditResults.isNotEmpty()) {
                        for (playlistEditResult in it.playlistEditResults) {
                            insertSetVideoId(
                                SetVideoIdEntity(
                                    playlistEditResult.playlistEditVideoAddedResultData.videoId,
                                    playlistEditResult.playlistEditVideoAddedResultData.setVideoId,
                                ),
                            )
                        }
                        emit(it.status)
                    } else {
                        emit("FAILED")
                    }
                }.onFailure {
                    emit("FAILED")
                }
        }
    }

    suspend fun loginToMusixMatch(
        email: String,
        password: String,
    ): Flow<MusixmatchCredential?> =
        flow {
            runCatching {
                val userToken = lyricsClient.musixmatchUserToken
                if (!userToken.isNullOrEmpty()) {
                    lyricsClient
                        .postMusixmatchCredentials(
                            email,
                            password,
                            userToken,
                        ).onSuccess { response ->
                            emit(response)
                        }.onFailure {
                            it.printStackTrace()
                            emit(null)
                        }
                } else {
                    lyricsClient
                        .getMusixmatchUserToken()
                        .onSuccess { usertoken ->
                            lyricsClient.musixmatchUserToken = usertoken.message.body.user_token
                            val newUserToken = usertoken.message.body.user_token
                            delay(2000)
                            lyricsClient
                                .postMusixmatchCredentials(
                                    email,
                                    password,
                                    newUserToken,
                                ).onSuccess { response ->
                                    emit(response)
                                }.onFailure {
                                    it.printStackTrace()
                                    emit(null)
                                }
                        }.onFailure { throwable ->
                            throwable.printStackTrace()
                            emit(null)
                        }
                }
            }
        }

    suspend fun updateWatchTime(
        playbackTrackingVideostatsWatchtimeUrl: String,
        watchTimeList: ArrayList<Float>,
        cpn: String,
        playlistId: String?,
    ): Flow<Int> =
        flow {
            runCatching {
                youTube
                    .updateWatchTime(
                        playbackTrackingVideostatsWatchtimeUrl,
                        watchTimeList,
                        cpn,
                        playlistId,
                    ).onSuccess { response ->
                        emit(response)
                    }.onFailure {
                        it.printStackTrace()
                        emit(0)
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun updateWatchTimeFull(
        watchTime: String,
        cpn: String,
        playlistId: String?,
    ): Flow<Int> =
        flow {
            runCatching {
                youTube
                    .updateWatchTimeFull(watchTime, cpn, playlistId)
                    .onSuccess { response ->
                        emit(response)
                    }.onFailure {
                        it.printStackTrace()
                        emit(0)
                    }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun addToYouTubeLiked(mediaId: String?): Flow<Int> =
        flow {
            if (mediaId != null) {
                runCatching {
                    youTube
                        .addToLiked(mediaId)
                        .onSuccess {
                            Log.d("Liked", "Success: $it")
                            emit(it)
                        }.onFailure {
                            it.printStackTrace()
                            emit(0)
                        }
                }
            }
        }.flowOn(Dispatchers.IO)

    suspend fun removeFromYouTubeLiked(mediaId: String?): Flow<Int> =
        flow {
            if (mediaId != null) {
                runCatching {
                    youTube
                        .removeFromLiked(mediaId)
                        .onSuccess {
                            Log.d("Liked", "Success: $it")
                            emit(it)
                        }.onFailure {
                            it.printStackTrace()
                            emit(0)
                        }
                }
            }
        }.flowOn(Dispatchers.IO)

    fun downloadToFile(
        path: String,
        videoId: String,
        isVideo: Boolean,
    ): Flow<DownloadProgress> = youTube.download(path, videoId, isVideo)
}