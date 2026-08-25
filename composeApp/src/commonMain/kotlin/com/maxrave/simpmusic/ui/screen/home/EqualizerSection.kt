package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.maxrave.simpmusic.ui.icon.Check
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.EQUALIZER_BAND_LABELS
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.equalizer_autoeq
import simpmusic.composeapp.generated.resources.equalizer_preamp
import simpmusic.composeapp.generated.resources.equalizer_preset_custom
import simpmusic.composeapp.generated.resources.equalizer_presets
import simpmusic.composeapp.generated.resources.equalizer_reset
import kotlin.math.roundToInt

/** Gain limits, in dB. Past this it stops being tone shaping and starts being distortion. */
private const val BAND_RANGE_DB = 12f

/**
 * The preamp only cuts — see the note where it is drawn.
 *
 * Deeper than the ±12 dB the bands span, because an AutoEq profile's own preamp is computed from
 * the summed response rather than from its tallest band, and so goes past −12: the lowest across a
 * sample of sixty published profiles was −12.1 dB. A stored value outside a [Slider]'s range is
 * pinned to the end of the track, which would have shown those profiles sitting at the limit while
 * holding a different number.
 */
private const val PREAMP_MIN_DB = -15f

private val CURVE_HEIGHT = 240.dp

/** Keeps the Presets and AutoEq triggers on one column instead of each starting at its own label. */
private val LABEL_COLUMN_WIDTH = 72.dp

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

    // The preamp under the finger, or null while the slider is at rest. Nothing is stored until the
    // thumb is released, so the readout has to follow this or the number sits still as it moves.
    var draftPreamp by remember { mutableStateOf<Float?>(null) }
    // Handed back to the stored value once the commit has been through storage. Clearing it in
    // onValueChangeFinished instead would show the pre-drag level for the length of that round trip.
    LaunchedEffect(preamp) { draftPreamp = null }

    var presetMenuOpen by remember { mutableStateOf(false) }
    val activePreset = remember(bands) { equalizerPresetFor(bands) }
    // Read back off the curve, the same way the preset name is: drag a band and the headphone
    // name goes on its own, with nothing having to notice and clear it.
    val autoEqProfile by viewModel.equalizerAutoEqProfile.collectAsStateWithLifecycle()
    val autoEqLabel = remember(bands, autoEqProfile) { autoEqLabelFor(autoEqProfile, bands) }

    // A surface of its own, a step lighter than the settings background. The block is a single
    // control made of several widgets; without a card behind it the curve, the preamp slider and
    // the reset button read as three unrelated rows in the list.
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Two questions, two rows: what the music should sound like, and what this pair of
            // headphones gets wrong. Both write the same curve, which is why either one going out
            // of date is visible immediately in the other's label.
            Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            ) {
            Text(
                text = stringResource(Res.string.equalizer_presets),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(LABEL_COLUMN_WIDTH),
            )
            Box {
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { presetMenuOpen = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        // Nothing on disk records which preset is on: the name is read back off the
                        // curve, so dragging a band drops it to Custom by itself and a preset
                        // re-selects itself if the curve is ever put back on it.
                        text = activePreset?.name ?: stringResource(Res.string.equalizer_preset_custom),
                        style = typo().bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = SimpIcons.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(expanded = presetMenuOpen, onDismissRequest = { presetMenuOpen = false }) {
                    EQUALIZER_PRESETS.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(text = preset.name, style = typo().bodyMedium) },
                            onClick = {
                                viewModel.applyEqualizerPreset(preset.bandsDb, preset.preampDb)
                                presetMenuOpen = false
                            },
                            leadingIcon = {
                                // The slot is filled either way, so the names hold one column
                                // instead of shifting sideways as the tick moves.
                                if (preset.name == activePreset?.name) {
                                    Icon(
                                        imageVector = SimpIcons.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp),
                                    )
                                } else {
                                    Box(modifier = Modifier.size(18.dp))
                                }
                            },
                        )
                    }
                }
            }
            }
            Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            ) {
            Text(
                text = stringResource(Res.string.equalizer_autoeq),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(LABEL_COLUMN_WIDTH),
            )
            AutoEqPicker(label = autoEqLabel)
            }
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
                value = draftPreamp ?: preamp,
                onValueChange = { draftPreamp = it },
                // Applied on release rather than per frame. Every write lands in storage and from
                // there in mpv, which drains and re-creates its whole audio filter graph to take it.
                onValueChangeFinished = { draftPreamp?.let(viewModel::setEqualizerPreamp) },
                valueRange = PREAMP_MIN_DB..0f,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(draftPreamp ?: preamp).roundToInt()} dB",
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
                onBandsChange = { viewModel.setEqualizerBands(it) },
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
 * curve can be drawn in one gesture instead of ten separate grabs. It is applied once, on release.
 */
@Composable
private fun EqualizerCurve(
    bands: List<Float>,
    curveColor: Color,
    gridColor: Color,
    onBandsChange: (List<Float>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = EQUALIZER_BAND_LABELS.size

    // The curve being dragged, or null when no pointer is down. It exists so the line can follow
    // the finger without any of it reaching storage on the way: applying per event wrote the
    // preferences file and rebuilt mpv's filter graph on every frame of a drag.
    var draft by remember { mutableStateOf<List<Float>?>(null) }
    val shown = draft ?: bands

    // The gesture handler is installed once, so it would otherwise keep the bands from the
    // composition that installed it — the flat placeholder, before the stored curve has loaded. A
    // drag started from that stale copy would commit it and wipe the saved shape.
    val currentBands by rememberUpdatedState(bands)
    val commit by rememberUpdatedState(onBandsChange)

    // Hands the curve back to the stored value once the commit has been through storage. Dropping
    // the draft at release instead would snap back to the pre-drag shape until that returns.
    LaunchedEffect(bands) { draft = null }

    Box(
        modifier =
            modifier.pointerInput(count) {
                // Raw event loop rather than detectDragGestures: that one waits for the pointer to
                // travel past a slop threshold before it reports anything, so a plain click set no
                // band at all and the first few pixels of every drag were swallowed. Here the
                // press itself already moves the band it lands on.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var working = currentBands.withBandAt(down.position, size.width, size.height, count)
                    draft = working
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        working = working.withBandAt(change.position, size.width, size.height, count)
                        draft = working
                        change.consume()
                    }
                    // One write per gesture. A sweep across the plot moves several bands, so the
                    // whole curve goes down rather than the band the finger happened to leave on.
                    commit(working)
                }
            }.drawBehind {
                val step = size.width / (count - 1)
                val midY = size.height / 2f

                drawLine(gridColor, Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1f)
                repeat(count) { i ->
                    val x = step * i
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                }

                val points = List(count) { i -> Offset(step * i, gainToY(shown.getOrElse(i) { 0f }, size.height)) }
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

/**
 * This curve with the band nearest [position] on the x axis moved to the gain [position] sits at.
 *
 * A new list rather than an edit in place: the draft it produces is read by the draw pass on a
 * later frame, and a mutated list would change underneath it. Its length is normalised to [count]
 * on the way, so a stored curve from a build with fewer bands still drags.
 */
private fun List<Float>.withBandAt(
    position: Offset,
    width: Int,
    height: Int,
    count: Int,
): List<Float> {
    if (width <= 0 || height <= 0) return this
    val step = width.toFloat() / (count - 1)
    val index = (position.x / step).roundToInt().coerceIn(0, count - 1)
    val gain = yToGain(position.y, height.toFloat())
    return List(count) { i -> if (i == index) gain else getOrElse(i) { 0f } }
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
