package com.maxrave.simpmusic.expect.ui

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.Bitmap.CompressFormat.PNG
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Image
import coil3.toBitmap
import java.io.ByteArrayOutputStream

private fun Bitmap.asSoftwareBitmap(): Bitmap? =
    if (config == Bitmap.Config.HARDWARE) {
        copy(Bitmap.Config.ARGB_8888, false)
    } else {
        this
    }

actual fun ImageBitmap.toByteArray(): ByteArray? =
    runCatching {
        val software = this.asAndroidBitmap().asSoftwareBitmap() ?: return null
        val byteArrayOutputStream = ByteArrayOutputStream()
        if (!software.compress(JPEG, 100, byteArrayOutputStream)) {
            return null
        }
        byteArrayOutputStream.toByteArray()
    }.getOrNull()

actual fun ImageBitmap.toPngByteArray(): ByteArray? =
    runCatching {
        val software = this.asAndroidBitmap().asSoftwareBitmap() ?: return null
        val byteArrayOutputStream = ByteArrayOutputStream()
        // The quality argument is ignored for PNG — it is lossless — but the signature still demands one.
        if (!software.compress(PNG, 100, byteArrayOutputStream)) {
            return null
        }
        byteArrayOutputStream.toByteArray()
    }.getOrNull()

actual fun Image.toImageBitmap(): ImageBitmap = this.toBitmap().asImageBitmap()

actual fun ImageBitmap.toReadableBitmap(
    width: Int,
    height: Int,
): ImageBitmap {
    val source = asAndroidBitmap()
    val readable = source.asSoftwareBitmap() ?: source
    return Bitmap.createScaledBitmap(readable, width, height, true).asImageBitmap()
}
