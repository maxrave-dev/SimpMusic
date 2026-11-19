package com.maxrave.simpmusic.expect

import java.io.File
import java.io.IOException

actual fun prepareDeviceDownloadTarget(
    videoId: String,
    displayBaseName: String,
): StagedDeviceDownload {
    val downloadDir = File(getDownloadFolderPath()).apply { mkdirs() }
    return StagedDeviceDownload(
        stagingBasePath = File(downloadDir, displayBaseName).absolutePath,
        displayBaseName = displayBaseName,
        requiresFinalMergeExport = false,
    )
}

actual fun writeStagedThumbnail(
    staged: StagedDeviceDownload,
    jpegBytes: ByteArray,
): Result<Unit> =
    runCatching {
        val file = File("${staged.stagingBasePath}.jpg")
        file.parentFile?.mkdirs()
        file.writeBytes(jpegBytes)
    }

actual fun hasFinalMergedDeviceOutput(
    staged: StagedDeviceDownload,
    isVideo: Boolean,
): Boolean =
    File("${staged.stagingBasePath}.webm").exists() ||
        File("${staged.stagingBasePath}-simpmusic.mp3").exists() ||
        File("${staged.stagingBasePath}-SimpMusic.mp4").exists()

actual suspend fun finalizeDeviceDownload(
    staged: StagedDeviceDownload,
    isVideo: Boolean,
): Result<Unit> =
    if (hasFinalMergedDeviceOutput(staged, isVideo)) {
        Result.success(Unit)
    } else {
        Result.failure(IOException("Downloaded file not found"))
    }

actual fun cleanupStagedDeviceDownload(staged: StagedDeviceDownload) {
    // Desktop writes directly to ~/Downloads, so nothing staged should be deleted.
}
