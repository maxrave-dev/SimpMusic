package com.maxrave.data.repository

import android.content.Context
import com.maxrave.common.R
import com.maxrave.data.db.LocalDataSource
import com.maxrave.data.extension.getFullDataFromDB
import com.maxrave.data.mapping.toListTrack
import com.maxrave.data.mapping.toTrack
import com.maxrave.data.mapping.toYouTubeWatchEndpoint
import com.maxrave.data.parser.parseLibraryPlaylist
import com.maxrave.data.parser.parseNextLibraryPlaylist
import com.maxrave.data.parser.parsePlaylistData
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SetVideoIdEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.playlist.Author
import com.maxrave.domain.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toTrack
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.pages.NextPage
import com.maxrave.kotlinytmusicscraper.parser.getPlaylistContinuation
import com.maxrave.kotlinytmusicscraper.parser.getPlaylistRadioEndpoint
import com.maxrave.kotlinytmusicscraper.parser.getPlaylistShuffleEndpoint
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

internal class PlaylistRepositoryImpl(
    private val context: Context,
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : PlaylistRepository {
    override fun getAllPlaylists(limit: Int): Flow<List<PlaylistEntity>> =
        flow {
            emit(localDataSource.getAllPlaylists(limit))
        }.flowOn(Dispatchers.IO)

    override fun getPlaylist(id: String): Flow<PlaylistEntity?> =
        flow {
            emit(localDataSource.getPlaylist(id))
        }.flowOn(Dispatchers.IO)

    override fun getLikedPlaylists(): Flow<List<PlaylistEntity>> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getLikedPlaylists(limit, offset)
                },
            )
        }.flowOn(Dispatchers.IO)

    override suspend fun insertPlaylist(playlistEntity: PlaylistEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertPlaylist(playlistEntity) }

    override suspend fun insertAndReplacePlaylist(playlistEntity: PlaylistEntity) =
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

    override suspend fun insertRadioPlaylist(playlistEntity: PlaylistEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertRadioPlaylist(playlistEntity) }

    override suspend fun updatePlaylistLiked(
        playlistId: String,
        likeStatus: Int,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updatePlaylistLiked(
            likeStatus,
            playlistId,
        )
    }

    override suspend fun updatePlaylistInLibrary(
        inLibrary: LocalDateTime,
        playlistId: String,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updatePlaylistInLibrary(
            inLibrary,
            playlistId,
        )
    }

    override suspend fun updatePlaylistDownloadState(
        playlistId: String,
        downloadState: Int,
    ) = withContext(Dispatchers.Main) {
        localDataSource.updatePlaylistDownloadState(
            downloadState,
            playlistId,
        )
    }

    override fun getAllDownloadedPlaylist(): Flow<List<PlaylistType>> =
        flow { emit(localDataSource.getAllDownloadedPlaylist()) }.flowOn(Dispatchers.IO)

    override fun getAllDownloadingPlaylist(): Flow<List<PlaylistType>> =
        flow { emit(localDataSource.getAllDownloadingPlaylist()) }.flowOn(Dispatchers.IO)

    private suspend fun insertSetVideoId(setVideoId: SetVideoIdEntity) = withContext(Dispatchers.IO) { localDataSource.insertSetVideoId(setVideoId) }

    override fun getRadio(
        radioId: String,
        originalTrack: SongEntity?,
        artist: ArtistEntity?,
    ): Flow<Resource<Pair<PlaylistBrowse, String?>>> =
        if (radioId.startsWith("RDAT")) {
            getRDATRadioData(radioId)
        } else {
            flow {
                runCatching {
                    youTube
                        .next(endpoint = WatchEndpoint(playlistId = radioId))
                        .onSuccess { next ->
                            Logger.w("Radio", "Title: ${next.title}")
                            val data: ArrayList<SongItem> = arrayListOf()
                            data.addAll(next.items)
                            var continuation = next.continuation
                            Logger.w("Radio", "data: ${data.size}")
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
                                        Logger.w("Radio", "data: ${data.size}")
                                        count++
                                    }.onFailure {
                                        count = 3
                                    }
                            }
                            val listTrackResult = data.toListTrack()
                            if (originalTrack != null) {
                                listTrackResult.add(0, originalTrack.toTrack())
                            }
                            Logger.w("Repository", "data: ${data.size}")
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
                            Logger.w("Repository", "playlistBrowse: $playlistBrowse")
                            emit(Resource.Success<Pair<PlaylistBrowse, String?>>(Pair(playlistBrowse, continuation)))
                        }.onFailure { exception ->
                            exception.printStackTrace()
                            emit(Resource.Error<Pair<PlaylistBrowse, String?>>(exception.message.toString()))
                        }
                }
            }.flowOn(Dispatchers.IO)
        }

    override fun getRDATRadioData(radioId: String): Flow<Resource<Pair<PlaylistBrowse, String?>>> =
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
                            Logger.d("Data", "data: $data")
                            Logger.d("Data", "data size: ${data.size}")
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
                        Logger.d("Header", "header: $header")
                        val finalContinueParam =
                            result.getPlaylistContinuation()
                        Logger.d("Repository", "playlist data: ${listContent.size}")
                        Logger.d("Repository", "continueParam: $finalContinueParam")
