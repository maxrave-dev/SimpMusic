package com.maxrave.simpmusic.ui.screen.home.wrapped.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedTokens
import com.maxrave.simpmusic.ui.screen.home.wrapped.bandBounds
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatPercent
import com.maxrave.simpmusic.ui.screen.home.wrapped.hourMeridiem
import com.maxrave.simpmusic.ui.screen.home.wrapped.hourNumber
import com.maxrave.simpmusic.viewModel.WrappedListeningBand
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped_clock_band_afternoon
import simpmusic.composeapp.generated.resources.wrapped_clock_band_evening
import simpmusic.composeapp.generated.resources.wrapped_clock_band_morning
import simpmusic.composeapp.generated.resources.wrapped_clock_band_night
import simpmusic.composeapp.generated.resources.wrapped_clock_caption
import simpmusic.composeapp.generated.resources.wrapped_clock_peak_hour
import simpmusic.composeapp.generated.resources.wrapped_clock_title
import simpmusic.composeapp.generated.resources.wrapped_clock_you_are
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Card 06 — the twenty-four hours of the day as a ring, with the peak hour in its middle.
 *
 * The ring is drawn the way the Analytics screen's clock is drawn, for the reason that screen
 * already learned: every hour keeps a dark wedge running the full depth of the ring behind its
 * value, because without that track an hour with one play and an hour with none look nearly
 * identical — the eye reads a short spoke as a missing tick rather than as "almost nothing here".
 * Geometry, hour order, the two-tier accent AND the roles they are drawn in are taken from
 * [com.maxrave.simpmusic.ui.screen.home.analytics.ListeningClockChart] rather than re-decided —
 * `primary` for the value, `onSurfaceVariant` at a tenth for the track — so a user who has seen
 * both cannot find them disagreeing about which hour was busiest or what colour "busy" is. The
 * drawing is repeated rather than called: that chart is a `Row` with a legend column beside it,
 * which neither this composition nor an always-dark share image can use, and its wedge helper is
 * private to that file.
 *
 * No colour is named anywhere below — the reel runs inside the shell's artwork-seeded
 * `MaterialExpressiveTheme` under `ForceDarkContent`, so every role here resolves against the
 * user's own covers and every text style already carries the colour it should print in.
 */
@Composable
fun WrappedClockCard(
    wrapped: WrappedYear,
    modifier: Modifier = Modifier,
) {
    val clock = wrapped.clock
    val use24Hour = prefers24HourClock()
    val (bandStart, bandEnd) = bandBounds(clock.band, use24Hour)
    val youAre = stringResource(Res.string.wrapped_clock_you_are)
    val bandLabel = stringResource(clock.band.labelRes())

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Spacer(Modifier.height(CARD_TOP_GAP))
        WrappedEyebrow(
            text = stringResource(Res.string.wrapped_clock_title),
            modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
        )

        // The ring is wider than the card and is meant to be: a Box does not clip, so it bleeds off
        // both edges the way the artboard draws it, and the day reads as bigger than the screen.
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            ListeningRing(
                playsByHour = clock.playsByHour,
                peakHour = clock.peakHour,
                use24Hour = use24Hour,
                modifier = Modifier.size(min(maxWidth * RING_BLEED, maxHeight)),
            )
        }

        Column(Modifier.padding(horizontal = WrappedTokens.ScreenPadding)) {
            Text(
                // Two deliberate lines rather than free wrapping: the band phrase is one unit in
                // every language, and breaking before it keeps the block the shape it is drawn as.
                text = "$youAre\n$bandLabel",
                // The card's second voice, straight off the scale at its own size. Only the one
                // figure inside the ring is big enough to need a size of its own.
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        lineHeight = STACKED_LINE_HEIGHT,
                        letterSpacing = (-0.015).em,
                    ),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text =
                    stringResource(
                        Res.string.wrapped_clock_caption,
                        formatPercent(clock.bandShare),
                        bandStart,
                        bandEnd,
                    ),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = CAPTION_LINE_HEIGHT),
            )
        }
        Spacer(Modifier.height(CARD_BOTTOM_GAP))
    }
}

/** The band's whole sentence fragment — "an evening listener", never assembled from parts. */
private fun WrappedListeningBand.labelRes() =
    when (this) {
        WrappedListeningBand.NIGHT -> Res.string.wrapped_clock_band_night
        WrappedListeningBand.MORNING -> Res.string.wrapped_clock_band_morning
        WrappedListeningBand.AFTERNOON -> Res.string.wrapped_clock_band_afternoon
        WrappedListeningBand.EVENING -> Res.string.wrapped_clock_band_evening
    }

