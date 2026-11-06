package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.utils.roomCodeFromText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

@Composable
actual fun RoomQrScanner(
    onRoomCode: (String) -> Unit,
    onUnreadable: () -> Unit,
    onDismiss: () -> Unit,
) {
    val currentOnRoomCode by rememberUpdatedState(onRoomCode)
    val currentOnUnreadable by rememberUpdatedState(onUnreadable)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        showOpenFileDialog(imageFileFilter) { file ->
            if (file == null) {
                currentOnDismiss()
            } else {
                scope.launch {
                    val code = withContext(Dispatchers.IO) { readQrCode(file) }?.let(::roomCodeFromText)
                    if (code != null) currentOnRoomCode(code) else currentOnUnreadable()
                }
            }
        }
    }
}

private fun readQrCode(file: File): String? =
    runCatching { ImageIO.read(file) }
        .onFailure { Logger.w("RoomQrScanner", "image: ${it.message}") }
        .getOrNull()
        // ImageIO answers null, not an exception, for a format it has no reader for (WebP).
        ?.let(::decodeQrCode)

internal fun decodeQrCode(image: BufferedImage): String? {
    val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
    val source = RGBLuminanceSource(image.width, image.height, pixels)
    return try {
        QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), mapOf(DecodeHintType.TRY_HARDER to true)).text
    } catch (e: ReaderException) {
        null
    }
}
