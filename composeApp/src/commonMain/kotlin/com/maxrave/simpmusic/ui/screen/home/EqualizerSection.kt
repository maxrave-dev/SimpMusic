package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.EQUALIZER_BAND_LABELS
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.equalizer_preamp
import simpmusic.composeapp.generated.resources.equalizer_reset
import kotlin.math.roundToInt

/** Gain limits, in dB. Past this it stops being tone shaping and starts being distortion. */
private const val BAND_RANGE_DB = 12f

/** The preamp only cuts — see the note where it is drawn. */
private const val PREAMP_MIN_DB = -12f

private val CURVE_HEIGHT = 240.dp

/**
 * The equalizer block, embedded in the settings list rather than pushed as its own screen.
 *
 * It is one control: a preamp, a curve, and a reset. Sending the user to a separate destination
 * for that puts a navigation step between them and the thing they came to adjust — and they
 * adjust it while listening, so the fewer screens between the curve and the music the better.
 */
@Composable
fun EqualizerSection(viewModel: SettingsViewModel = koinViewModel()) {
    val bands by viewModel.equalizerBands.collectAsStateWithLifecycle()
    val preamp by viewModel.equalizerPreamp.collectAsStateWithLifecycle()
    val curveColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    // Starts the collectors on THIS view model instance. They are normally kicked off from
    // SettingsViewModel.getData(); embedded here that call has already happened, but the effect
    // stays so the block keeps working if it is ever hosted somewhere else.
    LaunchedEffect(Unit) { viewModel.getEqualizer() }

    // A surface of its own, a step lighter than the settings background. The block is a single
    // control made of several widgets; without a card behind it the curve, the preamp slider and
    // the reset button read as three unrelated rows in the list.
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
            text = stringResource(Res.string.equalizer_preamp),
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
            )
            // Cut only. Ten boosted bands sum well past full scale long before any single one looks
            // extreme, so the preamp is there to make room for them — letting it boost as well would
            // only move the clipping somewhere else.
            Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            Slider(
                value = preamp,
                onValueChange = { viewModel.setEqualizerPreamp(it) },
                valueRange = PREAMP_MIN_DB..0f,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${preamp.roundToInt()} dB",
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(52.dp),
            )
            }

            // The dB scale sits beside the plot, not inside it: drawing text into the canvas would
            // put it under the curve the user is dragging.
            Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.width(44.dp).height(CURVE_HEIGHT).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "+${BAND_RANGE_DB.roundToInt()}dB",
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "-${BAND_RANGE_DB.roundToInt()}dB",
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EqualizerCurve(
                bands = bands,
                curveColor = curveColor,
                gridColor = gridColor,
                onBandChange = { index, gain -> viewModel.setEqualizerBand(index, gain) },
                modifier =
                    Modifier
                        .weight(1f)
                        .height(CURVE_HEIGHT)
                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
            )
            }

            // Aligned to the plot, not to the block: the 44dp gutter above holds the dB scale, so the
            // labels have to start where the curve does or every frequency sits off its own gridline.
            Row(
            modifier = Modifier.fillMaxWidth().padding(start = 44.dp + 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            ) {
            EQUALIZER_BAND_LABELS.forEach { label ->
                Text(
                    text = label,
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            }

            // Bottom right, the way a dialog puts its action — it applies to the whole block above.
            Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            ) {
            OutlinedButton(onClick = { viewModel.resetEqualizer() }) {
                Text(text = stringResource(Res.string.equalizer_reset), style = typo().labelMedium)
            }
            }
        }
    }
}

/**
 * The curve itself: one handle per band, joined by a smooth line over a filled area.
 *
 * A graph rather than ten sliders because the thing being edited is a *shape*. Sliders show ten
 * unrelated numbers and make you picture the result; here the result is the control.
 *
 * Dragging picks the band nearest the touch on the x axis and follows the finger on y, so a whole
 * curve can be drawn in one gesture instead of ten separate grabs.
 */
