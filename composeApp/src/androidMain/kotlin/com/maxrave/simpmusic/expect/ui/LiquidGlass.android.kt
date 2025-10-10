package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
import java.lang.ref.WeakReference
import kotlin.math.sign

actual class PlatformBackdrop {
    private var value: WeakReference<LayerBackdrop?>? = null

    fun set(layerBackdrop: LayerBackdrop) {
        value = WeakReference(layerBackdrop)
    }

    internal fun get(): LayerBackdrop? {
        return value?.get()
    }
}


actual fun Modifier.layerBackdrop(backdrop: PlatformBackdrop?): Modifier = backdrop?.get()?.let {
    this.layerBackdrop(it)
} ?: this

actual fun Modifier.drawBackdropCustomShape(
    backdrop: PlatformBackdrop?,
    layer: GraphicsLayer,
    luminanceAnimation: Float,
    shape: Shape
): Modifier {
    val backdrop = backdrop?.get() ?: return this
    return this.drawBackdrop(
        backdrop = backdrop,
        effects = {
            val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
            colorControls(
                brightness =
                    if (l > 0f) {
                        lerp(0.1f, 0.5f, l)
                    } else {
                        lerp(0.1f, -0.2f, -l)
                    },
                contrast =
                    if (l > 0f) {
                        lerp(1f, 0f, l)
                    } else {
                        1f
                    },
                saturation = 1.5f,
            )
            blur(
                if (l > 0f) {
                    lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                } else {
                    lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                },
            )
            refraction(24f.dp.toPx(), size.minDimension / 2f, true)
        },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
            layer.record { drawBackdrop() }
        },
        shape = { shape },
    )
}