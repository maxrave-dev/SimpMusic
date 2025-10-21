package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer

actual class PlatformBackdrop

actual fun Modifier.layerBackdrop(backdrop: PlatformBackdrop): Modifier = this
actual fun Modifier.drawBackdropCustomShape(
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape
) = this

@Composable
actual fun rememberBackdrop(): PlatformBackdrop = PlatformBackdrop()