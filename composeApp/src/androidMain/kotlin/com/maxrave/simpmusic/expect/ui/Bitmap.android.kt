package com.maxrave.simpmusic.expect.ui

import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.Bitmap.CompressFormat.PNG
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

actual fun ImageBitmap.toPngByteArray(): ByteArray? {
    val byteArrayOutputStream = ByteArrayOutputStream()
    // The quality argument is ignored for PNG — it is lossless — but the signature still demands one.
    this.asAndroidBitmap().compress(PNG, 100, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
}

actual fun Image.toImageBitmap(): ImageBitmap = this.toBitmap().asImageBitmap()
