package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Animatable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import com.maxrave.domain.repository.LyricsRomanizerRepository
import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported
import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentActions
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentState
import com.maxrave.simpmusic.ui.screen.player.content.stripRichSyncTimestamps
import org.koin.compose.koinInject
import androidx.navigation.NavController
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.simpmusic.extension.ParsedRichSyncLine
import com.maxrave.simpmusic.extension.animateScrollAndAnchorItemTop
import com.maxrave.simpmusic.extension.animateScrollAndCentralizeItem
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.parseRichSyncWords
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.max

private const val TAG = "LyricsView"

// The interval the player publishes its position on, on BOTH platforms (MediaServiceHandlerImpl
// and JvmMediaPlayerHandlerImpl both tick on `tickIntervalMs`). It is the ceiling on how far the interpolated
// playhead below is allowed to run ahead of the last real tick, so a stalled tick — paused
// playback, a wedged player — can never drift further than the granularity we already live with.
private const val PLAYHEAD_TICK_MS = 50L

// A rich-synced line followed by a silence at least this long gets a row of dots standing in for
// the instrumental, the way Apple Music marks one. Rich sync only: it is the one format that knows
// where a line actually STOPS, so it is the one format that can tell a real pause from a line whose
// end time was never written down.
private const val INTERLUDE_MIN_GAP_MS = 3_000L

// Three dots, each owning an equal third of the silence: the first fills over the first third, and
// the row is full exactly as the next line begins.
private const val INTERLUDE_DOT_COUNT = 3
private const val INTERLUDE_DOT = "\u2022"

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
private const val FLARE_REACH_CHARS = 3.5f

// How long a word's flare takes to fade in as it becomes the sung one, and to fade out again once
// the playhead has left it. Without this the flare was gated by a hard boolean, so the light died
// at every word boundary and was reborn on the next word — smooth WITHIN a word, broken BETWEEN
// them. Fading the gate makes the outgoing word's last characters still be glowing while the
// incoming word's first ones light up, and the two overlapping reads as one travelling light.
//
// Roughly one sung word long. Much shorter and the hand-over is not visible; much longer and half
// the line glows at once, which is a smear rather than a light.
private const val FLARE_FADE_MS = 2_500

// ...and how long it takes to come UP, which must be its own number. One tween served both
// directions until the release was lengthened for the wake, and that silently made the attack just
// as long: a word starting to be sung took two and a half seconds to reach full brightness, so the
// glow arrived long after the character had finished rising. Neighbouring words sat at different
// points on that slow climb, which is what read as flicker rather than one travelling light.
private const val FLARE_ATTACK_MS = 500

// Distance, in characters, over which the wake behind the playhead falls to about a third of full
// brightness. The decay is EXPONENTIAL, not linear, and that distinction is the whole point: a long
// linear ramp does not make a tail, it makes the whole word evenly bright, because the first
// character of a four-letter word still sits at ninety percent of it. An exponential keeps the
// light concentrated on the character being sung, drops away sharply behind it, and leaves a faint
// glow trailing for a long way — which is what a wake looks like.
private const val FLARE_TAIL_CHARS = 2.5f

// How far the character under the light lifts, as a fraction of the font size — Apple nudges the
// syllable being sung upward, and the eye reads the lift as the light having weight.
//
// Driven by the flare itself rather than by a second animation, so the rise travels with the light
// instead of chasing it. It is a graphicsLayer translation, which happens at DRAW time: the glyph
// moves without the line being measured again, so nothing reflows and no line ever changes height.
// No alpha goes through that layer — alpha below 1 would force an offscreen buffer the size of the
// glyph and slice off the very glow this is meant to accompany.
private const val CHAR_RISE_EM = 0.065f

// How long one character takes to travel from its resting line to the top, once the light has
// touched it. Deliberately longer than a character is sung for: the lift is NOT finished before the
// next character starts its own, so at any instant a stretch of the line is caught at different
// heights and the whole thing reads as a wave rolling behind the singing.
//
// This is why the lift cannot be computed from the word's progress. That number stops advancing the
// moment the word is done, so a word sung in less than this would freeze its characters half-risen.
// Each character needs a clock of its own, started when the light arrives and left to finish on its
// own time.
private const val CHAR_RISE_MS = 1_000

private const val FOOTER_ALPHA = 0.45f

private const val SUNG_BASE_GLOW_ALPHA = 0.95f
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

/**
 * The sheet's lines as they are RENDERED, plus the indices of the dots lines that were inserted
 * into them.
 *
 * One list, one index space. The sung line, the blur distance, the scroll target and the
 * translation lookup are all line INDICES, so a display-only list running alongside the real one
 * would leave four call sites quietly disagreeing about which line is which.
 */
