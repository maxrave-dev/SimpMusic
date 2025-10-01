@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.maxrave.data.repository

import android.content.Context
import androidx.annotation.StringRes
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.maxrave.common.R
import com.maxrave.data.db.Converters
import com.maxrave.data.db.LocalDataSource
import com.maxrave.data.extension.getFullDataFromDB
import com.maxrave.data.mapping.toListTrack
import com.maxrave.data.mapping.toTrack
import com.maxrave.data.paging.LocalPlaylistPagingSource
import com.maxrave.data.parser.parseSetVideoId
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.LocalPlaylistEntity.YouTubeSyncState.Synced
import com.maxrave.domain.data.entities.LocalPlaylistEntity.YouTubeSyncState.Syncing
import com.maxrave.domain.data.entities.PairSongLocalPlaylist
import com.maxrave.domain.data.entities.SetVideoIdEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.playlist.PlaylistState
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.utils.FilterState
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.toListVideoId
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.wrapDataResource
import com.maxrave.domain.utils.wrapMessageResource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.extension.verifyYouTubePlaylistId
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.response.SearchResponse
import com.maxrave.kotlinytmusicscraper.pages.NextPage
import com.maxrave.kotlinytmusicscraper.pages.SearchPage
import com.maxrave.kotlinytmusicscraper.parser.getPlaylistContinuation
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

private const val TAG = "LocalPlaylistRepositoryImpl"

