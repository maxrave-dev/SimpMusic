package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.sign
import com.kyant.backdrop.backdrops.layerBackdrop as nativeBackdrop

/**
 * The backdrop a glass surface samples from.
 *
 * This used to be an `expect class` with a real Android actual and a no-op JVM one,
 * because desktop was assumed to have no Kyant backend. It does: `io.github.kyant0:backdrop`
 * is a KMP artifact declared in `commonMain`, and its desktop variant ships the same
 * API backed by `SkikoRuntimeShader`. So there is nothing left to abstract over — the
 * whole effect is plain common code, and the alias only survives so the ~20 call sites
 * keep reading `PlatformBackdrop`.
 */
typealias PlatformBackdrop = LayerBackdrop

/** Marks a composable as the source layer that sibling glass surfaces refract. */
fun Modifier.layerBackdrop(backdrop: PlatformBackdrop): Modifier = this.nativeBackdrop(backdrop)

@Composable
fun rememberBackdrop(color: Color): PlatformBackdrop =
    rememberLayerBackdrop {
        drawRect(color)
        drawContent()
    }

fun Modifier.drawBackdropCustomShape(
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape,
): Modifier =
    this.drawBackdrop(
        backdrop = backdrop,
        effects = {
            val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
            vibrancy()
            colorControls(
                // Neutral brightness/contrast: the old curve washed the glass out to white on bright
                // backgrounds ("đục trắng"). Darkening is done in onDrawSurface (keeps "đục đen").
                brightness = 0.05f,
                contrast = 1f,
                saturation = 1.5f,
            )
            blur(
                if (l > 0f) {
                    lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                } else {
                    lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                },
            )
            lens(24f.dp.toPx(), size.minDimension / 2f, true)
        },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
            layer.record { drawBackdrop() }
        },
        shape = { shape },
        onDrawSurface = {
            val darken = lerp(0.12f, 0.5f, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
            drawRect(Color.Black.copy(alpha = darken))
        },
    )
