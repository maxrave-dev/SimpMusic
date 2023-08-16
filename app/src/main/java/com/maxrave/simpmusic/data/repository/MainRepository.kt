package com.maxrave.simpmusic.data.repository

import android.content.Context
import android.util.Log
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.response.PipedResponse
import com.maxrave.simpmusic.data.db.LocalDataSource
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.LyricsEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.artist.ArtistBrowse
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
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.data.parser.parseAlbumData
import com.maxrave.simpmusic.data.parser.parseArtistData
import com.maxrave.simpmusic.data.parser.parseChart
import com.maxrave.simpmusic.data.parser.parseGenreObject
import com.maxrave.simpmusic.data.parser.parseMixedContent
import com.maxrave.simpmusic.data.parser.parseMoodsMomentObject
import com.maxrave.simpmusic.data.parser.parsePlaylistData
import com.maxrave.simpmusic.data.parser.parseRelated
import com.maxrave.simpmusic.data.parser.search.parseSearchAlbum
import com.maxrave.simpmusic.data.parser.search.parseSearchArtist
import com.maxrave.simpmusic.data.parser.search.parseSearchPlaylist
import com.maxrave.simpmusic.data.parser.search.parseSearchSong
import com.maxrave.simpmusic.data.parser.search.parseSearchVideo
import com.maxrave.simpmusic.extension.toLyrics
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

//@ActivityRetainedScoped
@Singleton
class MainRepository @Inject constructor(private val localDataSource: LocalDataSource, @ApplicationContext private val context: Context) {
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

    suspend fun getHomeData(): Flow<Resource<ArrayList<HomeItem>>> = flow {
        runCatching {
            YouTube.customQuery(browseId = "FEmusic_home").onSuccess { result ->
                val list: ArrayList<HomeItem> = arrayListOf()
                var continueParam =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.continuations?.get(
                        0
                    )?.nextContinuationData?.continuation
                val data =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents
                list.addAll(parseMixedContent(data))
                var count = 0
                while (count < 3 && continueParam != null) {
                    YouTube.customQuery(browseId = "", continuation = continueParam).onSuccess { response ->
                        continueParam =
                            response.continuationContents?.sectionListContinuation?.continuations?.get(
                                0
                            )?.nextContinuationData?.continuation
                        val dataContinue =
                            response.continuationContents?.sectionListContinuation?.contents
                        list.addAll(parseMixedContent(dataContinue))
                        Log.d("Repository", "continueParam: $continueParam")
                        count++
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
    suspend fun getSuggestQuery(query: String): Flow<Resource<ArrayList<String>>> = flow {
        runCatching {
            YouTube.getSuggestQuery(query).onSuccess {
                emit(Resource.Success<ArrayList<String>>(it))
            }.onFailure { e ->
                Log.d("Suggest", "Error: ${e.message}")
                emit(Resource.Error<ArrayList<String>>(e.message.toString()))
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
            val q = query.replace(Regex("\\([^)]*?(feat|ft|cùng với)[^)]*?\\)"), "")
            Log.d("Lyrics", "query: $q")
            YouTube.authencation().onSuccess {token ->
                if (token.accessToken != null) {
                    YouTube.getSongId(token.accessToken!!, q).onSuccess { spotifyResult ->
                        Log.d("SongId", "id: ${spotifyResult.tracks?.items?.get(0)?.id}")
                        spotifyResult.tracks?.items?.get(0)?.let {
                            it.id?.let { it1 ->
                                Log.d("Lyrics", "id: $it1")
                                YouTube.getLyrics(it1).onSuccess { lyrics ->
                                    emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
                                }.onFailure {
                                    Log.d("Lyrics", "Error: ${it.message}")
                                    emit(Resource.Error<Lyrics>("Not found"))
                                }
                            }
                        }
                    }.onFailure {
                        Log.d("SongId", "Error: ${it.message}")
                        emit(Resource.Error<Lyrics>("Not found"))
                    }
                }

            }
            }.onFailure {
                Log.d("Lyrics", "Error: ${it.message}")
                emit(Resource.Error<Lyrics>(it.message.toString()))
            }
        }.flowOn(Dispatchers.IO)
    suspend fun getStream(videoId: String, itag: Int): Flow<String?> = flow{
            YouTube.player(videoId).onSuccess {
                emit(it.streamingData?.adaptiveFormats?.find { it.itag == itag }?.url)
            }.onFailure {
                emit(null)
            }
    }.flowOn(Dispatchers.IO)

    fun getSongFull(videoId: String): Flow<PipedResponse> = flow {
        YouTube.pipeStream(videoId).onSuccess {
            emit(it)
        }
    }.flowOn(Dispatchers.IO)
}