private data class DisplayLines(
    val lines: List<Line>,
    val interludeIndices: Set<Int>,
)

/**
 * Inserts a dots line into every silence of at least [INTERLUDE_MIN_GAP_MS] between two lines.
 *
 * The inserted line is an ordinary rich-synced [Line] carrying three dots, one timestamp each,
 * dividing the silence into equal thirds. Written as real markup rather than handled by a special
 * case in the renderer: it then goes through the same parser, the same wipe and the same layout as
 * a sung line, which is the only way its dots land on the same baseline and left edge as the words
 * above them.
 *
 * Rich sync only. It is the one format that records where a line STOPS, so it is the one format
 * that can tell a real pause from a line whose end was simply never written down.
 */
private fun buildDisplayLines(
    lines: List<Line>?,
    syncType: String?,
): DisplayLines {
    val source = lines.orEmpty()
    if (syncType != "RICH_SYNCED" || source.size < 2) return DisplayLines(source, emptySet())

    val out = ArrayList<Line>(source.size)
    val inserted = mutableSetOf<Int>()
    source.forEachIndexed { index, line ->
        out += line
        val nextStartMs = source.getOrNull(index + 1)?.startTimeMs?.toLongOrNull() ?: return@forEachIndexed
        // The rule, literally: once the line's LAST WORD has been lit for INTERLUDE_MIN_GAP_MS and
        // the next line still has not begun, the dots take over from there.
        //
        // Measured from where that word STARTS, not where it ends, and the dots begin three seconds
        // later rather than at the end of the line. Both of those matter for the same reason: the
        // dots become the sung line the moment they start, so starting them any earlier drags the
        // sheet away from a word the listener is still watching light up. For those three seconds
        // the last lyric stays the sung line, which is what it still is.
        val lastWordStartMs =
            parseRichSyncWords(line.words, line.startTimeMs, line.endTimeMs)
                ?.words
                ?.lastOrNull()
                ?.startTimeMs
                ?: return@forEachIndexed
        val dotsStartMs = lastWordStartMs + INTERLUDE_MIN_GAP_MS
        if (nextStartMs <= dotsStartMs) return@forEachIndexed
        val interlude = interludeLine(dotsStartMs, nextStartMs) ?: return@forEachIndexed
        inserted += out.size
        out += interlude
    }
    return DisplayLines(out, inserted)
}

/** Null when a timestamp will not fit the `MM:SS.cc` shape the rich-sync parser reads. */
private fun interludeLine(
    startMs: Long,
    endMs: Long,
): Line? {
    val stepMs = (endMs - startMs) / INTERLUDE_DOT_COUNT
    val words =
        buildString {
            repeat(INTERLUDE_DOT_COUNT) { dot ->
                append(richSyncTimestamp(startMs + stepMs * dot) ?: return null)
                append(INTERLUDE_DOT)
            }
        }
    return Line(
        startTimeMs = startMs.toString(),
        endTimeMs = endMs.toString(),
        syllables = null,
        words = words,
    )
}

private fun richSyncTimestamp(ms: Long): String? {
    val centis = ms / 10
    val minutes = centis / 6000
    // The parser's regex reads exactly two digits of minutes, so anything past 99:59 has no
    // spelling here. Only reachable on a track over an hour and a half long.
    if (minutes > 99) return null
    return "<${twoDigits(minutes)}:${twoDigits((centis / 100) % 60)}.${twoDigits(centis % 100)}>"
}

private fun twoDigits(value: Long): String = if (value < 10) "0$value" else value.toString()

