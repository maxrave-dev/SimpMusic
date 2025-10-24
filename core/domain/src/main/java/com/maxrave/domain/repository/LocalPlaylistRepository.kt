package com.maxrave.domain.repository

import androidx.paging.PagingData
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PairSongLocalPlaylist
import com.maxrave.domain.data.entities.SetVideoIdEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.browse.playlist.PlaylistState
import com.maxrave.domain.utils.FilterState
import com.maxrave.domain.utils.LocalResource
import kotlinx.coroutines.flow.Flow

interface LocalPlaylistRepository {
    fun getLocalPlaylist(id: Long): Flow<LocalResource<LocalPlaylistEntity?>>

    fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>>

    suspend fun updateLocalPlaylistTracks(
        tracks: List<String>,
        id: Long,
    )

    suspend fun updateLocalPlaylistDownloadState(
        downloadState: Int,
        id: Long,
    )

    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(
        id: Long,
        syncState: Int,
    )

    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist)

    fun getPlaylistPairSongByListPosition(
        playlistId: Long,
        listPosition: List<Int>,
    ): Flow<List<PairSongLocalPlaylist>?>

    fun getPlaylistPairSongByOffset(
        playlistId: Long,
        offset: Int,
        filterState: FilterState,
        totalCount: Int,
    ): Flow<List<PairSongLocalPlaylist>?>

    fun downloadStateFlow(id: Long): Flow<Int>

    fun getAllDownloadingLocalPlaylists(): Flow<List<LocalPlaylistEntity>>

    fun listTrackFlow(id: Long): Flow<List<String>>

    fun getTracksPaging(
        id: Long,
        filter: FilterState,
    ): Flow<PagingData<SongEntity>>

    suspend fun getFullPlaylistTracks(id: Long): List<SongEntity>

    suspend fun getListTrackVideoId(id: Long): List<String>

    fun insertLocalPlaylist(localPlaylist: LocalPlaylistEntity): Flow<LocalResource<String>>

    fun deleteLocalPlaylist(id: Long): Flow<LocalResource<String>>

    fun updateTitleLocalPlaylist(
        id: Long,
        newTitle: String,
    ): Flow<LocalResource<String>>

    fun updateThumbnailLocalPlaylist(
        id: Long,
        newThumbnail: String,
    ): Flow<LocalResource<String>>

    fun updateDownloadState(
        id: Long,
        downloadState: Int,
    ): Flow<LocalResource<String>>

    fun syncYouTubePlaylistToLocalPlaylist(
        playlist: PlaylistState,
        tracks: List<Track>,
    ): Flow<LocalResource<String>>

    fun syncLocalPlaylistToYouTubePlaylist(playlistId: Long): Flow<LocalResource<String>>

    fun unsyncLocalPlaylist(id: Long): Flow<LocalResource<String>>

    fun updateSyncState(
        id: Long,
        syncState: Int,
    ): Flow<LocalResource<String>>

    fun updateYouTubePlaylistId(
        id: Long,
        youtubePlaylistId: String,
    ): Flow<LocalResource<String>>

    fun updateListTrackSynced(id: Long): Flow<Boolean>

    fun addTrackToLocalPlaylist(
        id: Long,
        song: SongEntity,
    ): Flow<LocalResource<String>>

    fun removeTrackFromLocalPlaylist(
        id: Long,
        song: SongEntity,
    ): Flow<LocalResource<String>>

    fun getSuggestionsTrackForPlaylist(id: Long): Flow<LocalResource<Pair<String?, List<Track>>>>

    fun reloadSuggestionPlaylist(reloadParams: String): Flow<LocalResource<Pair<String?, List<Track>>>>

    fun getYouTubeSetVideoId(youtubePlaylistId: String): Flow<List<SetVideoIdEntity>>

    fun addYouTubePlaylistItem(
        youtubePlaylistId: String,
        videoId: String,
    ): Flow<LocalResource<String>>
}