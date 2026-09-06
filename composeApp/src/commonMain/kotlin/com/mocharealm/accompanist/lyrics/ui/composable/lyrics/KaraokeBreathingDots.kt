package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


data class KaraokeBreathingDotsDefaults(
    val number: Int = 3,
    val size: Dp = 10.dp,
    val margin: Dp = 6.dp,
    val enterDurationMs: Int = 3000,
    val preExitStillDuration: Int = 200,
    val preExitDipAndRiseDuration: Int = 3000,
    val exitDurationMs: Int = 200,
    val breathingDotsColor: Color = Color.White
)
/**
 * Displays breathing dots animation during instrumental intros or interludes.
 * The dots breathe/pulse and fade in/out to indicate progress during non-lyrical sections.
 *
 * @param alignment Alignment of the dots (Start or End).
 * @param startTimeMs Start time of the interlude.
 * @param endTimeMs End time of the interlude.
 * @param currentTimeProvider Provider for current playback time.
 * @param modifier Modifier for the layout.
 * @param defaults Configuration defaults for size, count, etc.
 */
@Composable
fun KaraokeBreathingDots(
    alignment: KaraokeAlignment,
    startTimeMs: Int,
    endTimeMs: Int,
    currentTimeProvider: () -> Int,
    modifier: Modifier = Modifier,
    defaults: KaraokeBreathingDotsDefaults = KaraokeBreathingDotsDefaults(),
) {
    val density = LocalDensity.current

    val sizePx = remember(density, defaults.size) { with(density) { defaults.size.toPx() } }
    val marginPx = remember(density, defaults.margin) { with(density) { defaults.margin.toPx() } }
    val totalWidthPx = sizePx * defaults.number + marginPx * (defaults.number - 1)

    val timeline = remember(startTimeMs, endTimeMs, defaults) {
        val totalAvailable = (endTimeMs - startTimeMs).toFloat()
        val defaultTotal = (defaults.enterDurationMs + defaults.preExitDipAndRiseDuration +
                defaults.preExitStillDuration + defaults.exitDurationMs).toFloat()

        val factor = if (totalAvailable < defaultTotal) totalAvailable / defaultTotal else 1f

        val enter = defaults.enterDurationMs * factor
        val dip = defaults.preExitDipAndRiseDuration * factor
        val still = defaults.preExitStillDuration * factor
        val exit = defaults.exitDurationMs * factor

        object {
            val enterEnd = startTimeMs + enter
            val dipStart = endTimeMs - exit - still - dip
            val stillStart = endTimeMs - exit - still
            val exitStart = endTimeMs - exit
            val breathingDuration = dipStart - enterEnd
        }
    }

    Box(modifier) {
        Canvas(
            Modifier
                .align(
                    when (alignment) {
                        KaraokeAlignment.Start -> Alignment.TopStart
                        KaraokeAlignment.End -> Alignment.TopEnd
                        else -> Alignment.TopStart
                    }
                )
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .size(
                    width = defaults.size * defaults.number + defaults.margin * (defaults.number - 1),
                    height = defaults.size
                )
        ) {
            if (totalWidthPx <= 0f) return@Canvas

            val currentTime = currentTimeProvider().toFloat()
            var scale: Float
            var alpha: Float
            var revealProgress: Float

            when {
                // Stage 1: Intro
                currentTime < timeline.enterEnd -> {
                    val progress = ((currentTime - startTimeMs) / (timeline.enterEnd - startTimeMs)).coerceIn(0f, 1f)
                    alpha = FastOutSlowInEasing.transform(progress)
                    scale = alpha * 0.8f
                    revealProgress = alpha
                }
                // Stage 2: Breathe
                currentTime < timeline.dipStart -> {
                    alpha = 1f
                    revealProgress = 1f
                    val timeInPhase = currentTime - timeline.enterEnd
                    val angle = (timeInPhase / 3000f) * 2 * PI
                    scale = 0.9f - 0.1f * cos(angle.toFloat())
                }
                // Stage 3: Pre-exit
                currentTime < timeline.stillStart -> {
                    alpha = 1f
                    revealProgress = 1f
                    val progress = (currentTime - timeline.dipStart) / (timeline.stillStart - timeline.dipStart)
                    scale = 0.8f + 0.2f * cos(progress * 2 * PI).toFloat()
                }
                // Stage 4: Still
                currentTime < timeline.exitStart -> {
                    alpha = 1f
                    revealProgress = 1f
                    scale = 1.0f
                }
                // Stage 5: Outro
                else -> {
                    val progress = ((endTimeMs - currentTime) / (endTimeMs - timeline.exitStart)).coerceIn(0f, 1f)
                    val eased = FastOutSlowInEasing.transform(progress)
                    alpha = eased
                    scale = eased
                    revealProgress = 1f
                }
            }

            withTransform({
                this.scale(scale = scale, pivot = Offset(totalWidthPx / 2f, sizePx / 2f))
            }) {
                repeat(defaults.number) { index ->
                    val dotAlpha = if (timeline.breathingDuration > 0 && currentTime >= timeline.enterEnd) {
                        val dotDuration = timeline.breathingDuration / defaults.number
                        val dotStart = timeline.enterEnd + (index * dotDuration)
                        ((currentTime - dotStart) / dotDuration).coerceIn(0f, 1f) * 0.6f + 0.4f
                    } else {
                        0.4f
                    }

                    val dotRevealAlpha = if (revealProgress >= 1f) 1f else {
                        val dotX = (sizePx + marginPx) * index
                        val revealEdge = revealProgress * (totalWidthPx + sizePx)
                        ((revealEdge - dotX) / sizePx).coerceIn(0f, 1f)
                    }

                    drawCircle(
                        color = defaults.breathingDotsColor.copy(alpha = dotAlpha * alpha * dotRevealAlpha),
                        radius = sizePx / 2,
                        center = Offset(sizePx / 2 + (sizePx + marginPx) * index, sizePx / 2)
                    )
                }
            }
        }
    }
}