internal data class TimedLineIndex(
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
internal fun List<TimedLineIndex>.activeIndexAt(nowMs: Long): Int {
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

    // The listener's audio-delay correction, applied HERE rather than to the lyric rows: the rows
    // are cached in Room and shared with the community lyrics database, so a local correction must
    // never be written into them. Reading it at this end also means a change lands on the next
    // frame instead of the next track.
    //
    // SUBTRACTED. Bluetooth buffers, so at player position P the ear is hearing P - offset; that
    // earlier moment is the one the sheet must light up for. Early in a track this can go negative,
    // which the `now <= 0L` guard below already reads as "no line yet" — correct, since in heard
    // time the song has not reached its first line.
    val lyricsOffsetMs by dataStoreManager.lyricsOffsetMs.collectAsStateWithLifecycle(0)

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

    // The lines this sheet actually renders: the sheet's own, plus a dots line inserted into every
    // long silence. Everything below counts indices against THIS list — the sung line, the blur
    // distance, the scroll target and the translation lookup — so the dots line is a line like any
    // other rather than something drawn on top of one.
    val displayLines =
        remember(lyricsData.lyrics.lines, lyricsData.lyrics.syncType) {
            buildDisplayLines(lyricsData.lyrics.lines, lyricsData.lyrics.syncType)
        }

    val timedLineIndexes =
        remember(displayLines) {
            val timed =
                displayLines.lines
                    .mapIndexedNotNull { index, line ->
                        line.startTimeMs.toLongOrNull()?.let { TimedLineIndex(index, it) }
                    }
            // An unsynced sheet still carries a startTimeMs on every line — the literal "0", on all
            // 307 unsynced rows of the author's own library. Those parse perfectly well, so this
            // list came out full of zeros and [activeIndexAt], asked for "the last line at or before
            // now", answered with the LAST LINE OF THE SONG from the first second onward. Every
            // other line then sat at a huge distance from it and blurred to maximum: the whole
            // sheet unreadable, with the one sharp line parked off the bottom of the screen.
            //
            // Tested on the timestamps rather than on syncType because it is the timestamps the
            // search actually reads: one distinct value cannot order anything, whatever the sheet
            // calls itself. With the list empty, currentLineIndex stays -1, and the renderer's
            // no-active-line branch dims every line uniformly and blurs none of them.
            if (timed.distinctBy { it.startTimeMs }.size <= 1) {
                emptyList()
            } else {
                timed.sortedBy { it.startTimeMs }
            }
        }

    val currentLineIndex by remember(timedLineIndexes) {
        derivedStateOf {
            val now = current.current - lyricsOffsetMs
            if (now <= 0L) -1 else timedLineIndexes.activeIndexAt(now)
        }
    }

    // Read off the SAME list the blur fix built, not off syncType: it is the timestamps that decide
    // whether a line can ever be "the sung one", and [timedLineIndexes] is already empty exactly
    // when they cannot order anything. Every line then renders as the current line — white, fully
    // opaque, unblurred — because a sheet with no sung line has no line to contrast one against.
    // Distinct from a SYNCED sheet's pre-roll, where currentLineIndex is also -1 but a sung line is
    // on its way and PRE_ROLL_LINE_ALPHA deliberately keeps the page dimmer than it.
    val allLinesCurrent = timedLineIndexes.isEmpty()

    val syncedTranslatedWordsByLineIndex =
        remember(
            displayLines,
            lyricsData.translatedLyrics?.first?.lines,
        ) {
            buildSyncedTranslatedWordsByLineIndex(
                originalLines = displayLines.lines,
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
            items(displayLines.lines.size) { index ->
                val line = displayLines.lines.getOrNull(index)
                // A dots line stands for silence, so it has nothing to translate or romanize. Worth
                // saying out loud for the translation: that map matches by TIME, and the silence
                // begins close enough to the line before it that it would otherwise pick up that
                // line's translation and print it under the dots.
                val isInterlude = index in displayLines.interludeIndices
                // Translated lyrics: synced -> precomputed map by line index, unsynced -> by index.
                val translatedWords =
                    when {
                        isInterlude -> null
                        lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED" ->
                            syncedTranslatedWordsByLineIndex[index]
                        else ->
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
                        if (isInterlude || romanizationLanguages.isEmpty()) {
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
                                        currentTimeMs = current.current - lyricsOffsetMs,
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
                                        // Same clause as the line-synced branch below, and it has to
                                        // be here too: [allLinesCurrent] already lifts this line's
                                        // opacity to full through appleMusicLyricFocus, so without it
                                        // the line would come out fully opaque in grey ink.
                                        isCurrent = index == currentLineIndex || allLinesCurrent,
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
                                    // That clause keys off syncType, which lit every line of a
                                    // LINE_SYNCED sheet too and put two lit lines on screen at once.
                                    // [allLinesCurrent] is the narrow version: it fires only when the
                                    // timestamps genuinely cannot order anything, i.e. an unsynced
                                    // sheet, where every line being current is the wanted result.
                                    isCurrent = index == currentLineIndex || allLinesCurrent,
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
                                            allLinesCurrent = allLinesCurrent,
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
    // Both halves live INSIDE the Crossfade. They did not before: the large one was in here and the
    // small one sat outside behind a plain `if`, so the small one was removed the instant the state
    // flipped while the large one was still fading in — the line appeared to blink out and come
    // back. Overlapped, one fades down as the other fades up.
    //
    // What this does NOT fix, and cannot: the two halves are different FONT SIZES, so they wrap
    // differently, and a Crossfade composes both during the transition — the box takes the taller
    // of the two the moment the transition starts. Re-wrapping is a measurement, and a crossfade
    // only touches opacity.
    Crossfade(targetState = isBold) { bold ->
        Column(
            modifier = modifier,
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = originalWords,
                style = if (bold) typo().headlineLarge else typo().headlineMedium,
                color = if (bold && isCurrent) Color.White else DimOriginalColor,
            )
            if (romanizedWords != null) {
                Text(
                    text = romanizedWords,
                    style = typo().bodyMedium,
                    // Neither the original's white nor the translation's yellow: a reading is a
                    // third KIND of thing, and giving it the translation's colour would read as
                    // two translations stacked.
                    color = if (bold && isCurrent) DimRomanizedCurrentColor else DimRomanizedColor,
                )
            }
            if (translatedWords != null) {
                Text(
                    text = translatedWords,
                    style = typo().bodyMedium,
                    color = if (bold && isCurrent) Color.Yellow else DimTranslatedColor,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * The playback position, carried forward between the player's 100 ms position ticks so that it
 * advances once per FRAME instead of ten times a second.
 *
 * Rich sync needs this and a line-level sheet does not. Word timings routinely sit tens of
 * milliseconds apart — the parser's own doc example is `<00:16.62> Và <00:16.64> em`, a gap of 20 ms
 * — so several words can begin inside one tick. Picking the sung word straight off the tick jumps
 * over every one of them but the last, and a word that is jumped over goes from "not yet" to "past"
 * without ever being active: no wipe, and the travelling flare never touches it, because the flare
 * is off whenever a word is not active. That is the stutter this exists to remove.
 *
 * Returned as [State] rather than as a value on purpose. Reading it during composition would
 * recompose the caller, and every word in the line with it, sixty times a second; feeding a
 * `derivedStateOf` instead means the caller only recomposes when the sung word actually changes.
 *
 * Drift is bounded by [PLAYHEAD_TICK_MS]: while the ticks keep coming this never runs more than one
 * tick ahead, and when they stop — paused playback — it settles one tick ahead and stays there.
 */
@Composable
private fun rememberSmoothPlayhead(
    rawMs: Long,
    enabled: Boolean,
): State<Long> {
    val playhead = remember { mutableLongStateOf(rawMs) }
    LaunchedEffect(rawMs, enabled) {
        if (!enabled) {
            playhead.longValue = rawMs
            return@LaunchedEffect
        }
        // Keyed on rawMs, so the next real tick cancels this loop and restarts it from the truth —
        // the interpolation can accumulate error for at most one tick before being corrected.
        var baseNanos = -1L
        while (true) {
            withFrameNanos { frameNanos ->
                if (baseNanos < 0L) baseNanos = frameNanos
                val elapsedMs = (frameNanos - baseNanos) / 1_000_000L
                playhead.longValue = rawMs + elapsedMs.coerceIn(0L, PLAYHEAD_TICK_MS)
            }
        }
    }
    return playhead
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
    val playhead = rememberSmoothPlayhead(currentTimeMs, enabled = isCurrent)

    // Remembered on the LINE rather than on the clock. The previous `remember(currentTimeMs, …)`
    // rebuilt this derived state on every tick, which gave away the one thing derivedStateOf is
    // for: it notifies only when the RESULT changes. Keyed this way, the playhead can move sixty
    // times a second inside it while this composable recomposes only when the sung word changes.
    val currentWordIndex by remember(parsedLine.words, isCurrent) {
        derivedStateOf {
            if (!isCurrent) return@derivedStateOf -1
            parsedLine.words.indexOfLast { it.startTimeMs <= playhead.value }
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
                    playheadMs = playhead,
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
    // The frame-interpolated playhead, taken as State and read ONLY inside the effect below. Taking
    // it as a plain Long would recompose every word in the line on every frame; a State read from a
    // suspend body is not a composition read at all.
    playheadMs: State<Long>,
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

    // The gap to the next word, and nothing else. Both the floor that used to sit here (100 ms) and
    // the one on the wipe below (150 ms) made a short word's animation outlive the word, so by the
    // time it finished the next two or three had started their own and the line looked like the
    // words were chasing each other. A syllable sung in 40 ms gets a 40 ms wipe; only the divide
    // needs protecting.
    val wordDurationMs = (wordEndTimeMs - wordStartTimeMs).coerceAtLeast(1L)
    val anim =
        remember(wordStartTimeMs, wordEndTimeMs) {
            val initial =
                ((currentTimeMs - wordStartTimeMs).toFloat() / wordDurationMs.toFloat())
                    .coerceIn(0f, 1f)
            androidx.compose.animation.core.Animatable(initial)
        }

    LaunchedEffect(wordStartTimeMs, wordEndTimeMs, isActive, isPast) {
        when {
            // Nothing to carry over any more: the wipe now runs for exactly the word's own span,
            // so it reaches 1f on the same frame this flips. Only a word shorter than one frame
            // arrives unfinished, and that one has no time to animate in anyway.
            isPast -> anim.snapTo(1f)
            isActive -> {
                // From the interpolated playhead, not from the last tick. isActive now flips within
                // a frame of the word's real start, so a tick-aged `now` would snap the wipe behind
                // where it belongs and then stretch it past the end of the word.
                val now = playheadMs.value
                val current =
                    ((now - wordStartTimeMs).toFloat() / wordDurationMs.toFloat())
                        .coerceIn(0f, 1f)
                anim.snapTo(current)
                // Exactly what is left of this word, so it finishes as the next one starts and no
                // two wipes are ever in flight at once.
                val remainingMs = (wordEndTimeMs - now).coerceAtLeast(0L).toInt().coerceAtLeast(1)
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
    // The flare's on/off, animated instead of switched. A word that has just been sung keeps its
    // light for FLARE_FADE_MS while the next word's comes up, so the hand-over happens across the
    // word boundary rather than at it. Settled words hold 0 and animate nothing.
    val flareGate by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        // The duration is picked from the direction being travelled: snap up with the singing, ebb
        // away slowly behind it.
        animationSpec =
            tween(
                durationMillis = if (isActive) FLARE_ATTACK_MS else FLARE_FADE_MS,
                easing = LinearEasing,
            ),
        label = "flareGate",
    )

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
                    if (flareGate <= 0f || glow == null) {
                        0f
                    } else {
                        // Ahead of the light, a short linear ramp so a character brightens as it
                        // is approached; behind it, an exponential wake.
                        val delta = wordProgress - charCenter
                        val shape =
                            if (delta > 0f) {
                                exp(-delta / (FLARE_TAIL_CHARS / charCount))
                            } else {
                                (1f - abs(delta) / reach).coerceIn(0f, 1f)
                            }
                        shape * flareGate
                    }
                // Lifted when the light touches it, over CHAR_RISE_MS, and LEFT THERE. Apple
                // never brings the glyph back down — the sung half of a line simply sits higher
                // than the half still to come — so there is no fall to animate, only an arrival.
                //
                // The trigger is instant, the travel is not: the light moves on long before the
                // character has finished rising, which is exactly what makes the line ripple
                // instead of stepping. Its own animation, on its own clock, so a word sung faster
                // than the rise still completes.
                val charRise by animateFloatAsState(
                    targetValue = if (glow != null && charProgress > 0f) 1f else 0f,
                    animationSpec = tween(CHAR_RISE_MS, easing = FastOutSlowInEasing),
                    label = "charRise",
                )
                val restingColor = pendingColorOverride ?: DimRichPendingColor
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                            translationY = -charRise * CHAR_RISE_EM * fontPx
                        },
                ) {
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

/**
 * Android's host for [FullscreenLyricsContent]: the page as a full-height bottom sheet. Desktop
 * hosts the same content in a full-window Popup instead (NowPlayingScreenContent).
 */
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FullscreenLyricsSheet(
    sharedViewModel: SharedViewModel,
    navController: NavController,
    color: Color = Color(0xFF242424),
    state: NowPlayingContentState,
    actions: NowPlayingContentActions,
    nowPlayingStyle: String,
    onDismiss: () -> Unit,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val coroutineScope = rememberCoroutineScope()
    // ModalBottomSheet caps its width at 640dp by default, which would squeeze the two-column
    // landscape layout into a centred strip on a phone turned sideways. Portrait keeps the default.
    val screenInfo = getScreenSizeInfo()
    val isLandscape = screenInfo.wDP > screenInfo.hDP

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        containerColor = Color.Black,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = .5f),
        sheetMaxWidth = if (isLandscape) Dp.Unspecified else BottomSheetDefaults.SheetMaxWidth,
        sheetState = sheetState,
        modifier =
            Modifier
                .fillMaxHeight(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        FullscreenLyricsContent(
            sharedViewModel = sharedViewModel,
            navController = navController,
            color = color,
            state = state,
            actions = actions,
            nowPlayingStyle = nowPlayingStyle,
            // A dismiss the page asks for itself slides the sheet away before it leaves
            // composition, instead of cutting it off mid-screen.
            onDismiss = {
                coroutineScope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            },
        )
    }
}
