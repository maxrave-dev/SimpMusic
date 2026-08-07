package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min

private val VinylColor = Color(0xFF0B0B0D)
private val RingTrackColor = Color.White.copy(alpha = 0.22f)
private val RingProgressColor = Color.White

/**
 * Maps a touch/click position inside the ring artwork to playback progress. Angles are measured
 * from the center; the top of the disc is 0% and dragging clockwise increases progress.
 */
internal fun seekProgressFromAngle(sizeWidth: Float, offset: Offset, onSeek: (Float) -> Unit) {
    val center = sizeWidth / 2f
    val angleDegrees = atan2(offset.y - center, offset.x - center) * 180f / PI.toFloat()
    val norm = ((angleDegrees + 90f) % 360f + 360f) % 360f
    onSeek((norm / 360f).coerceIn(0f, 1f))
}

/**
 * Ring player artwork: the album art rendered as a spinning vinyl record with a progress ring
 * around it. The ring doubles as a seek control — tap or drag around the disc to seek.
 *
 * Used by the main player and the mini player so both follow the ring/normal setting.
 */
@Composable
fun RingPlayerArtwork(
    thumbnailURL: String?,
    isPlaying: Boolean,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 4.dp,
    rimWidth: Dp = 3.dp,
    spin: Boolean = true,
) {
    // Advance rotation only while playing; freezing on pause keeps the last angle.
    var rotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying && spin) {
            var lastNanos = 0L
            withFrameNanos { lastNanos = it }
            while (true) {
                withFrameNanos { now ->
                    val deltaMillis = (now - lastNanos) / 1_000_000f
                    lastNanos = now
                    // ~0.045 deg/ms -> one full turn every ~8 seconds, like a real turntable.
                    rotation = (rotation + deltaMillis * 0.045f) % 360f
                }
            }
        }
    }

    Box(
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        seekProgressFromAngle(size.width.toFloat(), offset, onSeek)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            seekProgressFromAngle(size.width.toFloat(), offset, onSeek)
                        },
                        onDrag = { change, _ ->
                            seekProgressFromAngle(size.width.toFloat(), change.position, onSeek)
                        },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        // Progress ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = min(ringWidth.toPx(), this.size.minDimension / 3f)
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = RingTrackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (progress > 0f) {
                drawArc(
                    color = RingProgressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        // Vinyl disc: dark rim around the spinning artwork
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = ringWidth + rimWidth, vertical = ringWidth + rimWidth)
                    .clip(CircleShape)
                    .background(VinylColor),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = thumbnailURL,
                contentDescription = "Album Art",
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(rimWidth)
                        .clip(CircleShape)
                        .rotate(if (spin) rotation else 0f),
            )
        }

        // Center spindle hole
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(VinylColor),
        )
    }
}