@Composable
private fun ListeningRing(
    playsByHour: List<Int>,
    peakHour: Int,
    use24Hour: Boolean,
    modifier: Modifier = Modifier,
) {
    val hours = if (playsByHour.size == 24) playsByHour else List(24) { 0 }
    val max = hours.maxOrNull()?.coerceAtLeast(1) ?: 1
    val meridiem = hourMeridiem(peakHour, use24Hour)

    // Read once, outside the draw lambda: MaterialTheme is a CompositionLocal and a DrawScope is
    // not a composition.
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = TRACK_ALPHA)

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val outer = size.minDimension / 2f
            val inner = outer * RING_INNER_RATIO
            hours.forEachIndexed { hour, count ->
                val start = hour * HOUR_SWEEP + WEDGE_GAP / 2f - 90f
                val sweep = HOUR_SWEEP - WEDGE_GAP
                drawWedge(center, inner, outer, start, sweep, track)
                if (count > 0) {
                    drawWedge(
                        center = center,
                        inner = inner,
                        outer = inner + (outer - inner) * (count.toFloat() / max),
                        startDeg = start,
                        sweepDeg = sweep,
                        color =
                            if (count > max * BUSY_HOUR_FRACTION) {
                                accent
                            } else {
                                accent.copy(alpha = QUIET_HOUR_ALPHA)
                            },
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                // One Text, not two: the meridiem has to sit on the number's baseline, and a span
                // does that for free where a Row of two sizes only ever approximates it.
                text =
                    buildAnnotatedString {
                        append(hourNumber(peakHour, use24Hour))
                        if (meridiem.isNotEmpty()) {
                            withStyle(SpanStyle(fontSize = MERIDIEM_SIZE)) { append(" $meridiem") }
                        }
                    },
                // The card's one hero figure, and the only place a size is named. `titleLarge` is
                // the scale's Bold, title-coloured tier, so enlarging it needs neither a weight nor
                // a colour argument — `displayLarge` would need both (it is Normal, in body grey).
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = PEAK_HOUR_SIZE,
                        lineHeight = 1.em,
                    ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.wrapped_clock_peak_hour),
                style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.16.em),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** One annulus sector. `drawArc` can only stroke a ring, so the slice is built by hand. */
private fun DrawScope.drawWedge(
    center: Offset,
    inner: Float,
    outer: Float,
    startDeg: Float,
    sweepDeg: Float,
    color: Color,
) {
    val path =
        Path().apply {
            val steps = 6
            repeat(steps + 1) { i ->
                val a = ((startDeg + sweepDeg * i / steps) * PI / 180f).toFloat()
                val p = Offset(center.x + cos(a) * outer, center.y + sin(a) * outer)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            for (i in steps downTo 0) {
                val a = ((startDeg + sweepDeg * i / steps) * PI / 180f).toFloat()
                lineTo(center.x + cos(a) * inner, center.y + sin(a) * inner)
            }
            close()
        }
    drawPath(path, color)
}

/**
 * Whether the reader's part of the world reads "21" or "9 PM".
 *
 * Deliberately a short region list rather than a general rule, in the same spirit as the grouping
 * separator in `WrappedFormat`: these are the places CLDR marks as preferring a twelve-hour clock,
 * and everywhere else — including every language this app is most used in — reads twenty-four.
 * `region` comes back empty on some hosts, so English with no region falls back to twelve rather
 * than to the world default, which is where an unregioned `en` user would be most surprised.
 */
@Composable
private fun prefers24HourClock(): Boolean {
    val locale = Locale.current
    val region = locale.region.uppercase()
    return if (region.isNotEmpty()) {
        region !in TWELVE_HOUR_REGIONS
    } else {
        !locale.language.equals("en", ignoreCase = true)
    }
}

private val TWELVE_HOUR_REGIONS =
    setOf(
        "AS", "AU", "BD", "CA", "CO", "EG", "GU", "HN", "IE", "IN", "JO",
        "KR", "MP", "MX", "MY", "NI", "NZ", "PH", "PK", "PR", "SA", "SG",
        "SV", "US", "VI",
    )

/** 360° over 24 hours. */
private const val HOUR_SWEEP = 15f

/** Degrees taken out of each hour, so the ring reads as twenty-four things and not as one disc. */
private const val WEDGE_GAP = 2.2f

/** Where the hole starts, as a fraction of the outer radius — the artboard's 124px of 210px. */
private const val RING_INNER_RATIO = 0.41f

/** Above this share of the peak, an hour is drawn at full accent. Matches the Analytics clock. */
private const val BUSY_HOUR_FRACTION = 0.55f

/** …and below it, at the same 45% the Analytics clock steps down to. */
private const val QUIET_HOUR_ALPHA = 0.45f

/** Hour wedges with no plays still show, at the weight of a shadow — `ListeningClockChart`'s own. */
private const val TRACK_ALPHA = 0.10f

/** The ring is wider than the card on purpose, so it runs off both edges. */
private const val RING_BLEED = 1.08f

/**
 * The one figure this card is built around.
 *
 * Named in sp because it is a font size, and fed to `titleLarge.copy(fontSize = …)` rather than
 * built into a `TextStyle` — the family, the weight and the colour still arrive from the theme.
 */
private val PEAK_HOUR_SIZE = 52.sp

/** The meridiem rides the peak hour's baseline at two-fifths its size. */
private val MERIDIEM_SIZE = 21.sp

/** Relative, so the verdict stays one tight block whatever the scale's own size turns out to be. */
private val STACKED_LINE_HEIGHT = 1.12.em

private val CAPTION_LINE_HEIGHT = 1.5.em

/** Gap between the shell's header and this card's eyebrow. */
private val CARD_TOP_GAP = 14.dp

/** Gap between this card's last line and the shell's footer. */
private val CARD_BOTTOM_GAP = 22.dp
