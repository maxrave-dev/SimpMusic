package com.maxrave.simpmusic.data.repository

import android.content.Context
import android.util.Log
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.SearchSuggestions
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.simpmusic.GithubResponse
import com.maxrave.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import com.maxrave.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.LocalDataSource
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.FormatEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.LyricsEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.QueueEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SetVideoIdEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
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
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
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
import com.maxrave.simpmusic.data.parser.parsePlaylistData
import com.maxrave.simpmusic.data.parser.parseRelated
import com.maxrave.simpmusic.data.parser.parseSetVideoId
import com.maxrave.simpmusic.data.parser.search.parseSearchAlbum
import com.maxrave.simpmusic.data.parser.search.parseSearchArtist
import com.maxrave.simpmusic.data.parser.search.parseSearchPlaylist
import com.maxrave.simpmusic.data.parser.search.parseSearchSong
import com.maxrave.simpmusic.data.parser.search.parseSearchVideo
import com.maxrave.simpmusic.extension.bestMatchingIndex
import com.maxrave.simpmusic.extension.toListTrack
import com.maxrave.simpmusic.extension.toLyrics
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

//@ActivityRetainedScoped
@Singleton
class MainRepository @Inject constructor(private val localDataSource: LocalDataSource, private val dataStoreManager: DataStoreManager, @ApplicationContext private val context: Context) {
    //Database
    suspend fun getSearchHistory(): Flow<List<SearchHistory>> =
        flow { emit(localDataSource.getSearchHistory()) }.flowOn(Dispatchers.IO)

    suspend fun insertSearchHistory(searchHistory: SearchHistory) =
        withContext(Dispatchers.IO) { localDataSource.insertSearchHistory(searchHistory) }

    suspend fun deleteSearchHistory() =
        withContext(Dispatchers.IO) { localDataSource.deleteSearchHistory() }

    suspend fun getAllSongs(): Flow<List<SongEntity>> =
        flow { emit(localDataSource.getAllSongs()) }.flowOn(Dispatchers.IO)

    suspend fun getSongsByListVideoId(listVideoId: List<String>): Flow<List<SongEntity>> =
        flow { emit(localDataSource.getSongByListVideoId(listVideoId)) }.flowOn(Dispatchers.IO)

    suspend fun getDownloadedSongs(): Flow<List<SongEntity>> =
        flow { emit(localDataSource.getDownloadedSongs()) }.flowOn(Dispatchers.IO)

    suspend fun getDownloadingSongs(): Flow<List<SongEntity>> =
        flow { emit(localDataSource.getDownloadingSongs()) }.flowOn(Dispatchers.IO)
    suspend fun getPreparingSongs(): Flow<List<SongEntity>> =
        flow { emit(localDataSource.getPreparingSongs()) }.flowOn(Dispatchers.IO)

    suspend fun getLikedSongs(): Flow<List<SongEntity>> =
        flow { emit(localDataSource.getLikedSongs()) }.flowOn(Dispatchers.IO)

    suspend fun getLibrarySongs(): Flow<List<SongEntity>> =
        flow { emit(localDataSource.getLibrarySongs()) }.flowOn(Dispatchers.IO)

    suspend fun getSongById(id: String): Flow<SongEntity?> =
        flow { emit(localDataSource.getSong(id)) }.flowOn(Dispatchers.IO)

    suspend fun insertSong(songEntity: SongEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertSong(songEntity) }

    suspend fun updateListenCount(videoId: String) =
        withContext(Dispatchers.IO) { localDataSource.updateListenCount(videoId) }

    suspend fun updateLikeStatus(videoId: String, likeStatus: Int) =
        withContext(Dispatchers.Main) { localDataSource.updateLiked(likeStatus, videoId) }

    suspend fun updateSongInLibrary(inLibrary: LocalDateTime, videoId: String) =
        withContext(Dispatchers.Main) { localDataSource.updateSongInLibrary(inLibrary, videoId) }

    suspend fun getMostPlayedSongs(): Flow<List<SongEntity>> =
        flow { emit(localDataSource.getMostPlayedSongs()) }.flowOn(Dispatchers.IO)

    suspend fun updateDownloadState(videoId: String, downloadState: Int) =
        withContext(Dispatchers.Main) {
            localDataSource.updateDownloadState(
                downloadState,
                videoId
            )
        }

    suspend fun getAllArtists(): Flow<List<ArtistEntity>> =
        flow { emit(localDataSource.getAllArtists()) }.flowOn(Dispatchers.IO)

    suspend fun getArtistById(id: String): Flow<ArtistEntity> =
        flow { emit(localDataSource.getArtist(id)) }.flowOn(Dispatchers.IO)

    suspend fun insertArtist(artistEntity: ArtistEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertArtist(artistEntity) }

    suspend fun updateFollowedStatus(channelId: String, followedStatus: Int) =
        withContext(Dispatchers.Main) { localDataSource.updateFollowed(followedStatus, channelId) }

    suspend fun getFollowedArtists(): Flow<List<ArtistEntity>> =
        flow { emit(localDataSource.getFollowedArtists()) }.flowOn(Dispatchers.IO)

    suspend fun updateArtistInLibrary(inLibrary: LocalDateTime, channelId: String) =
        withContext(Dispatchers.Main) {
            localDataSource.updateArtistInLibrary(
                inLibrary,
                channelId
            )
        }

