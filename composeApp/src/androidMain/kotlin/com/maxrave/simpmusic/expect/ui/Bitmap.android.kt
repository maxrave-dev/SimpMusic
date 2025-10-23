package com.maxrave.simpmusic.expect.ui

import android.graphics.Bitmap.CompressFormat.JPEG
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Image
import coil3.toBitmap
import java.io.ByteArrayOutputStream

actual fun ImageBitmap.toByteArray(): ByteArray? {
    val byteArrayOutputStream = ByteArrayOutputStream()
    this.asAndroidBitmap().compress(JPEG, 100, byteArrayOutputStream)
    val bytesArray = byteArrayOutputStream.toByteArray()
    return bytesArray
}

actual fun Image.toImageBitmap(): ImageBitmap =
    this.toBitmap().asImageBitmap()