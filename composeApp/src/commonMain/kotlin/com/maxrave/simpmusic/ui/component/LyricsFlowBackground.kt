package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported
import com.maxrave.simpmusic.ui.screen.player.content.applemusic.rememberSweepPlayhead
import com.maxrave.simpmusic.viewModel.ANIMATED_LYRICS_BACKGROUND_KEY
import com.maxrave.simpmusic.viewModel.LYRICS_BACKGROUND_PULSE_KEY
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Whether lyrics pages draw [LyricsFlowBackground] instead of their own background: the Interface setting,
 * and only where Modifier.blur works (Android 12+, Desktop), since unblurred it is four turning squares.
 */
@Composable
internal fun rememberAnimatedLyricsBackground(): Boolean {
    val dataStoreManager = koinInject<DataStoreManager>()
    val setting by remember(dataStoreManager) { dataStoreManager.getString(ANIMATED_LYRICS_BACKGROUND_KEY) }
        .collectAsStateWithLifecycle(null)
    return setting == DataStoreManager.TRUE && isLyricsBlurSupported()
}

/**
 * Apple Music's lyrics background: four copies of the artwork turning slowly under a heavy blur, the
 * colours pushed past the cover's own. The layout is Apple Music web's `LyricsScene` (copies 125%, 80%,
 * 50% and 25% across, saturation ×2.75, web's own speed), with the 80% copy orbiting the centre as well as
 * the two small ones. Sizes follow the longer side, so the largest copy covers the page at any angle in
 * either orientation.
 *
 * With Pulse to the beat on, every bass hit of [videoId] at [positionMs] swells the page and spins the
 * copies faster for a moment (AMLL adds the bass level to its turn the same way), read from the audio the
 * player itself measured ([com.maxrave.domain.mediaservice.player.MediaPlayerInterface.bassPulse]).
 * It turns only while [playing], at most 30 times a second, and on Desktop not while the window is hidden.
 */
@Composable
internal fun LyricsFlowBackground(
    artwork: ImageBitmap?,
    videoId: String?,
    positionMs: Long,
    playing: Boolean,
    modifier: Modifier = Modifier,
    dim: Float = DEFAULT_DIM,
) {
    val dataStoreManager = koinInject<DataStoreManager>()
    val pulseSetting by remember(dataStoreManager) { dataStoreManager.getString(LYRICS_BACKGROUND_PULSE_KEY) }
        .collectAsStateWithLifecycle(null)
    val pulseOn = pulseSetting == DataStoreManager.TRUE
    val player = koinInject<MediaPlayerHandler>().player
    val playhead = rememberSweepPlayhead(positionMs, running = playing)
    val currentVideoId by rememberUpdatedState(videoId)
    // Seconds the copies have turned, and how hard the page is swelling right now.
    val turned = remember { mutableFloatStateOf(0f) }
    val pulse = remember { mutableFloatStateOf(0f) }
    // A page that never gets its artwork draws black with nothing moving; say so in the log.
    LaunchedEffect(artwork == null) {
        if (artwork == null) Logger.d(TAG, "No artwork for $videoId yet, the background stays black")
    }

    LaunchedEffect(playing, pulseOn) {
        if (!playing) {
            pulse.floatValue = 0f
            return@LaunchedEffect
        }
        var lastStep = -1L
        while (true) {
            // An infinite animation, so Desktop's frame governor stops it with the window hidden.
            withInfiniteAnimationFrameNanos { now ->
                if (lastStep < 0L) lastStep = now
                if (now - lastStep >= FRAME_INTERVAL_NS) {
                    val seconds = (now - lastStep) / 1_000_000_000f
                    lastStep = now
                    // A hit spins the copies faster for as long as it lasts.
                    turned.floatValue += seconds * (1f + PULSE_SPIN * pulse.floatValue)
                    val id = currentVideoId
                    val target = if (pulseOn && id != null) player.bassPulse(id, playhead.value) else 0f
                    // Rises at once and falls away, the way a kick drum lands.
                    pulse.floatValue = max(target, pulse.floatValue * exp(-seconds / PULSE_DECAY_S))
                }
            }
        }
    }

    val saturation = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(SATURATION) }) }
    Box(modifier = modifier.fillMaxSize().clipToBounds().background(Color.Black)) {
        if (artwork != null) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = 1f + PULSE_ZOOM * pulse.floatValue
                            scaleX = scale
                            scaleY = scale
                        }.blur(BLUR_RADIUS, BlurredEdgeTreatment.Rectangle),
            ) {
                val t = turned.floatValue
                val long = max(size.width, size.height)
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawCopy(artwork, cx, cy, long * 1.25f, SPIN[0] * t, saturation)
                // Web parks this copy off-centre; here it orbits the centre too, since a copy turning on the
                // spot leaves the middle of the page unchanged and the motion only showed at the edges.
                val mid = SPIN[1] * t
                drawCopy(artwork, cx + long / 6f * cos(mid * 0.75f), cy + long / 6f * sin(mid * 0.75f), long * 0.8f, mid, saturation)
                val small = SPIN[2] * t
                drawCopy(artwork, cx + long / 4f * cos(small * 0.75f), cy + long / 4f * sin(small * 0.75f), long * 0.5f, small, saturation)
                val smallest = SPIN[3] * t
                drawCopy(
                    artwork,
                    cx + long * 0.05f + long / 4f * cos(smallest * 0.75f),
                    cy + long * 0.05f + long / 4f * sin(smallest * 0.75f),
                    long * 0.25f,
                    smallest,
                    saturation,
                )
            }
        }
        if (dim > 0f) {
            Box(modifier = Modifier.fillMaxSize().drawBehind { drawRect(Color.Black, alpha = dim.coerceAtMost(1f)) })
        }
    }
}