    suspend fun getAllAlbums(): Flow<List<AlbumEntity>> =
        flow { emit(localDataSource.getAllAlbums()) }.flowOn(Dispatchers.IO)

    suspend fun getAlbum(id: String): Flow<AlbumEntity> =
        flow { emit(localDataSource.getAlbum(id)) }.flowOn(Dispatchers.IO)

    suspend fun getLikedAlbums(): Flow<List<AlbumEntity>> =
        flow { emit(localDataSource.getLikedAlbums()) }.flowOn(Dispatchers.IO)

    suspend fun insertAlbum(albumEntity: AlbumEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertAlbum(albumEntity) }

    suspend fun updateAlbumLiked(albumId: String, likeStatus: Int) =
        withContext(Dispatchers.Main) { localDataSource.updateAlbumLiked(likeStatus, albumId) }

    suspend fun updateAlbumInLibrary(inLibrary: LocalDateTime, albumId: String) =
        withContext(Dispatchers.Main) { localDataSource.updateAlbumInLibrary(inLibrary, albumId) }

    suspend fun updateAlbumDownloadState(albumId: String, downloadState: Int) =
        withContext(Dispatchers.Main) {
            localDataSource.updateAlbumDownloadState(
                downloadState,
                albumId
            )
        }

    suspend fun getAllPlaylists(): Flow<List<PlaylistEntity>> =
        flow { emit(localDataSource.getAllPlaylists()) }.flowOn(Dispatchers.IO)

    suspend fun getPlaylist(id: String): Flow<PlaylistEntity?> =
        flow { emit(localDataSource.getPlaylist(id)) }.flowOn(Dispatchers.IO)

    suspend fun getLikedPlaylists(): Flow<List<PlaylistEntity>> =
        flow { emit(localDataSource.getLikedPlaylists()) }.flowOn(Dispatchers.IO)

    suspend fun insertPlaylist(playlistEntity: PlaylistEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertPlaylist(playlistEntity) }

    suspend fun updatePlaylistLiked(playlistId: String, likeStatus: Int) =
        withContext(Dispatchers.Main) {
            localDataSource.updatePlaylistLiked(
                likeStatus,
                playlistId
            )
        }

    suspend fun updatePlaylistInLibrary(inLibrary: LocalDateTime, playlistId: String) =
        withContext(Dispatchers.Main) {
            localDataSource.updatePlaylistInLibrary(
                inLibrary,
                playlistId
            )
        }

    suspend fun updatePlaylistDownloadState(playlistId: String, downloadState: Int) =
        withContext(Dispatchers.Main) {
            localDataSource.updatePlaylistDownloadState(
                downloadState,
                playlistId
            )
        }

    suspend fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
        flow { emit(localDataSource.getAllLocalPlaylists()) }.flowOn(Dispatchers.IO)

    suspend fun getLocalPlaylist(id: Long): Flow<LocalPlaylistEntity> =
        flow { emit(localDataSource.getLocalPlaylist(id)) }.flowOn(Dispatchers.IO)

    suspend fun insertLocalPlaylist(localPlaylistEntity: LocalPlaylistEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertLocalPlaylist(localPlaylistEntity) }

    suspend fun deleteLocalPlaylist(id: Long) =
        withContext(Dispatchers.IO) { localDataSource.deleteLocalPlaylist(id) }

