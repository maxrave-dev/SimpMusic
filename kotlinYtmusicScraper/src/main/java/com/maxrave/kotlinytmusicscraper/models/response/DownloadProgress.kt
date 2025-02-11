package com.maxrave.kotlinytmusicscraper.models.response

data class DownloadProgress(
    val audioDownloadProgress: Float = 0f, // 0.0 - 1.0
    val videoDownloadProgress: Float = 0f, // 0.0 - 1.0
    val errorMessage: String = "",
    val isMerging: Boolean = false,
    val isError: Boolean = false,
    val isDone: Boolean = false,
) {
    companion object {
        fun failed(message: String) = DownloadProgress(0f, 0f, message, isMerging = false, isError = true, isDone = false)

        val AUDIO_DONE = DownloadProgress(1f, 0f, "", isMerging = false, isError = false, isDone = true)
        val VIDEO_DONE = DownloadProgress(1f, 1f, "", isMerging = false, isError = false, isDone = true)
        val MERGING = DownloadProgress(1f, 1f, "", isMerging = true, isError = false, isDone = false)
        val INIT = DownloadProgress(0f, 0f, "", isMerging = false, isError = false, isDone = false)
    }
}