//                        else {
//                            var listTrack = playlistBrowse.tracks.toMutableList()
                        Logger.d("Repository", "playlist final data: ${listContent.size}")
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
                        Logger.e("Playlist Data", e.message ?: "Error")
                        emit(Resource.Error(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getFullPlaylistData(playlistId: String): Flow<Resource<PlaylistBrowse>> =
        flow {
            runCatching {
                var id = ""
                id +=
                    if (!playlistId.startsWith("VL")) {
                        "VL$playlistId"
                    } else {
                        playlistId
                    }
                Logger.d("getPlaylistData", "playlist id: $id")
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
                            Logger.d("getPlaylistData", "data: $data")
                            Logger.d("getPlaylistData", "data size: ${data.size}")
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
                        Logger.d("getPlaylistData", "header: $header")
                        var continueParam =
                            result.getPlaylistContinuation()
                        var count = 0
                        Logger.d("getPlaylistData", "playlist data: ${listContent.size}")
                        Logger.d("getPlaylistData", "continueParam: $continueParam")
//                        else {
//                            var listTrack = playlistBrowse.tracks.toMutableList()
                        while (continueParam != null) {
                            youTube
                                .customQuery(
                                    browseId = null,
                                    continuation = continueParam,
                                    setLogin = true,
                                ).onSuccess { values ->
                                    Logger.d("getPlaylistData", "continue: $continueParam")
                                    Logger.d(
                                        "getPlaylistData",
                                        "values: ${values.onResponseReceivedActions}",
                                    )
                                    val dataMore: List<SongItem> =
                                        values.onResponseReceivedActions
                                            ?.firstOrNull()
                                            ?.appendContinuationItemsAction
                                            ?.continuationItems
                                            ?.apply {
                                                Logger.w("getPlaylistData", "dataMore: ${this.size}")
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
                                    Logger.e("getPlaylistData", "Error: ${it.message}")
                                    continueParam = null
                                    count++
                                }
                        }
                        Logger.d("getPlaylistData", "playlist final data: ${listContent.size}")
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
                        Logger.e("getPlaylistData", e.message ?: "Error")
                        emit(Resource.Error<PlaylistBrowse>(e.message.toString()))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getPlaylistData(playlistId: String): Flow<Resource<Pair<PlaylistBrowse, String?>>> =
        flow {
            runCatching {
                var id = ""
                id +=
                    if (!playlistId.startsWith("VL")) {
                        "VL$playlistId"
                    } else {
                        playlistId
                    }
                Logger.d("getPlaylistData", "playlist id: $id")
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
                            Logger.d("getPlaylistData", "data: $data")
                            Logger.d("getPlaylistData", "data size: ${data.size}")
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
                        Logger.d("getPlaylistData", "header: $header")
                        val continueParam =
                            result.getPlaylistContinuation()
                        val radioEndpoint =
                            result.getPlaylistRadioEndpoint()
                        val shuffleEndpoint =
                            result.getPlaylistShuffleEndpoint()
                        Logger.d("getPlaylistData", "Endpoint: $radioEndpoint $shuffleEndpoint")
                        try {
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
                                                shuffleEndpoint = shuffleEndpoint?.toYouTubeWatchEndpoint(),
                                                radioEndpoint = radioEndpoint?.toYouTubeWatchEndpoint(),
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
                        } catch (e: Exception) {
                            e.printStackTrace()
                            emit(
                                Resource.Error<
                                    Pair<PlaylistBrowse, String?>,
                                >(e.message.toString()),
                            )
                        }
                    }.onFailure { e ->
                        Logger.e("getPlaylistData", e.message ?: "Error")
                        emit(
                            Resource.Error<
                                Pair<PlaylistBrowse, String?>,
                            >(e.message.toString()),
                        )
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getLibraryPlaylist(): Flow<List<PlaylistsResult>?> =
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
                    val listItem = mutableListOf<PlaylistsResult>()
                    if (input.isNullOrEmpty()) {
                        Logger.w("Library", "No playlists found")
                        emit(null)
                        return@onSuccess
                    }
                    listItem.addAll(
                        parseLibraryPlaylist(input),
                    )
                    var continuation =
                        data.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.gridRenderer
                            ?.continuations
                            ?.firstOrNull()
                            ?.nextContinuationData
                            ?.continuation
                    while (continuation != null) {
                        youTube
                            .nextYouTubePlaylists(continuation)
                            .onSuccess { nextData ->
                                continuation = nextData.second
                                Logger.w("Library", "continuation: $continuation")
                                val nextInput = nextData.first
                                listItem.addAll(
                                    parseNextLibraryPlaylist(nextInput),
                                )
                            }.onFailure { exception ->
                                exception.printStackTrace()
                                Logger.e("Library", "Error: ${exception.message}")
                                continuation = null
                            }
                    }
                    if (listItem.isNotEmpty()) {
                        emit(listItem)
                    } else {
                        emit(null)
                    }
                }.onFailure { e ->
                    Logger.e("Library", "Error: ${e.message}")
                    e.printStackTrace()
                    emit(null)
                }
        }.flowOn(Dispatchers.IO)

    override fun getMixedForYou(): Flow<List<PlaylistsResult>?> =
        flow {
            youTube
                .getMixedForYou()
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
                    val listItem = mutableListOf<PlaylistsResult>()
                    if (input.isNullOrEmpty()) {
                        Logger.w("Mixed For You", "No playlists found")
                        emit(null)
                        return@onSuccess
                    }
                    listItem.addAll(
                        parseLibraryPlaylist(input),
                    )
                    var continuation =
                        data.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.gridRenderer
                            ?.continuations
                            ?.firstOrNull()
                            ?.nextContinuationData
                            ?.continuation
                    while (continuation != null) {
                        youTube
                            .nextYouTubePlaylists(continuation)
                            .onSuccess { nextData ->
                                continuation = nextData.second
                                Logger.w("Mixed For You", "continuation: $continuation")
                                val nextInput = nextData.first
                                listItem.addAll(
                                    parseNextLibraryPlaylist(nextInput),
                                )
                            }.onFailure { exception ->
                                exception.printStackTrace()
                                Logger.e("Mixed For You", "Error: ${exception.message}")
                                continuation = null
                            }
                    }
                    if (listItem.isNotEmpty()) {
                        emit(listItem)
                    } else {
                        emit(null)
                    }
                }
        }.flowOn(Dispatchers.IO)
}