internal class LocalPlaylistRepositoryImpl(
    private val context: Context,
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : LocalPlaylistRepository {
    override fun getLocalPlaylist(id: Long) =
        wrapDataResource {
            localDataSource.getLocalPlaylist(id)
        }

    override fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
        flow {
            val list = getFullDataFromDB { limit, offset ->
                localDataSource.getAllLocalPlaylists(limit, offset)
            }
            emit(list)
        }.flowOn(Dispatchers.IO)

    override suspend fun updateLocalPlaylistTracks(
        tracks: List<String>,
        id: Long,
    ) = withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistTracks(tracks, id) }

    override suspend fun updateLocalPlaylistDownloadState(
        downloadState: Int,
        id: Long,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistDownloadState(
            downloadState,
            id,
        )
    }

    override suspend fun updateLocalPlaylistYouTubePlaylistSyncState(
        id: Long,
        syncState: Int,
    ) = withContext(Dispatchers.IO) {
        localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)
    }

    override fun downloadStateFlow(id: Long): Flow<Int> = localDataSource.getDownloadStateFlowOfLocalPlaylist(id)

    override fun getAllDownloadingLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
        flow {
            emit(
                getFullDataFromDB { limit, offset ->
                    localDataSource.getAllDownloadingLocalPlaylists(limit, offset)
                }
            )
        }.flowOn(Dispatchers.IO)

    override fun listTrackFlow(id: Long): Flow<List<String>> =
        localDataSource
            .getListTracksFlowOfLocalPlaylist(id)
            .map { Converters().fromString(it.firstOrNull()) ?: emptyList() }

    override fun getTracksPaging(
        id: Long,
        filter: FilterState,
    ): Flow<PagingData<SongEntity>> {
        val totalCount = runBlocking(Dispatchers.IO) { localDataSource.getLocalPlaylist(id)?.tracks?.size ?: 0 }
        Logger.w(TAG, "getTracksPaging: $totalCount")
        return Pager(
            config = PagingConfig(pageSize = 100, prefetchDistance = 5),
            pagingSourceFactory = {
                LocalPlaylistPagingSource(
                    playlistId = id,
                    totalCount = totalCount,
                    filter = filter,
                    localDataSource = localDataSource,
                )
            },
        ).flow
    }

    override suspend fun getFullPlaylistTracks(id: Long): List<SongEntity> {
        val playlist = localDataSource.getLocalPlaylist(id) ?: return emptyList()
        Logger.d(TAG, "getFullPlaylistTracks: $playlist")
        val tracks = mutableListOf<SongEntity>()
        var currentPage = 0
        while (true) {
            val pairs =
                localDataSource.getPlaylistPairSongByOffset(
                    playlistId = id,
                    filterState = FilterState.OlderFirst,
                    offset = currentPage,
                    totalCount = playlist.tracks?.size ?: 0,
                )
            if (pairs.isNullOrEmpty()) {
                break
            }
            val songs =
                localDataSource
                    .getSongByListVideoIdFull(
                        pairs.map { it.songId },
                    )
            val idValue = songs.associateBy { it.videoId }
            val sorted =
                pairs.mapNotNull {
                    idValue[it.songId]
                }
            tracks.addAll(sorted)
            currentPage++
        }
        return tracks
    }

    override suspend fun getListTrackVideoId(id: Long): List<String> {
        val playlist = localDataSource.getLocalPlaylist(id)
        return playlist?.tracks ?: emptyList()
    }

    override fun insertLocalPlaylist(localPlaylist: LocalPlaylistEntity): Flow<LocalResource<String>> =
        wrapMessageResource(
            successMessage = getString(R.string.added_local_playlist),
        ) {
            localDataSource.insertLocalPlaylist(localPlaylist)
        }

    override fun deleteLocalPlaylist(id: Long) =
        wrapMessageResource(
            successMessage = getString(R.string.delete),
        ) {
            localDataSource.deleteLocalPlaylist(id)
        }

    override fun updateTitleLocalPlaylist(
        id: Long,
        newTitle: String,
    ): Flow<LocalResource<String>> =
        flow {
            emit(LocalResource.Loading<String>())
            runCatching {
                localDataSource.updateLocalPlaylistTitle(id = id, title = newTitle)
            }.onSuccess {
                emit(LocalResource.Success(getString(R.string.updated)))
                val localPlaylist = localDataSource.getLocalPlaylist(id)
                val ytId = localPlaylist?.youtubePlaylistId
                if (ytId != null) {
                    youTube
                        .editPlaylist(ytId, newTitle)
                        .onSuccess {
                            emit(LocalResource.Success(getString(R.string.updated_to_youtube_playlist)))
                        }.onFailure {
                            emit(LocalResource.Error<String>(it.message ?: getString(R.string.error)))
                        }
                }
            }.onFailure {
                emit(LocalResource.Error<String>(it.message ?: getString(R.string.error)))
            }
        }.flowOn(Dispatchers.IO)

    override fun updateThumbnailLocalPlaylist(
        id: Long,
        newThumbnail: String,
    ) = wrapMessageResource(
        successMessage = getString(R.string.updated),
    ) {
        localDataSource.updateLocalPlaylistThumbnail(id = id, thumbnail = newThumbnail)
    }

    override fun updateDownloadState(
        id: Long,
        downloadState: Int,
    ) = wrapMessageResource(
        successMessage = getString(R.string.updated),
    ) {
        localDataSource.updateLocalPlaylistDownloadState(id = id, downloadState = downloadState)
    }

    override fun syncYouTubePlaylistToLocalPlaylist(
        playlist: PlaylistState,
        tracks: List<Track>,
    ): Flow<LocalResource<String>> =
        flow<LocalResource<String>> {
            emit(LocalResource.Loading())
            val localPlaylistEntity =
                LocalPlaylistEntity(
                    title = playlist.title,
                    thumbnail = playlist.thumbnail,
                    youtubePlaylistId = playlist.id,
                    tracks = tracks.toListVideoId(),
                    downloadState = DownloadState.STATE_NOT_DOWNLOADED,
                    syncState = Syncing,
                )
            runBlocking { localDataSource.insertLocalPlaylist(localPlaylistEntity) }
            val localPlaylistId =
                localDataSource.getLocalPlaylistByYoutubePlaylistId(playlist.id)?.id
                    ?: throw Exception(getString(R.string.error))
            tracks.forEachIndexed { i, track ->
                runBlocking {
                    localDataSource.insertSong(
                        track.toSongEntity(),
                    )
                    localDataSource.insertPairSongLocalPlaylist(
                        PairSongLocalPlaylist(
                            playlistId = localPlaylistId,
                            songId = track.videoId,
                            position = i,
                            inPlaylist = LocalDateTime.now(),
                        ),
                    )
                }
            }
            val ytPlaylistId = playlist.id
            val id = ytPlaylistId.verifyYouTubePlaylistId()
            youTube
                .customQuery(browseId = id, setLogin = true)
                .onSuccess { res ->
                    val listContent: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                    val data =
                        res.contents
                            ?.twoColumnBrowseResultsRenderer
                            ?.secondaryContents
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicPlaylistShelfRenderer
                            ?.contents
                    data?.let { listContent.addAll(it) }
                    var continueParam =
                        res.contents
                            ?.twoColumnBrowseResultsRenderer
                            ?.secondaryContents
                            ?.sectionListRenderer
                            ?.continuations
                            ?.firstOrNull()
                            ?.nextContinuationData
                            ?.continuation
                    while (continueParam != null) {
                        youTube
                            .customQuery(
                                "",
                                continuation = continueParam,
                                setLogin = true,
                            ).onSuccess { values ->
                                val dataMore: List<MusicShelfRenderer.Content>? =
                                    values.continuationContents
                                        ?.sectionListContinuation
                                        ?.contents
                                        ?.firstOrNull()
                                        ?.musicShelfRenderer
                                        ?.contents
                                if (dataMore != null) {
                                    listContent.addAll(dataMore)
                                }
                                continueParam =
                                    values.continuationContents
                                        ?.sectionListContinuation
                                        ?.continuations
                                        ?.firstOrNull()
                                        ?.nextContinuationData
                                        ?.continuation
                            }.onFailure { continueParam = null }
                    }
                    if (listContent.isEmpty()) {
                        emit(LocalResource.Error("Can't get setVideoId"))
                    }
                    val parsed = parseSetVideoId(ytPlaylistId, listContent)
                    if (parsed.isEmpty()) {
                        emit(LocalResource.Error("Can't get setVideoId"))
                    }
                    parsed.forEach { setVideoId ->
                        localDataSource.insertSetVideoId(setVideoId)
                    }
                    localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(
                        localPlaylistId,
                        Synced,
                    )
                    emit(LocalResource.Success(getString(R.string.synced)))
                }.onFailure {
                    emit(LocalResource.Error("Can't get setVideoId"))
                }
        }.flowOn(
            Dispatchers.IO,
        )

    /**
     * Sync local playlist to YouTube playlist
     * return youtubePlaylistId
     * @param playlistId
     * @return Flow<LocalResource<String>>
     */
    override fun syncLocalPlaylistToYouTubePlaylist(playlistId: Long) =
        flow<LocalResource<String>> {
            emit(LocalResource.Loading())
            val playlist = localDataSource.getLocalPlaylist(playlistId) ?: return@flow
            val res =
                youTube.createPlaylist(
                    playlist.title,
                    playlist.tracks,
                )
            val value = res.getOrNull()
            if (res.isSuccess && value != null) {
                val ytId = value.playlistId
                Logger.d(TAG, "syncLocalPlaylistToYouTubePlaylist: $ytId")
                youTube
                    .getYouTubePlaylistFullTracksWithSetVideoId(ytId)
                    .onSuccess { list ->
                        Logger.d(TAG, "syncLocalPlaylistToYouTubePlaylist: onSuccess song ${list.map { it.first.title }}")
                        Logger.d(TAG, "syncLocalPlaylistToYouTubePlaylist: onSuccess setVideoId ${list.map { it.second }}")
                        list.forEach { new ->
                            localDataSource.insertSong(new.first.toTrack().toSongEntity())
                            localDataSource.insertSetVideoId(
                                SetVideoIdEntity(
                                    videoId = new.first.id,
                                    setVideoId = new.second,
                                    youtubePlaylistId = ytId,
                                ),
                            )
                        }
                        if (list.isEmpty()) Logger.w(TAG, "syncLocalPlaylistToYouTubePlaylist: SetVideoIds Empty list")
                        localDataSource.updateLocalPlaylistYouTubePlaylistId(playlistId, ytId)
                        localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(playlistId, Synced)
                        Logger.d(TAG, "syncLocalPlaylistToYouTubePlaylist: $ytId")
                        emit(LocalResource.Success(ytId))
                    }.onFailure {
                        emit(LocalResource.Error(it.message ?: getString(R.string.error)))
                    }
            } else {
                val e = res.exceptionOrNull()
                e?.printStackTrace()
                emit(LocalResource.Error(e?.message ?: getString(R.string.error)))
            }
        }

    override fun unsyncLocalPlaylist(id: Long) =
        wrapMessageResource(
            successMessage = getString(R.string.unsynced),
        ) {
            localDataSource.unsyncLocalPlaylist(id)
        }

    override fun updateSyncState(
        id: Long,
        syncState: Int,
    ) = wrapMessageResource(
        successMessage = getString(R.string.synced),
    ) {
        localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)
    }

    override fun updateYouTubePlaylistId(
        id: Long,
        youtubePlaylistId: String,
    ) = wrapMessageResource(
        successMessage = getString(R.string.updated),
    ) {
        localDataSource.updateLocalPlaylistYouTubePlaylistId(id, youtubePlaylistId)
    }

    override fun updateListTrackSynced(id: Long) =
        flow<Boolean> {
            val localPlaylist = localDataSource.getLocalPlaylist(id) ?: return@flow
            val tracks = localPlaylist.tracks ?: emptyList()
            val currentTracks = tracks.toMutableList()
            localPlaylist.youtubePlaylistId?.let { ytId ->
                Logger.d(TAG, "updateListTrackSynced: $ytId")
                youTube
                    .getYouTubePlaylistFullTracksWithSetVideoId(ytId)
                    .onSuccess { list ->
                        Logger.d(TAG, "updateListTrackSynced: onSuccess ${list.map { it.first.title }}")
                        val newTrack =
                            list
                                .map { it.first }
                                .toListTrack()
                                .map { it.videoId }
                                .toMutableSet()
                                .subtract(tracks.toMutableSet())
                        val newTrackList = list.filter { newTrack.contains(it.first.id) }
                        Logger.w(TAG, "updateListTrackSynced: newTrackList ${newTrackList.map { it.first.title }}")
                        newTrackList.forEach { new ->
                            localDataSource.insertSong(new.first.toTrack().toSongEntity())
                            Logger.i(TAG, "insertSong: ${new.first.toTrack().toSongEntity()}")
                            localDataSource.insertPairSongLocalPlaylist(
                                PairSongLocalPlaylist(
                                    playlistId = id,
                                    songId = new.first.id,
                                    position = currentTracks.size,
                                    inPlaylist = LocalDateTime.now(),
                                ),
                            )
                            localDataSource.insertSetVideoId(
                                SetVideoIdEntity(
                                    videoId = new.first.id,
                                    setVideoId = new.second,
                                    youtubePlaylistId = ytId,
                                ),
                            )
                            currentTracks.add(new.first.id)
                        }
                        localDataSource.updateLocalPlaylistTracks(currentTracks, id).let {
                            emit(true)
                        }
                    }.onFailure { e ->
                        Logger.e(TAG, "updateListTrackSynced: onFailure ${e.message}")
                        e.printStackTrace()
                        emit(false)
                    }
            }
            emit(false)
        }

    // Update
    override fun addTrackToLocalPlaylist(
        id: Long,
        song: SongEntity,
    ): Flow<LocalResource<String>> =
        flow {
            emit(LocalResource.Loading())
            val checkSong = localDataSource.getSong(song.videoId)
            if (checkSong == null) {
                localDataSource.insertSong(song)
            }
            val localPlaylist = localDataSource.getLocalPlaylist(id) ?: return@flow
            val nextPosition = localPlaylist.tracks?.size ?: 0
            val nextPair =
                PairSongLocalPlaylist(
                    playlistId = id,
                    songId = song.videoId,
                    position = nextPosition,
                    inPlaylist = LocalDateTime.now(),
                )
            runBlocking {
                localDataSource.insertPairSongLocalPlaylist(nextPair)
                localDataSource.updateLocalPlaylistTracks(
                    localPlaylist.tracks?.plus(song.videoId) ?: mutableListOf(song.videoId),
                    id,
                )
            }
            // Emit success message
            emit(LocalResource.Success(getString(R.string.added_to_playlist)))

            // Add to YouTube playlist
            val ytId = localPlaylist.youtubePlaylistId
            if (ytId != null) {
                youTube
                    .addPlaylistItem(ytId, song.videoId)
                    .onSuccess {
                        val data = it.playlistEditResults
                        if (data.isNotEmpty()) {
                            for (d in data) {
                                localDataSource.insertSetVideoId(
                                    SetVideoIdEntity(
                                        d.playlistEditVideoAddedResultData.videoId,
                                        d.playlistEditVideoAddedResultData.setVideoId,
                                    ),
                                )
                            }
                            emit(LocalResource.Success(getString(R.string.added_to_youtube_playlist)))
                        } else {
                            emit(LocalResource.Error<String>("${getString(R.string.can_t_add_to_youtube_playlist)}: Empty playlistEditResults"))
                        }
                    }.onFailure {
                        emit(LocalResource.Error<String>("${getString(R.string.can_t_add_to_youtube_playlist)}: ${it.message}"))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun removeTrackFromLocalPlaylist(
        id: Long,
        song: SongEntity,
    ): Flow<LocalResource<String>> =
        flow {
            emit(LocalResource.Loading())
            val localPlaylist = localDataSource.getLocalPlaylist(id) ?: return@flow
            val nextTracks = localPlaylist.tracks?.toMutableList() ?: mutableListOf()
            nextTracks.remove(song.videoId)
            localDataSource.updateLocalPlaylistTracks(nextTracks, id)
            localDataSource.deletePairSongLocalPlaylist(id, song.videoId)
            emit(LocalResource.Success(getString(R.string.delete_song_from_playlist)))
            val ytPlaylistId = localPlaylist.youtubePlaylistId
            if (ytPlaylistId != null) {
                val setVideoId = localDataSource.getSetVideoId(song.videoId)?.setVideoId
                if (setVideoId != null) {
                    youTube
                        .removeItemYouTubePlaylist(ytPlaylistId, song.videoId, setVideoId)
                        .onSuccess {
                            emit(LocalResource.Success(getString(R.string.removed_from_YouTube_playlist)))
                        }.onFailure {
                            emit(LocalResource.Error<String>("${getString(R.string.can_t_delete_from_youtube_playlist)}: ${it.message}"))
                        }
                } else {
                    emit(LocalResource.Error<String>("${getString(R.string.can_t_delete_from_youtube_playlist)}: SetVideoId is null"))
                }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSuggestionsTrackForPlaylist(id: Long): Flow<LocalResource<Pair<String?, List<Track>>>> =
        flow {
            val localPlaylist = localDataSource.getLocalPlaylist(id) ?: return@flow
            val ytPlaylistId = localPlaylist.youtubePlaylistId ?: return@flow

            youTube
                .getSuggestionsTrackForPlaylist(ytPlaylistId)
                .onSuccess { data ->
                    val listSongItem = data?.second?.map { it.toTrack() }
                    if (data != null && !listSongItem.isNullOrEmpty()) {
                        emit(
                            LocalResource.Success(
                                Pair(
                                    data.first,
                                    listSongItem,
                                ),
                            ),
                        )
                    } else {
                        emit(LocalResource.Error("List suggestions is null"))
                    }
                }.onFailure { e ->
                    e.printStackTrace()
                    emit(LocalResource.Error(e.message ?: "Error"))
                }
        }

    override fun reloadSuggestionPlaylist(reloadParams: String): Flow<LocalResource<Pair<String?, List<Track>>>> =
        flow {
            runCatching {
                emit(LocalResource.Loading())
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
                            emit(LocalResource.Success(Pair(reloadParamsNew, listTrack.toList())))
                        } else {
                            emit(LocalResource.Error("Empty data"))
                        }
                    }.onFailure { exception ->
                        exception.printStackTrace()
                        emit(LocalResource.Error(exception.message ?: "Error"))
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getYouTubeSetVideoId(youtubePlaylistId: String): Flow<List<SetVideoIdEntity>> =
        flow {
            runCatching {
                var id = ""
                if (!youtubePlaylistId.startsWith("VL")) {
                    id += "VL$youtubePlaylistId"
                } else {
                    id += youtubePlaylistId
                }
                Logger.d("Repository", "playlist id: $id")
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
                        Logger.d("Repository", "playlist data: ${listContent.size}")
                        Logger.d("Repository", "continueParam: $continueParam")
                        while (continueParam != null) {
                            youTube
                                .customQuery(
                                    browseId = "",
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
                                    listContent.addAll(dataMore)
                                    continueParam =
                                        values.getPlaylistContinuation()
                                    count++
                                }.onFailure {
                                    Logger.e("Continue", "Error: ${it.message}")
                                    continueParam = null
                                    count++
                                }
                        }
                        Logger.d("Repository", "playlist final data: ${listContent.size}")
                        parseSetVideoId(youtubePlaylistId, data ?: emptyList()).let { playlist ->
                            playlist.forEach { item ->
                                localDataSource.insertSetVideoId(item)
                            }
                            listContent.forEach { item ->
                                localDataSource.insertSetVideoId(
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
                        emit(emptyList())
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun addYouTubePlaylistItem(
        youtubePlaylistId: String,
        videoId: String,
    ): Flow<LocalResource<String>> =
        flow {
            emit(LocalResource.Loading())
            youTube
                .addPlaylistItem(youtubePlaylistId.verifyYouTubePlaylistId(), videoId)
                .onSuccess {
                    if (it.playlistEditResults.isNotEmpty()) {
                        for (playlistEditResult in it.playlistEditResults) {
                            localDataSource.insertSetVideoId(
                                SetVideoIdEntity(
                                    playlistEditResult.playlistEditVideoAddedResultData.videoId,
                                    playlistEditResult.playlistEditVideoAddedResultData.setVideoId,
                                ),
                            )
                        }
                        emit(LocalResource.Success(it.status))
                    } else {
                        emit(LocalResource.Error("FAILED"))
                    }
                }.onFailure {
                    emit(LocalResource.Error("FAILED"))
                }
        }.flowOn(Dispatchers.IO)

    override suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) =
        withContext(Dispatchers.IO) {
            localDataSource.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }

    override fun getPlaylistPairSongByListPosition(
        playlistId: Long,
        listPosition: List<Int>,
    ): Flow<List<PairSongLocalPlaylist>?> =
        flow {
            emit(localDataSource.getPlaylistPairSongByListPosition(playlistId, listPosition))
        }.flowOn(Dispatchers.IO)

    override fun getPlaylistPairSongByOffset(
        playlistId: Long,
        offset: Int,
        filterState: FilterState,
        totalCount: Int,
    ): Flow<List<PairSongLocalPlaylist>?> =
        flow {
            emit(localDataSource.getPlaylistPairSongByOffset(playlistId, offset, filterState, totalCount))
        }.flowOn(Dispatchers.IO)

    private fun getString(
        @StringRes resId: Int,
    ) = context.getString(resId)
}