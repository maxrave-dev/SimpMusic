package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.simpmusic.extension.KeepScreenOn
import com.maxrave.simpmusic.extension.ParsedRichSyncLine
import com.maxrave.simpmusic.extension.animateScrollAndCentralizeItem
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.hsvToColor
import com.maxrave.simpmusic.extension.parseRichSyncWords
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_more_vert_24
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.unavailable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "LyricsView"

@Composable
fun LyricsView(
    lyricsData: NowPlayingScreenData.LyricsData,
    timeLine: StateFlow<TimeLine>,
    onLineClick: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showScrollShadows: Boolean = false,
    backgroundColor: Color = Color(0xFF242424),
    hasBlurBackground: Boolean = false,
) {
    var currentLineHeight by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val current by timeLine.collectAsStateWithLifecycle()
    var currentLineIndex by rememberSaveable { mutableIntStateOf(-1) }

    LaunchedEffect(key1 = current) {
        val lines = lyricsData.lyrics.lines
        if (current.current > 0L) {
            lines?.indices?.forEach { i ->
                val sentence = lines[i]
                val startTimeMs = sentence.startTimeMs.toLong()
                val endTimeMs =
                    if (i < lines.size - 1) {
                        lines[i + 1].startTimeMs.toLong()
                    } else {
                        startTimeMs + 60000
                    }
                if (current.current in startTimeMs..endTimeMs) {
                    currentLineIndex = i
                }
            }
            if (!lines.isNullOrEmpty() &&
                (current.current in (0..(lines.getOrNull(0)?.startTimeMs ?: "0").toLong()))
            ) {
                currentLineIndex = -1
            }
        } else {
            currentLineIndex = -1
        }
    }

    LaunchedEffect(key1 = currentLineIndex, key2 = currentLineHeight, key3 = current) {
        if (currentLineIndex > -1 && currentLineHeight > 0 &&
            (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED") &&
            !current.isCrossfading
        ) {
            listState.animateScrollAndCentralizeItem(index = currentLineIndex, this)
        }
    }

    fun findClosestTranslatedLine(originalTimeMs: String): String? {
        val translatedLines = lyricsData.translatedLyrics?.first?.lines ?: return null
        if (translatedLines.isEmpty()) return null
        val originalTime = originalTimeMs.toLongOrNull() ?: return null
        return translatedLines
            .minByOrNull { abs((it.startTimeMs.toLongOrNull() ?: 0L) - originalTime) }
            ?.let {
                val abs = abs((it.startTimeMs.toLongOrNull() ?: 0L) - originalTime)
                if (abs < 1000L) it else null
            }?.words
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(lyricsData.lyrics.lines?.size ?: 0) { index ->
                val line = lyricsData.lyrics.lines?.getOrNull(index)
                val translatedWords =
                    if (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED") {
                        line?.startTimeMs?.let { findClosestTranslatedLine(it) }
                    } else {
                        lyricsData.translatedLyrics?.first?.lines?.getOrNull(index)?.words
                    }

                line?.words?.let { words ->
                    when {
                        lyricsData.lyrics.syncType == "RICH_SYNCED" -> {
                            val parsedLine =
                                remember(words, line.startTimeMs, line.endTimeMs) {
                                    parseRichSyncWords(words, line.startTimeMs, line.endTimeMs)
                                }

                            if (parsedLine != null) {
                                RichSyncLyricsLineItem(
                                    parsedLine = parsedLine,
                                    translatedWords = translatedWords,
                                    currentTimeMs = current.current,
                                    isCurrent = index == currentLineIndex,
                                    modifier =
                                        Modifier
                                            .clickable {
                                                onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                            }.onGloballyPositioned { c ->
                                                currentLineHeight = c.size.height
                                            },
                                )
                            } else {
                                LyricsLineItem(
                                    originalWords = words,
                                    translatedWords = translatedWords,
                                    isBold = index <= currentLineIndex,
                                    isCurrent = index == currentLineIndex,
                                    modifier =
                                        Modifier
                                            .clickable {
                                                onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                            }.onGloballyPositioned { c ->
                                                currentLineHeight = c.size.height
                                            },
                                )
                            }
                        }

                        else -> {
                            LyricsLineItem(
                                originalWords = words,
                                translatedWords = translatedWords,
                                isBold = index <= currentLineIndex || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                isCurrent = index == currentLineIndex || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                modifier =
                                    Modifier
                                        .clickable(enabled = lyricsData.lyrics.syncType == "LINE_SYNCED") {
                                            onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                        }.onGloballyPositioned { c ->
                                            currentLineHeight = c.size.height
                                        },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsLineItem(
    originalWords: String,
    translatedWords: String?,
    isBold: Boolean,
    isCurrent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Crossfade(targetState = isBold) {
        if (it) {
            Column(modifier = modifier) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    modifier = Modifier.then(if (isCurrent) Modifier else Modifier.blur(1.dp)),
                    text = originalWords,
                    style = typo().headlineLarge,
                    color = if (isCurrent) Color.White else Color.LightGray.copy(alpha = 0.35f),
                )
                if (translatedWords != null) {
                    Text(
                        modifier = Modifier.then(if (isCurrent) Modifier else Modifier.blur(1.dp)),
                        text = translatedWords,
                        style = typo().bodyMedium,
                        color = if (isCurrent) Color.Yellow else Color(0xFF97971A).copy(alpha = 0.3f),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
    if (!isBold) {
        Column(modifier = modifier) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                modifier = Modifier.blur(1.dp),
                text = originalWords,
                style = typo().headlineMedium,
                color = Color.LightGray.copy(alpha = 0.35f),
            )
            if (translatedWords != null) {
                Text(
                    modifier = Modifier.blur(1.dp),
                    text = translatedWords,
                    style = typo().bodyMedium,
                    color = Color(0xFF97971A).copy(alpha = 0.3f),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichSyncLyricsLineItem(
    parsedLine: ParsedRichSyncLine,
    translatedWords: String?,
    currentTimeMs: Long,
    isCurrent: Boolean,
    customFontSize: TextUnit? = null,
    customPadding: Dp = 12.dp,
    modifier: Modifier = Modifier,
) {
    val currentWordIndex by remember(currentTimeMs, parsedLine.words, isCurrent) {
        derivedStateOf {
            if (!isCurrent) return@derivedStateOf -1
            parsedLine.words.indexOfLast { it.startTimeMs <= currentTimeMs }
        }
    }

    val lineScale by animateFloatAsState(
        targetValue = if (isCurrent) 1.0f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "lineScale",
    )

    Column(
        modifier = modifier.graphicsLayer {
            scaleX = lineScale
            scaleY = lineScale
            transformOrigin = TransformOrigin(0f, 0.5f)
        },
    ) {
        Spacer(modifier = Modifier.height(customPadding))

        FlowRow(
            modifier = Modifier.then(
                if (!isCurrent) Modifier.blur(1.8.dp) else Modifier
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            parsedLine.words.forEachIndexed { index, wordTiming ->
                val wordEndMs = parsedLine.words.getOrNull(index + 1)?.startTimeMs
                    ?: (wordTiming.startTimeMs + 600L)

                KaraokeWord(
                    word = wordTiming.text,
                    wordStartMs = wordTiming.startTimeMs,
                    wordEndMs = wordEndMs,
                    currentTimeMs = currentTimeMs,
                    isActive = isCurrent && index == currentWordIndex,
                    isPast = isCurrent && index < currentWordIndex,
                    isCurrent = isCurrent,
                    customFontSize = customFontSize,
                )
            }
        }

        if (translatedWords != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                modifier = Modifier.then(
                    if (!isCurrent) Modifier.blur(1.5.dp) else Modifier
                ),
                text = translatedWords,
                style = typo().bodyMedium,
                color = if (isCurrent) Color.Yellow else Color(0xFF97971A).copy(alpha = 0.3f),
            )
        }

        Spacer(modifier = Modifier.height(customPadding))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// KARAOKE WIPE WORD  —  KMP-compatible glow
//
// How the glow works without any Android-only APIs:
//
//   Box (word-sized)
//   ├── Text  dim   — full width, low alpha  (always visible)
//   ├── Text  glow  — white, blurred via Modifier.blur(), alpha = effectiveGlow
//   │              clipped to wipeProgress width via drawWithContent + drawRect
//   └── Text  lit   — solid white, same clip, drawn sharp on top
//
// Modifier.blur() is a Compose multiplatform modifier — no Android import needed.
// The clip is done with drawWithContent so we never leave commonMain.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun KaraokeWord(
    word: String,
    wordStartMs: Long,
    wordEndMs: Long,
    currentTimeMs: Long,
    isActive: Boolean,
    isPast: Boolean,
    isCurrent: Boolean,
    customFontSize: TextUnit? = null,
    modifier: Modifier = Modifier,
) {
    val baseStyle = typo().headlineLarge.copy(
        fontSize = customFontSize ?: typo().headlineLarge.fontSize,
    )

    // ── Wipe progress 0.0 → 1.0 ──────────────────────────────────────────────
    val rawProgress = when {
        !isCurrent -> 0f
        isPast     -> 1f
        isActive   -> {
            val duration = (wordEndMs - wordStartMs).coerceAtLeast(1L)
            ((currentTimeMs - wordStartMs).toFloat() / duration).coerceIn(0f, 1f)
        }
        else       -> 0f
    }

    val wipeProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "wipeProgress_$word",
    )

    // ── Glow base: smooth ramp when isActive toggles ──────────────────────────
    val glowBase = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(isActive) {
        glowBase.animateTo(
            targetValue = if (isActive) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (isActive) 120 else 250,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    // ── Glow pulse: gentle breathe while active ───────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "wPulse_$word")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wPulseVal_$word",
    )

    val effectiveGlow = glowBase.value * (0.78f + 0.22f * glowPulse)

    // ── Wipe clip modifier (commonMain-safe) ──────────────────────────────────
    // drawWithContent lets us clip to [0, width * wipeProgress] without
    // any platform APIs — we simply skip drawing outside that rect by
    // only issuing drawContent() inside a clipRect call on the DrawScope.
    fun Modifier.wipeClip(progress: Float): Modifier = this.drawWithContent {
        if (progress <= 0f) return@drawWithContent
        val clipWidth = size.width * progress
        // clip to left portion and draw
        drawContext.canvas.save()
        drawContext.canvas.clipRect(
            left   = 0f,
            top    = 0f,
            right  = clipWidth,
            bottom = size.height,
        )
        drawContent()
        drawContext.canvas.restore()
    }

    Box(modifier = modifier) {
        // ── Layer 1: dim text — always full width ─────────────────────────────
        Text(
            text = word,
            style = baseStyle,
            color = Color.White.copy(alpha = if (!isCurrent) 0.22f else 0.35f),
        )

        // ── Layer 2: glow text — blurred, clipped to wipe width ───────────────
        // Modifier.blur() is pure Compose — works in commonMain.
        // Alpha is driven by effectiveGlow so it fades in/out and breathes.
        if (wipeProgress > 0f && effectiveGlow > 0.01f) {
            Text(
                text = word,
                style = baseStyle,
                color = Color.White.copy(alpha = (effectiveGlow * 0.9f).coerceIn(0f, 1f)),
                modifier = Modifier
                    .blur(
                        // blur radius grows with glow — 6dp max
                        radiusX = (6.dp * effectiveGlow),
                        radiusY = (6.dp * effectiveGlow),
                    )
                    .wipeClip(wipeProgress),
            )
        }

        // ── Layer 3: sharp lit text — clipped to wipe width ───────────────────
        if (wipeProgress > 0f) {
            Text(
                text = word,
                style = baseStyle,
                color = Color.White,
                modifier = Modifier.wipeClip(wipeProgress),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// APPLE MUSIC AURORA GRADIENT BACKGROUND
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AuroraBlobBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val fallback = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
    val palette = (colors + fallback).take(3)

    val animOrb0 = remember { Animatable(palette[0]) }
    val animOrb1 = remember { Animatable(palette[1]) }
    val animOrb2 = remember { Animatable(palette[2]) }

    LaunchedEffect(palette[0]) { animOrb0.animateTo(palette[0], tween(1800, easing = FastOutSlowInEasing)) }
    LaunchedEffect(palette[1]) { animOrb1.animateTo(palette[1], tween(2000, easing = FastOutSlowInEasing)) }
    LaunchedEffect(palette[2]) { animOrb2.animateTo(palette[2], tween(2200, easing = FastOutSlowInEasing)) }

    val orb0Color = animOrb0.value.copy(alpha = 0.60f)
    val orb1Color = animOrb1.value.copy(alpha = 0.55f)
    val orb2Color = animOrb2.value.copy(alpha = 0.50f)

    val infiniteTransition = rememberInfiniteTransition(label = "auroraMaster")
    val masterTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "masterTime",
    )

    fun multiSine(
        t: Float, center: Float, spread: Float,
        f1: Float, p1: Float, a1: Float,
        f2: Float, p2: Float, a2: Float,
        f3: Float, p3: Float, a3: Float,
    ): Float {
        val total = a1 + a2 + a3
        val raw = (
            a1 * sin(f1 * t + p1).toFloat() +
                a2 * sin(f2 * t + p2).toFloat() +
                a3 * sin(f3 * t + p3).toFloat()
            ) / total
        return (center + raw * spread * 0.5f).coerceIn(0.02f, 0.98f)
    }

    val orb0X = multiSine(masterTime, 0.32f, 0.52f, 1f, 0.00f, 0.50f, 3f, 1.20f, 0.30f, 5f, 2.80f, 0.20f)
    val orb0Y = multiSine(masterTime, 0.26f, 0.44f, 1f, 0.50f, 0.45f, 3f, 2.10f, 0.35f, 7f, 0.70f, 0.20f)
    val orb1X = multiSine(masterTime, 0.72f, 0.48f, 2f, 1.60f, 0.40f, 4f, 0.30f, 0.35f, 6f, 3.50f, 0.25f)
    val orb1Y = multiSine(masterTime, 0.68f, 0.48f, 2f, 2.40f, 0.42f, 4f, 0.90f, 0.33f, 6f, 4.10f, 0.25f)
    val orb2X = multiSine(masterTime, 0.54f, 0.58f, 3f, 3.10f, 0.38f, 5f, 1.70f, 0.37f, 7f, 0.40f, 0.25f)
    val orb2Y = multiSine(masterTime, 0.50f, 0.62f, 3f, 4.70f, 0.40f, 5f, 2.30f, 0.35f, 7f, 1.10f, 0.25f)

    Box(
        modifier = modifier.drawBehind {
            val w = size.width
            val h = size.height
            drawRect(Color(0xFF06060E))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb0Color, orb0Color.copy(alpha = orb0Color.alpha * 0.45f), Color.Transparent),
                    center = Offset(orb0X * w, orb0Y * h), radius = w * 0.82f,
                ),
                radius = w * 0.82f, center = Offset(orb0X * w, orb0Y * h),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb1Color, orb1Color.copy(alpha = orb1Color.alpha * 0.38f), Color.Transparent),
                    center = Offset(orb1X * w, orb1Y * h), radius = w * 0.72f,
                ),
                radius = w * 0.72f, center = Offset(orb1X * w, orb1Y * h),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb2Color, orb2Color.copy(alpha = orb2Color.alpha * 0.32f), Color.Transparent),
                    center = Offset(orb2X * w, orb2Y * h), radius = w * 0.64f,
                ),
                radius = w * 0.64f, center = Offset(orb2X * w, orb2Y * h),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xAA000000),
                        0.22f to Color.Transparent,
                        0.78f to Color.Transparent,
                        1.00f to Color(0xCC000000),
                    ),
                ),
            )
        },
    )
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FullscreenLyricsSheet(
    sharedViewModel: SharedViewModel,
    navController: NavController,
    color: Color = Color(0xFF242424),
    shouldHaze: Boolean,
    onDismiss: () -> Unit,
) {
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()

    val artworkColors by sharedViewModel.artworkGradientColors.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val windowInsets = WindowInsets.systemBars

    var sliderValue by rememberSaveable { mutableFloatStateOf(0f) }
    var showControlButtons by rememberSaveable { mutableStateOf(true) }
    var showNowPlayingSheet by rememberSaveable { mutableStateOf(false) }

    val startColor = remember { Animatable(color) }
    LaunchedEffect(color) {
        startColor.animateTo(color, tween(1200, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(key1 = showControlButtons) {
        if (showControlButtons) {
            delay(4000)
            showControlButtons = false
        }
    }

    LaunchedEffect(key1 = timelineState) {
        sliderValue =
            if (timelineState.total > 0L) {
                timelineState.current.toFloat() * 100 / timelineState.total.toFloat()
            } else {
                0f
            }
    }

    if (screenDataState.lyricsData != null) KeepScreenOn()

    var showQueueBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showInfoBottomSheet by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        containerColor = Color.Black,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = .5f),
        sheetState = sheetState,
        modifier =
            Modifier
                .fillMaxHeight()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { showControlButtons = true },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "crossfadeRainbow")
        val rainbowHue by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "rainbowHue",
        )
        val rainbowColor = hsvToColor(rainbowHue, 1f, 1f)
        val sliderTrackColor by animateColorAsState(
            targetValue = if (timelineState.isCrossfading) rainbowColor else Color.White,
            animationSpec = tween(300),
            label = "sliderCrossfadeColor",
        )

        Box(modifier = Modifier.fillMaxSize()) {
            val hazeState = rememberHazeState(blurEnabled = true)

            if (shouldHaze) {
                Box(modifier = Modifier.fillMaxSize().hazeSource(hazeState)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(screenDataState.thumbnailURL)
                            .crossfade(300)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(screenDataState.thumbnailURL)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                val resolvedColors = if (artworkColors.isNotEmpty()) artworkColors
                else listOf(color, color.copy(alpha = 0.7f), Color(0xFF0A0A0A))
                AuroraBlobBackground(colors = resolvedColors, modifier = Modifier.fillMaxSize())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (shouldHaze) Modifier.hazeEffect(hazeState, style = CupertinoMaterials.regular()) {
                            blurEnabled = true
                        } else Modifier
                    )
                    .padding(
                        bottom = with(localDensity) { windowInsets.getBottom(localDensity).toDp() },
                        top = with(localDensity) { windowInsets.getTop(localDensity).toDp() },
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(screenDataState.thumbnailURL)
                            .crossfade(300)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(screenDataState.thumbnailURL)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(45.dp).clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = screenDataState.nowPlayingTitle,
                            style = typo().labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier
                                .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                .focusable(),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                coroutineScope.launch {
                                    val song = sharedViewModel.nowPlayingState.value?.songEntity
                                    (
                                        song?.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() }
                                            ?: screenDataState.songInfoData?.authorId
                                        )?.let { channelId ->
                                            sheetState.hide()
                                            onDismiss()
                                            navController.navigate(ArtistDestination(channelId = channelId))
                                        }
                                }
                            },
                        ) {
                            if (screenDataState.isExplicit) {
                                ExplicitBadge(modifier = Modifier.size(16.dp).padding(end = 4.dp))
                            }
                            Text(
                                text = screenDataState.artistName,
                                style = typo().bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                modifier = Modifier
                                    .basicMarquee(iterations = Int.MAX_VALUE, animationMode = MarqueeAnimationMode.Immediately)
                                    .focusable(),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    HeartCheckBox(checked = controllerState.isLiked, size = 28) {
                        sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showNowPlayingSheet = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.baseline_more_vert_24),
                            contentDescription = "",
                            tint = Color.White,
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 50.dp)) {
                    Crossfade(targetState = screenDataState.lyricsData != null, modifier = Modifier.fillMaxSize()) {
                        if (it) {
                            screenDataState.lyricsData?.let { lyrics ->
                                LyricsView(
                                    lyricsData = lyrics,
                                    timeLine = sharedViewModel.timeline,
                                    onLineClick = { f -> sharedViewModel.onUIEvent(UIEvent.UpdateProgress(f)) },
                                    modifier = Modifier.fillMaxSize(),
                                    showScrollShadows = true,
                                    backgroundColor = startColor.value,
                                    hasBlurBackground = shouldHaze,
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(Res.string.unavailable),
                                    style = typo().bodyMedium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                Column {
                    Box(Modifier.padding(top = 15.dp).padding(horizontal = 40.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                            Crossfade(timelineState.loading) {
                                if (it) {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth().height(4.dp)
                                                .padding(horizontal = 3.dp).clip(RoundedCornerShape(8.dp)),
                                            color = Color.Gray,
                                            trackColor = Color.DarkGray,
                                            strokeCap = StrokeCap.Round,
                                        )
                                    }
                                } else {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                        LinearProgressIndicator(
                                            progress = { timelineState.bufferedPercent.toFloat() / 100 },
                                            modifier = Modifier.fillMaxWidth().height(4.dp)
                                                .padding(horizontal = 3.dp).clip(RoundedCornerShape(8.dp)),
                                            color = Color.Gray,
                                            trackColor = Color.DarkGray,
                                            strokeCap = StrokeCap.Round,
                                            drawStopIndicator = {},
                                        )
                                    }
                                }
                            }
                        }
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            Slider(
                                value = sliderValue,
                                onValueChange = { sharedViewModel.onUIEvent(UIEvent.UpdateProgress(it)) },
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth().padding(top = 3.dp).align(Alignment.TopCenter),
                                track = { sliderState ->
                                    SliderDefaults.Track(
                                        modifier = Modifier.height(5.dp),
                                        enabled = true,
                                        sliderState = sliderState,
                                        colors = SliderDefaults.colors().copy(
                                            thumbColor = sliderTrackColor,
                                            activeTrackColor = sliderTrackColor,
                                            inactiveTrackColor = Color.Transparent,
                                        ),
                                        thumbTrackGapSize = 0.dp,
                                        drawTick = { _, _ -> },
                                        drawStopIndicator = null,
                                    )
                                },
                                thumb = {
                                    SliderDefaults.Thumb(
                                        modifier = Modifier.height(18.dp).width(8.dp).padding(vertical = 4.dp),
                                        thumbSize = DpSize(8.dp, 8.dp),
                                        interactionSource = remember { MutableInteractionSource() },
                                        colors = SliderDefaults.colors().copy(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color.Transparent,
                                        ),
                                        enabled = true,
                                    )
                                },
                            )
                        }
                    }

                    LazyColumn {
                        item {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
                                Text(
                                    text = formatDuration(timelineState.current),
                                    style = typo().bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Left,
                                )
                                AnimatedVisibility(enter = fadeIn(), exit = fadeOut(), visible = timelineState.isCrossfading) {
                                    Text(
                                        text = stringResource(Res.string.crossfading),
                                        style = typo().bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Text(
                                    text = formatDuration(timelineState.total),
                                    style = typo().bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Right,
                                )
                            }
                            Spacer(modifier = Modifier.fillMaxWidth().height(5.dp))
                        }
                        item {
                            AnimatedVisibility(
                                visible = showControlButtons,
                                enter = expandVertically(tween(300)),
                                exit = shrinkVertically(tween(300)),
                            ) {
                                PlayerControlLayout(controllerState) { sharedViewModel.onUIEvent(it) }
                            }
                            AnimatedVisibility(
                                visible = showControlButtons,
                                enter = expandVertically(tween(300)),
                                exit = shrinkVertically(tween(300)),
                            ) {
                                Box(modifier = Modifier.height(32.dp).fillMaxWidth().padding(horizontal = 40.dp)) {
                                    IconButton(
                                        modifier = Modifier.size(24.dp).aspectRatio(1f).align(Alignment.CenterStart).clip(CircleShape),
                                        onClick = { showInfoBottomSheet = true; showControlButtons = true },
                                    ) {
                                        Icon(imageVector = Icons.Outlined.Info, tint = Color.White, contentDescription = "")
                                    }
                                    Row(Modifier.align(Alignment.CenterEnd)) {
                                        Spacer(modifier = Modifier.size(8.dp))
                                        IconButton(
                                            modifier = Modifier.size(24.dp).aspectRatio(1f).clip(CircleShape),
                                            onClick = { showQueueBottomSheet = true; showControlButtons = true },
                                        ) {
                                            Icon(imageVector = Icons.AutoMirrored.Outlined.QueueMusic, tint = Color.White, contentDescription = "")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }

                if (!showControlButtons) Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showQueueBottomSheet) QueueBottomSheet(onDismiss = { showQueueBottomSheet = false })
    if (showInfoBottomSheet) InfoPlayerBottomSheet(onDismiss = { showInfoBottomSheet = false })
    if (showNowPlayingSheet) {
        NowPlayingBottomSheet(
            onDismiss = { showNowPlayingSheet = false },
            navController = navController,
            onNavigateToOtherScreen = { onDismiss() },
            song = null,
            setSleepTimerEnable = true,
            changeMainLyricsProviderEnable = true,
        )
    }
}
