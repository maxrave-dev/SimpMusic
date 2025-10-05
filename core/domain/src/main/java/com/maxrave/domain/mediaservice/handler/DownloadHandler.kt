package com.maxrave.domain.mediaservice.handler

import kotlinx.coroutines.flow.StateFlow

interface DownloadHandler {
    suspend fun downloadTrack(
        videoId: String,
        title: String,
        thumbnail: String,
    )

    fun removeDownload(videoId: String)

    fun removeAllDownloads()

    val downloads: StateFlow<Map<String, Pair<Download?, Download?>>>

    val downloadTask: StateFlow<Map<String, Int>>

    /**
     * Copy from Media3
     */
    companion object State {
        const val STATE_QUEUED: Int = 0

        /** The download is stopped for a specified [.stopReason].  */
        const val STATE_STOPPED: Int = 1

        /** The download is currently started.  */
        const val STATE_DOWNLOADING: Int = 2

        /** The download completed.  */
        const val STATE_COMPLETED: Int = 3

        /** The download failed.  */
        const val STATE_FAILED: Int = 4

        /** The download is being removed.  */
        const val STATE_REMOVING: Int = 5

        /** The download will restart after all downloaded data is removed.  */
        const val STATE_RESTARTING: Int = 7
    }

    data class Download(
        val state: Int,
    )
}