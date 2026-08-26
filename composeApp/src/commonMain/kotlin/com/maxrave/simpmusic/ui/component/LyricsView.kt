package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import com.maxrave.domain.repository.LyricsRomanizerRepository
import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsSheet
import com.maxrave.simpmusic.ui.component.lyrics.toShareLyricsLines
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.screen.player.content.stripRichSyncTimestamps
import org.koin.compose.koinInject
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.simpmusic.extension.KeepScreenOn
import com.maxrave.simpmusic.extension.ParsedRichSyncLine
import com.maxrave.simpmusic.extension.animateScrollAndAnchorItemTop
import com.maxrave.simpmusic.extension.animateScrollAndCentralizeItem
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.hsvToColor
import com.maxrave.simpmusic.extension.parseRichSyncWords
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.share_lyrics
import simpmusic.composeapp.generated.resources.unavailable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.max

private const val TAG = "LyricsView"

// Minimum wipe animation duration. Words shorter than this still wipe over MIN_WIPE_MS so the
// motion stays perceivable; the snap-to-1f on isPast catches up at the actual word end.
private const val MIN_WIPE_MS = 150

// Emphasis maths ported from AMLL (applemusic-like-lyrics),
// packages/core/src/lyric-player/dom/lyric-line.ts -> initEmphasizeAnimation. Kept to its exact
// constants rather than re-derived: the shape of these curves IS the effect.
//
// The key part is the exponent switch. Below the reference duration the strength is CUBED, so a
// half-second word gets almost nothing; above it the strength is a SQUARE ROOT, so a very long
// note grows but flattens out. A linear ramp — which is what a first guess produces — makes every
// ordinary word shimmer and every held note underwhelming, i.e. exactly backwards.
private const val EMP_AMOUNT_REF_MS = 2000f
private const val EMP_BLUR_REF_MS = 3000f
private const val EMP_MIN_DURATION_MS = 1000f
private const val EMP_AMOUNT_GAIN = 0.6f
private const val EMP_BLUR_GAIN = 0.5f
private const val EMP_AMOUNT_CAP = 1.2f
private const val EMP_BLUR_CAP = 0.8f
// AMLL leans on the LAST word of a line — that is where a singer holds, so it earns extra.
private const val EMP_LAST_WORD_AMOUNT = 1.6f
private const val EMP_LAST_WORD_BLUR = 1.5f
// Both are fractions of the font size (AMLL expresses them in `em`), never fixed dp.
private const val EMP_SCALE_EM = 0.1f
private const val EMP_RISE_EM = 0.025f
private const val EMP_GLOW_RADIUS_EM = 0.3f

// The bloom every sung word carries; a held word replaces this with AMLL's larger emphasis glow.
// The flare on the word currently being sung. Bright, because it marks one word out of a line
// rather than washing over the whole sung half.
// The provider caption under the lyrics: quiet enough to sit beneath them, still readable on
// purpose — it is dimmed, never blurred.
// How far the travelling glow reaches, in characters. Below ~1 the light goes out between
// characters; far above it the whole word lifts at once.
private const val FLARE_REACH_CHARS = 1.5f

// The gutter this sheet has always used for its lyrics column.
private val FULLSCREEN_LYRICS_GUTTER = 50.dp

private const val FOOTER_ALPHA = 0.45f

private const val SUNG_BASE_GLOW_ALPHA = 0.75f
private const val SUNG_BASE_GLOW_EM = 0.26f


// makeEmpEasing(0.5): bezIn up to the midpoint, then 1 - bezOut back down — a bump that rises
// faster than it falls, which is why it reads as a voice pushing rather than a sine wobble.
private val EmpBezIn = CubicBezierEasing(0.2f, 0.4f, 0.58f, 1f)
private val EmpBezOut = CubicBezierEasing(0.3f, 0f, 0.58f, 1f)

private fun empEasing(x: Float): Float =
    if (x < 0.5f) {
        EmpBezIn.transform((x / 0.5f).coerceIn(0f, 1f))
    } else {
        1f - EmpBezOut.transform(((x - 0.5f) / 0.5f).coerceIn(0f, 1f))
    }

// Repeated lyrics palette tokens hoisted to file scope: avoids re-allocating
// the same Color() objects on every recomposition of every line item.
private val DimOriginalColor = Color.LightGray.copy(alpha = 0.35f)
private val DimTranslatedColor = Color(0xFF97971A).copy(alpha = 0.3f)

// A pronunciation guide sits BETWEEN the original and its translation in meaning, so it does the
// same visually: neutral grey rather than the translation's yellow, dimmer than the original it
// belongs to. Two values because the sung line lifts everything on it.
private val DimRomanizedCurrentColor = Color.White.copy(alpha = 0.7f)
private val DimRomanizedColor = Color.LightGray.copy(alpha = 0.3f)
private val DimRichPendingColor = Color.LightGray.copy(alpha = 0.6f)

private data class TimedLineIndex(
    val index: Int,
    val startTimeMs: Long,
)

