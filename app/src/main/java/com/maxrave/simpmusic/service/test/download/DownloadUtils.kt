package com.maxrave.simpmusic.service.test.download

import android.content.Context
import android.util.Log
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
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.service.test.download.MusicDownloadService.Companion.CHANNEL_ID
import com.maxrave.simpmusic.service.test.source.MergingMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.util.concurrent.Executor

@UnstableApi
class DownloadUtils(
    private val context: Context,
    private val playerCache: SimpleCache,
    private val downloadCache: SimpleCache,
    private val mainRepository: MainRepository,
    databaseProvider: DatabaseProvider,
) {
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
            Log.w("Stream", mediaId)
            Log.w("Stream", mediaId.startsWith(MergingMediaSourceFactory.isVideo).toString())
            val length = if (dataSpec.length >= 0) dataSpec.length else 1
            if (downloadCache.isCached(mediaId, dataSpec.position, length) || playerCache.isCached(mediaId, dataSpec.position, length)
            ) {
                return@Factory dataSpec
            }
            var dataSpecReturn: DataSpec = dataSpec
            runBlocking(Dispatchers.IO) {
                if (mediaId.contains(MergingMediaSourceFactory.isVideo)) {
                    val id = mediaId.removePrefix(MergingMediaSourceFactory.isVideo)
                    mainRepository.getNewFormat(id).firstOrNull()?.let {
                        if (it.videoUrl != null && it.expiredTime > LocalDateTime.now()) {
                            Log.d("Stream", it.videoUrl)
                            Log.w("Stream", "Video from format")
                            val is403Url = mainRepository.is403Url(it.videoUrl).firstOrNull() != false
                            if (!is403Url) {
                                dataSpecReturn = dataSpec.withUri(it.videoUrl.toUri())
                                return@runBlocking
                            }
                        }
                    }
                    mainRepository
                        .getStream(
                            id,
                            true,
                        ).singleOrNull()
                        ?.let {
                            dataSpecReturn = dataSpec.withUri(it.toUri())
                        }
                } else {
                    mainRepository.getNewFormat(mediaId).firstOrNull()?.let {
                        if (it.audioUrl != null && it.expiredTime > LocalDateTime.now()) {
                            Log.d("Stream", it.audioUrl)
                            Log.w("Stream", "Audio from format")
                            val is403Url = mainRepository.is403Url(it.audioUrl).firstOrNull() != false
                            if (!is403Url) {
                                dataSpecReturn = dataSpec.withUri(it.audioUrl.toUri())
                                return@runBlocking
                            }
                        }
                    }
                    mainRepository
                        .getStream(
                            mediaId,
                            isVideo = false,
                        ).singleOrNull()
                        ?.let {
                            dataSpecReturn = dataSpec.withUri(it.toUri())
                        }
                }
            }
            return@Factory dataSpecReturn
        }
    val downloadNotificationHelper = DownloadNotificationHelper(context, CHANNEL_ID)
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
    private var _downloads = MutableStateFlow<Map<String, Pair<Download?, Download?>>>(emptyMap())

    // Audio / Video
    val downloads: StateFlow<Map<String, Pair<Download?, Download?>>>
        get() = _downloads
    private val _downloadTask = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadTask: StateFlow<Map<String, Int>> get() = _downloadTask
    val downloadingVideoIds = MutableStateFlow<MutableSet<String>>(mutableSetOf())

    /**
     * Use thumbnail to check video or audio
     */
    suspend fun downloadTrack(
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
            val id = MergingMediaSourceFactory.isVideo + videoId
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

    fun removeDownload(videoId: String) {
        DownloadService.sendRemoveDownload(
            context,
            MusicDownloadService::class.java,
            videoId,
            false,
        )
        val id = MergingMediaSourceFactory.isVideo + videoId
        DownloadService.sendRemoveDownload(
            context,
            MusicDownloadService::class.java,
            id,
            false,
        )
    }

    fun removeAllDownloads() {
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
                            mainRepository.updateDownloadState(videoId, DownloadState.STATE_DOWNLOADED)
                        }
                        DownloadState.STATE_DOWNLOADING -> {
                            mainRepository.updateDownloadState(videoId, DownloadState.STATE_DOWNLOADING)
                        }
                        DownloadState.STATE_NOT_DOWNLOADED -> {
                            downloadingVideoIds.update {
                                it.apply {
                                    remove(videoId)
                                }
                            }
                            mainRepository.updateDownloadState(videoId, DownloadState.STATE_NOT_DOWNLOADED)
                        }
                        DownloadState.STATE_PREPARING -> {
                            mainRepository.updateDownloadState(videoId, DownloadState.STATE_PREPARING)
                        }
                    }
                }
            }
        }

        // Pair Audio and Video
        val result = mutableMapOf<String, Pair<Download?, Download?>>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            val id = cursor.download.request.id
            val isVideo = id.contains(MergingMediaSourceFactory.isVideo)
            val songId =
                if (id.contains(MergingMediaSourceFactory.isVideo)) {
                    id.removePrefix(MergingMediaSourceFactory.isVideo)
                } else {
                    id
                }
            result[songId] =
                if (isVideo) {
                    result[songId]?.copy(second = cursor.download) ?: Pair(null, cursor.download)
                } else {
                    result[songId]?.copy(first = cursor.download) ?: Pair(cursor.download, null)
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
                            if (id.contains(MergingMediaSourceFactory.isVideo)) {
                                isVideo = true
                                id.removePrefix(MergingMediaSourceFactory.isVideo)
                            } else {
                                id
                            }
                        _downloads.update { map ->
                            map.toMutableMap().apply {
                                val current = map.getOrDefault(songId, null)
                                if (isVideo) {
                                    set(songId, current?.copy(second = download) ?: Pair(null, download))
                                } else {
                                    set(songId, current?.copy(first = download) ?: Pair(download, null))
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
                                    mainRepository.updateDownloadState(songId, DownloadState.STATE_DOWNLOADING)
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