package com.maxrave.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.common.QUALITY
import com.maxrave.common.StreamHealthRegistry
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

internal class StreamRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
    context: Context,
) : StreamRepository {
    private val isWatchDevice = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)

    override suspend fun insertNewFormat(newFormat: NewFormatEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertNewFormat(newFormat)
        }

    override fun getNewFormat(videoId: String): Flow<NewFormatEntity?> =
        flow { emit(localDataSource.getNewFormat(videoId)) }
            .flowOn(Dispatchers.IO)

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
            try {
                val usedAccount = localDataSource.getUsedGoogleAccount()
                val dataStoreCookie = dataStoreManager.cookie.first()
                if (dataStoreCookie.isNotBlank()) {
                    // Keep YouTube client in sync even if CommonRepository collector hasn't run yet.
                    youTube.cookie = dataStoreCookie
                } else {
                    val fallbackCookie = usedAccount?.cache.orEmpty()
                    if (fallbackCookie.isNotBlank()) {
                        Logger.w("Stream", "Cookie missing in DataStore, restoring from used account")
                        dataStoreManager.setCookie(fallbackCookie, usedAccount?.pageId)
                        dataStoreManager.setLoggedIn(true)
                        youTube.cookie = fallbackCookie
                    }
                }

                val data =
                    runCatching { youTube.player(videoId, shouldYtdlp = itag == 774).getOrThrow() }
                        .recoverCatching { firstError ->
                            val isLoginRequired = firstError.message?.contains("LOGIN_REQUIRED", ignoreCase = true) == true
                            val fallbackCookie = usedAccount?.cache.orEmpty()
                            if (isLoginRequired && fallbackCookie.isNotBlank() && fallbackCookie != youTube.cookie) {
                                Logger.w("Stream", "LOGIN_REQUIRED, retrying with stored account cookie")
                                dataStoreManager.setCookie(fallbackCookie, usedAccount?.pageId)
                                dataStoreManager.setLoggedIn(true)
                                youTube.cookie = fallbackCookie
                                youTube.player(videoId, shouldYtdlp = itag == 774).getOrThrow()
                            } else {
                                throw firstError
                            }
                        }.getOrThrow()
                val response = data.second

                if (data.third == MediaType.Song) {
                    Logger.w("Stream", "response: is SONG")
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
                    response.streamingData?.adaptiveFormats?.filter { it.url.isNullOrEmpty().not() } ?: emptyList(),
                )

                Logger.w("Stream", "Get stream for video $isVideo")
                val normalizedMediaId = videoId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                val videoFormats = formatList.filter { !it.isAudio && it.url.isNullOrEmpty().not() }
                val audioFormats = formatList.filter { it.isAudio && it.url.isNullOrEmpty().not() }
                val preferredAudioFormat =
                    if (isWatchDevice) {
                        // On watch, prefer lower-bitrate Opus first; AAC(140) is kept as fallback.
                        // This has shown better reliability for signed googlevideo URLs.
                        audioFormats.find { it.itag == 251 }
                            ?: audioFormats.find { it.itag == 250 }
                            ?: audioFormats.find { it.itag == 249 }
                            ?: audioFormats.find { it.itag == 140 }
                            ?: audioFormats.find { it.itag == 139 }
                            ?: audioFormats.minByOrNull { it.bitrate ?: Int.MAX_VALUE }
                    } else {
                        formatList.find { it.itag == itag }
                            ?: formatList.find { it.itag == 141 }
                            ?: audioFormats.firstOrNull()
                    }

                val superFormat =
                    formatList
                        .filter { it.audioQuality == "AUDIO_QUALITY_HIGH" }
                        .let { highFormat ->
                            highFormat.firstOrNull { it.itag == 774 && it.url.isNullOrEmpty().not() }
                                ?: highFormat.firstOrNull { it.url.isNullOrEmpty().not() }
                        }

                fun rankByHealth(
                    candidates: List<PlayerResponse.StreamingData.Format>,
                ): List<PlayerResponse.StreamingData.Format> =
                    candidates
                        .distinctBy { it.url }
                        .mapIndexed { index, candidate ->
                            Triple(
                                index,
                                candidate,
                                StreamHealthRegistry.candidatePenalty(
                                    normalizedMediaId,
                                    candidate.itag,
                                    candidate.url,
                                ),
                            )
                        }.sortedWith(compareBy<Triple<Int, PlayerResponse.StreamingData.Format, Int>> { it.third }.thenBy { it.first })
                        .map { it.second }

                fun isPoisonedCandidate(candidate: PlayerResponse.StreamingData.Format): Boolean =
                    StreamHealthRegistry.isUrlBlocked(candidate.url) ||
                        StreamHealthRegistry.isClientBlockedByUrl(normalizedMediaId, candidate.url)

                suspend fun pickPlayableAudioFormat(): PlayerResponse.StreamingData.Format? {
                    val firstAdaptiveAudio =
                        response.streamingData
                            ?.adaptiveFormats
                            ?.firstOrNull { it.isAudio && it.url.isNullOrBlank().not() }
                    val orderedCandidates =
                        if (isWatchDevice) {
                            val byBitrate = audioFormats.sortedBy { it.bitrate ?: Int.MAX_VALUE }
                            val watchPriorityItags = listOf(251, 250, 249, 140, 139)
                            val byWatchPriority =
                                watchPriorityItags.mapNotNull { targetItag ->
                                    audioFormats.firstOrNull { it.itag == targetItag }
                                }
                            listOfNotNull(
                                preferredAudioFormat,
                                firstAdaptiveAudio,
                                superFormat,
                            ) + byWatchPriority + byBitrate
                        } else {
                            listOfNotNull(
                                preferredAudioFormat,
                                firstAdaptiveAudio,
                                formatList.find { it.itag == 141 },
                                superFormat,
                            ) + audioFormats
                        }
                    val rankedCandidates = rankByHealth(orderedCandidates)
                    val maxChecks = if (isWatchDevice) 8 else 6
                    val checkTimeoutMs = if (isWatchDevice) 3_000L else 4_000L
                    var fallbackUnchecked: PlayerResponse.StreamingData.Format? = null
                    var fallbackAny: PlayerResponse.StreamingData.Format? = null

                    for ((index, candidate) in rankedCandidates.withIndex()) {
                        if (fallbackAny == null) {
                            fallbackAny = candidate
                        }
                        if (index >= maxChecks) {
                            if (fallbackUnchecked == null && !isPoisonedCandidate(candidate)) {
                                fallbackUnchecked = candidate
                            }
                            continue
                        }
                        val candidateUrl = candidate.url ?: continue
                        StreamHealthRegistry.rememberCandidate(normalizedMediaId, candidate.itag, candidateUrl)
                        if (isPoisonedCandidate(candidate)) {
                            Logger.w(
                                "Stream",
                                "Skipping poisoned audio candidate itag=${candidate.itag} c=${StreamHealthRegistry.urlClient(candidate.url)}",
                            )
                            continue
                        }
                        val blocked =
                            withTimeoutOrNull(checkTimeoutMs) {
                                runCatching {
                                    !youTube.preflightGoogleVideoOrTrue(
                                        url = candidateUrl,
                                        mediaId = normalizedMediaId,
                                        sourceClientName = StreamHealthRegistry.urlClient(candidateUrl),
                                    )
                                }
                                    .onFailure { Logger.w("Stream", "Audio candidate check failed: ${it.message}") }
                                    .getOrDefault(false)
                            } ?: false
                        if (!blocked) {
                            StreamHealthRegistry.markUrlHealthy(candidateUrl)
                            return candidate
                        }
                        StreamHealthRegistry.markUrlFailure(candidateUrl)
                        Logger.w("Stream", "Skipping blocked audio candidate itag=${candidate.itag}")
                    }

                    if (fallbackUnchecked != null) {
                        Logger.w(
                            "Stream",
                            "All checked audio candidates blocked, trying unchecked itag=${fallbackUnchecked?.itag}",
                        )
                        return fallbackUnchecked
                    }

                    if (fallbackAny != null) {
                        Logger.w(
                            "Stream",
                            "Forcing retry with least-penalized audio candidate itag=${fallbackAny?.itag}",
                        )
                        StreamHealthRegistry.markUrlHealthy(fallbackAny?.url)
                        return fallbackAny
                    }

                    Logger.w("Stream", "No playable audio candidate found")
                    return null
                }

                suspend fun pickPlayableVideoFormat(): PlayerResponse.StreamingData.Format? {
                    val orderedVideoCandidates =
                        listOfNotNull(
                            videoFormats.find { it.itag == videoItag },
                            videoFormats.find { it.itag == 136 },
                            videoFormats.find { it.itag == 134 },
                        ) + videoFormats
                    val rankedCandidates = rankByHealth(orderedVideoCandidates)
                    val maxChecks = if (isWatchDevice) 5 else 4
                    val checkTimeoutMs = if (isWatchDevice) 3_000L else 4_000L

                    for ((index, candidate) in rankedCandidates.withIndex()) {
                        if (index >= maxChecks) break
                        val candidateUrl = candidate.url ?: continue
                        StreamHealthRegistry.rememberCandidate(normalizedMediaId, candidate.itag, candidateUrl)
                        if (isPoisonedCandidate(candidate)) {
                            Logger.w(
                                "Stream",
                                "Skipping poisoned video candidate itag=${candidate.itag} c=${StreamHealthRegistry.urlClient(candidate.url)}",
                            )
                            continue
                        }
                        val blocked =
                            withTimeoutOrNull(checkTimeoutMs) {
                                runCatching {
                                    !youTube.preflightGoogleVideoOrTrue(
                                        url = candidateUrl,
                                        mediaId = normalizedMediaId,
                                        sourceClientName = StreamHealthRegistry.urlClient(candidateUrl),
                                    )
                                }
                                    .onFailure { Logger.w("Stream", "Video candidate check failed: ${it.message}") }
                                    .getOrDefault(false)
                            } ?: false
                        if (!blocked) {
                            StreamHealthRegistry.markUrlHealthy(candidateUrl)
                            return candidate
                        }
                        StreamHealthRegistry.markUrlFailure(candidateUrl)
                    }
                    return rankedCandidates.firstOrNull { !isPoisonedCandidate(it) }
                        ?: rankedCandidates.firstOrNull()?.also { StreamHealthRegistry.markUrlHealthy(it.url) }
                }

                val videoFormat = if (isVideo) pickPlayableVideoFormat() else videoFormats.firstOrNull()
                val selectedAudioFormat = if (isVideo) null else pickPlayableAudioFormat()
                val fallbackAudioFormat =
                    rankByHealth(audioFormats)
                        .firstOrNull { candidate ->
                            candidate.url != preferredAudioFormat?.url && !isPoisonedCandidate(candidate)
                        } ?: preferredAudioFormat?.takeIf { !isPoisonedCandidate(it) }

                if (isVideo && videoFormat == null) {
                    Logger.w("Stream", "No playable video candidate found; aborting stream resolve")
                    StreamHealthRegistry.markMediaFailure(normalizedMediaId)
                    emit(null)
                    return@flow
                }
                if (!isVideo && selectedAudioFormat == null && fallbackAudioFormat == null) {
                    Logger.w("Stream", "All audio candidates are poisoned; aborting stream resolve")
                    StreamHealthRegistry.markMediaFailure(normalizedMediaId)
                    emit(null)
                    return@flow
                }

                var format =
                    if (isVideo) {
                        videoFormat
                    } else {
                        selectedAudioFormat ?: fallbackAudioFormat
                    }
                if (format == null) {
                    format = rankByHealth(formatList).firstOrNull { !isPoisonedCandidate(it) }
                }
                if (format == null) {
                    Logger.w("Stream", "No safe fallback format available; aborting stream resolve")
                    StreamHealthRegistry.markMediaFailure(normalizedMediaId)
                    emit(null)
                    return@flow
                }
                StreamHealthRegistry.rememberCandidate(normalizedMediaId, format?.itag, format?.url)
                StreamHealthRegistry.rememberCandidate(normalizedMediaId, videoFormat?.itag, videoFormat?.url)

                Logger.w("Stream", "Super format: $superFormat")
                Logger.w("Stream", "format: $format")
                Logger.d("Stream", "expireInSeconds ${response.streamingData?.expiresInSeconds}")
                Logger.w(
                    "Stream",
                    "expired at ${LocalDateTime.now().plusSeconds(response.streamingData?.expiresInSeconds?.toLong() ?: 0L)}",
                )

                insertNewFormat(
                    NewFormatEntity(
                        videoId = if (VIDEO_QUALITY.itags.contains(format?.itag)) "${MERGING_DATA_TYPE.VIDEO}$videoId" else videoId,
                        itag = format?.itag ?: itag ?: 141,
                        mimeType =
                            Regex("""([^;]+);\s*codecs=["']([^"']+)["']""")
                                .find(format?.mimeType ?: "")
                                ?.groupValues
                                ?.getOrNull(1)
                                ?: format?.mimeType
                                ?: "",
                        codecs =
                            Regex("""([^;]+);\s*codecs=["']([^"']+)["']""")
                                .find(format?.mimeType ?: "")
                                ?.groupValues
                                ?.getOrNull(2)
                                ?: format?.mimeType
                                ?: "",
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
                        audioUrl = format?.url ?: selectedAudioFormat?.url ?: preferredAudioFormat?.url ?: superFormat?.url,
                        videoUrl = videoFormat?.url,
                    ),
                )

                // Do not append extra query parameters (e.g. `range`, `cpn`) here.
                // The videoplayback URL is already signed; mutating it can cause 403.
                emit(format?.url)
            } catch (t: Throwable) {
                t.printStackTrace()
                Logger.e("Stream", "Error: ${t.message}")
                StreamHealthRegistry.markMediaFailure(videoId)
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
