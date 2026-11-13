package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.maxrave.data.io.fileDir
import com.maxrave.data.io.fileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.jetbrains.skia.Image
import platform.Foundation.NSURL

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? = runCatching {
    Image.makeFromEncoded(bytes).toComposeImageBitmap()
}.getOrNull()

actual suspend fun persistPickedImage(bytes: ByteArray, fileName: String): String? = withContext(Dispatchers.IO) {
    runCatching {
        val directory = "${fileDir()}/playlist_covers".toPath()
        val name = fileName.substringAfterLast('/').substringAfterLast('\\')
        require(name.isNotBlank() && name != "." && name != "..")
        fileSystem().createDirectories(directory)
        val file = directory / name
        fileSystem().write(file) { write(bytes) }
        NSURL.fileURLWithPath(file.toString()).absoluteString
    }.getOrNull()
}
