package com.maxrave.simpmusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop

/**
 * Desktop fallback: the Kyant backdrop modifiers are no-ops on JVM, so the glass
 * surface degrades to a plain clip. [interactive] is ignored here.
 */
@Composable
actual fun Modifier.liquidGlass(
    backdrop: PlatformBackdrop,
    shape: Shape,
    interactive: Boolean,
): Modifier = this.clip(shape)

@Composable
actual fun Modifier.liquidGlass(
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape,
    interactive: Boolean,
): Modifier = this.clip(shape)
