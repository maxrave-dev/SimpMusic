package com.maxrave.data.repository

import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.common.QUALITY
import com.maxrave.common.VIDEO_QUALITY
import com.maxrave.data.db.LocalDataSource
import com.maxrave.data.mapping.toSponsorSkipSegments
import com.maxrave.data.mapping.toTrack
import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.mediaService.SponsorSkipSegments
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.MediaType
import com.maxrave.kotlinytmusicscraper.models.response.PlayerResponse
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

internal class StreamRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : StreamRepository {
    override suspend fun insertNewFormat(newFormat: NewFormatEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertNewFormat(newFormat)
        }

    override fun getNewFormat(videoId: String): Flow<NewFormatEntity?> = flow { emit(localDataSource.getNewFormat(videoId)) }.flowOn(Dispatchers.Main)

    override suspend fun getFormatFlow(videoId: String) = localDataSource.getNewFormatAsFlow(videoId)

    override suspend fun updateFormat(videoId: String) {
        localDataSource.getNewFormat(videoId)?.let { oldFormat ->
            Logger.w("Stream", "oldFormatExpired: ${oldFormat.expiredTime}")
            Logger.w("Stream", "now: ${LocalDateTime.now()}")
            Logger.w("Stream", "isExpired: ${oldFormat.expiredTime.isBefore(LocalDateTime.now())}")
            if (oldFormat.expiredTime.isBefore(LocalDateTime.now())) {
                youTube
                    .player(videoId)
                    .onSuccess { triple ->
                        val response = triple.second
                        localDataSource.updateNewFormat(
                            oldFormat.copy(
                                expiredTime = LocalDateTime.now().plusSeconds(response.streamingData?.expiresInSeconds?.toLong() ?: 0L),
                                playbackTrackingVideostatsPlaybackUrl =
                                    response.playbackTracking?.videostatsPlaybackUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                playbackTrackingAtrUrl =
                                    response.playbackTracking?.atrUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                playbackTrackingVideostatsWatchtimeUrl =
                                    response.playbackTracking?.videostatsWatchtimeUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                cpn = triple.first,
                            ),
                        )
                        Logger.w("UpdateFormat", "Updated format for $videoId")
                    }.onFailure { throwable ->
                        Logger.e("UpdateFormat", "Error: ${throwable.message}")
                    }
            }
        }
    }

    override fun getStream(
        dataStoreManager: DataStoreManager,
        videoId: String,
        isVideo: Boolean,
    ): Flow<String?> =
        flow {
            val itag = QUALITY.itags.getOrNull(QUALITY.items.indexOf(dataStoreManager.quality.first()))
            val videoItag =
                VIDEO_QUALITY.itags.getOrNull(
                    VIDEO_QUALITY.items.indexOf(dataStoreManager.videoQuality.first()),
                )
                    ?: 134
            // 134, 136, 137
            youTube
                .player(videoId, shouldYtdlp = itag == 774)
                .onSuccess { data ->
                    val response = data.second
                    if (data.third == MediaType.Song) {
                        Logger.w(
                            "Stream",
                            "response: is SONG",
                        )
                    } else {
                        Logger.w("Stream", "response: is VIDEO")
                    }
                    Logger.w(
                        "Stream",
                        response.streamingData
                            ?.formats
                            ?.map { it.itag }
                            .toString() + " " +
                            response.streamingData
                                ?.adaptiveFormats
                                ?.map { it.itag }
                                .toString(),
                    )
                    val formatList = mutableListOf<PlayerResponse.StreamingData.Format>()
                    formatList.addAll(
                        response.streamingData?.formats?.filter { it.url.isNullOrEmpty().not() } ?: emptyList(),
                    )
                    formatList.addAll(
                        response.streamingData?.adaptiveFormats?.filter { it.url.isNullOrEmpty().not() }
                            ?: emptyList(),
                    )
                    Logger.w("Stream", "Get stream for video $isVideo")
                    val videoFormat =
                        formatList.find { it.itag == videoItag }
                            ?: formatList.find { it.itag == 136 }
                            ?: formatList.find { it.itag == 134 }
                            ?: formatList.find { !it.isAudio && it.url.isNullOrEmpty().not() }
                    val audioFormat =
                        formatList.find { it.itag == itag } ?: formatList.find { it.itag == 141 }
                            ?: formatList.find { it.isAudio && it.url.isNullOrEmpty().not() }
                    var format =
                        if (isVideo) {
                            videoFormat
                        } else {
                            audioFormat
                        }
                    if (format == null) {
                        format = formatList.lastOrNull { it.url.isNullOrEmpty().not() }
                    }
                    val superFormat =
                        formatList
                            .filter {
                                it.audioQuality == "AUDIO_QUALITY_HIGH"
                            }.let { highFormat ->
                                highFormat.firstOrNull {
                                    it.itag == 774 && it.url.isNullOrEmpty().not()
                                } ?: highFormat.firstOrNull {
                                    it.url.isNullOrEmpty().not()
                                }
                            }
                    if (!isVideo && superFormat != null) {
                        format = superFormat
                    }
                    Logger.w("Stream", "Super format: $superFormat")
                    Logger.w("Stream", "format: $format")
                    Logger.d("Stream", "expireInSeconds ${response.streamingData?.expiresInSeconds}")
                    Logger.w("Stream", "expired at ${LocalDateTime.now().plusSeconds(response.streamingData?.expiresInSeconds?.toLong() ?: 0L)}")
                    runBlocking {
                        insertNewFormat(
                            NewFormatEntity(
                                videoId = if (VIDEO_QUALITY.itags.contains(format?.itag)) "${MERGING_DATA_TYPE.VIDEO}$videoId" else videoId,
                                itag = format?.itag ?: itag ?: 141,
                                mimeType =
                                    Regex("""([^;]+);\s*codecs=["']([^"']+)["']""")
                                        .find(
                                            format?.mimeType ?: "",
                                        )?.groupValues
                                        ?.getOrNull(1) ?: format?.mimeType ?: "",
                                codecs =
                                    Regex("""([^;]+);\s*codecs=["']([^"']+)["']""")
                                        .find(
                                            format?.mimeType ?: "",
                                        )?.groupValues
                                        ?.getOrNull(2) ?: format?.mimeType ?: "",
                                bitrate = format?.bitrate,
                                sampleRate = format?.audioSampleRate,
                                contentLength = format?.contentLength,
                                loudnessDb =
                                    response.playerConfig
                                        ?.audioConfig
                                        ?.loudnessDb
                                        ?.toFloat(),
                                lengthSeconds = response.videoDetails?.lengthSeconds?.toInt(),
                                playbackTrackingVideostatsPlaybackUrl =
                                    response.playbackTracking?.videostatsPlaybackUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                playbackTrackingAtrUrl =
                                    response.playbackTracking?.atrUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                playbackTrackingVideostatsWatchtimeUrl =
                                    response.playbackTracking?.videostatsWatchtimeUrl?.baseUrl?.replace(
                                        "https://s.youtube.com",
                                        "https://music.youtube.com",
                                    ),
                                cpn = data.first,
                                expiredTime = LocalDateTime.now().plusSeconds(response.streamingData?.expiresInSeconds?.toLong() ?: 0L),
                                audioUrl = superFormat?.url ?: audioFormat?.url,
                                videoUrl = videoFormat?.url,
                            ),
                        )
                    }
                    if (data.first != null) {
                        emit(format?.url?.plus("&cpn=${data.first}&range=0-${format.contentLength ?: 10000000}"))
                    } else {
                        emit(format?.url?.plus("&range=0-${format.contentLength ?: 10000000}"))
                    }
                }.onFailure {
                    it.printStackTrace()
                    Logger.e("Stream", "Error: ${it.message}")
                    emit(null)
                }
        }.flowOn(Dispatchers.IO)

