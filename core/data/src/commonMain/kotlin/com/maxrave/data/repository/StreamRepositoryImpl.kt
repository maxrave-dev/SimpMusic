package com.maxrave.data.repository

import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.common.QUALITY
import com.maxrave.common.VIDEO_QUALITY
import com.maxrave.data.db.datasource.LocalDataSource
import com.maxrave.data.mapping.toSponsorSkipSegments
import com.maxrave.data.mapping.toTrack
import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.mediaService.SponsorSkipSegments
import com.maxrave.domain.extension.isBefore
import com.maxrave.domain.extension.now
import com.maxrave.domain.extension.plusSeconds
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.MediaType
import com.maxrave.kotlinytmusicscraper.models.response.PlayerResponse
import com.maxrave.kotlinytmusicscraper.utils.decodeBase64
import com.maxrave.kotlinytmusicscraper.utils.decodeTidalManifest
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

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
            Logger.w("Stream", "now: ${now()}")
            Logger.w("Stream", "isExpired: ${oldFormat.expiredTime.isBefore(now())}")
            if (oldFormat.expiredTime.isBefore(now())) {
                youTube
                    .player(videoId)
                    .onSuccess { triple ->
                        val response = triple.second
                        localDataSource.updateNewFormat(
                            oldFormat.copy(
                                expiredTime = now().plusSeconds(response.streamingData?.expiresInSeconds?.toLong() ?: 0L),
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
        isDownloading: Boolean,
        isVideo: Boolean,
        muxed: Boolean,
    ): Flow<String?> =
        flow {
            val forceVideo = dataStoreManager.watchVideoInsteadOfPlayingAudio.first() == DataStoreManager.TRUE
            val actualIsVideo = isVideo || forceVideo

            val itag =
                if (isDownloading) {
                    QUALITY.itags.getOrNull(QUALITY.items.indexOf(dataStoreManager.downloadQuality.first()))
                } else {
                    QUALITY.itags.getOrNull(QUALITY.items.indexOf(dataStoreManager.quality.first()))
                }

            val qualityIndex = if (isDownloading) {
                VIDEO_QUALITY.items.indexOf(dataStoreManager.videoDownloadQuality.first())
            } else {
                VIDEO_QUALITY.items.indexOf(dataStoreManager.videoQuality.first())
            }

            val videoItag =
                if (!muxed) {
                    VIDEO_QUALITY.itags.getOrNull(qualityIndex) ?: 134
                } else {
                    18
                }

            youTube
                .player(videoId, noLogIn = muxed)
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
                    
                    Logger.w("Stream", "Get stream for video $actualIsVideo")
                    
                    var videoFormat = formatList.find { it.itag == videoItag }
                    
                    if (videoFormat == null && !muxed && qualityIndex > 0) {
                        for (i in qualityIndex - 1 downTo 0) {
                            val fallbackItag = VIDEO_QUALITY.itags.getOrNull(i)
                            videoFormat = formatList.find { it.itag == fallbackItag }
                            if (videoFormat != null) break
                        }
                    }
                    
                    if (videoFormat == null) {
                        videoFormat = formatList.find { !it.isAudio && it.url.isNullOrEmpty().not() }
                    }

                    val audioFormat =
                        formatList.find { it.itag == itag } ?: formatList.find { it.itag == 141 }
                            ?: formatList.find { it.isAudio && it.url.isNullOrEmpty().not() }

                    var format =
                        if (actualIsVideo) {
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

                    if (!actualIsVideo && superFormat != null) {
                        format = superFormat
                    }

                    if (muxed) {
                        format = formatList
                            .filter {
                                val url = it.url
                                url != null && youTube.isManifestUrl(url)
                            }.maxByOrNull { it.width ?: 0 } ?: formatList.find { it.itag == videoItag }
                    }

                    Logger.w("Stream", "Selected hls ${response.streamingData?.hlsManifestUrl}")
                    Logger.w("Stream", "Super format: $superFormat")
                    Logger.w("Stream", "format: $format")
                    Logger.d("Stream", "expireInSeconds ${response.streamingData?.expiresInSeconds}")
                    Logger.w("Stream", "expired at ${now().plusSeconds(response.streamingData?.expiresInSeconds?.toLong() ?: 0L)}")
                    
                    val prefer320kbps = dataStoreManager.prefer320kbpsStream.first() == DataStoreManager.TRUE
                    val durationSecond = response.videoDetails?.lengthSeconds?.toIntOrNull()

                    var tidalBpm: Int? = null
                    var tidalMusicKey: String? = null
                    var tidalKeyScale: String? = null

                    if (prefer320kbps && !actualIsVideo && durationSecond != null && data.third == MediaType.Song) {
                        val your320kbpsUrl = dataStoreManager.your320kbpsUrl.first()
                        Logger.d("Stream", "Prefer 320kbps enabled ${response.videoDetails}")
                        val title = response.videoDetails?.title ?: ""
                        val author = response.videoDetails?.author ?: ""
                        val q =
                            "$title $author"
                                .replace(
                                    Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                                    " ",
                                ).replace(
                                    Regex("( và | & | и | e | und |, |和| dan)"),
                                    " ",
                                ).replace("  ", " ")
                                .replace(Regex("([()])"), "")
                                .replace(".", " ")
                                .replace("  ", " ")
                        Logger.d("Stream", "Search query for 320kbps: $q")
                        
                        val tidalResult =
                            youTube
                                .getTidalStream(your320kbpsUrl, q, durationSecond)
                                .apply {
                                    onSuccess {
                                        Logger.w("Stream", "Tidal response: $this")
                                    }.onFailure {
                                        Logger.e("Stream", "Tidal error: ${it.message}", it)
                                    }
                                }.getOrNull()

                        tidalBpm = tidalResult?.bpm
                        tidalMusicKey = tidalResult?.musicKey
                        tidalKeyScale = tidalResult?.keyScale
                        
                        val audioData =
                            tidalResult
                                ?.stream
                                ?.data
                                ?.manifest
                                ?.decodeTidalManifest()
                                
                        if (audioData != null) {
                            Logger.d("Stream", "Found potential 320kbps stream from Tidal: $tidalResult")
                            format =
                                format?.copy(
                                    itag = 0,
                                    url = audioData.urls.firstOrNull() ?: format.url,
                                    mimeType = "${audioData.mimeType}; codecs=\"${audioData.codecs}\"",
                                    bitrate = 320000,
                                )
                        } else if (tidalResult
                                ?.stream
                                ?.data
                                ?.manifest
                                ?.decodeBase64()
                                ?.contains("MPD") == true
                        ) {
                            Logger.d("Stream", "Found potential 320kbps stream from Tidal manifest DASH: ${tidalResult.stream.data?.manifest}")
                            format =
                                format?.copy(
                                    itag = 0,
                                    url =
                                        tidalResult.stream.data
                                            ?.manifest
                                            ?.decodeBase64(),
                                    bitrate = 320000,
                                )
                        }
                    } else if (!actualIsVideo && durationSecond != null && data.third == MediaType.Song) {
                        val your320kbpsUrl = dataStoreManager.your320kbpsUrl.first()
                        val title = response.videoDetails?.title ?: ""
                        val author = response.videoDetails?.author ?: ""
                        val q =
                            "$title $author"
                                .replace(
                                    Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                                    " ",
                                ).replace(
                                    Regex("( và | & | и | e | und |, |和| dan)"),
                                    " ",
                                ).replace("  ", " ")
                                .replace(Regex("([()])"), "")
                                .replace(".", " ")
                                .replace("  ", " ")
                        Logger.d("Stream", "Search Tidal metadata for: $q")
                        youTube
                            .searchTidalMetadata(your320kbpsUrl, q, durationSecond)
                            .onSuccess { metadata ->
                                Logger.w("Stream", "Tidal metadata: $metadata")
                                tidalBpm = metadata.bpm
                                tidalMusicKey = metadata.musicKey
                                tidalKeyScale = metadata.keyScale
                            }.onFailure {
                                Logger.e("Stream", "Tidal metadata error: ${it.message}", it)
                            }
                    }

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
                            expiredTime = now().plusSeconds(response.streamingData?.expiresInSeconds?.toLong() ?: 0L),
                            audioUrl = if (muxed) response.streamingData?.hlsManifestUrl else format?.url,
                            videoUrl = if (muxed) response.streamingData?.hlsManifestUrl else videoFormat?.url,
                            bpm = tidalBpm,
                            musicKey = tidalMusicKey,
                            keyScale = tidalKeyScale,
                        ),
                    )

                    if (data.first != null) {
                        emit(
                            if (prefer320kbps && !actualIsVideo) {
                                format?.url
                            } else if (muxed) {
                                response.streamingData?.hlsManifestUrl
                            } else {
                                format?.url?.let { url ->
                                    if (youTube.isManifestUrl(url)) {
                                        url.plus("&cpn=${data.first}")
                                    } else {
                                        url.plus("&cpn=${data.first}&range=0-${format.contentLength ?: 10000000}")
                                    }
                                }
                            },
                        )
                    } else {
                        emit(
                            if (prefer320kbps && !actualIsVideo) {
                                format?.url
                            } else if (muxed) {
                                response.streamingData?.hlsManifestUrl
                            } else {
                                format?.url?.let { url ->
                                    if (youTube.isManifestUrl(url)) {
                                        url
                                    } else {
                                        url.plus("&range=0-${format.contentLength ?: 10000000}")
                                    }
                                }
                            },
                        )
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

    override suspend fun invalidateFormat(videoId: String) {
        withContext(Dispatchers.IO) {
            localDataSource.getNewFormat(videoId)?.let { format ->
                Logger.d("Stream", "Invalidating cached format for $videoId")
                localDataSource.updateNewFormat(
                    format.copy(expiredTime = now().plusSeconds(-1)),
                )
            }
        }
    }
}