package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.File

private const val TAG = "ImageIo"
private const val COVER_DIR = "playlist_covers"

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? =
    runCatching {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }.onFailure { Logger.w(TAG, "Could not decode image: ${it.message}") }.getOrNull()

actual suspend fun persistPickedImage(
    bytes: ByteArray,
    fileName: String,
): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            // Same place the rest of the desktop app keeps its data.
            val dir = File(System.getProperty("user.home"), ".simpmusic/$COVER_DIR").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(bytes)
            file.toURI().toString()
        }.onFailure { Logger.w(TAG, "Could not persist image: ${it.message}") }.getOrNull()
    }
