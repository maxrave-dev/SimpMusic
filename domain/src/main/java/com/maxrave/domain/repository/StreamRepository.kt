package com.maxrave.domain.repository

import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.mediaService.SponsorSkipSegments
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface StreamRepository {
    suspend fun insertNewFormat(newFormat: NewFormatEntity)

    fun getNewFormat(videoId: String): Flow<NewFormatEntity?>

    suspend fun getFormatFlow(videoId: String): Flow<NewFormatEntity?>

    suspend fun updateFormat(videoId: String)

    fun getStream(
        dataStoreManager: DataStoreManager,
        videoId: String,
        isVideo: Boolean,
    ): Flow<String?>

    fun initPlayback(
        playback: String,
        atr: String,
        watchTime: String,
        cpn: String,
        playlistId: String?,
    ): Flow<Pair<Int, Float>>

    fun updateWatchTimeFull(
        watchTime: String,
        cpn: String,
        playlistId: String?,
    ): Flow<Int>

    fun updateWatchTime(
        playbackTrackingVideostatsWatchtimeUrl: String,
        watchTimeList: ArrayList<Float>,
        cpn: String,
        playlistId: String?,
    ): Flow<Int>

    fun getSkipSegments(videoId: String): Flow<Resource<List<SponsorSkipSegments>>>

    fun getFullMetadata(videoId: String): Flow<Resource<Track>>

    fun is403Url(url: String): Flow<Boolean>
}