    suspend fun updateLocalPlaylistTitle(title: String, id: Long) =
        withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistTitle(title, id) }

    suspend fun updateLocalPlaylistThumbnail(thumbnail: String, id: Long) =
        withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistThumbnail(thumbnail, id) }

    suspend fun updateLocalPlaylistTracks(tracks: List<String>, id: Long) =
        withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistTracks(tracks, id) }

    suspend fun updateLocalPlaylistDownloadState(downloadState: Int, id: Long) =
        withContext(Dispatchers.IO) {
            localDataSource.updateLocalPlaylistDownloadState(
                downloadState,
                id
            )
        }

    suspend fun updateLocalPlaylistInLibrary(inLibrary: LocalDateTime, id: Long) =
        withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistInLibrary(inLibrary, id) }

    suspend fun getDownloadedLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
        flow { emit(localDataSource.getDownloadedLocalPlaylists()) }.flowOn(Dispatchers.IO)

    suspend fun getAllRecentData(): Flow<List<Any>> =
        flow { emit(localDataSource.getAllRecentData()) }.flowOn(Dispatchers.IO)

    suspend fun getAllDownloadedPlaylist(): Flow<List<Any>> =
        flow { emit(localDataSource.getAllDownloadedPlaylist()) }.flowOn(Dispatchers.IO)

    suspend fun getRecentSong(limit: Int, offset: Int) =
        localDataSource.getRecentSongs(limit, offset)

    suspend fun getSavedLyrics(videoId: String): Flow<LyricsEntity?> =
        flow { emit(localDataSource.getSavedLyrics(videoId)) }.flowOn(Dispatchers.IO)

    suspend fun insertLyrics(lyricsEntity: LyricsEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertLyrics(lyricsEntity) }

    suspend fun insertFormat(format: FormatEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertFormat(format) }

    suspend fun getFormat(videoId: String): Flow<FormatEntity?> =
        flow { emit(localDataSource.getFormat(videoId)) }.flowOn(Dispatchers.IO)


    suspend fun recoverQueue(temp: List<Track>) {
        val queueEntity = QueueEntity(listTrack = temp)
        withContext(Dispatchers.IO) { localDataSource.recoverQueue(queueEntity) }
    }

    suspend fun removeQueue(){
        withContext(Dispatchers.IO) { localDataSource.deleteQueue() }
    }

    suspend fun getSavedQueue(): Flow<List<QueueEntity>?> =
        flow { emit(localDataSource.getQueue())  }.flowOn(Dispatchers.IO)

    suspend fun getLocalPlaylistByYoutubePlaylistId(playlistId: String): Flow<LocalPlaylistEntity?> =
        flow { emit(localDataSource.getLocalPlaylistByYoutubePlaylistId(playlistId)) }.flowOn(Dispatchers.IO)

    suspend fun insertSetVideoId(setVideoId: SetVideoIdEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertSetVideoId(setVideoId) }

    suspend fun getSetVideoId(videoId: String): Flow<SetVideoIdEntity?> =
        flow { emit(localDataSource.getSetVideoId(videoId)) }.flowOn(Dispatchers.IO)


    suspend fun updateLocalPlaylistYouTubePlaylistId(id: Long, ytId: String?) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistYouTubePlaylistId(id, ytId)
    }

    suspend fun updateLocalPlaylistYouTubePlaylistSynced(id: Long, synced: Int) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistYouTubePlaylistSynced(id, synced)
    }

    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(id: Long, syncState: Int) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)
    }

    suspend fun getHomeData(): Flow<Resource<ArrayList<HomeItem>>> = flow {
        runCatching {
            YouTube.customQuery(browseId = "FEmusic_home").onSuccess { result ->
                val list: ArrayList<HomeItem> = arrayListOf()
                if (result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.strapline?.runs?.get(0)?.text != null) {
                    val accountName = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.strapline?.runs?.get(0)?.text ?: ""
                    val accountThumbUrl = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.get(0)?.url?.replace("s88", "s352") ?: ""
                    if (accountName != "" && accountThumbUrl != "") {
                        dataStoreManager.putString("AccountName", accountName)
                        dataStoreManager.putString("AccountThumbUrl", accountThumbUrl)
                    }
                }
                var continueParam =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.continuations?.get(
                        0
                    )?.nextContinuationData?.continuation
                val data =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents
                list.addAll(parseMixedContent(data))
                var count = 0
                while (count < 5 && continueParam != null) {
                    YouTube.customQuery(browseId = "", continuation = continueParam).onSuccess { response ->
                        continueParam =
                            response.continuationContents?.sectionListContinuation?.continuations?.get(
                                0
                            )?.nextContinuationData?.continuation
                        Log.d("Repository", "continueParam: $continueParam")
                        val dataContinue =
                            response.continuationContents?.sectionListContinuation?.contents
                        list.addAll(parseMixedContent(dataContinue))
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

    suspend fun getChartData(countryCode: String = "KR"): Flow<Resource<Chart>> = flow {
        runCatching {
            YouTube.customQuery("FEmusic_charts", country = countryCode).onSuccess { result ->
                val data =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer
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

    suspend fun getMoodAndMomentsData(): Flow<Resource<Mood>> = flow {
        runCatching {
            YouTube.moodAndGenres().onSuccess { result ->
                val listMoodMoments: ArrayList<MoodsMoment> = arrayListOf()
                val listGenre: ArrayList<Genre> = arrayListOf()
                result[0].let { moodsmoment ->
                    for (item in moodsmoment.items) {
                        listMoodMoments.add(
                            MoodsMoment(
                                params = item.endpoint.params ?: "",
                                title = item.title
                            )
                        )
                    }
                }
                result[1].let { genres ->
                    for (item in genres.items) {
                        listGenre.add(
                            Genre(
                                params = item.endpoint.params ?: "",
                                title = item.title
                            )
                        )
                    }
                }
                emit(Resource.Success<Mood>(Mood(listGenre, listMoodMoments)))

            }.onFailure { e ->
                emit(Resource.Error<Mood>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getMoodData(params: String): Flow<Resource<MoodsMomentObject>> = flow {
        runCatching {
            YouTube.customQuery(browseId = "FEmusic_moods_and_genres_category", params = params)
                .onSuccess { result ->
                    val data = parseMoodsMomentObject(result)
                    if (data != null) {
                        emit(Resource.Success<MoodsMomentObject>(data))
                    } else {
                        emit(Resource.Error<MoodsMomentObject>("Error"))
                    }
                }
                .onFailure { e ->
                    emit(Resource.Error<MoodsMomentObject>(e.message.toString()))
                }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getGenreData(params: String): Flow<Resource<GenreObject>> = flow {
        kotlin.runCatching {
            YouTube.customQuery(browseId = "FEmusic_moods_and_genres_category", params = params)
                .onSuccess { result ->
                    val data = parseGenreObject(result)
                    if (data != null) {
                        emit(Resource.Success<GenreObject>(data))
                    } else {
                        emit(Resource.Error<GenreObject>("Error"))
                    }
                }
                .onFailure { e ->
                    emit(Resource.Error<GenreObject>(e.message.toString()))
                }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getRadio(radioId: String, title: String?, thumbnail: String?): Flow<Resource<PlaylistBrowse>> = flow {
        runCatching {
            YouTube.next(endpoint = WatchEndpoint(playlistId = radioId)).onSuccess { next ->
                val data: ArrayList<SongItem> = arrayListOf()
                data.addAll(next.items)
                var continuation = next.continuation
                Log.w("Radio", "data: ${data.size}")
                var count = 0
                while (continuation != null && count < 3) {
                    YouTube.next(endpoint = WatchEndpoint(playlistId = radioId), continuation = continuation).onSuccess { nextContinue ->
                        data.addAll(nextContinue.items)
                        continuation = nextContinue.continuation
                        if (data.size >= 50) {
                            continuation = null
                        }
                        Log.w("Radio", "data: ${data.size}")
                        count++
                    }.onFailure {
                        continuation = null
                        count++
                    }
                }
                Log.w("Repository", "data: ${data.size}")
                val playlistBrowse = PlaylistBrowse(
                    author = Author(id = "", name = "YouTube Music"),
                    description = context.getString(R.string.auto_created_by_youtube_music),
                    duration = "",
                    durationSeconds = 0,
                    id = radioId,
                    privacy = "PRIVATE",
                    thumbnails = listOf(Thumbnail(544, thumbnail ?: "", 544)),
                    title = title ?: "",
                    trackCount = data.size,
                    tracks = data.toListTrack(),
                    year = LocalDateTime.now().year.toString()
                )
                Log.w("Repository", "playlistBrowse: $playlistBrowse")
                emit(Resource.Success<PlaylistBrowse>(playlistBrowse))
            }
            .onFailure {
                exception ->
                exception.printStackTrace()
                emit(Resource.Error<PlaylistBrowse>(exception.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getPlaylistData(playlistId: String): Flow<Resource<PlaylistBrowse>> = flow {
        runCatching {
            var id = ""
            if (!playlistId.startsWith("VL")) {
                id += "VL$playlistId"
            }
            else {
                id += playlistId
            }
            Log.d("Repository", "playlist id: $id")
            YouTube.customQuery(browseId = id, setLogin = true).onSuccess { result ->
                val listContent: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                val data: List<MusicShelfRenderer.Content>? = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicPlaylistShelfRenderer?.contents
                if (data != null) {
                    Log.d("Data", "data: $data")
                    Log.d("Data", "data size: ${data.size}")
                    listContent.addAll(data)
                }
                val header = result.header?.musicDetailHeaderRenderer ?: result.header?.musicEditablePlaylistDetailHeaderRenderer
                Log.d("Header", "header: $header")
                var continueParam = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicPlaylistShelfRenderer?.continuations?.get(0)?.nextContinuationData?.continuation
                var count = 0
                Log.d("Repository", "playlist data: ${listContent.size}")
                Log.d("Repository", "continueParam: $continueParam")
                while (continueParam != null) {
                    YouTube.customQuery(browseId = "", continuation = continueParam, setLogin = true).onSuccess { values ->
                        Log.d("Continue", "continue: $continueParam")
                        val dataMore: List<MusicShelfRenderer.Content>? = values.continuationContents?.musicPlaylistShelfContinuation?.contents
                        if (dataMore != null) {
                            listContent.addAll(dataMore)
                        }
                        continueParam = values.continuationContents?.musicPlaylistShelfContinuation?.continuations?.get(0)?.nextContinuationData?.continuation
                        count++
                    }.onFailure {
                        Log.e("Continue", "Error: ${it.message}")
                        continueParam = null
                        count++
                    }
                }
                Log.d("Repository", "playlist final data: ${listContent.size}")
                parsePlaylistData(header, listContent, playlistId)?.let { playlist ->
                    emit(Resource.Success<PlaylistBrowse>(playlist))
                } ?: emit(Resource.Error<PlaylistBrowse>("Error"))
            }.onFailure { e ->
                emit(Resource.Error<PlaylistBrowse>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getAlbumData(browseId: String): Flow<Resource<AlbumBrowse>> = flow {
        runCatching {
            YouTube.album(browseId, withSongs = true).onSuccess { result ->
                emit(Resource.Success<AlbumBrowse>(parseAlbumData(result)))
            }.onFailure { e ->
                Log.d("Album", "Error: ${e.message}")
                emit(Resource.Error<AlbumBrowse>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getArtistData(channelId: String): Flow<Resource<ArtistBrowse>> = flow {
        runCatching {
            YouTube.artist(channelId).onSuccess { result ->
                emit(Resource.Success<ArtistBrowse>(parseArtistData(result, context)))
            }.onFailure { e ->
                Log.d("Artist", "Error: ${e.message}")
                emit(Resource.Error<ArtistBrowse>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getSearchDataSong(query: String): Flow<Resource<ArrayList<SongsResult>>> = flow {
        runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).onSuccess { result ->
                val listSongs: ArrayList<SongsResult> = arrayListOf()
                var countinueParam = result.continuation
                parseSearchSong(result).let { list ->
                    listSongs.addAll(list)
                }
                var count = 0
                while (count < 2 && countinueParam != null) {
                    YouTube.searchContinuation(countinueParam).onSuccess { values ->
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
    suspend fun getSearchDataVideo(query: String): Flow<Resource<ArrayList<VideosResult>>> = flow {
        runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).onSuccess { result ->
                val listSongs: ArrayList<VideosResult> = arrayListOf()
                var countinueParam = result.continuation
                parseSearchVideo(result).let { list ->
                    listSongs.addAll(list)
                }
                var count = 0
                while (count < 2 && countinueParam != null) {
                    YouTube.searchContinuation(countinueParam).onSuccess { values ->
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
    suspend fun getSearchDataArtist(query: String): Flow<Resource<ArrayList<ArtistsResult>>> = flow {
        runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).onSuccess { result ->
                val listArtist: ArrayList<ArtistsResult> = arrayListOf()
                var countinueParam = result.continuation
                parseSearchArtist(result).let { list ->
                    listArtist.addAll(list)
                }
                var count = 0
                while (count < 2 && countinueParam != null) {
                    YouTube.searchContinuation(countinueParam).onSuccess { values ->
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
    suspend fun getSearchDataAlbum(query: String): Flow<Resource<ArrayList<AlbumsResult>>> = flow {
        runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).onSuccess { result ->
                val listAlbum: ArrayList<AlbumsResult> = arrayListOf()
                var countinueParam = result.continuation
                parseSearchAlbum(result).let { list ->
                    listAlbum.addAll(list)
                }
                var count = 0
                while (count < 2 && countinueParam != null) {
                    YouTube.searchContinuation(countinueParam).onSuccess { values ->
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
    suspend fun getSearchDataPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> = flow {
        runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST).onSuccess { result ->
                val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                var countinueParam = result.continuation
                parseSearchPlaylist(result).let { list ->
                    listPlaylist.addAll(list)
                }
                var count = 0
                while (count < 2 && countinueParam != null) {
                    YouTube.searchContinuation(countinueParam).onSuccess { values ->
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
    suspend fun getSuggestQuery(query: String): Flow<Resource<SearchSuggestions>> = flow {
        runCatching {
//            YouTube.getSuggestQuery(query).onSuccess {
//                emit(Resource.Success<ArrayList<String>>(it))
//            }.onFailure { e ->
//                Log.d("Suggest", "Error: ${e.message}")
//                emit(Resource.Error<ArrayList<String>>(e.message.toString()))
//            }
            YouTube.getYTMusicSearchSuggestions(query).onSuccess {
                emit(Resource.Success(it))
            }.onFailure { e ->
                Log.d("Suggest", "Error: ${e.message}")
                emit(Resource.Error<SearchSuggestions>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getRelatedData(videoId: String): Flow<Resource<ArrayList<Track>>> = flow {
        runCatching {
            YouTube.nextCustom(videoId).onSuccess { result ->
                val listSongs: ArrayList<Track> = arrayListOf()
                val data = result.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs[0].tabRenderer.content?.musicQueueRenderer?.content?.playlistPanelRenderer?.contents
                parseRelated(data)?.let { list ->
                    listSongs.addAll(list)
                }
                emit(Resource.Success<ArrayList<Track>>(listSongs))
            }.onFailure { e ->
                Log.d("Related", "Error: ${e.message}")
                emit(Resource.Error<ArrayList<Track>>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getLyricsData(query: String): Flow<Resource<Lyrics>> = flow {
        runCatching {
//            val q = query.replace(Regex("\\([^)]*?(feat.|ft.|cùng với|con)[^)]*?\\)"), "")
//                .replace("  ", " ")
            val q = query.replace(Regex("\\((feat\\.|ft.|cùng với|con) "), " ").replace(
                Regex("( và | & | и | e | und |, )"), " ").replace("  ", " ").replace(Regex("([()])"), "")
            Log.d("Lyrics", "query: $q")
            var musixMatchUserToken = YouTube.musixmatchUserToken
            if (musixMatchUserToken == null) {
                YouTube.getMusixmatchUserToken().onSuccess { usertoken ->
                    YouTube.musixmatchUserToken = usertoken.message.body.user_token
                    musixMatchUserToken = usertoken.message.body.user_token
                }
                    .onFailure { throwable ->
                        throwable.printStackTrace()
                        emit(Resource.Error<Lyrics>("Not found"))
                    }
            }
            YouTube.searchMusixmatchTrackId(q, musixMatchUserToken!!).onSuccess { searchResult ->
                val list = arrayListOf<String>()
                for (i in searchResult.message.body.track_list) {
                    list.add(i.track.track_name + " " + i.track.artist_name)
                }
                var id = ""
                val bestMatchingIndex = bestMatchingIndex(q, list)
                if (list.get(bestMatchingIndex).contains(searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name)) {
                    Log.w("Lyrics", "item: ${searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name}")
                    id += searchResult.message.body.track_list.get(bestMatchingIndex).track.track_id.toString()
                }
                else {
                    Log.w("Lyrics", "item: ${searchResult.message.body.track_list.get(0).track.track_name}")
                    id += searchResult.message.body.track_list.get(0).track.track_id.toString()
                }
                YouTube.getMusixmatchLyrics(id, musixMatchUserToken!!).onSuccess {
                    if (it != null) {
                        emit(Resource.Success<Lyrics>(it.toLyrics()))
                    }
                    else {
                        Log.w("Lyrics", "Error: Lỗi getLyrics ${it.toString()}")
                        emit(Resource.Error<Lyrics>("Not found"))
                    }
                }.onFailure {
                    it.printStackTrace()
                    emit(Resource.Error<Lyrics>("Not found"))
                }
//                bestMatchingIndex(q, list).let { index ->
//                    Log.w("Lyrics", "item: ${searchResult.message.body.track_list.get(index).track.track_name}")
//                    searchResult.message.body.track_list.get(index).track.track_id.let { trackId ->
//
//                    }
//                }
            }
                .onFailure { throwable ->
                    throwable.printStackTrace()
                    emit(Resource.Error<Lyrics>("Not found"))
                }

//            YouTube.authentication().onSuccess { token ->
//                if (token.accessToken != null) {
//                    YouTube.getSongId(token.accessToken!!, q).onSuccess { spotifyResult ->
//                        Log.d("SongId", "id: ${spotifyResult.tracks?.items?.get(0)?.id}")
//                        if (!spotifyResult.tracks?.items.isNullOrEmpty()) {
//                            val list = arrayListOf<String>()
//                            for (index in spotifyResult.tracks?.items!!.indices) {
//                                list.add(
//                                    (spotifyResult.tracks?.items?.get(index)?.name ?: "") + " " + (spotifyResult.tracks?.items?.get(
//                                        index
//                                    )?.artists?.connectArtistsSpotify() ?: "")
//                                )
//                            }
//                            Log.w("Lyrics", "list: $list")
//                            var id = ""
//                            val bestMatchingIndex = bestMatchingIndex(q, list)
//                            if (q.contains(spotifyResult.tracks?.items?.get(bestMatchingIndex)?.name.toString()) && q.contains(spotifyResult.tracks?.items?.get(bestMatchingIndex)?.artists?.firstOrNull()?.name.toString())) {
//                                id += spotifyResult.tracks?.items?.get(bestMatchingIndex)?.id
//                                Log.w("Lyrics", "item: ${spotifyResult.tracks?.items?.get(bestMatchingIndex)?.name}")
//                            }
//                            else {
//                                id += spotifyResult.tracks?.items?.get(0)?.id
//                                Log.w("Lyrics", "item: ${spotifyResult.tracks?.items?.get(0)?.name}")
//                            }
//                            if (id != "") {
//                                if (dataStoreManager.spotifyAccessTokenExpire.first() != 0L && dataStoreManager.spotifyAccessToken.first() != "" && (dataStoreManager.spotifyAccessTokenExpire.first()
//                                        .toLong()) > Instant.now().toEpochMilli()) {
//                                    Log.d(
//                                        "Lyrics",
//                                        "token: ${dataStoreManager.spotifyAccessToken.first()}"
//                                    )
//                                    YouTube.getLyrics(
//                                        id,
//                                        dataStoreManager.spotifyAccessToken.first()
//                                    ).onSuccess { lyrics ->
//                                        emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
//                                    }.onFailure { throwable ->
//                                        Log.d(
//                                            "Lyrics",
//                                            "Error: Lỗi getLyrics ${throwable.message}"
//                                        )
//                                        spotifyResult.tracks?.items?.firstOrNull()?.id?.let { it2 ->
//                                            YouTube.getLyrics(
//                                                it2,
//                                                dataStoreManager.spotifyAccessToken.first()
//                                            ).onSuccess {
//                                                emit(Resource.Success<Lyrics>(it.toLyrics()))
//                                            }
//                                                .onFailure {
//                                                    Log.d(
//                                                        "Lyrics",
//                                                        "Error: Lỗi getLyrics lần 2 ${it.message}"
//                                                    )
//                                                    emit(Resource.Error<Lyrics>("Not found"))
//                                                }
//                                        }
//                                        emit(Resource.Error<Lyrics>("Not found"))
//                                    }
//                                }
//                                else {
//                                    YouTube.getAccessToken()
//                                        .onSuccess { value: AccessToken ->
//                                            dataStoreManager.setSpotifyAccessToken(value.accessToken!!)
//                                            dataStoreManager.setSpotifyAccessTokenExpire(
//                                                value.accessTokenExpirationTimestampMs!!
//                                            )
//                                            Log.d(
//                                                "Lyrics",
//                                                "token: ${value.accessToken}"
//                                            )
//                                            YouTube.getLyrics(id, value.accessToken)
//                                                .onSuccess { lyrics ->
//                                                    emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
//                                                }.onFailure { throwable ->
//                                                    throwable.printStackTrace()
//                                                    spotifyResult.tracks?.items?.firstOrNull()?.id?.let { it2 ->
//                                                        YouTube.getLyrics(
//                                                            it2,
//                                                            value.accessToken
//                                                        ).onSuccess {
//                                                            emit(
//                                                                Resource.Success<Lyrics>(
//                                                                    it.toLyrics()
//                                                                )
//                                                            )
//                                                        }
//                                                            .onFailure {
//                                                                Log.d(
//                                                                    "Lyrics",
//                                                                    "Error: Lỗi getLyrics lần 2 ${it.message}"
//                                                                )
//                                                                emit(
//                                                                    Resource.Error<Lyrics>(
//                                                                        "Not found"
//                                                                    )
//                                                                )
//                                                            }
//                                                    }
//                                                    emit(Resource.Error<Lyrics>("Not found"))
//                                                }
//                                        }
//                                        .onFailure { e ->
//                                            e.printStackTrace()
//                                            emit(Resource.Error<Lyrics>("Not found"))
//                                        }
//                                }
//                            }
//                            else {
//                                Log.w("Lyrics", "Can't find song id")
//                                emit(Resource.Error<Lyrics>("Not found"))
//                            }
//                            //                            bestMatchingIndex(q, list).let {
////                            bestMatchingIndex(q, list).let { index ->
////                                spotifyResult.tracks?.items?.get(index)?.let { item ->
////                                    if (list[index].contains(item.name.toString())) {
////                                        Log.w("Lyrics", "item: ${item.name}")
////                                        item.id?.let { it1 ->
////                                            Log.d("Lyrics", "id: $it1")
////                                            if (dataStoreManager.spotifyAccessTokenExpire.first() != 0L && dataStoreManager.spotifyAccessToken.first() != "" && (dataStoreManager.spotifyAccessTokenExpire.first()
////                                                    .toLong()) > Instant.now().toEpochMilli()
////                                            ) {
////                                                Log.d(
////                                                    "Lyrics",
////                                                    "token: ${dataStoreManager.spotifyAccessToken.first()}"
////                                                )
////                                                YouTube.getLyrics(
////                                                    it1,
////                                                    dataStoreManager.spotifyAccessToken.first()
////                                                ).onSuccess { lyrics ->
////                                                    emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
////                                                }.onFailure {
////                                                    Log.d(
////                                                        "Lyrics",
////                                                        "Error: Lỗi getLyrics ${it.message}"
////                                                    )
////                                                    spotifyResult.tracks?.items?.firstOrNull()?.id?.let { it2 ->
////                                                        YouTube.getLyrics(
////                                                            it2,
////                                                            dataStoreManager.spotifyAccessToken.first()
////                                                        ).onSuccess {
////                                                            emit(Resource.Success<Lyrics>(it.toLyrics()))
////                                                        }
////                                                            .onFailure {
////                                                                Log.d(
////                                                                    "Lyrics",
////                                                                    "Error: Lỗi getLyrics lần 2 ${it.message}"
////                                                                )
////                                                                emit(Resource.Error<Lyrics>("Not found"))
////                                                            }
////                                                    }
////                                                    emit(Resource.Error<Lyrics>("Not found"))
////                                                }
////                                            } else {
////                                                YouTube.getAccessToken()
////                                                    .onSuccess { value: AccessToken ->
////                                                        dataStoreManager.setSpotifyAccessToken(value.accessToken!!)
////                                                        dataStoreManager.setSpotifyAccessTokenExpire(
////                                                            value.accessTokenExpirationTimestampMs!!
////                                                        )
////                                                        Log.d(
////                                                            "Lyrics",
////                                                            "token: ${value.accessToken}"
////                                                        )
////                                                        YouTube.getLyrics(it1, value.accessToken)
////                                                            .onSuccess { lyrics ->
////                                                                emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
////                                                            }.onFailure {
////                                                                it.printStackTrace()
////                                                                spotifyResult.tracks?.items?.firstOrNull()?.id?.let { it2 ->
////                                                                    YouTube.getLyrics(
////                                                                        it2,
////                                                                        value.accessToken
////                                                                    ).onSuccess {
////                                                                        emit(
////                                                                            Resource.Success<Lyrics>(
////                                                                                it.toLyrics()
////                                                                            )
////                                                                        )
////                                                                    }
////                                                                        .onFailure {
////                                                                            Log.d(
////                                                                                "Lyrics",
////                                                                                "Error: Lỗi getLyrics lần 2 ${it.message}"
////                                                                            )
////                                                                            emit(
////                                                                                Resource.Error<Lyrics>(
////                                                                                    "Not found"
////                                                                                )
////                                                                            )
////                                                                        }
////                                                                }
////                                                                emit(Resource.Error<Lyrics>("Not found"))
////                                                            }
////                                                    }
////                                                    .onFailure { e ->
////                                                        e.printStackTrace()
////                                                        emit(Resource.Error<Lyrics>("Not found"))
////                                                    }
////                                            }
////                                        }
////                                    }
////                                    else {
////
////                                    }
////                                }
////                            }
//
//                        }
//                    }
//                } else {
//                    emit(Resource.Error<Lyrics>("Not found"))
//                }
//            }.onFailure {
//                Log.d("SongId", "Error: ${it.message}")
//                emit(Resource.Error<Lyrics>("Not found"))
//            }
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getStream(videoId: String, itag: Int): Flow<String?> = flow{
        YouTube.player(videoId).onSuccess { response ->
            val format = response.streamingData?.adaptiveFormats?.find { it.itag == itag}
                runBlocking {
                    insertFormat(
                        FormatEntity(
                            videoId = videoId,
                            itag = format?.itag ?: itag,
                            mimeType = format?.mimeType,
                            bitrate = format?.bitrate?.toLong(),
                            contentLength = format?.contentLength,
                            lastModified = format?.lastModified,
                            loudnessDb = response.playerConfig?.audioConfig?.loudnessDb?.toFloat(),
                            uploader = response.videoDetails?.author?.replace(Regex(" - Topic| - Chủ đề|"), ""),
                            uploaderId = response.videoDetails?.channelId,
                            uploaderThumbnail = response.videoDetails?.authorAvatar,
                            uploaderSubCount = response.videoDetails?.authorSubCount,
                            description = response.videoDetails?.description,
                            playbackTrackingVideostatsPlaybackUrl = response.playbackTracking?.videostatsPlaybackUrl?.baseUrl,
                            playbackTrackingAtrUrl = response.playbackTracking?.atrUrl?.baseUrl,
                            playbackTrackingVideostatsWatchtimeUrl = response.playbackTracking?.videostatsWatchtimeUrl?.baseUrl,
                        )
                    )
                }
                emit(response.streamingData?.adaptiveFormats?.find { it.itag == itag }?.url)
            }.onFailure {
                it.printStackTrace()
            Log.e("Stream", "Error: ${it.message}")
                emit(null)
            }
    }.flowOn(Dispatchers.IO)
    suspend fun getLibraryPlaylist(): Flow<ArrayList<PlaylistsResult>?> = flow {
        YouTube.getLibraryPlaylists().onSuccess { data ->
            val input = data.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.gridRenderer?.items ?: null
            if (input != null) {
                Log.w("Library", "input: ${input.size}")
                val list = parseLibraryPlaylist(input)
                emit(list)
            }
            else {
                emit(null)
            }
        }
            .onFailure {e ->
                Log.e("Library", "Error: ${e.message}")
                e.printStackTrace()
                emit(null)
            }
    }
    suspend fun initPlayback(playback: String, atr: String, watchTime: String): Flow<Int> = flow {
        YouTube.initPlayback(playback, atr, watchTime).onSuccess { response ->
            emit(response)
        }.onFailure {
            Log.e("InitPlayback", "Error: ${it.message}")
            emit(0)
        }
    }.flowOn(Dispatchers.IO)
    suspend fun getSkipSegments(videoId: String): Flow<List<SkipSegments>?> = flow {
        YouTube.getSkipSegments(videoId).onSuccess {
            emit(it)
        }.onFailure {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    fun getFullMetadata(videoId: String): Flow<YouTubeInitialPage?> = flow {
        YouTube.getFullMetadata(videoId).onSuccess {
            emit(it)
        }.onFailure {
            Log.e("getFullMetadata", "Error: ${it.message}")
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    fun checkForUpdate(): Flow<GithubResponse?> = flow {
        YouTube.checkForUpdate().onSuccess {
            emit(it)
        }
            .onFailure {
                emit(null)
            }
    }

    suspend fun getYouTubeSetVideoId(youtubePlaylistId: String): Flow<ArrayList<SetVideoIdEntity>?> = flow {
        YouTube.playlist(youtubePlaylistId).onSuccess { playlistPage ->
            flow {
                runCatching {
                    var id = ""
                    if (!youtubePlaylistId.startsWith("VL")) {
                        id += "VL$youtubePlaylistId"
                    }
                    else {
                        id += youtubePlaylistId
                    }
                    Log.d("Repository", "playlist id: $id")
                    YouTube.customQuery(browseId = id, setLogin = true).onSuccess { result ->
                        val listContent: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                        val data: List<MusicShelfRenderer.Content>? = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicPlaylistShelfRenderer?.contents
                        if (data != null) {
                            Log.d("Data", "data: $data")
                            Log.d("Data", "data size: ${data.size}")
                            listContent.addAll(data)
                        }
                        var continueParam = result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.musicPlaylistShelfRenderer?.continuations?.get(0)?.nextContinuationData?.continuation
                        var count = 0
                        Log.d("Repository", "playlist data: ${listContent.size}")
                        Log.d("Repository", "continueParam: $continueParam")
                        while (continueParam != null) {
                            YouTube.customQuery(browseId = "", continuation = continueParam, setLogin = true).onSuccess { values ->
                                Log.d("Continue", "continue: $continueParam")
                                val dataMore: List<MusicShelfRenderer.Content>? = values.continuationContents?.musicPlaylistShelfContinuation?.contents
                                if (dataMore != null) {
                                    listContent.addAll(dataMore)
                                }
                                continueParam = values.continuationContents?.musicPlaylistShelfContinuation?.continuations?.get(0)?.nextContinuationData?.continuation
                                count++
                            }.onFailure {
                                Log.e("Continue", "Error: ${it.message}")
                                continueParam = null
                                count++
                            }
                        }
                        Log.d("Repository", "playlist final data: ${listContent.size}")
                        parseSetVideoId(listContent).let { playlist ->
                            playlist.forEach { item ->
                                insertSetVideoId(item)
                            }
                            emit(playlist)
                        }
                    }.onFailure { e ->
                        e.printStackTrace()
                        emit(null)
                    }
                }
            }.flowOn(Dispatchers.IO)
        }
    }

    suspend fun createYouTubePlaylist(playlist: LocalPlaylistEntity): Flow<String?> = flow {
        runCatching {
            YouTube.createPlaylist(playlist.title, playlist.tracks).onSuccess {
                emit(it.playlistId)
            }.onFailure {
                it.printStackTrace()
                emit(null)
            }
        }
    }

    suspend fun editYouTubePlaylist(title: String, youtubePlaylistId: String): Flow<Int> = flow {
        runCatching {
            YouTube.editPlaylist(youtubePlaylistId, title).onSuccess { response ->
                emit(response)
            }.onFailure {
                it.printStackTrace()
                emit(0)
            }
        }
    }

    suspend fun removeYouTubePlaylistItem(youtubePlaylistId: String, videoId: String) = flow {
        runCatching {
            getSetVideoId(videoId).collect() { setVideoId ->
                if (setVideoId?.setVideoId != null) {
                    YouTube.removeItemYouTubePlaylist(youtubePlaylistId, videoId, setVideoId.setVideoId).onSuccess {
                        emit(it)
                    }.onFailure {
                        emit(0)
                    }
                }
                else {
                    emit(0)
                }
            }
        }
    }

    suspend fun addYouTubePlaylistItem(youtubePlaylistId: String, videoId: String) = flow {
        runCatching {
            YouTube.addPlaylistItem(youtubePlaylistId, videoId).onSuccess {
                if (it.playlistEditResults.isNotEmpty()) {
                    for (playlistEditResult in it.playlistEditResults) {
                        insertSetVideoId(SetVideoIdEntity(playlistEditResult.playlistEditVideoAddedResultData.videoId, playlistEditResult.playlistEditVideoAddedResultData.setVideoId))
                    }
                    emit(it.status)
                }
                else {
                    emit("FAILED")
                }
            }.onFailure {
                emit("FAILED")
            }
        }
    }
}