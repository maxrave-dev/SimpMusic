package com.maxrave.simpmusic.data.repository

import android.util.Log
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.simpmusic.data.api.BaseApiResponse
import com.maxrave.simpmusic.data.api.search.RemoteDataSource
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
import com.maxrave.simpmusic.data.model.browse.artist.ChannelId
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.model.explore.mood.Genre
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.explore.mood.MoodsMoment
import com.maxrave.simpmusic.data.model.explore.mood.genre.GenreObject
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.MoodsMomentObject
import com.maxrave.simpmusic.data.model.home.HomeItem
import com.maxrave.simpmusic.data.model.home.HomeResponse
import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.data.model.thumbnailUrl
import com.maxrave.simpmusic.data.parser.parseChart
import com.maxrave.simpmusic.data.parser.parseGenreObject
import com.maxrave.simpmusic.data.parser.parseMixedContent
import com.maxrave.simpmusic.data.parser.parseMoodsMomentObject
import com.maxrave.simpmusic.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

//@ActivityRetainedScoped
class MainRepository @Inject constructor(private val remoteDataSource: RemoteDataSource, private val localDataSource: LocalDataSource): BaseApiResponse() {
    suspend fun getThumbnails(songId: String): Flow<Resource<ArrayList<thumbnailUrl>>> =
        flow { emit(safeApiCall { remoteDataSource.getThumbnails(songId) }) }.flowOn(Dispatchers.IO)

    //search
    suspend fun searchAll(query: String, regionCode: String, language: String) =
        remoteDataSource.searchAll(query, regionCode, language)

