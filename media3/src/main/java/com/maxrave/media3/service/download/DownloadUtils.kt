package com.maxrave.media3.service.download

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.repository.StreamRepository
import com.maxrave.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.util.concurrent.Executor

@UnstableApi
internal class DownloadUtils(
    private val context: Context,
    private val playerCache: SimpleCache,
    private val downloadCache: SimpleCache,
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
    private val songRepository: SongRepository,
    databaseProvider: DatabaseProvider,
) : DownloadHandler {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient
                            .Builder()
                            .build(),
                    ),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            Logger.w("Stream", mediaId)
            Logger.w("Stream", mediaId.startsWith(MERGING_DATA_TYPE.VIDEO).toString())
            val length = if (dataSpec.length >= 0) dataSpec.length else 1
            if (downloadCache.isCached(mediaId, dataSpec.position, length) || playerCache.isCached(mediaId, dataSpec.position, length)) {
                return@Factory dataSpec
            }
            var dataSpecReturn: DataSpec = dataSpec
            runBlocking(Dispatchers.IO) {
                if (mediaId.contains(MERGING_DATA_TYPE.VIDEO)) {
                    val id = mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
                    streamRepository.getNewFormat(id).lastOrNull()?.let {
                        val videoUrl = it.videoUrl
                        if (videoUrl != null && it.expiredTime > LocalDateTime.now()) {
                            Logger.d("Stream", videoUrl)
                            Logger.w("Stream", "Video from format")
                            val is403Url = streamRepository.is403Url(videoUrl).firstOrNull() != false
                            if (!is403Url) {
                                dataSpecReturn = dataSpec.withUri(videoUrl.toUri())
                                return@runBlocking
                            }
                        }
                    }
                    streamRepository
                        .getStream(
                            dataStoreManager,
                            id,
                            true,
                        ).lastOrNull()
                        ?.let {
                            dataSpecReturn = dataSpec.withUri(it.toUri())
                        }
                } else {
                    streamRepository.getNewFormat(mediaId).lastOrNull()?.let {
                        val audioUrl = it.audioUrl
                        if (audioUrl != null && it.expiredTime > LocalDateTime.now()) {
                            Logger.d("Stream", audioUrl)
                            Logger.w("Stream", "Audio from format")
                            val is403Url = streamRepository.is403Url(audioUrl).firstOrNull() != false
                            if (!is403Url) {
                                dataSpecReturn = dataSpec.withUri(audioUrl.toUri())
                                return@runBlocking
                            }
                        }
                    }
                    streamRepository
                        .getStream(
                            dataStoreManager,
                            mediaId,
                            isVideo = false,
                        ).lastOrNull()
                        ?.let {
                            dataSpecReturn = dataSpec.withUri(it.toUri())
                        }
                }
            }
            return@Factory dataSpecReturn
        }
    val downloadNotificationHelper =
        DownloadNotificationHelper(
            context,
            MusicDownloadService.Companion.CHANNEL_ID,
        )
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run),
        ).apply {
            maxParallelDownloads = 20
            minRetryCount = 3
            addListener(
                MusicDownloadService.TerminalStateNotificationHelper(
                    context = context,
                    notificationHelper = downloadNotificationHelper,
                    nextNotificationId = MusicDownloadService.NOTIFICATION_ID + 1,
                ),
            )
        }
    private var _downloads = MutableStateFlow<Map<String, Pair<DownloadHandler.Download?, DownloadHandler.Download?>>>(emptyMap())

    // Audio / Video
    override val downloads: StateFlow<Map<String, Pair<DownloadHandler.Download?, DownloadHandler.Download?>>>
        get() = _downloads
    private val _downloadTask = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val downloadTask: StateFlow<Map<String, Int>> get() = _downloadTask
    val downloadingVideoIds = MutableStateFlow<MutableSet<String>>(mutableSetOf())

    /**
     * Use thumbnail to check video or audio
     */
    override suspend fun downloadTrack(
        videoId: String,
        title: String,
        thumbnail: String,
    ) {
        var isVideo = false
        val request =
            ImageRequest
                .Builder(context)
                .data(thumbnail)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        val imageResult = ImageLoader(context).execute(request)
        if (imageResult.image?.height != imageResult.image?.width && imageResult.image != null) {
            isVideo = true
        }
        val downloadRequest =
            DownloadRequest
                .Builder(videoId, videoId.toUri())
                .setData(title.toByteArray())
                .setCustomCacheKey(videoId)
                .build()
        DownloadService.sendAddDownload(
            context,
            MusicDownloadService::class.java,
            downloadRequest,
            false,
        )
        if (isVideo) {
            val id = MERGING_DATA_TYPE.VIDEO + videoId
            val downloadRequestVideo =
                DownloadRequest
                    .Builder(id, id.toUri())
                    .setData("Video $title".toByteArray())
                    .setCustomCacheKey(id)
                    .build()
            DownloadService.sendAddDownload(
                context,
                MusicDownloadService::class.java,
                downloadRequestVideo,
                false,
            )
        }
    }

    override fun removeDownload(videoId: String) {
        DownloadService.sendRemoveDownload(
            context,
            MusicDownloadService::class.java,
            videoId,
            false,
        )
        val id = MERGING_DATA_TYPE.VIDEO + videoId
        DownloadService.sendRemoveDownload(
            context,
            MusicDownloadService::class.java,
            id,
            false,
        )
    }

    override fun removeAllDownloads() {
        _downloads.value = emptyMap()
        _downloadTask.value = emptyMap()
        downloadingVideoIds.value = mutableSetOf()
        downloadManager.removeAllDownloads()
    }

    init {
        coroutineScope.launch {
            downloads.collect { download ->
                download.forEach {
                    val videoId = it.key
                    val audio = it.value.first?.state
                    val video = it.value.second?.state
                    val combineState =
                        when (audio to video) {
                            Download.STATE_COMPLETED to Download.STATE_COMPLETED -> DownloadState.STATE_DOWNLOADED
                            Download.STATE_FAILED to Download.STATE_FAILED -> DownloadState.STATE_NOT_DOWNLOADED
                            Download.STATE_QUEUED to Download.STATE_QUEUED -> DownloadState.STATE_PREPARING
                            Download.STATE_COMPLETED to null -> DownloadState.STATE_DOWNLOADED
                            Download.STATE_FAILED to null -> DownloadState.STATE_NOT_DOWNLOADED
                            Download.STATE_QUEUED to null -> DownloadState.STATE_PREPARING
                            null to Download.STATE_COMPLETED -> DownloadState.STATE_DOWNLOADING
                            null to Download.STATE_QUEUED -> DownloadState.STATE_PREPARING
                            null to Download.STATE_FAILED -> DownloadState.STATE_NOT_DOWNLOADED
                            else -> DownloadState.STATE_DOWNLOADING
                        }
                    _downloadTask.update {
                        it.toMutableMap().apply {
                            set(videoId, combineState)
                        }
                    }
                    when (combineState) {
                        DownloadState.STATE_DOWNLOADED -> {
                            downloadingVideoIds.update {
                                it.apply {
                                    remove(videoId)
                                }
                            }
                            songRepository.updateDownloadState(videoId, DownloadState.STATE_DOWNLOADED)
                        }
                        DownloadState.STATE_DOWNLOADING -> {
                            songRepository.updateDownloadState(videoId, DownloadState.STATE_DOWNLOADING)
                        }
                        DownloadState.STATE_NOT_DOWNLOADED -> {
                            downloadingVideoIds.update {
                                it.apply {
                                    remove(videoId)
                                }
                            }
                            songRepository.updateDownloadState(videoId, DownloadState.STATE_NOT_DOWNLOADED)
                        }
                        DownloadState.STATE_PREPARING -> {
                            songRepository.updateDownloadState(videoId, DownloadState.STATE_PREPARING)
                        }
                    }
                }
            }
        }

        // Pair Audio and Video
        val result = mutableMapOf<String, Pair<DownloadHandler.Download?, DownloadHandler.Download?>>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            val id = cursor.download.request.id
            val isVideo = id.contains(MERGING_DATA_TYPE.VIDEO)
            val songId =
                if (id.contains(MERGING_DATA_TYPE.VIDEO)) {
                    id.removePrefix(MERGING_DATA_TYPE.VIDEO)
                } else {
                    id
                }
            result[songId] =
                if (isVideo) {
                    result[songId]?.copy(second = DownloadHandler.Download(cursor.download.state))
                        ?: Pair(null, DownloadHandler.Download(cursor.download.state))
                } else {
                    result[songId]?.copy(first = DownloadHandler.Download(cursor.download.state))
                        ?: Pair(DownloadHandler.Download(cursor.download.state), null)
                }
        }
        _downloads.value = result
        downloadManager.addListener(
            object : DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?,
                ) {
                    download.request.id.let { id ->
                        var isVideo = false
                        val songId =
                            if (id.contains(MERGING_DATA_TYPE.VIDEO)) {
                                isVideo = true
                                id.removePrefix(MERGING_DATA_TYPE.VIDEO)
                            } else {
                                id
                            }
                        _downloads.update { map ->
                            map.toMutableMap().apply {
                                val current = map.getOrDefault(songId, null)
                                if (isVideo) {
                                    set(
                                        songId,
                                        current?.copy(second = DownloadHandler.Download(download.state))
                                            ?: Pair(null, DownloadHandler.Download(download.state)),
                                    )
                                } else {
                                    set(
                                        songId,
                                        current?.copy(first = DownloadHandler.Download(download.state))
                                            ?: Pair(DownloadHandler.Download(download.state), null),
                                    )
                                }
                            }
                        }
                        when (download.state) {
                            Download.STATE_COMPLETED -> {
                                playerCache.removeResource(id)
                            }

                            Download.STATE_DOWNLOADING -> {
                                coroutineScope.launch {
                                    downloadingVideoIds.update {
                                        it.apply {
                                            add(songId)
                                        }
                                    }
                                    songRepository.updateDownloadState(songId, DownloadState.STATE_DOWNLOADING)
                                }
                            }
                            else -> {
                            }
                        }
                    }
                }
            },
        )
    }
}