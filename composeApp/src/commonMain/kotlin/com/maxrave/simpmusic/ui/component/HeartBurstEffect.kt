package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Owns the live bursts and fires new ones — from the LIKE TAP, never from state.
 *
 * The first version of this effect watched `checked` for a false → true edge, which cannot tell a
 * tap from a track change: skipping from an unliked song to an already-liked one flips the same
 * boolean (the like status lands from the DB a beat after the track does), and the button
 * celebrated a like nobody gave. Only the click handler knows a human acted, so the click handler
 * is the only thing allowed to call [fire].
 */
@Stable
class HeartBurstState internal constructor(
    private val scope: CoroutineScope,
) {
    internal val bursts = mutableStateListOf<HeartBurst>()
    internal var colors: List<Color> = HeartBurstDefaults.colors

    /** One burst, now. Call from the tap that LIKES (i.e. while the heart is still unchecked). */
    fun fire() {
        val burst = HeartBurst(List(PARTICLES_PER_BURST) { randomSpark(colors) })
        bursts += burst
        scope.launch {
            burst.progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(BURST_DURATION_MS, easing = LinearEasing),
            )
            bursts.remove(burst)
        }
    }
}

@Composable
fun rememberHeartBurstState(): HeartBurstState {
    val scope = rememberCoroutineScope()
    return remember { HeartBurstState(scope) }
}

/**
 * Celebration burst for like buttons: stars and glitter shards shoot upward from the button when
 * [state]'s [HeartBurstState.fire] is called.
 *
 * Pure Compose drawing — no Lottie/compottie, no platform APIs — so it behaves identically on
 * Android and Desktop. The particles are drawn OUTSIDE the button bounds, which works because
 * draw modifiers are not clipped by default: apply this modifier BEFORE any `.clip(...)` in the
 * chain, and know that a clipping ancestor (rounded card, capsule) will still trim whatever
 * flies past its own edge.
 */
@Composable
fun Modifier.heartBurst(
    state: HeartBurstState,
    colors: List<Color> = HeartBurstDefaults.colors,
): Modifier {
    state.colors = colors
    val starPath = remember { buildUnitStarPath() }

    return this.drawWithContent {
        drawContent()
        if (state.bursts.isEmpty()) return@drawWithContent
        // Launch point sits slightly above the button centre; travel distance scales with the
        // button so the effect reads the same on a 24dp list heart and a 48dp player heart.
        val origin = Offset(size.width / 2f, size.height * 0.3f)
        val reach = size.height * 2.2f
        state.bursts.forEach { burst ->
            val progress = burst.progress.value
            if (progress <= 0f) return@forEach
            // Decelerating flight + quadratic gravity pulling the sparks back down.
            val flight = 1f - (1f - progress) * (1f - progress)
            val fall = progress * progress * reach * 0.35f
            val alpha = (1f - progress).coerceIn(0f, 1f)
            burst.sparks.forEach { spark ->
                val distance = spark.speed * flight * reach
                val x = origin.x + cos(spark.angleRad) * distance
                val y = origin.y + sin(spark.angleRad) * distance + fall
                val sparkSize = size.height * spark.relativeSize
                withTransform({
                    translate(x, y)
                    rotate(degrees = spark.spin * progress * 360f, pivot = Offset.Zero)
                }) {
                    when (spark.shape) {
                        HeartSparkShape.STAR ->
                            withTransform({ scale(sparkSize, sparkSize, Offset.Zero) }) {
                                drawPath(starPath, color = spark.color, alpha = alpha)
                            }

                        HeartSparkShape.GLITTER ->
                            drawRect(
                                color = spark.color,
                                alpha = alpha,
                                topLeft = Offset(-sparkSize * 0.18f, -sparkSize * 0.55f),
                                size = Size(sparkSize * 0.36f, sparkSize * 1.1f),
                            )
                    }
                }
            }
        }
    }
}

object HeartBurstDefaults {
    /** Gold, warm white, and pink glitter — reads against both dark and artwork backdrops. */
    val colors: List<Color> =
        listOf(
            Color(0xFFFFD54F),
            Color(0xFFFFF59D),
            Color(0xFFFF8A80),
            Color(0xFFF48FB1),
            Color.White,
        )
}

private const val BURST_DURATION_MS = 750
private const val PARTICLES_PER_BURST = 16

internal enum class HeartSparkShape { STAR, GLITTER }

internal class HeartSpark(
    val angleRad: Float,
    val speed: Float,
    val relativeSize: Float,
    val spin: Float,
    val shape: HeartSparkShape,
    val color: Color,
)

internal class HeartBurst(
    val sparks: List<HeartSpark>,
) {
    val progress = Animatable(0f)
}

private fun randomSpark(colors: List<Color>): HeartSpark {
    // Upward fan: from ~150° to ~30° in screen coordinates (negative y is up).
    val angle = (-PI * 5f / 6f + Random.nextFloat() * (PI * 4f / 6f)).toFloat()
    return HeartSpark(
        angleRad = angle,
        speed = 0.55f + Random.nextFloat() * 0.45f,
        relativeSize = 0.12f + Random.nextFloat() * 0.10f,
        spin = -2f + Random.nextFloat() * 4f,
        shape = if (Random.nextFloat() < 0.45f) HeartSparkShape.STAR else HeartSparkShape.GLITTER,
        color = colors[Random.nextInt(colors.size)],
    )
}

/** Five-pointed star of unit outer radius centred on the origin; scaled at draw time. */
private fun buildUnitStarPath(): Path =
    Path().apply {
        val innerRadius = 0.42f
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) 1f else innerRadius
            val angle = (-PI / 2 + i * PI / 5).toFloat()
            val x = cos(angle) * radius
            val y = sin(angle) * radius
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
