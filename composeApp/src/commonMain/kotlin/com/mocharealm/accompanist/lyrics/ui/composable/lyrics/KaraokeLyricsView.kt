package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.ui.utils.isRtl
import com.mocharealm.accompanist.lyrics.ui.utils.modifier.springPlacement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

internal data class FocusState(
    val firstIndex: Int,
    val allIndices: List<Int>,
    val activeInterludeIndex: Int?,
    val activeIntro: Boolean
)

/**
 * A comprehensive lyrics view that supports Karaoke and Synced lyrics with advanced rendering.
 *
 * This composable handles:
 * - Scrolling and auto-scrolling to the current line
 * - Rendering karaoke lines with syllable-level timing and animations
 * - Rendering synced lines
 * - Displaying breathing dots during instrumental interludes
 * - Determining active and accompaniment lines
 *
 * @param listState The scroll state for the lazy list.
 * @param lyrics The lyrics data to display.
 * @param currentPosition A lambda returning the current playback position in milliseconds.
 * @param onLineClicked Callback when a line is clicked (seek to position).
 * @param onLinePressed Callback when a line is long-pressed (share/menu).
 * @param modifier The modifier to apply to the layout.
 * @param normalLineTextStyle The style for normal text lines.
 * @param accompanimentLineTextStyle The style for accompaniment/background vocals lines.
 * @param textColor The primary text color.
 * @param breathingDotsDefaults Styling defaults for the breathing dots.
 * @param blendMode The blend mode used for rendering text (e.g., [BlendMode.Plus] for glowing effects).
 * @param useBlurEffect Whether to apply blur effect to non-active lines.
 * @param offset The vertical padding/offset at the start and end of the list.
 * @param showDebugRectangles Debug flag to draw bounding boxes around glyphs.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun KaraokeLyricsView(
    listState: LazyListState,
    lyrics: SyncedLyrics,
    currentPosition: () -> Int,
    onLineClicked: (ISyncedLine) -> Unit,
    onLinePressed: (ISyncedLine) -> Unit,
    modifier: Modifier = Modifier,
    normalLineTextStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        textMotion = TextMotion.Animated,
    ),
    accompanimentLineTextStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        textMotion = TextMotion.Animated,
    ),
    textColor: Color = Color.White,
    breathingDotsDefaults: KaraokeBreathingDotsDefaults = KaraokeBreathingDotsDefaults(),
    phoneticTextStyle: TextStyle = normalLineTextStyle.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
    ),
//    TODO: expose it
//    verticalFadeBrush: Brush = Brush.verticalGradient(
//        0f to Color.White.copy(0f),
//        0.05f to Color.White,
//        0.6f to Color.White,
//        1f to Color.White.copy(0f)
//    ),
    blendMode: BlendMode = BlendMode.Plus,
    useBlurEffect: Boolean = true,
    showTranslation: Boolean = true,
    showPhonetic: Boolean = true,
    offset: Dp = 32.dp,
    keepAliveZone: Dp = 100.dp,
    blurDelta: Float = 3f,
    showDebugRectangles: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    footerContent: (@Composable () -> Unit)? = null,
) {
    val density = LocalDensity.current
    val stableNormalTextStyle = remember(normalLineTextStyle) { normalLineTextStyle }
    val stableAccompanimentTextStyle =
        remember(accompanimentLineTextStyle) { accompanimentLineTextStyle }
    val stablePhoneticTextStyle = remember(phoneticTextStyle) { phoneticTextStyle }
    val stableOffset = remember(offset) { offset }
    val stableOffsetPx =
        remember(stableOffset) { with(density) { stableOffset.toPx().fastRoundToInt() } }
    val keepAliveZonePx = with(density) { keepAliveZone.toPx() }
    val stableBlendMode = remember(blendMode) { blendMode }

    val textMeasurer = rememberTextMeasurer()
    val layoutCache = remember { mutableStateMapOf<Int, List<SyllableLayout>>() }

    LaunchedEffect(
        lyrics,
        stableNormalTextStyle,
        stableAccompanimentTextStyle,
        stablePhoneticTextStyle
    ) {
        layoutCache.clear()
        withContext(Dispatchers.Default) {
            val normalStyle = stableNormalTextStyle.copy(textDirection = TextDirection.Content)
            val accompanimentStyle =
                stableAccompanimentTextStyle.copy(textDirection = TextDirection.Content)
            val phoneticStyle = stablePhoneticTextStyle.copy(textDirection = TextDirection.Content)

            val normalSpaceWidth = textMeasurer.measure(" ", normalStyle).size.width.toFloat()
            val accompanimentSpaceWidth =
                textMeasurer.measure(" ", accompanimentStyle).size.width.toFloat()

            val batchedLayouts = mutableMapOf<Int, List<SyllableLayout>>()

            lyrics.lines.forEachIndexed { index, line ->
                if (!isActive) return@forEachIndexed
                if (line is KaraokeLine) {
                    val style =
                        if (line is KaraokeLine.AccompanimentKaraokeLine) accompanimentStyle else normalStyle
                    val spaceWidth =
                        if (line is KaraokeLine.AccompanimentKaraokeLine) accompanimentSpaceWidth else normalSpaceWidth

                    val processedSyllables = if (line.alignment == KaraokeAlignment.End) {
                        line.syllables.dropLastWhile { it.content.isBlank() }
                    } else {
                        line.syllables
                    }

                    val layout = measureSyllablesAndDetermineAnimation(
                        syllables = processedSyllables,
                        textMeasurer = textMeasurer,
                        style = style,
                        phoneticStyle = phoneticStyle,
                        isAccompanimentLine = line is KaraokeLine.AccompanimentKaraokeLine,
                        spaceWidth = spaceWidth
                    )

                    batchedLayouts[index] = layout
                }
            }

            withContext(Dispatchers.Main) {
                layoutCache.putAll(batchedLayouts)
            }
        }
    }

    val currentTimeMs: () -> Int = currentPosition

    val timeProvider = remember { currentPosition }

    val accompanimentToMainMap = remember(lyrics.lines) {
        val map = mutableMapOf<Int, Int>()
        val mainLinesIndices = lyrics.lines.indices.filter { index ->
            val line = lyrics.lines[index]
            line !is KaraokeLine || line !is KaraokeLine.AccompanimentKaraokeLine
        }
        if (mainLinesIndices.isNotEmpty()) {
            lyrics.lines.forEachIndexed { index, line ->
                if (line is KaraokeLine && line is KaraokeLine.AccompanimentKaraokeLine) {
                    // Find the main line that is closest in time (either the one just before or just after)
                    val beforeIdx = mainLinesIndices.findLast { it <= index }
                    val afterIdx = mainLinesIndices.find { it >= index }

                    val anchorIndex = when {
                        beforeIdx != null && afterIdx != null -> {
                            val distBefore =
                                (line.start - lyrics.lines[beforeIdx].start).absoluteValue
                            val distAfter =
                                (lyrics.lines[afterIdx].start - line.start).absoluteValue
                            if (distBefore <= distAfter) beforeIdx else afterIdx
                        }

                        beforeIdx != null -> beforeIdx
                        afterIdx != null -> afterIdx
                        else -> mainLinesIndices.first()
                    }
                    map[index] = anchorIndex
                }
            }
        }
        map
    }
    val effectiveEndTimes = remember(lyrics.lines) {
        val count = lyrics.lines.size
        val ends = IntArray(count) { index ->
            val line = lyrics.lines[index]
            var maxEnd = line.end
            if (line is KaraokeLine.MainKaraokeLine) {
                line.accompanimentLines?.forEach { acc ->
                    if (acc.end > maxEnd) maxEnd = acc.end
                }
            }
            val nextLine = lyrics.lines.getOrNull(index + 1)
            if (nextLine != null && (nextLine.start - maxEnd <= 7000)) {
                nextLine.start
            } else {
                maxEnd
            }
        }

        // 1. Grouped Multi-Line Co-Termination:
        // If Line A (i) and Line B (i+1) are active together (overlapping start/ends),
        // they must stay active together and ONLY terminate together when Line C starts or at a >7s instrumental gap.
        for (i in 0 until count - 1) {
            val currentLine = lyrics.lines[i]
            val nextLine = lyrics.lines[i + 1]
            val currentEnd = currentLine.end
            val nextEnd = nextLine.end

            if (nextLine.start < ends[i] || nextLine.start < currentEnd) {
                val thirdLine = lyrics.lines.getOrNull(i + 2)
                val groupEnd = if (thirdLine != null && (thirdLine.start - nextEnd <= 7000)) {
                    thirdLine.start
                } else {
                    maxOf(currentEnd, nextEnd)
                }
                ends[i] = groupEnd
                ends[i + 1] = groupEnd
            }
        }

        // 2. Maximum 2 Active Lines Guard:
        // If line 1 is active and line 2 is active, but line 3 starts,
        // line 1 must terminate immediately the instant line 3 begins.
        for (i in 0 until count - 2) {
            val thirdLine = lyrics.lines[i + 2]
            if (thirdLine.start < ends[i]) {
                ends[i] = thirdLine.start
            }
        }

        ends
    }

    val firstLine = lyrics.lines.firstOrNull()

    val haveDotsIntro by remember(firstLine) {
        derivedStateOf {
            if (firstLine == null) false
            else (firstLine.start > 7000)
        }
    }

    val lyricsFocusState by remember(lyrics, effectiveEndTimes, accompanimentToMainMap, haveDotsIntro) {
        derivedStateOf {
            val time = currentTimeMs()
            val lastLyricEnd = effectiveEndTimes.lastOrNull() ?: lyrics.lines.lastOrNull()?.end ?: 0
            val isLyricsFinished = lyrics.lines.isNotEmpty() && time >= lastLyricEnd

            if (isLyricsFinished) {
                // When lyrics have finished completely, terminate active state:
                // No line is active, allIndices is empty, no auto-scroll.
                return@derivedStateOf FocusState(-1, emptyList(), null, false)
            }

            val activeIndex = lyrics.lines.indices.find { idx ->
                time >= lyrics.lines[idx].start && time < effectiveEndTimes[idx]
            }

            val activeInterludeIndex = lyrics.lines.indices.find { index ->
                val line = lyrics.lines[index]
                val previousLine = lyrics.lines.getOrNull(index - 1)
                previousLine != null && (line.start - previousLine.end > 7000) && time in previousLine.end..line.start
            }
            val activeIntro = haveDotsIntro && time in 0 until (firstLine?.start ?: 0)

            // Switch lines only when the upcoming line starts (or during an instrumental gap).
            // Do not jump ahead to line B during a short pause; stay focused on line A.
            val first = when {
                activeIntro -> 0
                activeInterludeIndex != null -> activeInterludeIndex
                activeIndex != null -> activeIndex
                else -> {
                    val prevIdx = lyrics.lines.indexOfLast { it.start <= time }
                    if (prevIdx != -1) prevIdx else 0
                }
            }

            val base = lyrics.lines.indices.filter { index ->
                time >= lyrics.lines[index].start && time < effectiveEndTimes[index]
            }
            val result = base.toMutableSet()
            if (result.isEmpty() && !activeIntro && activeInterludeIndex == null && lyrics.lines.isNotEmpty()) {
                result.add(first)
            }
            base.forEach { index ->
                val line = lyrics.lines.getOrNull(index)
                if (line is KaraokeLine && line is KaraokeLine.AccompanimentKaraokeLine) {
                    accompanimentToMainMap[index]?.let { result.add(it) }
                }
            }

            FocusState(first, result.toList().sorted(), activeInterludeIndex, activeIntro)
        }
    }

    val scrollInCode = remember { mutableStateOf(false) }

    val isUserInteracting = remember { mutableStateOf(false) }

    // Detect user scroll gestures and debounce auto-scroll resume by 3 seconds (AmLyrics.ts)
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !scrollInCode.value) {
            isUserInteracting.value = true
        } else if (!listState.isScrollInProgress && isUserInteracting.value) {
            delay(3000L)
            isUserInteracting.value = false
        }
    }

    val isManualScrolling by remember {
        derivedStateOf {
            isUserInteracting.value || (listState.isScrollInProgress && !scrollInCode.value)
        }
    }

    LaunchedEffect(
        layoutCache,
        stableOffsetPx,
    ) {
        androidx.compose.runtime.snapshotFlow { lyricsFocusState.firstIndex to isUserInteracting.value }
            .collect { (firstIndex, userInteracting) ->
                if (firstIndex >= 0 && !scrollInCode.value && !userInteracting && !listState.isScrollInProgress) {
                    val items = listState.layoutInfo.visibleItemsInfo
                    val targetItem = items.firstOrNull { it.index == firstIndex }
                    val scrollOffset =
                        (targetItem?.offset?.minus(listState.layoutInfo.viewportStartOffset + stableOffsetPx + keepAliveZonePx))
                    try {
                        scrollInCode.value = true
                        if (scrollOffset != null) {
                            listState.scrollBy(scrollOffset.toFloat())
                        } else {
                            listState.animateScrollToItem(
                                firstIndex,
                                (-stableOffsetPx - keepAliveZonePx).toInt()
                            )
                        }
                    } catch (_: Exception) {
                    } finally {
                        scrollInCode.value = false
                    }
                }
            }
    }
    LookaheadScope {
        Crossfade(lyrics) { lyrics ->
            Box(modifier = modifier.clipToBounds()) {
                LazyColumn(
                    state = listState,
                    modifier = modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                val topFade = 20.dp.toPx() / size.height
                                val bottomFade = 100.dp.toPx() / size.height
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        topFade to Color.Black,
                                        1f - bottomFade to Color.Black,
                                        1f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                        .layout { measurable, constraints ->
                            val extraHeightPx = (keepAliveZone * 2).roundToPx()

                            val placeable = measurable.measure(
                                constraints.copy(
                                    maxHeight = constraints.maxHeight + extraHeightPx
                                )
                            )

                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(0, -(keepAliveZone.roundToPx()))
                            }
                        },
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        top = stableOffset + keepAliveZone + contentPadding.calculateTopPadding(),
                        end = contentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                        bottom = stableOffset + keepAliveZone + contentPadding.calculateBottomPadding()
                    )
                ) {
                    itemsIndexed(
                        items = lyrics.lines,
                        key = { index, line -> "${line.start}-${line.end}-$index" }
                    ) { index, line ->
                        val isCurrentFocusLine = index in lyricsFocusState.allIndices
                        val isLineRtl =
                            when (line) {
                                is KaraokeLine -> {
                                    remember(line.syllables) { line.syllables.any { it.content.isRtl() } }
                                }

                                else -> false
                            }
                        val isLineRightAligned = when (line) {
                            is KaraokeLine -> {
                                remember { line.alignment == KaraokeAlignment.End }
                            }

                            else -> false
                        }
                        val isVisualRightAligned = remember(isLineRightAligned, isLineRtl) {
                            if (isLineRightAligned) !isLineRtl
                            else isLineRtl
                        }

                        val distanceWeightState = remember(useBlurEffect, lyricsFocusState) {
                            derivedStateOf {
                                if (lyricsFocusState.allIndices.isEmpty()) {
                                    0
                                } else {
                                    val start = lyricsFocusState.allIndices.first()
                                    val end = lyricsFocusState.allIndices.last()
                                    maxOf(0, start - index, index - end)
                                }
                            }
                        }

                        val dynamicStiffness by remember(distanceWeightState.value) {
                            derivedStateOf {
                                (140f - (distanceWeightState.value * 18f)).coerceIn(25f, 140f)
                            }
                        }

                        val dynamicDamping by remember(distanceWeightState.value) {
                            derivedStateOf {
                                (0.82f + (distanceWeightState.value * 0.02f)).coerceIn(0.82f, 0.96f)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .springPlacement(
                                    this@LookaheadScope,
                                    "${line.start}-${line.end}-$index",
                                    isManualScrolling,
                                    stiffness = dynamicStiffness,
                                    dampingRatio = dynamicDamping
                                ),
                            horizontalAlignment = if (isVisualRightAligned) Alignment.End else Alignment.Start
                        ) {
                            val animDuration = 600

                            val previousLine = lyrics.lines.getOrNull(index - 1)

                            val showDotsInterlude = lyricsFocusState.activeInterludeIndex == index
                            val showDotsIntro = lyricsFocusState.activeIntro && index == 0

                            AnimatedVisibility(showDotsInterlude || showDotsIntro) {
                                KaraokeBreathingDots(
                                    alignment = when (val line = previousLine ?: firstLine) {
                                        is KaraokeLine -> line.alignment
                                        is SyncedLine -> if (line.content.isRtl()) KaraokeAlignment.End else KaraokeAlignment.Start
                                        else -> KaraokeAlignment.Start
                                    },
                                    startTimeMs = previousLine?.end ?: 0,
                                    endTimeMs = if (showDotsIntro) firstLine!!.start else line.start,
                                    currentTimeProvider = timeProvider,
                                    defaults = breathingDotsDefaults,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }


                            val itemBlurRadius: () -> Float = {
                                if (!useBlurEffect || isManualScrolling || distanceWeightState.value <= 0) 0f
                                else distanceWeightState.value * blurDelta
                            }

                            when (line) {
                                is KaraokeLine -> {
                                    if (line is KaraokeLine.MainKaraokeLine) {
                                        val nextLine = lyrics.lines.getOrNull(index + 1)
                                        val nextLineStartMs = nextLine?.start
                                        val nextLineHasBackground = (nextLine as? KaraokeLine.MainKaraokeLine)?.accompanimentLines?.isNotEmpty() == true

                                        LyricsLineItem(
                                            isFocused = isCurrentFocusLine,
                                            isRightAligned = isVisualRightAligned,
                                            onLineClicked = {
                                                isUserInteracting.value = false
                                                onLineClicked(line)
                                            },
                                            onLinePressed = { onLinePressed(line) },
                                            blurRadius = itemBlurRadius,
                                            blendMode = stableBlendMode,
                                        ) {
                                            KaraokeLineText(
                                                line = line,
                                                currentTimeProvider = timeProvider,
                                                nextLineStartMs = nextLineStartMs,
                                                nextLineHasBackground = nextLineHasBackground,
                                                effectiveEndTimeMs = effectiveEndTimes.getOrNull(index),
                                                normalLineTextStyle = stableNormalTextStyle,
                                                accompanimentLineTextStyle = stableAccompanimentTextStyle,
                                                phoneticTextStyle = stablePhoneticTextStyle,
                                                activeColor = textColor,
                                                blendMode = stableBlendMode,
                                                showDebugRectangles = showDebugRectangles,
                                                showTranslation = showTranslation,
                                                showPhonetic = showPhonetic,
                                                precalculatedLayouts = layoutCache[index]
                                            )
                                        }
                                    }
                                }

                                is SyncedLine -> {
                                    val isLineRtl = remember(line.content) { line.content.isRtl() }
                                    LyricsLineItem(
                                        isFocused = isCurrentFocusLine,
                                        isRightAligned = isLineRtl,
                                        onLineClicked = {
                                            isUserInteracting.value = false
                                            onLineClicked(line)
                                        },
                                        onLinePressed = { onLinePressed(line) },
                                        blurRadius = itemBlurRadius,
                                        blendMode = stableBlendMode,
                                    ) {
                                        SyncedLineText(
                                            line = line,
                                            isLineRtl = isLineRtl,
                                            textStyle = stableNormalTextStyle,
                                            textColor = textColor,
                                            showTranslation = showTranslation,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (footerContent != null) {
                        item("FooterContent") {
                            footerContent()
                        }
                    }
                    item("BottomSpacing") {
                        Spacer(
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                    }
                }
            }
        }
    }
}