/**
 * Returns the original line index of the last [TimedLineIndex] whose [TimedLineIndex.startTimeMs]
 * is `<= nowMs`. Assumes the receiver is sorted ascending by [TimedLineIndex.startTimeMs].
 *
 * Rules:
 *  - empty list -> -1
 *  - nowMs strictly before the first start time -> -1
 *  - nowMs after the last start time -> the last entry's original index (sticky last line)
 */
private fun List<TimedLineIndex>.activeIndexAt(nowMs: Long): Int {
    if (isEmpty()) return -1
    if (nowMs < first().startTimeMs) return -1
    // Binary search for the last item whose startTimeMs <= nowMs.
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

/**
 * Builds a [Map] from each ORIGINAL line index to its closest synced translated `words`
 * within [thresholdMs]. Two-pointer over both sorted lists; on ties an earlier translated
 * line (smaller startTimeMs in the sorted list) wins for determinism.
 *
 * Lines with invalid `startTimeMs` on either side are skipped.
 */
private fun buildSyncedTranslatedWordsByLineIndex(
    originalLines: List<com.maxrave.domain.data.model.metadata.Line>,
    translatedLines: List<com.maxrave.domain.data.model.metadata.Line>,
    thresholdMs: Long = 1000L,
): Map<Int, String> {
    if (originalLines.isEmpty() || translatedLines.isEmpty()) return emptyMap()

    // Sort translated entries by start time. We keep the original list order as a
    // tie-breaker via stable sort: the FIRST translated line in the SORTED list wins
    // when the time delta is equal.
    val sortedTranslated =
        translatedLines
            .mapNotNull { line ->
                val ts = line.startTimeMs.toLongOrNull() ?: return@mapNotNull null
                ts to line.words
            }.sortedBy { it.first }

    if (sortedTranslated.isEmpty()) return emptyMap()

    // Original lines paired with their parsed timestamp + original index, sorted by time.
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
        // Advance j so that sortedTranslated[j] is the first translated entry with ts >= orig.ts,
        // or the last entry if everything is smaller.
        while (j + 1 < sortedTranslated.size && sortedTranslated[j + 1].first <= orig.ts) {
            j++
        }
        // Candidate window: j and j+1 (the next one), pick whichever is closer.
        val candA = sortedTranslated[j]
        val diffA = abs(candA.first - orig.ts)
        var bestTs = candA.first
        var bestWords = candA.second
        var bestDiff = diffA
        if (j + 1 < sortedTranslated.size) {
            val candB = sortedTranslated[j + 1]
            val diffB = abs(candB.first - orig.ts)
            // Tie-break: prefer earlier (smaller startTimeMs) translated line.
            if (diffB < bestDiff) {
                bestTs = candB.first
                bestWords = candB.second
                bestDiff = diffB
            }
        }
        if (bestDiff < thresholdMs) {
            result[orig.index] = bestWords
            // Suppress unused warning while keeping the chosen ts visible for future tweaks.
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
    backgroundColor: Color = Color(0xFF242424),
    // Optional trailing slot rendered as the LAST list item, so a caller's caption scrolls with
    // the lyrics instead of sitting anchored below them. Null by default: every existing caller
    // renders exactly as before.
    footerContent: (@Composable () -> Unit)? = null,
    dataStoreManager: DataStoreManager = koinInject(),
    romanizer: LyricsRomanizerRepository = koinInject(),
) {
    val listState = rememberLazyListState()
    // AMLL drops blur to zero while the user is scrolling by hand (resolveBlurLevel:
    // `if (this.scrollState.isTouchScrolled || isFocused) return 0`) — you are reading ahead at
    // that moment, and out-of-focus text is not readable. Dragged, not isScrollInProgress: the
    // latter is also true for the player's own animated scroll, which must stay blurred.
    val isDragging by listState.interactionSource.collectIsDraggedAsState()
    val current by timeLine.collectAsStateWithLifecycle()

    // Read here rather than taken as a parameter: all four call sites (the fullscreen sheet and
    // the three player styles) want the user's one choice, so making them each thread it through
    // would be four copies of the same lookup. Re-checked against isLyricsBlurSupported() even
    // though Settings hides the option below Android 12 — a DataStore restored from a backup, or
    // carried to another device, can still hold APPLE_MUSIC on a phone that cannot draw it.
    val lyricsStyle by dataStoreManager.lyricsStyle.collectAsStateWithLifecycle(DataStoreManager.LYRICS_STYLE_CLASSIC)
    val appleStyle = lyricsStyle == DataStoreManager.LYRICS_STYLE_APPLE_MUSIC && isLyricsBlurSupported()

    // Read here for the same reason the style is: all four call sites want the user's one choice,
    // so threading it through each of them would be four copies of the same lookup. The stored
    // value is a comma-separated list of enum names; parsing it per RECOMPOSITION would rebuild the
    // set on every frame of the sung-line animation, hence the remember on the raw string.
    val romanizationStored by dataStoreManager.romanizationLanguages.collectAsStateWithLifecycle("")
    val romanizationLanguages =
        remember(romanizationStored) { RomanizationLanguage.parse(romanizationStored) }

    // One text row plus the padding that separates two lyric items — the exact amount of the
    // previous line that stays on screen above the sung one.
    val exposedRowPx =
        with(LocalDensity.current) {
            AppleMusicLyricLineHeight.toPx() + AppleMusicLyricGap.toPx()
        }

    val timedLineIndexes =
        remember(lyricsData.lyrics.lines) {
            lyricsData.lyrics.lines
                .orEmpty()
                .mapIndexedNotNull { index, line ->
                    line.startTimeMs.toLongOrNull()?.let { TimedLineIndex(index, it) }
                }.sortedBy { it.startTimeMs }
        }

    val currentLineIndex by remember(timedLineIndexes) {
        derivedStateOf {
            val now = current.current
            if (now <= 0L) -1 else timedLineIndexes.activeIndexAt(now)
        }
    }

    val syncedTranslatedWordsByLineIndex =
        remember(
            lyricsData.lyrics.lines,
            lyricsData.translatedLyrics?.first?.lines,
        ) {
            buildSyncedTranslatedWordsByLineIndex(
                originalLines = lyricsData.lyrics.lines.orEmpty(),
                translatedLines = lyricsData.translatedLyrics?.first?.lines.orEmpty(),
                thresholdMs = 1000L,
            )
        }
    LaunchedEffect(currentLineIndex, lyricsData.lyrics.syncType, appleStyle) {
        if (currentLineIndex > -1 &&
            (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED")
        ) {
            if (appleStyle) {
                // NEAR the top, not against it: Apple leaves exactly ONE physical row of the
                // previous lyric visible above the line being sung. Scrolling to `index - 1`
                // instead — which is what this did first — anchors the whole previous ITEM, and a
                // lyric that wraps is one item spanning two or three rows, so the entire wrapped
                // block hung above the sung line. Anchoring the sung line itself and backing off by
                // one row's height is row-accurate no matter how the previous line wrapped.
                listState.animateScrollAndAnchorItemTop(currentLineIndex, -exposedRowPx)
            } else {
                listState.animateScrollAndCentralizeItem(currentLineIndex)
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        // Apple keeps the sung line at the TOP even when it is the last line of the song — which
        // is only possible if there is empty space below it to scroll into. Without this tail the
        // list simply runs out of content and the closing lines pile up against the bottom edge,
        // so the final third of every song reads bottom-anchored instead of top-anchored.
        val tailPadding = if (appleStyle) maxHeight * 0.72f else 0.dp
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = tailPadding),
        ) {
            items(lyricsData.lyrics.lines?.size ?: 0) { index ->
                val line = lyricsData.lyrics.lines?.getOrNull(index)
                // Translated lyrics: synced -> precomputed map by line index, unsynced -> by index.
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
                    // Signed distance from the line being sung. The shell is the only place that
                    // knows it, and it is the ONLY extra input the Apple Music renderer needs —
                    // which is why the split lives here and not inside the line items.
                    val distanceFromCurrent = if (currentLineIndex < 0) 0 else index - currentLineIndex

                    // Romanization is its OWN row, between the original and the translation, so the
                    // reader can follow the original script and still know how to pronounce it.
                    //
                    // Being a separate row is also what makes rich sync painless: the original
                    // keeps its <mm:ss.xx> markers untouched and goes on lighting up word by word,
                    // while this row is plain static text. It only has to have the markers STRIPPED
                    // before romanizing, or the timestamps themselves would be transliterated.
                    val romanizedWords =
                        if (romanizationLanguages.isEmpty()) {
                            null
                        } else {
                            remember(words, romanizationLanguages) {
                                val source =
                                    if (lyricsData.lyrics.syncType == "RICH_SYNCED") words.stripRichSyncTimestamps() else words
                                romanizer.romanize(source, romanizationLanguages)
                            }
                        }

                    val renderLine: @Composable () -> Unit = {
                        when {
                            // Rich sync: parse and use RichSyncLyricsLineItem
                            lyricsData.lyrics.syncType == "RICH_SYNCED" -> {
                                val parsedLine =
                                    remember(words, line.startTimeMs, line.endTimeMs) {
                                        val result = parseRichSyncWords(words, line.startTimeMs, line.endTimeMs)
                                        result
                                    }

                                if (parsedLine != null) {
                                    // Reused verbatim by BOTH styles: word-by-word highlighting is
                                    // already what Apple does with a rich-synced line, so there is
                                    // nothing to re-implement — only the focus treatment differs,
                                    // and that is applied by the wrapper below.
                                    RichSyncLyricsLineItem(
                                        parsedLine = parsedLine,
                                        translatedWords = translatedWords,
                                        romanizedWords = romanizedWords,
                                        currentTimeMs = current.current,
                                        isCurrent = index == currentLineIndex,
                                        customFontSize = if (appleStyle) AppleMusicLyricFontSize else null,
                                        glow = if (appleStyle && index == currentLineIndex) AppleMusicActiveLineGlow else null,
                                        pendingColorOverride = if (appleStyle) AppleMusicPendingWordColor else null,
                                        translatedColorOverride = if (appleStyle) AppleMusicTranslatedColor else null,
                                        translatedStyleOverride =
                                            if (appleStyle) {
                                                typo().bodyMedium.copy(
                                                    fontSize = AppleMusicSubLineFontSize,
                                                    lineHeight = AppleMusicSubLineHeight,
                                                )
                                            } else {
                                                null
                                            },
                                        customPadding = if (appleStyle) AppleMusicLyricGap else 12.dp,
                                        wrappedLineSpacing = if (appleStyle) AppleMusicWrappedLineSpacing else 0.dp,
                                        // Apple takes its click from the wrapper Column below, which
                                        // covers the translation too; a click here as well would put
                                        // a second, smaller target on top of it.
                                        modifier =
                                            if (appleStyle) {
                                                Modifier
                                            } else {
                                                Modifier.clickable {
                                                    onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                                }
                                            },
                                    )
                                } else if (appleStyle) {
                                    // Parsing failed — fall back to a plain line, but still the
                                    // Apple-shaped one, or a single unparsable line would render
                                    // at a different size than every line around it.
                                    AppleMusicLyricsLineItem(
                                        originalWords = words,
                                        translatedWords = translatedWords,
                                        isCurrent = index == currentLineIndex,
                                        romanizedWords = romanizedWords,
                                    )
                                } else {
                                    // Fallback to regular line item if parsing fails
                                    LyricsLineItem(
                                        originalWords = words,
                                        translatedWords = translatedWords,
                                        isBold = index <= currentLineIndex,
                                        isCurrent = index == currentLineIndex,
                                        romanizedWords = romanizedWords,
                                        modifier =
                                            Modifier
                                                .clickable {
                                                    onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                                },
                                    )
                                }
                            }

                            // Line sync or unsynced
                            appleStyle -> {
                                AppleMusicLyricsLineItem(
                                    originalWords = words,
                                    translatedWords = translatedWords,
                                    romanizedWords = romanizedWords,
                                    // Strictly the sung line — NOT Classic's `|| syncType != LINE_SYNCED`.
                                    // That clause exists so an unsynced lyric sheet renders every line
                                    // bold instead of every line dimmed. Carried over here it makes
                                    // EVERY line "current": white, glowing, unblurred — which is why
                                    // two lines showed lit at once. An unsynced sheet has no sung line,
                                    // and this style says so by leaving them all grey.
                                    isCurrent = index == currentLineIndex,
                                )
                            }

                            // Line sync or unsynced: use existing LyricsLineItem
                            else -> {
                                LyricsLineItem(
                                    originalWords = words,
                                    translatedWords = translatedWords,
                                    romanizedWords = romanizedWords,
                                    isBold = index <= currentLineIndex || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                    isCurrent = index == currentLineIndex || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                    modifier =
                                        Modifier
                                            .clickable(enabled = lyricsData.lyrics.syncType == "LINE_SYNCED") {
                                                onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                            },
                                )
                            }
                        }
                    }

                    if (appleStyle) {
                        // The whole wrapper is the tap target — original line AND translation — the
                        // way AMLL's .lyricLineWrapper is, rather than each Text separately. The
                        // press shows as a tinted rounded panel; indication is null because a
                        // ripple centred on the finger reads as a button, and these are lyrics.
                        val lineInteraction = remember { MutableInteractionSource() }
                        val linePressed by lineInteraction.collectIsPressedAsState()
                        Column(
                            modifier =
                                Modifier
                                    // background(colour, shape), NOT clip(shape) + background().
                                    // clip() cuts everything that leaves this line's box — which is
                                    // exactly what blur and glow are supposed to do. It sliced the
                                    // blur off square at the left edge and erased the bloom around
                                    // the sung line entirely. background() with a shape paints the
                                    // rounded press panel without clipping any of the content.
                                    .background(
                                        color = if (linePressed) AppleMusicLyricPressedBackground else Color.Transparent,
                                        shape = RoundedCornerShape(AppleMusicLyricCornerRadius),
                                    ).clickable(
                                        interactionSource = lineInteraction,
                                        indication = null,
                                        enabled = lyricsData.lyrics.syncType == "LINE_SYNCED" ||
                                            lyricsData.lyrics.syncType == "RICH_SYNCED",
                                    ) {
                                        onLineClick(line.startTimeMs.toFloat() * 100 / timeLine.value.total)
                                    },
                        ) {
                            // Blur spans the FULL width; the gutter is applied inside it. Ordered
                            // the other way — gutter outside, blur around the text — the blurred
                            // box begins exactly where the glyphs begin, so there is no margin for
                            // the softened edge to spill into and it comes out sliced flat down the
                            // left. This is the same problem AMLL solves with `margin: -1em;
                            // padding: 1em`, which widens the painted area without moving the
                            // layout; Compose has no negative padding, so the equivalent is to blur
                            // the wide box and inset the content within it.
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .appleMusicLyricFocus(
                                            distanceFromCurrent,
                                            blurEnabled = !isDragging,
                                            hasActiveLine = currentLineIndex >= 0,
                                        ),
                            ) {
                                Box(modifier = Modifier.padding(horizontal = AppleMusicLyricPaddingX)) {
                                    renderLine()
                                }
                            }
                        }
                    } else {
                        renderLine()
                    }
                }
            }
            footerContent?.let { footer ->
                item {
                    if (appleStyle) {
                        // Same gutter as the lyrics, and dimmed — but NOT blurred. Blur means
                        // "further away in the song"; the caption is not part of the song at all,
                        // it is a note about where the words came from. Blurring it made it look
                        // like a lyric line the reader had lost, and it is meant to stay legible
                        // when someone actually goes looking for it.
                        Box(
                            modifier =
                                Modifier
                                    .padding(horizontal = AppleMusicLyricPaddingX)
                                    .alpha(FOOTER_ALPHA),
                        ) {
                            footer()
                        }
                    } else {
                        footer()
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
    // Between the original and the translation, never in place of either: the point is to read the
    // original script AND know how to say it. Null when romanization is off for this language.
    romanizedWords: String? = null,
    modifier: Modifier = Modifier,
) {
    Crossfade(targetState = isBold) {
        if (it) {
            Column(
                modifier = modifier,
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = originalWords,
                    style = typo().headlineLarge,
                    color = if (isCurrent) Color.White else DimOriginalColor,
                )
                if (romanizedWords != null) {
                    Text(
                        text = romanizedWords,
                        style = typo().bodyMedium,
                        // Neither the original's white nor the translation's yellow: a reading is a
                        // third KIND of thing, and giving it the translation's colour would read as
                        // two translations stacked.
                        color = if (isCurrent) DimRomanizedCurrentColor else DimRomanizedColor,
                    )
                }
                if (translatedWords != null) {
                    Text(
                        text = translatedWords,
                        style = typo().bodyMedium,
                        color = if (isCurrent) Color.Yellow else DimTranslatedColor,
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
                style = typo().headlineMedium,
                color = DimOriginalColor,
            )
            if (romanizedWords != null) {
                Text(
                    text = romanizedWords,
                    style = typo().bodyMedium,
                    color = DimRomanizedColor,
                )
            }
            if (translatedWords != null) {
                Text(
                    text = translatedWords,
                    style = typo().bodyMedium,
                    color = DimTranslatedColor,
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
    // Static text under a line that lights up word by word. Keeping it out of the FlowRow above is
    // deliberate: the wipe is driven by per-word timings this row does not have and must not fake.
    romanizedWords: String? = null,
    currentTimeMs: Long,
    isCurrent: Boolean,
    customFontSize: TextUnit? = null,
    customPadding: Dp = 12.dp,
    // Null for the Classic renderer, so nothing about it changes. The Apple Music style passes a
    // bloom here for the line being sung — applied per WORD rather than per line, because in a
    // rich-synced line only the words already sung are lit.
    glow: Shadow? = null,
    pendingColorOverride: Color? = null,
    // Same deal: null keeps Classic's yellow (and its dimmed variant) exactly as it was. Apple
    // renders translations in white, and this is a SHARED composable — hardcoding white here
    // would repaint the Classic style too.
    translatedColorOverride: Color? = null,
    // Null / 1f keep Classic's bodyMedium at full opacity. Apple Music passes AMLL's sub-line
    // ratios (0.5em, 1.5em leading, 0.3 opacity) — again as parameters, because this composable is
    // shared and hardcoding them would resize Classic's translations too.
    translatedStyleOverride: TextStyle? = null,
    // Zero keeps FlowRow's original Arrangement.Center, so Classic wraps exactly as it always has.
    // Apple Music passes real spacing because its lines are large enough to wrap often, and
    // FlowRow ignores the lineHeight that spaces the non-wrapped renderer's lines.
    wrappedLineSpacing: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val currentWordIndex by remember(currentTimeMs, parsedLine.words) {
        derivedStateOf {
            if (!isCurrent) return@derivedStateOf -1
            parsedLine.words.indexOfLast { it.startTimeMs <= currentTimeMs }
        }
    }

    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(customPadding))

        // Original lyrics with rich sync highlighting - using FlowRow for word wrapping
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement =
                if (wrappedLineSpacing > 0.dp) Arrangement.spacedBy(wrappedLineSpacing) else Arrangement.Center,
        ) {
            parsedLine.words.forEachIndexed { index, wordTiming ->
                // Calculate word end time (start time of next word or line end time)
                // If last word and lineEndTimeMs is invalid (Long.MAX_VALUE), estimate based on previous word duration
                val wordEndTimeMs =
                    if (index < parsedLine.words.size - 1) {
                        parsedLine.words[index + 1].startTimeMs
                    } else if (parsedLine.lineEndTimeMs == Long.MAX_VALUE || parsedLine.lineEndTimeMs <= wordTiming.startTimeMs) {
                        // Estimate: use previous word duration or default 500ms
                        if (index > 0 && parsedLine.words[index - 1].startTimeMs < wordTiming.startTimeMs) {
                            val prevWordDuration = wordTiming.startTimeMs - parsedLine.words[index - 1].startTimeMs
                            wordTiming.startTimeMs + prevWordDuration
                        } else {
                            wordTiming.startTimeMs + 500L // Default 500ms if no reference
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
                    glow = glow,
                    isLastWord = index == parsedLine.words.lastIndex,
                    pendingColorOverride = pendingColorOverride,
                )
            }
        }

        if (romanizedWords != null) {
            Text(
                text = romanizedWords,
                style = translatedStyleOverride ?: typo().bodyMedium,
                color = if (isCurrent) DimRomanizedCurrentColor else DimRomanizedColor,
            )
        }

        // Translated lyrics (line-level, no word sync)
        if (translatedWords != null) {
            Text(
                text = translatedWords,
                style = translatedStyleOverride ?: typo().bodyMedium,
                color = translatedColorOverride ?: if (isCurrent) Color.Yellow else DimTranslatedColor,
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
    glow: Shadow? = null,
    // AMLL emphasises the closing word of a line more than the rest — that is where a singer holds.
    isLastWord: Boolean = false,
    // Null keeps Classic's DimRichPendingColor untouched.
    pendingColorOverride: Color? = null,
) {
    val style =
        typo().headlineLarge.copy(
            fontSize = customFontSize ?: typo().headlineLarge.fontSize,
        )

    if (!isCurrent) {
        Text(text = word, style = style, color = pendingColorOverride ?: DimOriginalColor)
        return
    }

    // Wall-clock wipe driven by an Animatable.
    // - Future word (not active, not past): progress stays at 0.
    // - Active word: snap to current % then animateTo(1f) over the remaining duration of the
    //   word, in real wall-clock time. Independent of timeline emit rate, so wipe is smooth.
    // - Past word: snap to 1f.
    val wordDurationMs = (wordEndTimeMs - wordStartTimeMs).coerceAtLeast(100L)
    val anim =
        remember(wordStartTimeMs, wordEndTimeMs) {
            val initial =
                ((currentTimeMs - wordStartTimeMs).toFloat() / wordDurationMs.toFloat())
                    .coerceIn(0f, 1f)
            androidx.compose.animation.core.Animatable(initial)
        }

    LaunchedEffect(wordStartTimeMs, wordEndTimeMs, isActive, isPast) {
        when {
            isPast -> anim.snapTo(1f)
            isActive -> {
                val now = currentTimeMs
                val current =
                    ((now - wordStartTimeMs).toFloat() / wordDurationMs.toFloat())
                        .coerceIn(0f, 1f)
                anim.snapTo(current)
                // Ensure minimum visible wipe duration so very short words don't flash.
                // Tradeoff: visual may finish ~MIN_WIPE_MS after the actual word end, but the
                // next isPast=true transition will snap to 1f so it stays consistent.
                val remainingMs =
                    (wordEndTimeMs - now).coerceAtLeast(0L).toInt().coerceAtLeast(MIN_WIPE_MS)
                anim.animateTo(1f, tween(remainingMs, easing = LinearEasing))
            }
            // Future word (not active, not past): keep current value.
            // Don't snap to 0 — playback position can jitter backwards by a few ms,
            // briefly flipping isActive false. Snapping would jerk the wipe back.
        }
    }

    val progress = anim.value

    // The word's position, resolved by STATE rather than read raw. The Animatable is what carries
    // the 100ms timeline ticks at frame rate — it snaps to the real position and then animates to
    // the end of the word in wall-clock time, so the wipe keeps moving between ticks instead of
    // stepping ten times a second.
    //
    // But it is only ever snapped on the isActive and isPast branches. A word that has not started
    // holds whatever it was constructed with, which is why reading it directly lit the first letter
    // of every upcoming word. Deciding 0 / anim / 1 from the state fixes that without giving up the
    // interpolation.
    val wordProgress =
        when {
            isPast -> 1f
            isActive -> progress
            else -> 0f
        }

    // AMLL's emphasis strengths, derived from the word's OWN duration.
    val emphasisDurationMs = max(EMP_MIN_DURATION_MS, wordDurationMs.toFloat())
    val amount =
        if (glow == null) {
            0f
        } else {
            val raw = emphasisDurationMs / EMP_AMOUNT_REF_MS
            val shaped = if (raw > 1f) sqrt(raw) else raw * raw * raw
            min(EMP_AMOUNT_CAP, shaped * EMP_AMOUNT_GAIN * if (isLastWord) EMP_LAST_WORD_AMOUNT else 1f)
        }
    val blurAmount =
        if (glow == null) {
            0f
        } else {
            val raw = emphasisDurationMs / EMP_BLUR_REF_MS
            val shaped = if (raw > 1f) sqrt(raw) else raw * raw * raw
            min(EMP_BLUR_CAP, shaped * EMP_BLUR_GAIN * if (isLastWord) EMP_LAST_WORD_BLUR else 1f)
        }
    val eased = if (amount <= 0f && blurAmount <= 0f) 0f else empEasing(wordProgress)
    val fontPx = with(LocalDensity.current) { style.fontSize.toPx() }
    val heldGlow =
        if (eased * blurAmount <= 0.01f) {
            null
        } else {
            glow?.copy(
                color = glow.color.copy(alpha = (eased * blurAmount).coerceIn(0f, 1f)),
                blurRadius = min(EMP_GLOW_RADIUS_EM, blurAmount * EMP_GLOW_RADIUS_EM) * fontPx,
            )
        }

    Box(
        modifier =
            Modifier.graphicsLayer {
                val scale = 1f + eased * EMP_SCALE_EM * amount
                scaleX = scale
                scaleY = scale
                // Rises slightly as it swells — AMLL's offsetY. Negative is upward.
                translationY = -eased * EMP_RISE_EM * amount * fontPx
            },
    ) {
        val chars = word.toCharArray()
        val charCount = chars.size.coerceAtLeast(1)
        Row {
            chars.forEachIndexed { charIndex, ch ->
                val charFrom = charIndex.toFloat() / charCount
                val charTo = (charIndex + 1).toFloat() / charCount
                val charProgress = ((wordProgress - charFrom) / (charTo - charFrom)).coerceIn(0f, 1f)
                val charPast = wordProgress >= charTo
                val charActive = isActive && wordProgress >= charFrom && wordProgress < charTo
                // The flare is a CONTINUOUS falloff from the playhead, not an on/off per character:
                // switching per character killed the light at every boundary and lit it again on the
                // next one, which read as flicker. Fading by distance lets the brightness hand over
                // between neighbours, so one travelling point of light moves across the word.
                val charCenter = (charFrom + charTo) / 2f
                val reach = (FLARE_REACH_CHARS / charCount).coerceAtLeast(0.0001f)
                val charFlare =
                    if (!isActive || glow == null) {
                        0f
                    } else {
                        (1f - abs(wordProgress - charCenter) / reach).coerceIn(0f, 1f)
                    }
                val restingColor = pendingColorOverride ?: DimRichPendingColor
                Box {
                    // Glow: transparent ink, so only the Shadow lands and it follows the glyph
                    // outline instead of boxing the character.
                    //
                    val glowShadow = heldGlow ?: glow?.copy(blurRadius = SUNG_BASE_GLOW_EM * fontPx)
                    if (glowShadow != null) {
                        Text(
                            text = ch.toString(),
                            // Intensity goes into the Shadow's own alpha. NOT graphicsLayer.alpha:
                            // any alpha below 1 forces Compose to render the node into an offscreen
                            // layer first, and that layer is only as big as the Text's bounds — so
                            // the bloom, which by definition spills outside them, comes back sliced
                            // into a rectangle.
                            //
                            // The node is still composed unconditionally, at alpha 0 when the
                            // playhead is far away. That is what stops the hand-over from
                            // flickering: A fades down and B fades up, neither is ever added to or
                            // removed from the tree.
                            style =
                                style.copy(
                                    shadow =
                                        glowShadow.copy(
                                            color = glowShadow.color.copy(alpha = SUNG_BASE_GLOW_ALPHA * charFlare),
                                        ),
                                ),
                            color = Color.Transparent,
                        )
                    }
                    Text(
                        text = ch.toString(),
                        style = style,
                        color =
                            when {
                                charPast -> Color.White
                                charActive -> lerp(restingColor, Color.White, charProgress)
                                else -> restingColor
                            },
                    )
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FullscreenLyricsSheet(
    sharedViewModel: SharedViewModel,
    navController: NavController,
    color: Color = Color(0xFF242424),
    onDismiss: () -> Unit,
) {
    // The Apple Music renderer applies its own gutter inside LyricsView — it has to, because the
    // blur needs margin of its own to spill into and a caller-side gutter leaves it sliced flat at
    // the edge. This sheet's own 50dp then stacked on top of it, insetting the text by 70dp.
    val fullscreenLyricsStyle by sharedViewModel
        .getLyricsStyle()
        .collectAsStateWithLifecycle(DataStoreManager.LYRICS_STYLE_CLASSIC)
    val fullscreenAppleLyrics =
        fullscreenLyricsStyle == DataStoreManager.LYRICS_STYLE_APPLE_MUSIC && isLyricsBlurSupported()
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

    var sliderValue by rememberSaveable {
        mutableFloatStateOf(0f)
    }

    // Auto-hide controls state - Only hide control buttons, not title/progress
    var showControlButtons by rememberSaveable {
        mutableStateOf(true)
    }

    var showNowPlayingSheet by rememberSaveable {
        mutableStateOf(false)
    }

    // Animated gradient colors - SMOOTH ANIMATION
    val startColor = remember { Animatable(color) }
    val midColor1 = remember { Animatable(color.copy(alpha = 0.95f)) }
    val midColor2 = remember { Animatable(color.copy(alpha = 0.85f)) }
    val endColor = remember { Animatable(Color.Black) }

    // Dynamic gradient animation - MULTIPLE DIRECTIONS
    // Replaces the previous `while(true) { delay(16) }` loop with a Compose
    // infinite transition.
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
    val gradientAngle = animatedAngle
    val gradientOffsetX = animatedOffsetX
    val gradientOffsetY = animatedOffsetY

    // Smooth color animation based on lyrics color
    LaunchedEffect(color) {
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
                targetValue = Color.Black,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            )
        }
    }

    // Reset auto-hide timer when controls are shown
    LaunchedEffect(key1 = showControlButtons) {
        if (showControlButtons) {
            delay(4000) // Hide after 4 seconds
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

    var showShareLyricsSheet by rememberSaveable {
        mutableStateOf(false)
    }

    // This sheet renders lyrics through LyricsView, which computes the sung line internally and
    // keeps it to itself. The share sheet needs the same number to open on that line, so it is
    // recomputed here from the same two inputs, using the same helper.
    val shareTimedLineIndexes =
        remember(screenDataState.lyricsData?.lyrics?.lines) {
            screenDataState.lyricsData
                ?.lyrics
                ?.lines
                .orEmpty()
                .mapIndexedNotNull { index, line ->
                    line.startTimeMs.toLongOrNull()?.let { TimedLineIndex(index, it) }
                }.sortedBy { it.startTimeMs }
        }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
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
                ) {
                    // Show controls on tap
                    showControlButtons = true
                },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        // Crossfade: RGB rainbow color cycling when transitioning between tracks
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
            targetValue = if (timelineState.isCrossfading) rainbowColor else Color.White,
            animationSpec = tween(300),
            label = "sliderCrossfadeColor",
        )
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated gradient background
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

            // ── Foreground content column ─────────────────────────────────────
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
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
                // New Apple Music Style Header
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Song Poster (Small, Top Left)
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

                    // Song Info Column
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        // Song Name
                        Text(
                            text = screenDataState.nowPlayingTitle,
                            style = typo().labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Artist Name with Explicit Badge
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
                                color = Color.White.copy(alpha = 0.7f),
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

                    // Like Button (Heart)
                    HeartCheckBox(
                        checked = controllerState.isLiked,
                        size = 28,
                    ) {
                        sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Share lyrics — only when there are lyrics to share.
                    if (screenDataState.lyricsData != null) {
                        IconButton(
                            onClick = { showShareLyricsSheet = true },
                        ) {
                            Icon(
                                imageVector = SimpIcons.Share,
                                contentDescription = stringResource(Res.string.share_lyrics),
                                tint = Color.White,
                            )
                        }
                    }

                    // Three Dot Menu
                    IconButton(
                        onClick = { showNowPlayingSheet = true },
                    ) {
                        Icon(
                            imageVector = SimpIcons.MoreVert,
                            contentDescription = "",
                            tint = Color.White,
                        )
                    }
                }

                // Lyrics Content - Expands when controls are hidden
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            // Same TOTAL gutter either way, just split differently. The Apple
                            // renderer applies AppleMusicLyricPaddingX itself so its blur has
                            // margin to spill into, so the sheet contributes the remainder — the
                            // text still lines up with this screen's header and slider. Dropping
                            // the sheet's side to zero (which this did for one revision) left the
                            // lyrics sitting further out than everything else on the page.
                            .padding(
                                horizontal =
                                    if (fullscreenAppleLyrics) {
                                        FULLSCREEN_LYRICS_GUTTER - AppleMusicLyricPaddingX
                                    } else {
                                        FULLSCREEN_LYRICS_GUTTER
                                    },
                            ),
                ) {
                    Crossfade(
                        targetState = screenDataState.lyricsData != null,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (it) {
                            screenDataState.lyricsData?.let { lyrics ->
                                LyricsView(
                                    lyricsData = lyrics,
                                    timeLine = sharedViewModel.timeline,
                                    onLineClick = { f ->
                                        sharedViewModel.onUIEvent(UIEvent.UpdateProgress(f))
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    showScrollShadows = true,
                                    backgroundColor = startColor.value,
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
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

                // Progress Bar and Time - Always visible
                Column {
                    // Real Slider
                    Box(
                        Modifier
                            .padding(
                                top = 15.dp,
                            ).padding(horizontal = 40.dp),
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
                                            color = Color.Gray,
                                            trackColor = Color.DarkGray,
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
                                // Fraction, not 0..100 — see the note in NowPlayingScreen:
                                // material3 alpha25 drops valueRange on its binary-compatibility
                                // overload.
                                value = sliderValue / 100f,
                                onValueChange = {
                                    sharedViewModel.onUIEvent(
                                        UIEvent.UpdateProgress(it * 100f),
                                    )
                                },
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
                            // Time Layout
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 40.dp),
                            ) {
                                Text(
                                    text = formatDuration(timelineState.current),
                                    style = typo().bodyMedium,
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

                            Spacer(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(5.dp),
                            )
                        }

                        item {
                            // Control Buttons - Animated visibility
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
                                PlayerControlLayout(controllerState) {
                                    sharedViewModel.onUIEvent(it)
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
                                // List Bottom Buttons
                                Box(
                                    modifier =
                                        Modifier
                                            .height(32.dp)
                                            .fillMaxWidth()
                                            .padding(horizontal = 40.dp),
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
                                        Icon(imageVector = SimpIcons.Info, tint = Color.White, contentDescription = "")
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
                                                imageVector = SimpIcons.QueueMusic,
                                                tint = Color.White,
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

                // When control buttons are hidden, add spacer to maintain proper spacing
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

    screenDataState.lyricsData?.let { lyricsData ->
        if (showShareLyricsSheet) {
            ShareLyricsSheet(
                lines = lyricsData.toShareLyricsLines(),
                songTitle = screenDataState.nowPlayingTitle,
                artistName = screenDataState.artistName,
                artwork = screenDataState.bitmap,
                seedColor = color,
                initialLineIndex = shareTimedLineIndexes.activeIndexAt(timelineState.current),
                onDismiss = { showShareLyricsSheet = false },
            )
        }
    }
}