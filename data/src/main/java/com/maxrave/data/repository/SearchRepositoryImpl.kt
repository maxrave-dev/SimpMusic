package com.maxrave.data.repository

import com.maxrave.data.db.LocalDataSource
import com.maxrave.data.mapping.toDomainSearchSuggestions
import com.maxrave.data.parser.parsePodcast
import com.maxrave.data.parser.search.parseSearchAlbum
import com.maxrave.data.parser.search.parseSearchArtist
import com.maxrave.data.parser.search.parseSearchPlaylist
import com.maxrave.data.parser.search.parseSearchSong
import com.maxrave.data.parser.search.parseSearchVideo
import com.maxrave.domain.data.entities.SearchHistory
import com.maxrave.domain.data.model.searchResult.SearchSuggestions
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.data.model.searchResult.artists.ArtistsResult
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.videos.VideosResult
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

internal class SearchRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : SearchRepository {
    override fun getSearchHistory(): Flow<List<SearchHistory>> =
        flow {
            emit(localDataSource.getSearchHistory())
        }.flowOn(Dispatchers.IO)

    override fun insertSearchHistory(searchHistory: SearchHistory): Flow<Long> =
        flow {
            emit(localDataSource.insertSearchHistory(searchHistory))
        }.flowOn(Dispatchers.IO)

    override suspend fun deleteSearchHistory() =
        withContext(Dispatchers.IO) {
            localDataSource.deleteSearchHistory()
        }

    override fun getSearchDataSong(query: String): Flow<Resource<ArrayList<SongsResult>>> =
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
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }

                        emit(Resource.Success<ArrayList<SongsResult>>(listSongs))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<SongsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataVideo(query: String): Flow<Resource<ArrayList<VideosResult>>> =
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
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }

                        emit(Resource.Success<ArrayList<VideosResult>>(listSongs))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<VideosResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataPodcast(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
        flow {
            runCatching {
                youTube
                    .search(query, YouTube.SearchFilter.FILTER_PODCAST)
                    .onSuccess { result ->
                        println(query)
                        val listPlaylist: ArrayList<PlaylistsResult> = arrayListOf()
                        var countinueParam = result.continuation
                        Logger.w("Podcast", "result: $result")
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
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataFeaturedPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
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
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataArtist(query: String): Flow<Resource<ArrayList<ArtistsResult>>> =
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
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<ArtistsResult>>(listArtist))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<ArtistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataAlbum(query: String): Flow<Resource<ArrayList<AlbumsResult>>> =
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
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<AlbumsResult>>(listAlbum))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<AlbumsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSearchDataPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>> =
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
                                    Logger.e("Continue", "Error: ${it.message}")
                                    countinueParam = null
                                    count++
                                }
                        }
                        emit(Resource.Success<ArrayList<PlaylistsResult>>(listPlaylist))
                    }.onFailure { e ->
                        Logger.d("Search", "Error: ${e.message}")
                        emit(Resource.Error<ArrayList<PlaylistsResult>>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSuggestQuery(query: String): Flow<Resource<SearchSuggestions>> =
        flow {
            runCatching {
                youTube
                    .getYTMusicSearchSuggestions(query)
                    .onSuccess {
                        emit(Resource.Success(it.toDomainSearchSuggestions()))
                    }.onFailure { e ->
                        Logger.d("Suggest", "Error: ${e.message}")
                        emit(Resource.Error<SearchSuggestions>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)
}