    override fun initPlayback(
        playback: String,
        atr: String,
        watchTime: String,
        cpn: String,
        playlistId: String?,
    ): Flow<Pair<Int, Float>> =
        flow {
            youTube
                .initPlayback(playback, atr, watchTime, cpn, playlistId)
                .onSuccess { response ->
                    emit(response)
                }.onFailure {
                    Logger.e("InitPlayback", "Error: ${it.message}")
                    emit(Pair(0, 0f))
                }
        }.flowOn(Dispatchers.IO)

    override fun updateWatchTimeFull(
        watchTime: String,
        cpn: String,
        playlistId: String?,
    ): Flow<Int> =
        flow {
            runCatching {
                youTube
                    .updateWatchTimeFull(watchTime, cpn, playlistId)
                    .onSuccess { response ->
                        emit(response)
                    }.onFailure {
                        it.printStackTrace()
                        emit(0)
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun updateWatchTime(
        playbackTrackingVideostatsWatchtimeUrl: String,
        watchTimeList: ArrayList<Float>,
        cpn: String,
        playlistId: String?,
    ): Flow<Int> =
        flow {
            runCatching {
                youTube
                    .updateWatchTime(
                        playbackTrackingVideostatsWatchtimeUrl,
                        watchTimeList,
                        cpn,
                        playlistId,
                    ).onSuccess { response ->
                        emit(response)
                    }.onFailure {
                        it.printStackTrace()
                        emit(0)
                    }
            }
        }.flowOn(Dispatchers.IO)

    override fun getSkipSegments(videoId: String): Flow<Resource<List<SponsorSkipSegments>>> =
        flow {
            youTube
                .getSkipSegments(videoId)
                .onSuccess { data ->
                    emit(Resource.Success(data.map { it.toSponsorSkipSegments() }))
                }.onFailure {
                    emit(Resource.Error(it.message ?: "Unknown error"))
                }
        }.flowOn(Dispatchers.IO)

    override fun getFullMetadata(videoId: String): Flow<Resource<Track>> =
        flow {
            Logger.w("getFullMetadata", "videoId: $videoId")
            youTube
                .getFullMetadata(videoId)
                .onSuccess {
                    emit(Resource.Success(it.toTrack()))
                }.onFailure {
                    Logger.e("getFullMetadata", "Error: ${it.message}")
                    emit(Resource.Error(it.message ?: "Unknown error"))
                }
        }.flowOn(Dispatchers.IO)

    override fun is403Url(url: String) = flow { emit(youTube.is403Url(url)) }.flowOn(Dispatchers.IO)
}