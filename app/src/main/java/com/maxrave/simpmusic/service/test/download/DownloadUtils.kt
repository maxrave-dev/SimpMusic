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
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.service.test.download.MusicDownloadService.Companion.CHANNEL_ID
import com.maxrave.simpmusic.service.test.source.MergingMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.Executors

@UnstableApi
class DownloadUtils (
    private val context: Context,
    private val playerCache: SimpleCache,
    private val downloadCache: SimpleCache,
    private val mainRepository: MainRepository,
    databaseProvider: DatabaseProvider
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val dataSourceFactory = ResolvingDataSource.Factory(
        CacheDataSource.Factory()
            .setCache(playerCache)
            .setCacheWriteDataSinkFactory(null)
            .setUpstreamDataSourceFactory(
                OkHttpDataSource.Factory(
                    OkHttpClient.Builder()
                        .build()
                )
            )
    ) { dataSpec ->
        val mediaId = dataSpec.key ?: error("No media id")
        Log.w("Stream", mediaId)
        Log.w("Stream", mediaId.startsWith(MergingMediaSourceFactory.isVideo).toString())
        val chunkLength = 512 * 1024L
        if (downloadCache.isCached(
                mediaId,
                dataSpec.position,
                if (dataSpec.length >= 0) dataSpec.length else 1
            ) || playerCache.isCached(mediaId, dataSpec.position, chunkLength)
        ) {
            return@Factory dataSpec
        }
        var dataSpecReturn: DataSpec = dataSpec
        runBlocking(Dispatchers.IO) {
            if (mediaId.contains(MergingMediaSourceFactory.isVideo)) {
                val id = mediaId.removePrefix(MergingMediaSourceFactory.isVideo)
                mainRepository.getStream(
                    id, true
                ).cancellable().collect {
                    if (it != null) {
                        dataSpecReturn = dataSpec.withUri(it.toUri())
                    }
                }
            }
            else {
                mainRepository.getStream(
                    mediaId, isVideo = false
                ).cancellable().collect {
                    if (it != null) {
                        dataSpecReturn = dataSpec.withUri(it.toUri())
                    }
                }
            }
        }
        return@Factory dataSpecReturn
    }
    val downloadNotificationHelper = DownloadNotificationHelper(context, CHANNEL_ID)
    val downloadManager: DownloadManager = DownloadManager(
        context,
        databaseProvider,
        downloadCache,
        dataSourceFactory,
        Executors.newFixedThreadPool(10)
    ).apply {
        maxParallelDownloads = 20
        minRetryCount = 3
        addListener(
            MusicDownloadService.TerminalStateNotificationHelper(
                context = context,
                notificationHelper = downloadNotificationHelper,
                nextNotificationId = MusicDownloadService.NOTIFICATION_ID + 1
            )
        )
    }
    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    val downloadingVideoIds = MutableStateFlow<MutableSet<String>>(mutableSetOf())

    fun downloadTrack(videoId: String, title: String) {
        val downloadRequest =
            DownloadRequest.Builder(videoId, videoId.toUri())
                .setData(title.toByteArray())
                .setCustomCacheKey(videoId)
                .build()
        DownloadService.sendAddDownload(
            context,
            MusicDownloadService::class.java,
            downloadRequest,
            false,
        )
    }

    fun removeDownload(videoId: String) {
        DownloadService.sendRemoveDownload(
            context,
            MusicDownloadService::class.java,
            videoId,
            false
        )
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    init {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
        downloadManager.addListener(
            object : DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?
                ) {
                    download.request.id.let { id ->
                        val songId = if (id.contains(MergingMediaSourceFactory.isVideo)) {
                            id.removePrefix(MergingMediaSourceFactory.isVideo)
                        } else {
                            id
                        }
                        when(download.state) {
                            Download.STATE_COMPLETED -> {
                                coroutineScope.launch {
                                    downloadingVideoIds.update {
                                        it.apply {
                                            remove(songId)
                                        }
                                    }
                                    mainRepository.updateDownloadState(songId, DownloadState.STATE_DOWNLOADED)
                                }
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

                            Download.STATE_FAILED -> {
                                coroutineScope.launch {
                                    downloadingVideoIds.update {
                                        it.apply {
                                            remove(songId)
                                        }
                                    }
                                    mainRepository.updateDownloadState(songId, DownloadState.STATE_NOT_DOWNLOADED)
                                }
                            }

                            Download.STATE_QUEUED -> {
                                coroutineScope.launch {
                                    downloadingVideoIds.update {
                                        it.apply {
                                            add(songId)
                                        }
                                    }
                                    mainRepository.updateDownloadState(songId, DownloadState.STATE_PREPARING)
                                }
                            }

                            Download.STATE_REMOVING -> {
                                coroutineScope.launch {
                                    downloadingVideoIds.update {
                                        it.apply {
                                            remove(songId)
                                        }
                                    }
                                    mainRepository.updateDownloadState(songId, DownloadState.STATE_NOT_DOWNLOADED)
                                }
                            }

                            Download.STATE_RESTARTING -> {
                                coroutineScope.launch {
                                    downloadingVideoIds.update {
                                        it.apply {
                                            add(songId)
                                        }
                                    }
                                    mainRepository.updateDownloadState(songId, DownloadState.STATE_DOWNLOADING)
                                }
                            }

                            Download.STATE_STOPPED -> {
                                coroutineScope.launch {
                                    downloadingVideoIds.update {
                                        it.apply {
                                            remove(songId)
                                        }
                                    }
                                    mainRepository.updateDownloadState(songId, DownloadState.STATE_NOT_DOWNLOADED)
                                }
                            }
                            else -> {

                            }
                        }
                    }
                    downloads.update { map ->
                        map.toMutableMap().apply {
                            val id = download.request.id
                            if (id.contains(MergingMediaSourceFactory.isVideo)) {
                                set(id.removePrefix(MergingMediaSourceFactory.isVideo), download)
                            }
                            else {
                                set(download.request.id, download)
                            }
                        }
                    }

                }
            }
        )
    }
}