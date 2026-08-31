package com.maxrave.simpmusic.expect.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.File

private const val TAG = "ImageIo"
private const val COVER_DIR = "playlist_covers"

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? =
    runCatching {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.onFailure { Logger.w(TAG, "Could not decode image: ${it.message}") }.getOrNull()

actual suspend fun persistPickedImage(
    bytes: ByteArray,
    fileName: String,
): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val context = getKoin().get<Context>()
            val dir = File(context.filesDir, COVER_DIR).apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(bytes)
            file.toURI().toString()
        }.onFailure { Logger.w(TAG, "Could not persist image: ${it.message}") }.getOrNull()
    }
