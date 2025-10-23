package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image

expect fun ImageBitmap.toByteArray(): ByteArray?

expect fun Image.toImageBitmap(): ImageBitmap