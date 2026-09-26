package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The two looping indicators that used to be Lottie files.
 *
 * They were the app's ONLY use of compottie, and compottie is the one dependency here that reached
 * past Compose into skiko — which pinned the whole project to a single skiko release, because skiko
 * broke binary compatibility three times running (0.148.1 deleted `GradientStyle`, 0.148.2 made
 * `Matrix33` a value class, 0.150.1 added a parameter to `Image.encodeToData`) while haze,
 * kyant/backdrop and composenativetray each need a different side of those lines.
 *
 * Both are TRANSCRIBED from the JSON keyframes rather than reinvented — every constant below is a
 * number read out of the source file, and each indicator draws in that file's own coordinate space
 * and letterboxes, so nothing has to be re-derived when the slot size changes.
 */

// ---------------------------------------------------------------------------------------------
// Audio playing — from files/audio_playing_animation.json
// ---------------------------------------------------------------------------------------------

/**
 * Six-bar equalizer shown in place of the artwork while a row is the playing one.
 *
 * What the source actually does, all of which is easy to get wrong by eye:
 * - Bars grow from the VERTICAL CENTRE (every keyframe is centred on y=75 of a 150-tall comp),
 *   not up from a baseline.
 * - They occupy the middle THIRD of the width: six 6-wide capsules on a 12 pitch, spanning
 *   x 70.8..130.9 of 200. Spreading them across the full box is far too wide.
 * - The fill is a fixed cyan, not a theme colour.
 * - One loop is 1.44s — seven keyframes, 24 frames apart, at 100fps.
 *
 * The motion lives in animated PATHS (the rectangle is redrawn vertex by vertex each keyframe),
 * not in any transform, which is why every layer and parent in the file reads `p=(0,0)`.
 */
