package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.maxrave.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

private const val SAMPLE_SIZE = 5
private const val SAMPLE_INTERVAL_MS = 2_500L
private const val LUMINANCE_CHANGE_THRESHOLD = 0.02f

/** Samples a small GPU-rendered copy; reading pixels also handles Android hardware bitmaps. */
@Composable
internal fun rememberGlassLuminance(
    source: GraphicsLayer,
    enabled: Boolean = true,
): State<Float> {
    val sample = rememberGraphicsLayer()
    val luminance = remember { Animatable(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    LaunchedEffect(source, sample, enabled, lifecycle, density, layoutDirection) {
        if (!enabled) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
            while (isActive) {
                val sourceSize = source.size
                // A layer has no display list until its first draw.
                if (sourceSize.width > 0 && sourceSize.height > 0) {
                    val target =
                        try {
                            sample.record(density, layoutDirection, IntSize(SAMPLE_SIZE, SAMPLE_SIZE)) {
                                scale(
                                    scaleX = SAMPLE_SIZE.toFloat() / sourceSize.width,
                                    scaleY = SAMPLE_SIZE.toFloat() / sourceSize.height,
                                    pivot = Offset.Zero,
                                ) {
                                    drawLayer(source)
                                }
                            }
                            sample.toImageBitmap().readPixels(pixels)
                            val average =
                                pixels.sumOf { color ->
                                    val r = (color shr 16 and 0xFF) / 255.0
                                    val g = (color shr 8 and 0xFF) / 255.0
                                    val b = (color and 0xFF) / 255.0
                                    0.2126 * r + 0.7152 * g + 0.0722 * b
                                } / pixels.size
                            average.toFloat().coerceIn(0.3f, 0.8f)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            // Keep the last tint and retry on the next lifecycle start, not in a loop.
                            Logger.e("GlassLuminance", "Unable to sample glass background", error)
                            return@repeatOnLifecycle
                        }
                    if (abs(target - luminance.targetValue) >= LUMINANCE_CHANGE_THRESHOLD) {
                        luminance.animateTo(target, tween(500))
                    }
                }
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }
    return luminance.asState()
}