/** A square copy of [image] (its centre square, for a wide video thumbnail) of [side], centred on ([cx], [cy]). */
private fun DrawScope.drawCopy(
    image: ImageBitmap,
    cx: Float,
    cy: Float,
    side: Float,
    radians: Float,
    colorFilter: ColorFilter,
) {
    val crop = min(image.width, image.height)
    rotate(degrees = radians * 180f / PI.toFloat(), pivot = Offset(cx, cy)) {
        drawImage(
            image = image,
            srcOffset = IntOffset((image.width - crop) / 2, (image.height - crop) / 2),
            srcSize = IntSize(crop, crop),
            dstOffset = IntOffset((cx - side / 2f).roundToInt(), (cy - side / 2f).roundToInt()),
            dstSize = IntSize(side.roundToInt(), side.roundToInt()),
            colorFilter = colorFilter,
            filterQuality = FilterQuality.Low,
        )
    }
}

// Radians a second for the 125%, 80%, 50% and 25% copies: 60% of Apple Music web's per-frame steps
// (+0.003, -0.008, -0.006, +0.004 at 30 fps); full speed and then 75% both read too fast. The iPhone app
// measured at a tenth of web, and both a tenth and 0.3 read as standing still under this blur.
private val SPIN = floatArrayOf(0.054f, -0.144f, -0.108f, 0.072f)
private val BLUR_RADIUS = 80.dp
private const val SATURATION = 2.75f
private const val FRAME_INTERVAL_NS = 33_333_333L

/** Darkening for pages with nothing on top, so white lyrics stay readable over a bright cover. */
private const val DEFAULT_DIM = 0.3f

// Pulse: how far the page swells at the hardest hit, how many times faster the copies turn then, and how
// fast a hit fades. A white flash was tried first and drowned the motion out.
private const val PULSE_ZOOM = 0.08f
private const val PULSE_SPIN = 3f
private const val PULSE_DECAY_S = 0.14f

private const val TAG = "LyricsBackground"