@Composable
fun AudioPlayingIndicator(
    modifier: Modifier = Modifier,
    color: Color = AudioIndicatorCyan,
) {
    val transition = rememberInfiniteTransition(label = "audioPlaying")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = AUDIO_CYCLE_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "phase",
    )

    Canvas(modifier = modifier) {
        val scale = minOf(size.width / AUDIO_COMP_WIDTH, size.height / AUDIO_COMP_HEIGHT)
        val originX = (size.width - AUDIO_COMP_WIDTH * scale) / 2f
        val originY = (size.height - AUDIO_COMP_HEIGHT * scale) / 2f
        val centreY = originY + AUDIO_COMP_CENTRE_Y * scale
        val strokeWidth = AUDIO_BAR_WIDTH * scale
        val capRadius = strokeWidth / 2f

        AUDIO_BAR_X.forEachIndexed { bar, barX ->
            val height = audioBarHeight(bar, phase) * scale
            // Round caps extend half a stroke past each end, so the drawn segment is the bar minus
            // its two caps — otherwise every bar renders one full stroke width too tall.
            val half = (height / 2f - capRadius).coerceAtLeast(0f)
            val x = originX + barX * scale
            drawLine(
                color = color,
                start = Offset(x, centreY - half),
                end = Offset(x, centreY + half),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Height of [bar] at [phase] (0..1 through the loop), in the source's 150-tall space.
 *
 * Smoothstep between keyframes rather than linear: the source eases every segment hard
 * (`i.x = 1, o.x = 0`), and linear interpolation makes the bars visibly tick between steps.
 */
private fun audioBarHeight(
    bar: Int,
    phase: Float,
): Float {
    val heights = AUDIO_BAR_HEIGHTS[bar]
    val steps = heights.size - 1
    val position = phase.coerceIn(0f, 1f) * steps
    val index = position.toInt().coerceAtMost(steps - 1)
    return lerpSmooth(position - index, heights[index], heights[index + 1])
}

// ---------------------------------------------------------------------------------------------
// Downloading — from files/downloading_animation.json
// ---------------------------------------------------------------------------------------------

/**
 * Download indicator: a ring that erases and redraws while arrows fall through it.
 *
 * The source is a RING, not a tray — an ellipse stroked with round caps. Its trim path erases it
 * over frames 0..20, then a second copy, MIRRORED (`s = [-100, 100]`), redraws it over frames
 * 22..42 so the sweep returns the other way. Two arrows share the loop: one falls out of the
 * circle while the ring erases, the second falls in while it redraws. Both are clipped to a
 * slightly smaller circle — the alpha matte layers — which is what makes them appear and vanish at
 * the rim rather than at the edge of the box. 50 frames at 25fps.
 */
@Composable
fun DownloadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    val transition = rememberInfiniteTransition(label = "downloading")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = DOWNLOAD_CYCLE_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "phase",
    )

    Canvas(modifier = modifier) {
        val scale = minOf(size.width, size.height) / DL_COMP
        val originX = (size.width - DL_COMP * scale) / 2f
        val originY = (size.height - DL_COMP * scale) / 2f
        val centre = Offset(originX + DL_CENTRE * scale, originY + DL_CENTRE * scale)
        val frame = phase * DL_FRAMES

        drawTrimmedRing(color, centre, scale, keyframe(frame, 0f, 20f, 0f, 100f))
        // The redraw copy is the same ring under a horizontal mirror. It has to be a real transform
        // on the draw scope, not a flipped angle, so the round caps land on the mirrored ends too.
        withTransform({ scale(-1f, 1f, pivot = centre) }) {
            drawTrimmedRing(color, centre, scale, keyframe(frame, 22f, 42f, 100f, 0f))
        }

        // Both arrows are drawn every frame; the matte hides whichever is out of view, exactly as
        // in the source — no visibility branching is needed.
        val matteRadius = DL_MATTE_DIAMETER / 2f * scale
        clipPath(
            Path().apply {
                addOval(
                    Rect(
                        centre.x - matteRadius,
                        centre.y - matteRadius,
                        centre.x + matteRadius,
                        centre.y + matteRadius,
                    ),
                )
            },
        ) {
            drawArrow(color, centre, keyframe(frame, 0f, 15f, 500f, 880f), scale)
            drawArrow(color, centre, keyframe(frame, 22f, 37f, 120f, 500f), scale)
        }
    }
}

/** The ellipse stroke, showing `trimStart`%..100% of a path that begins at 12 o'clock. */
private fun DrawScope.drawTrimmedRing(
    color: Color,
    centre: Offset,
    scale: Float,
    trimStart: Float,
) {
    if (trimStart >= 100f) return
    val radius = DL_RING_DIAMETER / 2f * scale
    drawArc(
        color = color,
        startAngle = -90f + 3.6f * trimStart,
        sweepAngle = 3.6f * (100f - trimStart),
        useCenter = false,
        topLeft = Offset(centre.x - radius, centre.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = DL_RING_STROKE * scale, cap = StrokeCap.Round),
    )
}

/**
 * Shaft + head, positioned so the layer anchor (0, 34) lands on [centreY] in source coordinates.
 *
 * The head is the source's 3-point polystar: outer radius 125, rotated 180 so it points down, its
 * group scaled to 80% vertically. Its 20-unit corner rounding is dropped — at the size this
 * renders (28dp) that is under a third of a pixel.
 */
private fun DrawScope.drawArrow(
    color: Color,
    centre: Offset,
    centreY: Float,
    scale: Float,
) {
    val x = centre.x
    val y = centre.y + (centreY - DL_CENTRE - DL_ARROW_ANCHOR_Y) * scale
    val halfShaftW = DL_SHAFT_WIDTH / 2f * scale
    val halfShaftH = DL_SHAFT_HEIGHT / 2f * scale
    drawRoundRect(
        color = color,
        topLeft = Offset(x - halfShaftW, y - halfShaftH),
        size = Size(halfShaftW * 2f, halfShaftH * 2f),
        cornerRadius = CornerRadius(DL_SHAFT_CORNER * scale),
    )
    val head =
        Path().apply {
            DL_HEAD_ANGLES.forEachIndexed { i, degrees ->
                val radians = degrees * PI.toFloat() / 180f
                val px = x + DL_HEAD_RADIUS * cos(radians) * scale
                val py =
                    y + (DL_HEAD_RADIUS * sin(radians) + DL_HEAD_OFFSET_Y) * DL_HEAD_SQUASH * scale
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
    drawPath(path = head, color = color)
}

// ---------------------------------------------------------------------------------------------

/**
 * One eased keyframe segment: [from] before [t0], [to] after [t1], smoothstepped between.
 *
 * Held at the end values outside the range, which is what Lottie does with a property whose
 * keyframes cover only part of the timeline — and is why the arrows need no visibility flags.
 */
private fun keyframe(
    frame: Float,
    t0: Float,
    t1: Float,
    from: Float,
    to: Float,
): Float = lerpSmooth(((frame - t0) / (t1 - t0)).coerceIn(0f, 1f), from, to)

private fun lerpSmooth(
    t: Float,
    from: Float,
    to: Float,
): Float = from + (to - from) * t * t * (3f - 2f * t)

/** Fill colour of the source animation (#05EBFF), constant across every keyframe. */
private val AudioIndicatorCyan = Color(0xFF05EBFF)

private const val AUDIO_COMP_WIDTH = 200f
private const val AUDIO_COMP_HEIGHT = 150f
private const val AUDIO_COMP_CENTRE_Y = 75f
private const val AUDIO_BAR_WIDTH = 6f

/** Bar centres in source x, left to right — 12 apart, i.e. 6 wide with a 6 gap. */
private val AUDIO_BAR_X = floatArrayOf(70.808f, 82.933f, 94.933f, 106.808f, 118.933f, 130.933f)

/**
 * Bar heights at the seven keyframes (frames 0, 24, 48, 72, 96, 120, 144 at 100fps) in source
 * units, one row per bar in [AUDIO_BAR_X] order. Read out of the animated paths in the JSON.
 */
private val AUDIO_BAR_HEIGHTS =
    arrayOf(
        floatArrayOf(40f, 15.2f, 40f, 25.7f, 16.7f, 15.2f, 40f),
        floatArrayOf(26.7f, 15.2f, 22.3f, 70.2f, 40f, 40f, 15.2f),
        floatArrayOf(56.5f, 70.2f, 40f, 15.2f, 13.8f, 15.2f, 68.2f),
        floatArrayOf(24.7f, 40f, 15.2f, 51.8f, 40f, 57.2f, 15.2f),
        floatArrayOf(40f, 15.2f, 40f, 70.2f, 40f, 15.2f, 40f),
        floatArrayOf(70.3f, 15.2f, 62.8f, 29f, 71.2f, 40f, 15.2f),
    )

/** 144 frames at 100fps. */
private const val AUDIO_CYCLE_MS = 1_440

/** 50 frames at 25fps. */
private const val DOWNLOAD_CYCLE_MS = 2_000
private const val DL_FRAMES = 50f
private const val DL_COMP = 1000f
private const val DL_CENTRE = 500f
private const val DL_RING_DIAMETER = 500f
private const val DL_RING_STROKE = 20f
private const val DL_MATTE_DIAMETER = 480f
private const val DL_ARROW_ANCHOR_Y = 34f
private const val DL_SHAFT_WIDTH = 75f
private const val DL_SHAFT_HEIGHT = 200f
private const val DL_SHAFT_CORNER = 10f
private const val DL_HEAD_RADIUS = 125f
private const val DL_HEAD_OFFSET_Y = 85f
private const val DL_HEAD_SQUASH = 0.8f
private val DL_HEAD_ANGLES = floatArrayOf(90f, 210f, 330f)
