package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer

expect class PlatformBackdrop

expect fun Modifier.layerBackdrop(
    backdrop: PlatformBackdrop
): Modifier

expect fun Modifier.drawBackdropCustomShape(
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape,
): Modifier

@Composable
expect fun rememberBackdrop(): PlatformBackdrop