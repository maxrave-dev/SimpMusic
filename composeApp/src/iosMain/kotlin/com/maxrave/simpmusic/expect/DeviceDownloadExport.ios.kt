package com.maxrave.simpmusic.expect

actual fun prepareDeviceDownloadTarget(
    videoId: String,
    displayBaseName: String,
): StagedDeviceDownload =
    StagedDeviceDownload(
        stagingBasePath = "",
        displayBaseName = displayBaseName,
        requiresFinalMergeExport = false,
    )

actual fun writeStagedThumbnail(
    staged: StagedDeviceDownload,
    jpegBytes: ByteArray,
): Result<Unit> = Result.failure(UnsupportedOperationException("Not supported on iOS"))

actual fun hasFinalMergedDeviceOutput(
    staged: StagedDeviceDownload,
    isVideo: Boolean,
): Boolean = false

actual suspend fun finalizeDeviceDownload(
    staged: StagedDeviceDownload,
    isVideo: Boolean,
): Result<Unit> = Result.failure(UnsupportedOperationException("Not supported on iOS"))

actual fun cleanupStagedDeviceDownload(staged: StagedDeviceDownload) {
    // No-op on iOS
}