@Composable
private fun EqualizerCurve(
    bands: List<Float>,
    curveColor: Color,
    gridColor: Color,
    onBandChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = EQUALIZER_BAND_LABELS.size
    Box(
        modifier =
            modifier.pointerInput(count) {
                // Raw event loop rather than detectDragGestures: that one waits for the pointer to
                // travel past a slop threshold before it reports anything, so a plain click set no
                // band at all and the first few pixels of every drag were swallowed. Here the
                // press itself already moves the band it lands on.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    reportBand(down.position, size.width, size.height, count, onBandChange)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        reportBand(change.position, size.width, size.height, count, onBandChange)
                        change.consume()
                    }
                }
            }.drawBehind {
                val step = size.width / (count - 1)
                val midY = size.height / 2f

                drawLine(gridColor, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1f)
                repeat(count) { i ->
                    val x = step * i
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                }

                val points = List(count) { i -> Offset(step * i, gainToY(bands.getOrElse(i) { 0f }, size.height)) }
                val line = smoothPath(points)

                // Filled underneath, the way a spectrum reads — the area says "this much of this range",
                // which a bare line does not.
                val area = Path().apply {
                    addPath(line)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    area,
                    Brush.verticalGradient(
                listOf(curveColor.copy(alpha = 0.35f), curveColor.copy(alpha = 0.02f)),
                    ),
                )
                drawPath(line, curveColor, style = Stroke(width = 3f))
                points.forEach { drawCircle(curveColor, radius = 7f, center = it) }
            },
    )
}

/** Map a gain in dB to a y coordinate, +12 dB at the top and −12 dB at the bottom. */
private fun gainToY(
    gainDb: Float,
    height: Float,
): Float = height / 2f - (gainDb / BAND_RANGE_DB) * (height / 2f)

/** Inverse of [gainToY], clamped so a drag past the edge parks at the limit instead of overshooting. */
private fun yToGain(
    y: Float,
    height: Float,
): Float = (((height / 2f - y) / (height / 2f)) * BAND_RANGE_DB).coerceIn(-BAND_RANGE_DB, BAND_RANGE_DB)

/** Send the band nearest [position] on the x axis to [onBandChange], at the gain [position] sits at. */
private fun reportBand(
    position: Offset,
    width: Int,
    height: Int,
    count: Int,
    onBandChange: (Int, Float) -> Unit,
) {
    if (width <= 0 || height <= 0) return
    val step = width.toFloat() / (count - 1)
    val index = (position.x / step).roundToInt().coerceIn(0, count - 1)
    onBandChange(index, yToGain(position.y, height.toFloat()))
}

/**
 * A smooth curve through [points], Catmull-Rom converted to cubic Bézier.
 *
 * The previous version placed both control points on the current point's own y, which flattened
 * the line at every handle and pushed the whole climb into the gap between two handles — a row of
 * bevelled steps rather than a curve. Catmull-Rom aims each control point along the direction set
 * by a point's *neighbours*, so the line arrives and leaves at the slope the shape implies.
 *
 * Control points are clamped to the span of their own segment. Catmull-Rom otherwise overshoots
 * after a steep change and dips past a handle the user placed, which on an equalizer reads as the
 * curve disobeying them.
 */
private fun smoothPath(points: List<Offset>): Path =
    Path().apply {
        if (points.isEmpty()) return@apply
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val p0 = points[(i - 1).coerceAtLeast(0)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[(i + 2).coerceAtMost(points.size - 1)]
            val lo = minOf(p1.y, p2.y)
            val hi = maxOf(p1.y, p2.y)
            cubicTo(
                p1.x + (p2.x - p0.x) / 6f,
                (p1.y + (p2.y - p0.y) / 6f).coerceIn(lo, hi),
                p2.x - (p3.x - p1.x) / 6f,
                (p2.y - (p3.y - p1.y) / 6f).coerceIn(lo, hi),
                p2.x,
                p2.y,
            )
        }
    }
