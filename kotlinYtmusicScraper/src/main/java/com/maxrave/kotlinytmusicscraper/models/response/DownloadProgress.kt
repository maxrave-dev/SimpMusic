package com.maxrave.kotlinytmusicscraper.models.response

data class DownloadProgress(
    val audioDownloadProgress: Float = 0f, // 0.0 - 1.0
    val videoDownloadProgress: Float = 0f, // 0.0 - 1.0
    val isMerging: Boolean = false,
    val isError: Boolean = false,
    val isDone: Boolean = false,
) {
    companion object {
        val FAILED = DownloadProgress(0f, 0f, false, true, false)
        val AUDIO_DONE = DownloadProgress(1f, 0f, false, false, true)
        val VIDEO_DONE = DownloadProgress(1f, 1f, false, false, true)
        val MERGING = DownloadProgress(1f, 1f, true, false, false)
        val INIT = DownloadProgress(0f, 0f, false, false, false)
    }
}