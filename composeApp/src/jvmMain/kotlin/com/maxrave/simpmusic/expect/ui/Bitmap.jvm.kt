package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import coil3.toBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun ImageBitmap.toByteArray(): ByteArray? {
    val image = Image.makeFromBitmap(this.asSkiaBitmap())
    val bytesArray =
        image.encodeToData(EncodedImageFormat.JPEG, 100)?.bytes
    return bytesArray
}

actual fun coil3.Image.toImageBitmap(): ImageBitmap =
    this.toBitmap().asComposeImageBitmap()