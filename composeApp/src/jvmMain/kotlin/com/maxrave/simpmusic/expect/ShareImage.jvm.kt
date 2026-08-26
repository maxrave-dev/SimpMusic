package com.maxrave.simpmusic.expect

import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File

private const val TAG = "ShareImage"

actual suspend fun saveImageToDevice(
    bytes: ByteArray,
    fileName: String,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            writeToDownloads(bytes, fileName)
            true
        }.getOrElse { error ->
            Logger.e(TAG, "Could not save $fileName: ${error.message}")
            false
        }
    }

/**
 * Desktop has no share sheet, so the honest equivalent is: write the file, then put the user in
 * front of it. [chooserTitle] is unused here for the same reason — there is no chooser to title.
 */
actual suspend fun shareImage(
    bytes: ByteArray,
    fileName: String,
    chooserTitle: String,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val file = writeToDownloads(bytes, fileName)
            revealInFileManager(file)
            true
        }.getOrElse { error ->
            Logger.e(TAG, "Could not share $fileName: ${error.message}")
            false
        }
    }

private fun writeToDownloads(
    bytes: ByteArray,
    fileName: String,
): File {
    val folder = File(getDownloadFolderPath()).apply { if (!exists()) mkdirs() }
    return File(folder, fileName).apply { writeBytes(bytes) }
}

/**
 * Opens the containing folder, selecting the file where the platform can.
 *
 * `Desktop.open` is tried first but cannot be relied on — on Linux it hangs off a dlopen of the
 * system libgio, which fails once another native library has claimed the libglib soname (the same
 * trap documented at the top of `runDesktopApp`). Hence the per-OS fallback, mirroring `openUrl`.
 */
private fun revealInFileManager(file: File) {
    val os = System.getProperty("os.name", "").lowercase()

    val revealed =
        runCatching {
            if (!Desktop.isDesktopSupported()) return@runCatching false
            val desktop = Desktop.getDesktop()
            if (!desktop.isSupported(Desktop.Action.OPEN)) return@runCatching false
            desktop.open(file.parentFile)
            true
        }.getOrElse { error ->
            Logger.w(TAG, "Desktop.open failed for ${file.parent}: ${error.message}")
            false
        }
    if (revealed) return

    val candidates =
        when {
            os.contains("mac") -> listOf(listOf("open", "-R", file.absolutePath))
            os.contains("windows") -> listOf(listOf("explorer", "/select,${file.absolutePath}"))
            // No Linux file manager agrees on a select-this-file flag, so open the folder.
            else ->
                listOf(
                    listOf("xdg-open", file.parent),
                    listOf("gio", "open", file.parent),
                )
        }

    candidates.any { command ->
        runCatching { ProcessBuilder(command).start() }
            .onFailure { error -> Logger.w(TAG, "${command.first()} failed for ${file.parent}: ${error.message}") }
            .isSuccess
    }
}
