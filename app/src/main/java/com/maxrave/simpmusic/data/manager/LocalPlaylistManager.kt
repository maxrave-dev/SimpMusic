package com.maxrave.simpmusic.data.manager

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity.YouTubeSyncState.Synced
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity.YouTubeSyncState.Syncing
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.SetVideoIdEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.manager.base.BaseManager
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.parser.parseSetVideoId
import com.maxrave.simpmusic.extension.toListVideoId
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.pagination.localPlaylistPaging.LocalPlaylistPagingSource
import com.maxrave.simpmusic.utils.LocalResource
import com.maxrave.simpmusic.viewModel.FilterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

class LocalPlaylistManager(
    context: Context,
) : BaseManager(context) {
    override val tag: String = this.javaClass.simpleName

    suspend fun getLocalPlaylist(id: Long) =
        wrapDataResource {
            localDataSource.getLocalPlaylist(id)
        }

    fun downloadStateFlow(id: Long): Flow<Int> = localDataSource.getDownloadStateFlowOfLocalPlaylist(id)

    fun listTrackFlow(id: Long): Flow<List<String>> = localDataSource.getListTracksFlowOfLocalPlaylist(id).map { it ?: emptyList() }

    fun getTracksPaging(
        id: Long,
        filter: FilterState,
    ): Flow<PagingData<SongEntity>> {
        val totalCount = runBlocking(Dispatchers.IO) { localDataSource.getLocalPlaylist(id).tracks?.size ?: 0 }
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

    suspend fun updateTitleLocalPlaylist(
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
                if (localPlaylist.youtubePlaylistId != null) {
                    YouTube
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

    suspend fun syncYouTubePlaylistToLocalPlaylist(playlist: PlaylistBrowse): Flow<LocalResource<String>> =
        flow<LocalResource<String>> {
            emit(LocalResource.Loading())
            val localPlaylistEntity =
                LocalPlaylistEntity(
                    title = playlist.title,
                    thumbnail = playlist.thumbnails.lastOrNull()?.url,
                    youtubePlaylistId = playlist.id,
                    syncedWithYouTubePlaylist = 1,
                    tracks = playlist.tracks.toListVideoId(),
                    downloadState = DownloadState.STATE_NOT_DOWNLOADED,
                    syncState = Syncing,
                )
            runBlocking { localDataSource.insertLocalPlaylist(localPlaylistEntity) }
            val localPlaylistId =
                localDataSource.getLocalPlaylistByYoutubePlaylistId(playlist.id)?.id
                    ?: throw Exception(getString(R.string.error))
            playlist.tracks.forEachIndexed { i, track ->
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
            val id = if (ytPlaylistId.startsWith("VL")) ytPlaylistId else "VL$ytPlaylistId"
            YouTube
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
                        YouTube
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
                    if (listContent.isEmpty) {
                        emit(LocalResource.Error("Can't get setVideoId"))
                    }
                    val parsed = parseSetVideoId(ytPlaylistId, listContent)
                    if (parsed.isEmpty) {
                        emit(LocalResource.Error("Can't get setVideoId"))
                    }
                    parsed.forEach { setVideoId ->
                        localDataSource.insertSetVideoId(setVideoId)
                    }
                    emit(LocalResource.Success(getString(R.string.synced)))
                }.onFailure {
                    emit(LocalResource.Error("Can't get setVideoId"))
                }
            localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(
                localPlaylistId,
                Synced,
            )
            emit(LocalResource.Success(getString(R.string.synced)))
        }.flowOn(
            Dispatchers.IO,
        )

    /**
     * Sync local playlist to YouTube playlist
     * return youtubePlaylistId
     * @param LocalPlaylistEntity
     * @return Flow<LocalResource<String>>
     */
    suspend fun syncLocalPlaylistToYouTubePlaylist(playlist: LocalPlaylistEntity) =
        wrapResultResource {
            YouTube
                .createPlaylist(
                    playlist.title,
                    playlist.tracks,
                ).map {
                    it.playlistId
                }
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
            val localPlaylist = localDataSource.getLocalPlaylist(id)
            val nextPosition = localPlaylist.tracks?.size ?: 0
            val nextPair =
                PairSongLocalPlaylist(
                    playlistId = id,
                    songId = song.videoId,
                    position = nextPosition,
                    inPlaylist = LocalDateTime.now(),
                )
            localDataSource.updateLocalPlaylistTracks(
                localPlaylist.tracks?.plus(song.videoId) ?: mutableListOf(song.videoId),
                id,
            )
            localDataSource.insertPairSongLocalPlaylist(nextPair)
            // Emit success message
            emit(LocalResource.Success(getString(R.string.added_to_playlist)))

            // Add to YouTube playlist
            if (localPlaylist.youtubePlaylistId != null) {
                YouTube
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
            val localPlaylist = localDataSource.getLocalPlaylist(id)
            val nextTracks = localPlaylist.tracks?.toMutableList() ?: mutableListOf()
            nextTracks.remove(song.videoId)
            localDataSource.updateLocalPlaylistTracks(nextTracks, id)
            localDataSource.deletePairSongLocalPlaylist(id, song.videoId)
            emit(LocalResource.Success(getString(R.string.delete_song_from_playlist)))
            if (localPlaylist.youtubePlaylistId != null) {
                val setVideoId = localDataSource.getSetVideoId(song.videoId)
                if (setVideoId?.setVideoId != null) {
                    YouTube
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
}