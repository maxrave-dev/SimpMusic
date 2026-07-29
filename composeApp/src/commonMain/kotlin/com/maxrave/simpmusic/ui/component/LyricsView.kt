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
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
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
import dev.chrisbanes.haze.HazeTint
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

private const val MIN_WIPE_MS = 150

private val DimOriginalColor = Color.LightGray.copy(alpha = 0.35f)
private val DimTranslatedColor = Color(0xFF97971A).copy(alpha = 0.3f)
private val DimRichPendingColor = Color.LightGray.copy(alpha = 0.6f)

private data class TimedLineIndex(
    val index: Int,
    val startTimeMs: Long,
)

private fun List<TimedLineIndex>.activeIndexAt(nowMs: Long): Int {
    if (isEmpty()) return -1
    if (nowMs < first().startTimeMs) return -1
    var lo = 0
    var hi = size - 1
    var ans = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (this[mid].startTimeMs <= nowMs) {
            ans = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return if (ans >= 0) this[ans].index else -1
}

private fun buildSyncedTranslatedWordsByLineIndex(
    originalLines: List<com.maxrave.domain.data.model.metadata.Line>,
    translatedLines: List<com.maxrave.domain.data.model.metadata.Line>,
    thresholdMs: Long = 1000L,
): Map<Int, String> {
    if (originalLines.isEmpty() || translatedLines.isEmpty()) return emptyMap()

    val sortedTranslated =
        translatedLines
            .mapNotNull { line ->
                val ts = line.startTimeMs.toLongOrNull() ?: return@mapNotNull null
                ts to line.words
            }.sortedBy { it.first }

    if (sortedTranslated.isEmpty()) return emptyMap()

    data class OriginalEntry(val index: Int, val ts: Long)

    val sortedOriginal =
        originalLines
            .mapIndexedNotNull { index, line ->
                val ts = line.startTimeMs.toLongOrNull() ?: return@mapIndexedNotNull null
                OriginalEntry(index, ts)
            }.sortedBy { it.ts }

    if (sortedOriginal.isEmpty()) return emptyMap()

    val result = HashMap<Int, String>(sortedOriginal.size)
    var j = 0
    for (orig in sortedOriginal) {
        while (j + 1 < sortedTranslated.size && sortedTranslated[j + 1].first <= orig.ts) {
            j++
        }
        val candA = sortedTranslated[j]
        val diffA = abs(candA.first - orig.ts)
        var bestTs = candA.first
        var bestWords = candA.second
        var bestDiff = diffA
        if (j + 1 < sortedTranslated.size) {
            val candB = sortedTranslated[j + 1]
            val diffB = abs(candB.first - orig.ts)
            if (diffB < bestDiff) {
                bestTs = candB.first
                bestWords = candB.second
                bestDiff = diffB
            }
        }
        if (bestDiff < thresholdMs) {
            result[orig.index] = bestWords
            @Suppress("UNUSED_VARIABLE")
            val _bt = bestTs
        }
    }
    return result
}

@Composable
fun LyricsView(
    lyricsData: NowPlayingScreenData.LyricsData,
    timeLine: StateFlow<TimeLine>,
    onLineClick: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showScrollShadows: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    hasBlurBackground: Boolean = false,
    syncOffsetMs: Long = 0L
) {
    val listState = rememberLazyListState()
    val current by timeLine.collectAsStateWithLifecycle()
    var currentLineIndex by remember { mutableIntStateOf(-1) }
    var currentLineHeight by remember { mutableIntStateOf(0) }
    val effectiveTime by remember(current.current, syncOffsetMs) {
        derivedStateOf { current.current + syncOffsetMs }
    }
    val syncedTranslatedWordsByLineIndex = remember(lyricsData) {
        buildSyncedTranslatedWordsByLineIndex(
            originalLines = lyricsData.lyrics.lines ?: emptyList(),
            translatedLines = lyricsData.translatedLyrics?.first?.lines ?: emptyList(),
        )
    }

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
                (
                    current.current in (
                        0..(
                            lines.getOrNull(0)?.startTimeMs
                                ?: "0"
                            ).toLong()
                        )
                    )
            ) {
                currentLineIndex = -1
            }
        } else {
            currentLineIndex = -1
        }
    }
    LaunchedEffect(currentLineIndex, lyricsData.lyrics.syncType) {
        if (currentLineIndex > -1 &&
            (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED")
        ) {
            listState.animateScrollAndCentralizeItem(currentLineIndex)
        }
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
                        syncedTranslatedWordsByLineIndex[index]
                    } else {
                        lyricsData.translatedLyrics
                            ?.first
                            ?.lines
                            ?.getOrNull(index)
                            ?.words
                    }

                line?.words?.let { words ->
                    when {
                        lyricsData.lyrics.syncType == "RICH_SYNCED" -> {
                            val parsedLine =
                                remember(words, line.startTimeMs, line.endTimeMs) {
                                    val result = parseRichSyncWords(words, line.startTimeMs, line.endTimeMs)
                                    result
                                }

                            if (parsedLine != null) {
                                RichSyncLyricsLineItem(
                                    parsedLine = parsedLine,
                                    translatedWords = translatedWords,
                                    currentTimeMs = effectiveTime,
                                    isCurrent = index == currentLineIndex,
                                    modifier =
                                        Modifier
                                            .clickable {
                                                val targetTime = (line.startTimeMs.toFloat() - syncOffsetMs).coerceAtLeast(0f)
                                                onLineClick(targetTime * 100 / timeLine.value.total)
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
                                                val targetTime = (line.startTimeMs.toFloat() - syncOffsetMs).coerceAtLeast(0f)
                                                onLineClick(targetTime * 100 / timeLine.value.total)
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
                                            val targetTime = (line.startTimeMs.toFloat() - syncOffsetMs).coerceAtLeast(0f)
                                            onLineClick(targetTime * 100 / timeLine.value.total)
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
    val activeColor = LocalContentColor.current
    val inactiveColor = LocalContentColor.current.copy(alpha = 0.35f)
    // CORRECCIÓN: Usamos el mismo color dinámico pero con opacidad para lograr un gris perfecto (claro u oscuro).
    val activeTranslatedColor = LocalContentColor.current.copy(alpha = 0.7f)
    val inactiveTranslatedColor = LocalContentColor.current.copy(alpha = 0.35f)

    Crossfade(targetState = isBold) {
        if (it) {
            Column(
                modifier = modifier,
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = originalWords,
                    style = LocalTextStyle.current.merge(typo().headlineLarge),
                    color = if (isCurrent) activeColor else inactiveColor,
                )
                if (translatedWords != null) {
                    Text(
                        text = translatedWords,
                        style = LocalTextStyle.current.merge(typo().bodyMedium),
                        color = if (isCurrent) activeTranslatedColor else inactiveTranslatedColor,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
    if (!isBold) {
        Column(
            modifier = modifier,
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = originalWords,
                style = LocalTextStyle.current.merge(typo().headlineMedium),
                color = inactiveColor,
            )
            if (translatedWords != null) {
                Text(
                    text = translatedWords,
                    style = LocalTextStyle.current.merge(typo().bodyMedium),
                    color = inactiveTranslatedColor,
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
    val currentWordIndex by remember(currentTimeMs, parsedLine.words) {
        derivedStateOf {
            if (!isCurrent) return@derivedStateOf -1
            parsedLine.words.indexOfLast { it.startTimeMs <= currentTimeMs }
        }
    }

    // CORRECCIÓN: Usamos el mismo color dinámico pero con opacidad para la sincronización palabra por palabra.
    val activeTranslatedColor = LocalContentColor.current.copy(alpha = 0.7f)
    val inactiveTranslatedColor = LocalContentColor.current.copy(alpha = 0.35f)

    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(customPadding))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            parsedLine.words.forEachIndexed { index, wordTiming ->
                val wordEndTimeMs =
                    if (index < parsedLine.words.size - 1) {
                        parsedLine.words[index + 1].startTimeMs
                    } else if (parsedLine.lineEndTimeMs == Long.MAX_VALUE || parsedLine.lineEndTimeMs <= wordTiming.startTimeMs) {
                        if (index > 0 && parsedLine.words[index - 1].startTimeMs < wordTiming.startTimeMs) {
                            val prevWordDuration = wordTiming.startTimeMs - parsedLine.words[index - 1].startTimeMs
                            wordTiming.startTimeMs + prevWordDuration
                        } else {
                            wordTiming.startTimeMs + 500L
                        }
                    } else {
                        parsedLine.lineEndTimeMs
                    }
                AnimatedWord(
                    word = wordTiming.text,
                    wordIndex = index,
                    wordStartTimeMs = wordTiming.startTimeMs,
                    wordEndTimeMs = wordEndTimeMs,
                    currentTimeMs = currentTimeMs,
                    isActive = isCurrent && index == currentWordIndex,
                    isPast = isCurrent && index < currentWordIndex,
                    isCurrent = isCurrent,
                    customFontSize = customFontSize,
                )
            }
        }

        if (translatedWords != null) {
            Text(
                text = translatedWords,
                style = LocalTextStyle.current.merge(typo().bodyMedium),
                color = if (isCurrent) activeTranslatedColor else inactiveTranslatedColor,
            )
        }

        Spacer(modifier = Modifier.height(customPadding))
    }
}

@Composable
private fun AnimatedWord(
    word: String,
    wordIndex: Int,
    wordStartTimeMs: Long,
    wordEndTimeMs: Long,
    currentTimeMs: Long,
    isActive: Boolean,
    isPast: Boolean,
    isCurrent: Boolean,
    customFontSize: TextUnit? = null,
) {
    val activeColor = LocalContentColor.current
    val inactiveColor = LocalContentColor.current.copy(alpha = 0.4f)
    val style = LocalTextStyle.current.merge(typo().headlineLarge).copy(
        fontSize = customFontSize ?: typo().headlineLarge.fontSize,
    )

    if (!isCurrent) {
        Text(
            text = word,
            style = style,
            color = inactiveColor,
        )
        return
    }

    val wordDuration = (wordEndTimeMs - wordStartTimeMs).coerceAtLeast(100L)

    Row(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        word.forEachIndexed { charIndex, char ->
            val charStartTimeMs = wordStartTimeMs + (wordDuration * charIndex / word.length)
            val charEndTimeMs =
                if (charIndex < word.length - 1) {
                    wordStartTimeMs + (wordDuration * (charIndex + 1) / word.length)
                } else {
                    wordEndTimeMs
                }

            val charProgress = when {
                isPast || currentTimeMs >= charEndTimeMs -> 1f
                currentTimeMs <= charStartTimeMs -> 0f
                else -> ((currentTimeMs - charStartTimeMs).toFloat() / (charEndTimeMs - charStartTimeMs).toFloat()).coerceIn(0f, 1f)
            }

            val animatedCharProgress by animateFloatAsState(
                targetValue = charProgress,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "charProgress",
            )

            val charColor = androidx.compose.ui.graphics.lerp(inactiveColor, activeColor, animatedCharProgress)

            Text(
                text = char.toString(),
                style = style,
                color = charColor,
            )
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FullscreenLyricsSheet(
    sharedViewModel: SharedViewModel,
    navController: NavController,
    color: Color = MaterialTheme.colorScheme.background,
    shouldHaze: Boolean,
    onDismiss: () -> Unit,
) {
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val windowInsets = WindowInsets.systemBars
    val bgColor = MaterialTheme.colorScheme.background

    var sliderValue by rememberSaveable {
        mutableFloatStateOf(0f)
    }

    var showControlButtons by rememberSaveable {
        mutableStateOf(true)
    }

    var showNowPlayingSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var autoSyncOffsetMs by rememberSaveable { mutableLongStateOf(0L) }
    var isCheckingForIntroSkip by rememberSaveable { mutableStateOf(true) }
    var lastTimeForSync by remember { mutableLongStateOf(-1L) }

    LaunchedEffect(screenDataState.nowPlayingTitle) {
        autoSyncOffsetMs = 0L
        isCheckingForIntroSkip = true
        lastTimeForSync = -1L
    }

    LaunchedEffect(timelineState.current) {
        val current = timelineState.current
        if (isCheckingForIntroSkip) {
            if (lastTimeForSync in 0L..1500L && current > 2000L && current < 120000L) {
                autoSyncOffsetMs = -current
                isCheckingForIntroSkip = false
            } else if (current > 1500L) {
                isCheckingForIntroSkip = false
            }
        }
        lastTimeForSync = current
    }

    val startColor = remember { Animatable(color) }
    val midColor1 = remember { Animatable(color.copy(alpha = 0.95f)) }
    val midColor2 = remember { Animatable(color.copy(alpha = 0.85f)) }
    val endColor = remember { Animatable(bgColor) }

    val gradientAngle: Float
    val gradientOffsetX: Float
    val gradientOffsetY: Float
    if (!shouldHaze) {
        val gradientTransition = rememberInfiniteTransition(label = "lyricsGradient")
        val animatedAngle by gradientTransition.animateFloat(
            initialValue = -45f,
            targetValue = 45f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "lyricsGradientAngle",
        )
        val animatedOffsetX by gradientTransition.animateFloat(
            initialValue = -1500f,
            targetValue = 1500f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "lyricsGradientOffsetX",
        )
        val animatedOffsetY by gradientTransition.animateFloat(
            initialValue = -1000f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "lyricsGradientOffsetY",
        )
        gradientAngle = animatedAngle
        gradientOffsetX = animatedOffsetX
        gradientOffsetY = animatedOffsetY
    } else {
        gradientAngle = 0f
        gradientOffsetX = 0f
        gradientOffsetY = 0f
    }

    LaunchedEffect(color) {
        if (!shouldHaze) {
            launch {
                startColor.animateTo(
                    targetValue = color,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                )
            }
            launch {
                midColor1.animateTo(
                    targetValue = color.copy(alpha = 0.95f),
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                )
            }
            launch {
                midColor2.animateTo(
                    targetValue = color.copy(alpha = 0.85f),
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                )
            }
            launch {
                endColor.animateTo(
                    targetValue = bgColor,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                )
            }
        }
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

    if (screenDataState.lyricsData != null) {
        KeepScreenOn()
    }

    var showQueueBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showInfoBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    val isLightFullScreenBg = startColor.value.luminance() > 0.5f
    val fullScreenTextColor = if (isLightFullScreenBg) Color.Black else Color.White

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.background,
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
                ) {
                    showControlButtons = true
                },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "crossfadeRainbow")
        val rainbowHue by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "rainbowHue",
        )
        val rainbowColor = hsvToColor(rainbowHue, 1f, 1f)
        val sliderTrackColor by animateColorAsState(
            targetValue = if (timelineState.isCrossfading) rainbowColor else fullScreenTextColor,
            animationSpec = tween(300),
            label = "sliderCrossfadeColor",
        )
        Box(modifier = Modifier.fillMaxSize()) {
            val hazeState = rememberHazeState(blurEnabled = true)

            if (shouldHaze) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .hazeSource(hazeState),
                ) {
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
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
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            startColor.value,
                                            midColor1.value,
                                            midColor2.value,
                                            endColor.value.copy(alpha = 0.9f),
                                            endColor.value,
                                        ),
                                    start =
                                        Offset(
                                            x = gradientOffsetX + (cos(gradientAngle * PI.toFloat() / 180f) * 800f),
                                            y = gradientOffsetY + (sin(gradientAngle * PI.toFloat() / 180f) * 800f),
                                        ),
                                    end =
                                        Offset(
                                            x = gradientOffsetX + 2500f + (cos((gradientAngle + 180f) * PI.toFloat() / 180f) * 800f),
                                            y = gradientOffsetY + 2500f + (sin((gradientAngle + 180f) * PI.toFloat() / 180f) * 800f),
                                        ),
                                ),
                            ),
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (shouldHaze) {
                                Modifier.hazeEffect(
                                    hazeState,
                                    style = CupertinoMaterials.regular(),
                                ) {
                                    blurEnabled = true
                                    tints = listOf(HazeTint(Color.Black.copy(alpha = 0.5f)))
                                }
                            } else {
                                Modifier
                            },
                        ).padding(
                            bottom =
                                with(localDensity) {
                                    windowInsets.getBottom(localDensity).toDp()
                                },
                            top =
                                with(localDensity) {
                                    windowInsets.getTop(localDensity).toDp()
                                },
                        ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
                                .data(screenDataState.thumbnailURL)
                                .crossfade(300)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(screenDataState.thumbnailURL)
                                .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(45.dp)
                                .clip(RoundedCornerShape(8.dp)),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = screenDataState.nowPlayingTitle,
                            style = typo().labelSmall,
                            color = fullScreenTextColor,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.clickable {
                                    coroutineScope.launch {
                                        val song = sharedViewModel.nowPlayingState.value?.songEntity
                                        (
                                            song?.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() }
                                                ?: screenDataState.songInfoData?.authorId
                                            )?.let { channelId ->
                                                sheetState.hide()
                                                onDismiss()
                                                navController.navigate(
                                                    ArtistDestination(
                                                        channelId = channelId,
                                                    ),
                                                )
                                            }
                                    }
                                },
                        ) {
                            if (screenDataState.isExplicit) {
                                ExplicitBadge(
                                    modifier =
                                        Modifier
                                            .size(16.dp)
                                            .padding(end = 4.dp),
                                )
                            }
                            Text(
                                text = screenDataState.artistName,
                                style = typo().bodySmall,
                                color = fullScreenTextColor.copy(alpha = 0.7f),
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    CompositionLocalProvider(LocalContentColor provides fullScreenTextColor) {
                        HeartCheckBox(
                            checked = controllerState.isLiked,
                            size = 28,
                        ) {
                            sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showNowPlayingSheet = true },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.baseline_more_vert_24),
                            contentDescription = "",
                            tint = fullScreenTextColor,
                        )
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                ) {
                    Crossfade(
                        targetState = screenDataState.lyricsData != null,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (it) {
                            screenDataState.lyricsData?.let { lyrics ->
                                CompositionLocalProvider(
                                    LocalContentColor provides fullScreenTextColor,
                                    LocalTextStyle provides typo().bodyMedium
                                ) {
                                    LyricsView(
                                        lyricsData = lyrics,
                                        timeLine = sharedViewModel.timeline,
                                        onLineClick = { f ->
                                            sharedViewModel.onUIEvent(UIEvent.UpdateProgress(f))
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        showScrollShadows = true,
                                        backgroundColor = startColor.value,
                                        hasBlurBackground = shouldHaze,
                                        syncOffsetMs = autoSyncOffsetMs
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.unavailable),
                                    style = typo().bodyMedium,
                                    color = fullScreenTextColor,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                Column {
                    Box(
                        Modifier
                            .padding(
                                top = 15.dp,
                            ).padding(horizontal = 20.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Crossfade(timelineState.loading) {
                                if (it) {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                        LinearProgressIndicator(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .padding(
                                                        horizontal = 3.dp,
                                                    ).clip(
                                                        RoundedCornerShape(8.dp),
                                                    ),
                                            color = fullScreenTextColor.copy(alpha = 0.5f),
                                            trackColor = fullScreenTextColor.copy(alpha = 0.2f),
                                            strokeCap = StrokeCap.Round,
                                        )
                                    }
                                } else {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                        LinearProgressIndicator(
                                            progress = { timelineState.bufferedPercent.toFloat() / 100 },
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .padding(
                                                        horizontal = 3.dp,
                                                    ).clip(
                                                        RoundedCornerShape(8.dp),
                                                    ),
                                            color = fullScreenTextColor.copy(alpha = 0.5f),
                                            trackColor = fullScreenTextColor.copy(alpha = 0.2f),
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
                                onValueChange = {
                                    sharedViewModel.onUIEvent(
                                        UIEvent.UpdateProgress(it),
                                    )
                                },
                                valueRange = 0f..100f,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 3.dp)
                                        .align(
                                            Alignment.TopCenter,
                                        ),
                                track = { sliderState ->
                                    SliderDefaults.Track(
                                        modifier =
                                            Modifier
                                                .height(5.dp),
                                        enabled = true,
                                        sliderState = sliderState,
                                        colors =
                                            SliderDefaults.colors().copy(
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
                                        modifier =
                                            Modifier
                                                .height(18.dp)
                                                .width(8.dp)
                                                .padding(
                                                    vertical = 4.dp,
                                                ),
                                        thumbSize = DpSize(8.dp, 8.dp),
                                        interactionSource =
                                            remember {
                                                MutableInteractionSource()
                                            },
                                        colors =
                                            SliderDefaults.colors().copy(
                                                thumbColor = fullScreenTextColor,
                                                activeTrackColor = fullScreenTextColor,
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
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                            ) {
                                Text(
                                    text = formatDuration(timelineState.current),
                                    style = typo().bodyMedium,
                                    color = fullScreenTextColor,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Left,
                                )
                                AnimatedVisibility(
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    visible = timelineState.isCrossfading,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.crossfading),
                                        style = typo().bodyMedium,
                                        color = fullScreenTextColor,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Text(
                                    text = formatDuration(timelineState.total),
                                    style = typo().bodyMedium,
                                    color = fullScreenTextColor,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Right,
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(5.dp),
                            )
                        }

                        item {
                            AnimatedVisibility(
                                visible = showControlButtons,
                                enter =
                                    expandVertically(
                                        tween(300),
                                    ),
                                exit =
                                    shrinkVertically(
                                        tween(300),
                                    ),
                            ) {
                                CompositionLocalProvider(LocalContentColor provides fullScreenTextColor) {
                                    PlayerControlLayout(controllerState) {
                                        sharedViewModel.onUIEvent(it)
                                    }
                                }
                            }
                            AnimatedVisibility(
                                visible = showControlButtons,
                                enter =
                                    expandVertically(
                                        tween(300),
                                    ),
                                exit =
                                    shrinkVertically(
                                        tween(300),
                                    ),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .height(32.dp)
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp),
                                ) {
                                    IconButton(
                                        modifier =
                                            Modifier
                                                .size(24.dp)
                                                .aspectRatio(1f)
                                                .align(Alignment.CenterStart)
                                                .clip(
                                                    CircleShape,
                                                ),
                                        onClick = {
                                            showInfoBottomSheet = true
                                            showControlButtons = true
                                        },
                                    ) {
                                        Icon(imageVector = Icons.Outlined.Info, tint = fullScreenTextColor, contentDescription = "")
                                    }
                                    Row(
                                        Modifier.align(Alignment.CenterEnd),
                                    ) {
                                        Spacer(modifier = Modifier.size(8.dp))
                                        IconButton(
                                            modifier =
                                                Modifier
                                                    .size(24.dp)
                                                    .aspectRatio(1f)
                                                    .clip(
                                                        CircleShape,
                                                    ),
                                            onClick = {
                                                showQueueBottomSheet = true
                                                showControlButtons = true
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                                                tint = fullScreenTextColor,
                                                contentDescription = "",
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }

                if (!showControlButtons) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
    if (showQueueBottomSheet) {
        QueueBottomSheet(
            onDismiss = {
                showQueueBottomSheet = false
            },
        )
    }
    if (showInfoBottomSheet) {
        InfoPlayerBottomSheet(
            onDismiss = {
                showInfoBottomSheet = false
            },
        )
    }
    if (showNowPlayingSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showNowPlayingSheet = false
            },
            navController = navController,
            onNavigateToOtherScreen = {
                onDismiss()
            },
            song = null,
            setSleepTimerEnable = true,
            changeMainLyricsProviderEnable = true,
        )
    }
}