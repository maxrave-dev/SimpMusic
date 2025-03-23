package com.maxrave.simpmusic.data.manager

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.extension.verifyYouTubePlaylistId
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.Converters
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity.YouTubeSyncState.Synced
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity.YouTubeSyncState.Syncing
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.SetVideoIdEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.manager.base.BaseManager
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.parser.parseSetVideoId
import com.maxrave.simpmusic.extension.toListTrack
import com.maxrave.simpmusic.extension.toListVideoId
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.pagination.localPlaylistPaging.LocalPlaylistPagingSource
import com.maxrave.simpmusic.utils.LocalResource
import com.maxrave.simpmusic.viewModel.FilterState
import com.maxrave.simpmusic.viewModel.PlaylistState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

class LocalPlaylistManager(
    context: Context,
    private val youTube: YouTube,
) : BaseManager(context) {
    suspend fun getLocalPlaylist(id: Long) =
        wrapDataResource {
            localDataSource.getLocalPlaylist(id)
        }

    fun downloadStateFlow(id: Long): Flow<Int> = localDataSource.getDownloadStateFlowOfLocalPlaylist(id)

    fun listTrackFlow(id: Long): Flow<List<String>> =
        localDataSource
            .getListTracksFlowOfLocalPlaylist(id)
            .map { Converters().fromString(it?.firstOrNull()) ?: emptyList() }

    fun getTracksPaging(
        id: Long,
        filter: FilterState,
    ): Flow<PagingData<SongEntity>> {
        val totalCount = runBlocking(Dispatchers.IO) { localDataSource.getLocalPlaylist(id)?.tracks?.size ?: 0 }
        Log.w(tag, "getTracksPaging: $totalCount")
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

    suspend fun getFullPlaylistTracks(id: Long): List<SongEntity> {
        val playlist = localDataSource.getLocalPlaylist(id) ?: return emptyList()
        Log.d(tag, "getFullPlaylistTracks: $playlist")
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

    suspend fun getListTrackVideoId(id: Long): List<String> {
        val playlist = localDataSource.getLocalPlaylist(id)
        return playlist?.tracks ?: emptyList()
    }

    suspend fun insertLocalPlaylist(localPlaylist: LocalPlaylistEntity): Flow<LocalResource<String>> =
        wrapMessageResource(
            successMessage = getString(R.string.added_local_playlist),
        ) {
            localDataSource.insertLocalPlaylist(localPlaylist)
        }

    suspend fun deleteLocalPlaylist(id: Long) =
        wrapMessageResource(
            successMessage = getString(R.string.delete),
        ) {
            localDataSource.deleteLocalPlaylist(id)
        }

    fun updateTitleLocalPlaylist(
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
                if (localPlaylist?.youtubePlaylistId != null) {
                    youTube
                        .editPlaylist(localPlaylist.youtubePlaylistId, newTitle)
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

    suspend fun updateThumbnailLocalPlaylist(
        id: Long,
        newThumbnail: String,
    ) = wrapMessageResource(
        successMessage = getString(R.string.updated),
    ) {
        localDataSource.updateLocalPlaylistThumbnail(id = id, thumbnail = newThumbnail)
    }

    suspend fun updateDownloadState(
        id: Long,
        downloadState: Int,
    ) = wrapMessageResource(
        successMessage = getString(R.string.updated),
    ) {
        localDataSource.updateLocalPlaylistDownloadState(id = id, downloadState = downloadState)
    }

    fun syncYouTubePlaylistToLocalPlaylist(
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
    fun syncLocalPlaylistToYouTubePlaylist(playlistId: Long) =
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
                Log.d(tag, "syncLocalPlaylistToYouTubePlaylist: $ytId")
                youTube
                    .getYouTubePlaylistFullTracksWithSetVideoId(ytId)
                    .onSuccess { list ->
                        Log.d(tag, "syncLocalPlaylistToYouTubePlaylist: onSuccess song ${list.map { it.first.title }}")
                        Log.d(tag, "syncLocalPlaylistToYouTubePlaylist: onSuccess setVideoId ${list.map { it.second }}")
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
                        if (list.isEmpty()) Log.w(tag, "syncLocalPlaylistToYouTubePlaylist: SetVideoIds Empty list")
                        localDataSource.updateLocalPlaylistYouTubePlaylistId(playlistId, ytId)
                        localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(playlistId, Synced)
                        Log.d(tag, "syncLocalPlaylistToYouTubePlaylist: $ytId")
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

    suspend fun unsyncLocalPlaylist(id: Long) =
        wrapMessageResource(
            successMessage = getString(R.string.unsynced),
        ) {
            localDataSource.unsyncLocalPlaylist(id)
        }

    suspend fun updateSyncState(
        id: Long,
        syncState: Int,
    ) = wrapMessageResource(
        successMessage = getString(R.string.synced),
    ) {
        localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)
    }

    suspend fun updateYouTubePlaylistId(
        id: Long,
        youtubePlaylistId: String,
    ) = wrapMessageResource(
        successMessage = getString(R.string.updated),
    ) {
        localDataSource.updateLocalPlaylistYouTubePlaylistId(id, youtubePlaylistId)
    }

    suspend fun updateListTrackSynced(id: Long) =
        flow<Boolean> {
            val localPlaylist = localDataSource.getLocalPlaylist(id) ?: return@flow
            val tracks = localPlaylist.tracks ?: emptyList()
            val currentTracks = tracks.toMutableList()
            localPlaylist.youtubePlaylistId?.let { ytId ->
                Log.d(tag, "updateListTrackSynced: $ytId")
                youTube
                    .getYouTubePlaylistFullTracksWithSetVideoId(ytId)
                    .onSuccess { list ->
                        Log.d(tag, "updateListTrackSynced: onSuccess ${list.map { it.first.title }}")
                        val newTrack =
                            list
                                .map { it.first }
                                .toListTrack()
                                .map { it.videoId }
                                .toMutableSet()
                                .subtract(tracks.toMutableSet())
                        val newTrackList = list.filter { newTrack.contains(it.first.id) }
                        Log.w(tag, "updateListTrackSynced: newTrackList ${newTrackList.map { it.first.title }}")
                        newTrackList.forEach { new ->
                            localDataSource.insertSong(new.first.toTrack().toSongEntity())
                            Log.i(tag, "insertSong: ${new.first.toTrack().toSongEntity()}")
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
                        Log.e(tag, "updateListTrackSynced: onFailure ${e.message}")
                        e.printStackTrace()
                        emit(false)
                    }
            }
            emit(false)
        }

    // Update
    suspend fun addTrackToLocalPlaylist(
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
            if (localPlaylist.youtubePlaylistId != null) {
                youTube
                    .addPlaylistItem(localPlaylist.youtubePlaylistId, song.videoId)
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

    suspend fun removeTrackFromLocalPlaylist(
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
            if (localPlaylist.youtubePlaylistId != null) {
                val setVideoId = localDataSource.getSetVideoId(song.videoId)
                if (setVideoId?.setVideoId != null) {
                    youTube
                        .removeItemYouTubePlaylist(localPlaylist.youtubePlaylistId, song.videoId, setVideoId.setVideoId)
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

    suspend fun getSuggestionsTrackForPlaylist(id: Long): Flow<LocalResource<Pair<String?, List<Track>>>> =
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
}