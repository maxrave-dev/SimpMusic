package com.maxrave.domain.data.model.download

data class DownloadProgress(
    val audioDownloadProgress: Float = 0f, // 0.0 - 1.0
    val videoDownloadProgress: Float = 0f, // 0.0 - 1.0
    val downloadSpeed: Int = 0, // kb/s
    val errorMessage: String = "",
    val isMerging: Boolean = false,
    val isError: Boolean = false,
    val isDone: Boolean = false,
) {
    companion object {
        fun failed(message: String) = DownloadProgress(0f, 0f, 0, message, isMerging = false, isError = true, isDone = false)

        val AUDIO_DONE = DownloadProgress(1f, 0f, 0, "", isMerging = false, isError = false, isDone = true)
        val VIDEO_DONE = DownloadProgress(1f, 1f, 0, "", isMerging = false, isError = false, isDone = true)
        val MERGING = DownloadProgress(1f, 1f, 0, "", isMerging = true, isError = false, isDone = false)
        val INIT = DownloadProgress(0f, 0f, 0, "", isMerging = false, isError = false, isDone = false)
    }
}