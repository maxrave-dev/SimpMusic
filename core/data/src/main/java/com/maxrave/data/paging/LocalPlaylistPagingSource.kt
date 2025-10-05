package com.maxrave.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.maxrave.data.db.LocalDataSource
import com.maxrave.domain.data.entities.PairSongLocalPlaylist
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.utils.FilterState
import com.maxrave.logger.Logger

internal class LocalPlaylistPagingSource(
    private val playlistId: Long,
    private val totalCount: Int,
    private val filter: FilterState,
    private val localDataSource: LocalDataSource,
) : PagingSource<Int, SongEntity>() {
    override fun getRefreshKey(state: PagingState<Int, SongEntity>): Int? = state.anchorPosition

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SongEntity> {
        return try {
            val currentPage = params.key ?: 0
            val pairs =
                localDataSource.getPlaylistPairSongByOffset(
                    playlistId = playlistId,
                    filterState = filter,
                    offset = currentPage,
                    totalCount = totalCount,
                )
            Logger.d("LocalPlaylistPagingSource", "load: $pairs")
            val songs =
                localDataSource
                    .getSongByListVideoIdFull(
                        pairs?.map { it.songId } ?: emptyList(),
                    )
            val idValue = songs.associateBy { it.videoId }
            val sorted =
                (pairs ?: mutableListOf<PairSongLocalPlaylist>()).mapNotNull {
                    idValue[it.songId]
                }
            Logger.d("LocalPlaylistPagingSource", "load: $songs")
            return LoadResult.Page(
                data = sorted,
                prevKey = if (currentPage == 0) null else currentPage - 1,
                nextKey = if (songs.isEmpty()) null else currentPage + 1,
            )
        } catch (e: Exception) {
            Logger.e("LocalPlaylistPagingSource", "load: ${e.printStackTrace()}")
            LoadResult.Error(e)
        }
    }
}