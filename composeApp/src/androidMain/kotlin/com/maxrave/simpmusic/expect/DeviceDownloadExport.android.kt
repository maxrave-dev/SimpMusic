package com.maxrave.simpmusic.expect

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.File
import java.io.IOException

private const val TAG = "DeviceDownloadExport"

actual fun prepareDeviceDownloadTarget(
    videoId: String,
    displayBaseName: String,
): StagedDeviceDownload {
    val context = getKoin().get<Context>()
    val dir = File(context.cacheDir, "device_downloads").apply { mkdirs() }
    val stem = buildSafeStagingStem(videoId, System.currentTimeMillis())
    return StagedDeviceDownload(
        stagingBasePath = File(dir, stem).absolutePath,
        displayBaseName = displayBaseName,
        requiresFinalMergeExport = true,
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
    }.onFailure { error ->
        Logger.e(TAG, "Failed to write staged thumbnail: ${error.message}")
    }

actual fun hasFinalMergedDeviceOutput(
    staged: StagedDeviceDownload,
    isVideo: Boolean,
): Boolean {
    val expected =
        if (isVideo) {
            File("${staged.stagingBasePath}-SimpMusic.mp4")
        } else {
            File("${staged.stagingBasePath}-simpmusic.mp3")
        }
    return expected.exists() && expected.length() > 0L
}

actual suspend fun finalizeDeviceDownload(
    staged: StagedDeviceDownload,
    isVideo: Boolean,
): Result<Unit> =
    withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile =
                if (isVideo) {
                    File("${staged.stagingBasePath}-SimpMusic.mp4")
                } else {
                    File("${staged.stagingBasePath}-simpmusic.mp3")
                }
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                throw IOException("Merged output file missing")
            }

            val targetFileName =
                if (isVideo) {
                    "${staged.displayBaseName}-SimpMusic.mp4"
                } else {
                    "${staged.displayBaseName}-simpmusic.mp3"
                }
            val mimeType = if (isVideo) "video/mp4" else "audio/mpeg"
            val context = getKoin().get<Context>()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values =
                    ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, targetFileName)
                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                val uri =
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: throw IOException("Failed to create MediaStore download entry")
                try {
                    resolver.openOutputStream(uri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    } ?: throw IOException("Could not open MediaStore output stream")
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                } catch (t: Throwable) {
                    runCatching { resolver.delete(uri, null, null) }
                    throw t
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir =
                    Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .apply { mkdirs() }
                val destFile = resolveAvailableDownloadFile(downloadsDir, targetFileName)
                try {
                    sourceFile.copyTo(destFile, overwrite = true)
                } catch (t: Throwable) {
                    runCatching { destFile.delete() }
                    throw t
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(mimeType),
                    null,
                )
            }
            runCatching { sourceFile.delete() }
            Unit
        }.onFailure { error ->
            Logger.e(TAG, "Failed to finalize device download: ${error.message}")
        }
    }

actual fun cleanupStagedDeviceDownload(staged: StagedDeviceDownload) {
    listOf(".jpg", ".webm", ".mp4", ".mp3", "-simpmusic.mp3", "-SimpMusic.mp4").forEach { suffix ->
        runCatching { File("${staged.stagingBasePath}$suffix").delete() }
    }
}

private fun resolveAvailableDownloadFile(
    downloadsDir: File,
    fileName: String,
): File {
    val initial = File(downloadsDir, fileName)
    if (!initial.exists()) return initial
    val dotIndex = fileName.lastIndexOf('.')
    val base = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
    val ext = if (dotIndex > 0) fileName.substring(dotIndex) else ""
    var index = 1
    while (true) {
        val candidate = File(downloadsDir, "$base ($index)$ext")
        if (!candidate.exists()) return candidate
        index++
    }
}
