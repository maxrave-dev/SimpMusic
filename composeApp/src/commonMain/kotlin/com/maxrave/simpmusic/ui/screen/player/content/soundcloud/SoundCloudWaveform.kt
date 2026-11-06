package com.maxrave.simpmusic.ui.screen.player.content.soundcloud

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.maxrave.domain.mediaservice.player.WAVEFORM_BARS_PER_SECOND
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.ui.theme.typo
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * SoundCloud's seek bar. The track's waveform slides past a playhead fixed at the centre, played
 * bars in [playedColor] and the rest in the scheme's onSurface, each mirrored as a faint reflection
 * under the baseline. The time sits on a black label at the playhead.
 *
 * Dragging moves the waveform itself, so a drag to the LEFT goes forward in time: the opposite of a
 * slider. While [bars] is null (still decoding, or a backend with no waveform) the same bars are
 * drawn as a flat line, so the bar still scrolls and still scrubs; when they arrive they rise out of
 * that line from the playhead outward, and a track change sinks them back first. [fold] (0 open,
 * 1 folded, read while drawing) sinks the bars into one line along the baseline, as SoundCloud does
 * while paused.
 */
@Composable
internal fun SoundCloudWaveform(
    bars: FloatArray?,
    position: () -> Long,
    durationMs: Long,
    playedColor: Color,
    fold: () -> Float,
    onScrubStart: () -> Unit,
    onScrub: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // The step and the bar width are whole pixels, so every bar sits at the same fraction of a pixel and
    // the waveform glides as one. With a 3.8px step each bar landed on a different fraction, sharp
    // beside smeared, and that pattern crawled across the bars like an old TV. The origin itself stays
    // fractional: snapping it too made the waveform tick along in one-pixel steps.
    val stepPx = with(density) { BAR_STEP.roundToPx() }.coerceAtLeast(2)
    val pxPerMs = stepPx * WAVEFORM_BARS_PER_SECOND / 1000f
    // Read while drawing (and once per drag start), never during composition: the playhead moves
    // every frame, and only the canvas has to follow it. The label recomposes once a second.
    val currentPosition by rememberUpdatedState(position)
    // The whole second the playhead is in, so the label recomposes once a second (formatDuration is
    // @Composable, so it is called below, not in here).
    val elapsedSecondMs by remember { derivedStateOf { currentPosition().let { if (it < 0L) it else it / 1000L * 1000L } } }
    val latestDuration by rememberUpdatedState(durationMs)
    // The drag handler outlives recompositions (pointerInput is keyed on the scale only), so it
    // must reach the caller's current callbacks, not the ones from when the gesture started.
    val currentOnScrubStart by rememberUpdatedState(onScrubStart)
    val currentOnScrub by rememberUpdatedState(onScrub)
    val currentOnScrubEnd by rememberUpdatedState(onScrubEnd)
    var elapsedWidth by remember { mutableIntStateOf(0) }
    var labelHeight by remember { mutableIntStateOf(0) }
    val playedReflection = playedColor.copy(alpha = REFLECTION_ALPHA)
    val colors = MaterialTheme.colorScheme
    val unplayedColor = colors.onSurface
    val unplayedReflection = unplayedColor.copy(alpha = REFLECTION_ALPHA)
    // The bars on screen, and how far they have risen out of the flat line (0 flat, 1 risen). A track's
    // waveform arrives a few seconds after the track, once it is decoded: it rises out of the line
    // instead of popping in, and on a track change the old one sinks back into the line first.
    var shownBars by remember { mutableStateOf<FloatArray?>(null) }
    val rise = remember { Animatable(0f) }
    LaunchedEffect(bars) {
        if (bars === shownBars) return@LaunchedEffect
        if (shownBars != null) rise.animateTo(0f, tween(SINK_MS, easing = LinearEasing))
        shownBars = bars
        if (bars != null) rise.animateTo(1f, tween(RISE_MS, easing = LinearEasing))
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(WAVEFORM_HEIGHT)
                .pointerInput(pxPerMs) {
                    var scrubMs = 0L
                    detectHorizontalDragGestures(
                        onDragStart = {
                            scrubMs = currentPosition()
                            currentOnScrubStart()
                        },
                        onDragEnd = { currentOnScrubEnd(scrubMs) },
                        onDragCancel = { currentOnScrubEnd(scrubMs) },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scrubMs = (scrubMs - dragAmount / pxPerMs).roundToLong().coerceIn(0L, latestDuration.coerceAtLeast(0L))
                            currentOnScrub(scrubMs)
                        },
                    )
                },
    ) {
        val playheadX = floor(with(density) { maxWidth.toPx() } / 2f)
        Canvas(modifier = Modifier.fillMaxWidth().height(WAVEFORM_HEIGHT)) {
            val positionMs = currentPosition()
            val shown = shownBars
            val barCount = shown?.size ?: (durationMs / 1000f * WAVEFORM_BARS_PER_SECOND).toInt()
            if (barCount <= 0) return@Canvas
            val step = stepPx.toFloat()
            val barWidth = BAR_WIDTH.roundToPx().coerceIn(1, stepPx - 1).toFloat()
            val baseline = BAR_MAX_UP.roundToPx().toFloat()
            val maxDown = BAR_MAX_DOWN.toPx()
            val gap = BASELINE_GAP.roundToPx().toFloat()
            val folded = fold()
            val open = 1f - folded
            val lineHeight = FOLDED_LINE_HEIGHT.roundToPx().coerceAtLeast(1).toFloat()
            // x of bar 0, so that the moment the song has reached sits exactly under the playhead.
            val origin = playheadX - positionMs / 1000f * WAVEFORM_BARS_PER_SECOND * step
            val first = floor(-origin / step).toInt().coerceAtLeast(0)
            val last = ceil((size.width - origin) / step).toInt().coerceAtMost(barCount - 1)
            // Bars near the playhead rise first and the rise travels outward (a sink runs back the
            // same way): each bar starts a little after its neighbour nearer the playhead, and eases
            // in and out on smoothstep.
            val risen = rise.value
            val playheadBar = (playheadX - origin) / step
            val reach = (size.width / 2f / step).coerceAtLeast(1f)

            fun levelAt(i: Int): Float {
                val target = shown?.getOrNull(i) ?: return FLAT_LEVEL
                val t = (risen * (1f + RISE_STAGGER) - abs(i - playheadBar) / reach * RISE_STAGGER).coerceIn(0f, 1f)
                return FLAT_LEVEL + (target - FLAT_LEVEL) * t * t * (3f - 2f * t)
            }

            fun DrawScope.drawBars(
                color: Color,
                reflection: Color,
                lineAlpha: Float,
            ) {
                for (i in first..last) {
                    val x = origin + i * step
                    val level = levelAt(i)
                    val up = level * baseline * open
                    drawRect(color = color, topLeft = Offset(x, baseline - up), size = Size(barWidth, up))
                    drawRect(color = reflection, topLeft = Offset(x, baseline + gap), size = Size(barWidth, level * maxDown * open))
                }
                // Folding, the bars sink into this line as it comes up.
                val start = origin + first * step
                val end = origin + last * step + barWidth
                if (lineAlpha > 0f && end > start) {
                    drawRect(
                        color = color,
                        alpha = lineAlpha,
                        topLeft = Offset(start, baseline - lineHeight),
                        size = Size(end - start, lineHeight),
                    )
                }
            }
            // Drawn twice and cut at the playhead, the way the word-by-word lyrics light a word
            // (AppleMusicLyricStrip): the bars slide past the cut every frame, so a bar fills in as it
            // crosses instead of changing colour whole.
            clipRect(left = playheadX) { drawBars(unplayedColor, unplayedReflection, folded * FOLDED_UNPLAYED_ALPHA) }
            clipRect(right = playheadX) { drawBars(playedColor, playedReflection, folded) }
        }
        Row(
            modifier =
                Modifier
                    .onSizeChanged { labelHeight = it.height }
                    // Where SoundCloud sets it: standing on the baseline, its divider on the playhead,
                    // the time played to the left and the length to the right.
                    .offset {
                        IntOffset(
                            (playheadX - elapsedWidth - 2 * TIME_LABEL_PADDING.toPx()).roundToInt(),
                            BAR_MAX_UP.roundToPx() - labelHeight,
                        )
                    }
                    // Paused, the label loses its strip like the card's other labels.
                    .drawBehind { drawRect(color = colors.surfaceContainerLowest, alpha = 1f - fold()) }
                    .padding(horizontal = TIME_LABEL_PADDING, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatDuration(elapsedSecondMs),
                style = typo().labelSmall,
                color = colors.onSurface,
                modifier = Modifier.onSizeChanged { elapsedWidth = it.width },
            )
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = TIME_LABEL_PADDING)
                        .width(1.dp)
                        .height(10.dp)
                        .background(colors.outlineVariant),
            )
            Text(text = formatDuration(durationMs), style = typo().labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

// Read off SoundCloud's own player (Mobbin, iOS, 2024), converted from its 390pt-wide frame.
private val BAR_WIDTH = 2.2.dp
private val BAR_STEP = 3.8.dp
private val BAR_MAX_UP = 64.dp
private val BAR_MAX_DOWN = 38.dp
private val BASELINE_GAP = 1.dp
internal val WAVEFORM_HEIGHT = BAR_MAX_UP + BASELINE_GAP + BAR_MAX_DOWN
private val TIME_LABEL_PADDING = 5.dp
// Folded, what has played stays lit and what is still to come is a faint hairline (the owner's iOS
// captures of SoundCloud paused).
private val FOLDED_LINE_HEIGHT = 1.dp
private const val FOLDED_UNPLAYED_ALPHA = 0.35f
private const val REFLECTION_ALPHA = 0.45f

// A waveform that arrives rises out of the flat line over RISE_MS, the farthest bar starting
// RISE_STAGGER of the way in; the one a track change replaces sinks back over SINK_MS first.
// 700/250 ms read as slow (owner, 2026-11-06).
private const val RISE_MS = 350
private const val SINK_MS = 150
private const val RISE_STAGGER = 0.6f

/**
 * The playhead the waveform scrolls by, steady where the reports are not.
 *
 * On Desktop mpv moves `time-pos` only a few times a second and the handler smooths it but holds
 * rather than step back, so the reported position runs, pauses and runs again. The Apple Music lyric
 * strip's playhead leans toward each report within 150 ms and passed that on: measured on a screen
 * recording, the bars scrolled fast for about 0.2 s and nearly stopped for the next 0.2 s, over and
 * over. This one runs on the frame clock and eases toward the last report carried forward at 1x over
 * [STEADY_DRIFT_MS], so the end of a hold slows it by a few percent for a moment and never moves it
 * back. Only a gap wider than [STEADY_SNAP_MS] (a seek, a new track) is jumped across: a first
 * version also jumped whenever a report broke 400 ms from the run, which fired at the end of every
 * hold and threw the bars back.
 */
@Composable
internal fun rememberSteadyPlayhead(
    rawMs: Long,
    running: Boolean,
): State<Long> {
    val playhead = remember { mutableLongStateOf(rawMs) }
    val latestRaw by rememberUpdatedState(rawMs)
    LaunchedEffect(running) {
        if (!running) {
            snapshotFlow { latestRaw }.collect { playhead.longValue = it }
            return@LaunchedEffect
        }
        var position = latestRaw.toDouble()
        var reported = latestRaw
        var reportedAt = -1L
        var lastFrame = -1L
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                if (reportedAt < 0L || latestRaw != reported) {
                    reported = latestRaw
                    reportedAt = now
                }
                // ponytail: carries the report forward at 1x; at another playback speed the bars trail
                // by about (speed - 1) × STEADY_DRIFT_MS. Hand the speed in if that ever shows.
                // Capped, so a player that stops reporting does not drag the bars on.
                val trend = reported + ((now - reportedAt) / 1_000_000.0).coerceAtMost(STEADY_MAX_CARRY_MS)
                val step = if (lastFrame < 0L) 0.0 else (now - lastFrame) / 1_000_000.0
                lastFrame = now
                position =
                    if (abs(trend - position) > STEADY_SNAP_MS) {
                        trend
                    } else {
                        position + step + (trend - position) * (step / STEADY_DRIFT_MS).coerceAtMost(1.0)
                    }
                playhead.longValue = position.roundToLong()
            }
        }
    }
    return playhead
}

private const val STEADY_DRIFT_MS = 3_000.0
private const val STEADY_SNAP_MS = 2_000.0
private const val STEADY_MAX_CARRY_MS = 1_000.0
private const val FLAT_LEVEL = 0.04f