    suspend fun searchSongs(
        query: String,
        filter: String = "songs",
        regionCode: String,
        language: String
    ): Flow<Resource<ArrayList<SongsResult>>> = flow<Resource<ArrayList<SongsResult>>> {
        emit(safeApiCall {
            remoteDataSource.searchSongs(
                query,
                filter,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    suspend fun searchArtists(
        query: String,
        filter: String = "artists",
        regionCode: String,
        language: String
    ): Flow<Resource<ArrayList<ArtistsResult>>> = flow<Resource<ArrayList<ArtistsResult>>> {
        emit(safeApiCall {
            remoteDataSource.searchArtists(
                query,
                filter,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    suspend fun searchAlbums(
        query: String,
        filter: String = "albums",
        regionCode: String,
        language: String
    ): Flow<Resource<ArrayList<AlbumsResult>>> = flow<Resource<ArrayList<AlbumsResult>>> {
        emit(safeApiCall {
            remoteDataSource.searchAlbums(
                query,
                filter,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    suspend fun searchPlaylists(
        query: String,
        filter: String = "playlists",
        regionCode: String,
        language: String
    ): Flow<Resource<ArrayList<PlaylistsResult>>> = flow<Resource<ArrayList<PlaylistsResult>>> {
        emit(safeApiCall {
            remoteDataSource.searchPlaylists(
                query,
                filter,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    suspend fun searchVideos(
        query: String,
        filter: String = "videos",
        regionCode: String,
        language: String
    ): Flow<Resource<ArrayList<VideosResult>>> = flow<Resource<ArrayList<VideosResult>>> {
        emit(safeApiCall {
            remoteDataSource.searchVideos(
                query,
                filter,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    //suggest query
    suspend fun suggestQuery(query: String): Flow<Resource<ArrayList<String>>> =
        flow<Resource<ArrayList<String>>> { emit(safeApiCall { remoteDataSource.suggestQuery(query) }) }.flowOn(
            Dispatchers.IO
        )

    //getHome
    fun getHomeData(regionCode: String, language: String): Flow<HomeResponse> = combine(
        getHome(regionCode, language),
        exploreMood(regionCode, language),
        exploreChart("ZZ", language)
    ) { home, exploreMood, exploreChart ->
        HomeResponse(home, exploreMood, exploreChart)
    }

    fun getHome(regionCode: String, language: String): Flow<Resource<ArrayList<HomeItem>>> =
        flow<Resource<ArrayList<HomeItem>>> {
            emit(safeApiCall {
                remoteDataSource.getHome(
                    regionCode,
                    language
                )
            })
        }.flowOn(Dispatchers.IO)

    //exploreMood
    fun exploreMood(regionCode: String, language: String): Flow<Resource<Mood>> =
        flow<Resource<Mood>> {
            emit(safeApiCall {
                remoteDataSource.exploreMood(
                    regionCode,
                    language
                )
            })
        }.flowOn(Dispatchers.IO)

    suspend fun getMood(
        params: String,
        regionCode: String,
        language: String
    ): Flow<Resource<MoodsMomentObject>> = flow<Resource<MoodsMomentObject>> {
        emit(safeApiCall {
            remoteDataSource.getMood(
                params,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    suspend fun getGenre(
        params: String,
        regionCode: String,
        language: String
    ): Flow<Resource<GenreObject>> = flow<Resource<GenreObject>> {
        emit(safeApiCall {
            remoteDataSource.getGenre(
                params,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    //browse
    //artist
    suspend fun browseArtist(
        channelId: String,
        regionCode: String,
        language: String
    ): Flow<Resource<ArtistBrowse>> = flow<Resource<ArtistBrowse>> {
        emit(safeApiCall {
            remoteDataSource.browseArtist(
                channelId,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    //album
    suspend fun browseAlbum(
        browseId: String,
        regionCode: String,
        language: String
    ): Flow<Resource<AlbumBrowse>> = flow<Resource<AlbumBrowse>> {
        emit(safeApiCall {
            remoteDataSource.browseAlbum(
                browseId,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    //playlist
    suspend fun browsePlaylist(
        id: String,
        regionCode: String,
        language: String
    ): Flow<Resource<PlaylistBrowse>> = flow<Resource<PlaylistBrowse>> {
        emit(safeApiCall {
            remoteDataSource.browsePlaylist(
                id,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    //chart
    fun exploreChart(regionCode: String, language: String): Flow<Resource<Chart>> =
        flow<Resource<Chart>> {
            emit(safeApiCall {
                remoteDataSource.exploreChart(
                    regionCode,
                    language
                )
            })
        }.flowOn(Dispatchers.IO)

    //metadata
    suspend fun getMetadata(
        videoId: String,
        regionCode: String,
        language: String
    ): Flow<Resource<MetadataSong>> = flow<Resource<MetadataSong>> {
        emit(safeApiCall {
            remoteDataSource.getMetadata(
                videoId,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    suspend fun getLyrics(query: String): Flow<Resource<Lyrics>> =
        flow<Resource<Lyrics>> { emit(safeApiCall { remoteDataSource.getLyrics(query) }) }.flowOn(
            Dispatchers.IO
        )

    //related
    suspend fun getRelated(
        videoId: String,
        regionCode: String,
        language: String
    ): Flow<Resource<ArrayList<Track>>> = flow<Resource<ArrayList<Track>>> {
        emit(safeApiCall {
            remoteDataSource.getRelated(
                videoId,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    suspend fun getVideoRelated(
        videoId: String,
        regionCode: String,
        language: String
    ): Flow<Resource<ArrayList<VideosResult>>> = flow<Resource<ArrayList<VideosResult>>> {
        emit(safeApiCall {
            remoteDataSource.getVideoRelated(
                videoId,
                regionCode,
                language
            )
        })
    }.flowOn(Dispatchers.IO)

    suspend fun convertNameToId(name: String): Flow<Resource<ChannelId>> =
        flow<Resource<ChannelId>> { emit(safeApiCall { remoteDataSource.convertNameToId(name) }) }.flowOn(
            Dispatchers.IO
        )

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
    }

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
    }

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
    }

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
    }
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
    }
}