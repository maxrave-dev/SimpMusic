package com.maxrave.simpmusic.ui.screen.player.content.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Seekable wavy progress bar for the M3 Expressive Now Playing style, built on material3's
 * [LinearWavyProgressIndicator] (stable in the pinned material3 1.12.0-alpha01 artifact).
 *
 * Behavior (same pattern as Metrolist / ArchiveTune / PixelPlayer):
 * - The wave amplitude animates to 1f while playing and flattens to 0f when paused or while
 *   the user is scrubbing, so the line is calm under the finger.
 * - A transparent pointer layer maps taps and horizontal drags to a 0..100 percent scale
 *   (the scale [onSliderChange] / UIEvent.UpdateProgress are built around). While the user
 *   is interacting the displayed fraction comes from the drag, not from the player, so the
 *   bar never fights the position updates that keep streaming in mid-scrub.
 * - The thumb morphs from a Ø14dp circle into a 6×22dp rounded bar while dragging.
 *
 * @param progressFraction current playback progress in 0..1 (shell's sliderValue / 100f).
 * @param onSliderChange scrub callback on the 0..100 scale, called during drag and on tap.
 * @param onSliderChangeFinished called once when the interaction ends (commits the seek).
 */
@Composable
fun WavySeekBar(
    progressFraction: Float,
    isPlaying: Boolean,
    activeColor: Color,
    trackColor: Color,
    thumbColor: Color,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isInteracting by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }

    val displayedFraction = (if (isInteracting) dragFraction else progressFraction).coerceIn(0f, 1f)

    // RAW target only — never a pre-animated value. material3 1.5.0-alpha26's wavy node animates
    // amplitude ITSELF (Increasing/DecreasingAmplitudeAnimationSpec) and its update guard drops
    // any new target while that internal animation is still running. Feeding it an externally
    // tweened value therefore froze the wave near full ripple on pause: the first frame started
    // an internal animation towards ~0.97 and every following frame's smaller target was
    // swallowed by the guard (seen on device). A hard 0f/1f flip recomposes once, the fresh
    // lambda invalidates the node's draw cache, and the built-in spec renders the flatten.
    val targetAmplitude = if (isPlaying && !isInteracting) 1f else 0f
    // 0f = idle circle, 1f = dragging tall bar.
    val thumbMorph by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(250),
        label = "wavySeekBarThumbMorph",
    )

    fun fractionAt(x: Float): Float = if (widthPx <= 0) 0f else (x / widthPx).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            modifier
                .fillMaxWidth()
                // ~40dp hit area — comfortably taller than the wave itself.
                .height(40.dp)
                .onSizeChanged { widthPx = it.width }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = fractionAt(offset.x)
                        dragFraction = fraction
                        onSliderChange(fraction * 100f)
                        onSliderChangeFinished()
                    }
                }.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isInteracting = true
                            val fraction = fractionAt(offset.x)
                            dragFraction = fraction
                            onSliderChange(fraction * 100f)
                        },
                        onDragEnd = {
                            isInteracting = false
                            onSliderChangeFinished()
                        },
                        onDragCancel = {
                            isInteracting = false
                            onSliderChangeFinished()
                        },
                        onHorizontalDrag = { change, _ ->
                            // Consume so the artwork pager / vertical sheet underneath never
                            // steal a scrub in progress.
                            change.consume()
                            val fraction = fractionAt(change.position.x)
                            dragFraction = fraction
                            onSliderChange(fraction * 100f)
                        },
                    )
                },
    ) {
        LinearWavyProgressIndicator(
            progress = { displayedFraction },
            color = activeColor,
            trackColor = trackColor,
            // No wave on the empty part of the track, and none at progress 0.
            amplitude = { p -> if (p > 0f) targetAmplitude else 0f },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
        )
        // Thumb: Ø14dp circle morphing to a 6×22dp rounded bar while dragging.
        val thumbWidthDp = (14f + (6f - 14f) * thumbMorph).dp
        val thumbHeightDp = (14f + (22f - 14f) * thumbMorph).dp
        Box(
            modifier =
                Modifier
                    .offset {
                        val thumbWidthPx = thumbWidthDp.toPx()
                        IntOffset(
                            x = ((widthPx - thumbWidthPx) * displayedFraction).roundToInt(),
                            y = 0,
                        )
                    }.size(width = thumbWidthDp, height = thumbHeightDp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(thumbColor),
        )
    }
}
