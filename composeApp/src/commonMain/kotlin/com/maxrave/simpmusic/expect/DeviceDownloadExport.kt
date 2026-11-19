package com.maxrave.simpmusic.expect

import com.maxrave.domain.data.model.download.DownloadProgress

data class StagedDeviceDownload(
    val stagingBasePath: String,
    val displayBaseName: String,
    val requiresFinalMergeExport: Boolean,
)

fun sanitizeDownloadBaseName(
    title: String,
    artist: String,
): String {
    val raw =
        if (artist.isNotBlank()) {
            "$title - $artist"
        } else {
            title
        }
    val cleaned =
        raw
            .replace(Regex("""[|\\/?*<":>'`\r\n\t\u0000-\u001f]"""), "")
            .trim()
            .replace(Regex("""\s+"""), "_")
            .trim('.', '_')
            .take(120)
            .trim('.', '_')
    return cleaned.ifEmpty { "SimpMusic_Track" }
}

fun buildSafeStagingStem(
    videoId: String,
    timestampMs: Long,
): String {
    val safeId =
        videoId
            .replace(Regex("""[^A-Za-z0-9_-]"""), "_")
            .take(40)
            .ifEmpty { "track" }
    return "stage_${safeId}_$timestampMs"
}

fun normalizeDeviceDownloadProgress(
    progress: DownloadProgress,
    requiresFinalMergeExport: Boolean,
    finalMergedOutputExists: Boolean,
): DownloadProgress {
    if (progress.isDone && !progress.isError && requiresFinalMergeExport && !finalMergedOutputExists) {
        return progress.copy(
            isMerging = true,
            isDone = false,
        )
    }
    return progress
}

expect fun prepareDeviceDownloadTarget(
    videoId: String,
    displayBaseName: String,
): StagedDeviceDownload

expect fun writeStagedThumbnail(
    staged: StagedDeviceDownload,
    jpegBytes: ByteArray,
): Result<Unit>

expect fun hasFinalMergedDeviceOutput(
    staged: StagedDeviceDownload,
    isVideo: Boolean,
): Boolean

expect suspend fun finalizeDeviceDownload(
    staged: StagedDeviceDownload,
    isVideo: Boolean,
): Result<Unit>

expect fun cleanupStagedDeviceDownload(staged: StagedDeviceDownload)
