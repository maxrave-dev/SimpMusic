package com.maxrave.simpmusic.ui.screen.home.wrapped.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.maxrave.domain.data.model.analytics.ListeningFingerprint
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedTokens
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatPercent
import com.maxrave.simpmusic.viewModel.WrappedArchetype
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.analytics_axis_concentration
import simpmusic.composeapp.generated.resources.analytics_axis_consistency
import simpmusic.composeapp.generated.resources.analytics_axis_discovery
import simpmusic.composeapp.generated.resources.analytics_axis_diversity
import simpmusic.composeapp.generated.resources.analytics_axis_replay
import simpmusic.composeapp.generated.resources.analytics_no_previous
import simpmusic.composeapp.generated.resources.wrapped_archetype_deep_diver
import simpmusic.composeapp.generated.resources.wrapped_archetype_deep_diver_desc
import simpmusic.composeapp.generated.resources.wrapped_archetype_devotee
import simpmusic.composeapp.generated.resources.wrapped_archetype_devotee_desc
import simpmusic.composeapp.generated.resources.wrapped_archetype_explorer
import simpmusic.composeapp.generated.resources.wrapped_archetype_explorer_desc
import simpmusic.composeapp.generated.resources.wrapped_archetype_omnivore
import simpmusic.composeapp.generated.resources.wrapped_archetype_omnivore_desc
import simpmusic.composeapp.generated.resources.wrapped_archetype_regular
import simpmusic.composeapp.generated.resources.wrapped_archetype_regular_desc
import simpmusic.composeapp.generated.resources.wrapped_type_the
import simpmusic.composeapp.generated.resources.wrapped_type_title
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Card 08 — the five behaviour axes as a radar, and the name that falls out of the highest one.
 *
 * The axis order, the start angle, the clockwise sweep AND the roles the two polygons are drawn in
 * are the same ones [com.maxrave.simpmusic.ui.screen.home.analytics.FingerprintChart] uses — this
 * year in `primary` over a fill of it, last year in `onSurfaceVariant` at 55% — and the labels are
 * the same five strings, so a user who has seen both cannot find them disagreeing about which
 * corner is which or what the accent means. It is redrawn rather than called because that chart is
 * a `Row` whose other half is a list of percentages, a reading aid on a screen but noise on a
 * poster, and its polygon helpers are private to that file.
 *
 * Last year is the second polygon and it matters more than it looks: every axis is self-normalised
 * 0..1, so a lone shape says almost nothing. When there is no last year the card says so in the
 * legend instead of drawing a ghost, because an absent comparison and a flat one look identical.
 */
@Composable
fun WrappedTypeCard(
    wrapped: WrappedYear,
    modifier: Modifier = Modifier,
) {
    val previous = wrapped.previousStats?.fingerprint
    val theWord = stringResource(Res.string.wrapped_type_the)
    val archetypeName = stringResource(wrapped.archetype.nameRes())

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Spacer(Modifier.height(CARD_TOP_GAP))
        WrappedEyebrow(
            text = stringResource(Res.string.wrapped_type_title),
            modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val diameter = min(maxWidth, maxHeight)
            FingerprintRadar(
                current = wrapped.stats.fingerprint,
                previous = previous,
                diameter = diameter,
                modifier = Modifier.size(diameter),
            )
        }

        Column(Modifier.padding(horizontal = WrappedTokens.ScreenPadding)) {
            RadarLegend(year = wrapped.year, hasPrevious = previous != null)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "$theWord\n$archetypeName",
                // The card's hero, and its one named size: there is no big number here, so the name
                // itself is the figure. `titleLarge` is already Bold in the scale's title colour,
                // so enlarging it takes no weight and no colour argument.
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = ARCHETYPE_SIZE,
                        lineHeight = ARCHETYPE_LINE_HEIGHT,
                        letterSpacing = (-0.03).em,
                    ),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(wrapped.archetype.descRes(), archetypeFigure(wrapped)),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.55.em),
            )
        }
        Spacer(Modifier.height(CARD_BOTTOM_GAP))
    }
}

@Composable
private fun FingerprintRadar(
    current: ListeningFingerprint,
    previous: ListeningFingerprint?,
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    val labels =
        listOf(
            stringResource(Res.string.analytics_axis_consistency),
            stringResource(Res.string.analytics_axis_discovery),
            stringResource(Res.string.analytics_axis_diversity),
            stringResource(Res.string.analytics_axis_concentration),
            stringResource(Res.string.analytics_axis_replay),
        )

    // Read outside the Canvas: a DrawScope is not a composition, so it cannot see MaterialTheme.
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f * POLYGON_RADIUS_RATIO
            val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))

            // The outermost ring prints strongest: it is the one the shapes are measured
            // against, and the inner two are only there to give the eye a scale.
            GRID_RINGS.forEach { (fraction, alpha) ->
                drawPath(
                    path = axisPath(center, radius, List(AXES) { fraction }),
                    color = muted.copy(alpha = alpha),
                    style = Stroke(1.dp.toPx()),
                )
            }
            previous?.let {
                val path = axisPath(center, radius, it.axes)
                drawPath(path, muted.copy(alpha = PREVIOUS_FILL_ALPHA))
                drawPath(
                    path = path,
                    color = muted.copy(alpha = PREVIOUS_STROKE_ALPHA),
                    style = Stroke(2.dp.toPx(), pathEffect = dash, join = StrokeJoin.Round),
                )
            }
            val now = axisPath(center, radius, current.axes)
            drawPath(now, accent.copy(alpha = CURRENT_FILL_ALPHA))
            drawPath(
                path = now,
                color = accent,
                style = Stroke(3.dp.toPx(), join = StrokeJoin.Round),
            )
        }

        // Each label rides the same angle its vertex does, so the two can never drift apart. They
        // are the scale's `bodySmall`, which is a good deal larger than the artboard's 8.5px — so
        // the polygon gives back a slice of its radius and each label is capped at a share of the
        // diameter and allowed a second line, rather than being shrunk back to fit.
        labels.forEachIndexed { index, label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.10.em),
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier =
                    Modifier
                        .widthIn(max = diameter * LABEL_MAX_WIDTH_RATIO)
                        .offset {
                            // Driven by the diameter the caller measured rather than by the node's
                            // own size: inside this lambda the receiver is a Density, which knows
                            // neither.
                            val ring = diameter.toPx() / 2f * POLYGON_RADIUS_RATIO * LABEL_RADIUS_RATIO
                            val angle = axisAngle(index)
                            IntOffset(
                                (cos(angle) * ring).roundToInt(),
                                (sin(angle) * ring).roundToInt(),
                            )
                        },
            )
        }
    }
}

/**
 * Which shapes are on the chart — never a colour key with nothing to key.
 *
 * With no previous year the second row is words rather than a swatch: a legend entry whose swatch
 * points at nothing drawn is worse than no entry at all, and simply omitting the row leaves the
 * reader to work out for themselves that the comparison is missing rather than absent by choice.
 */
@Composable
private fun RadarLegend(
    year: Int,
    hasPrevious: Boolean,
) {
    val legendStyle = MaterialTheme.typography.bodySmall
    val previousInk = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PREVIOUS_STROKE_ALPHA)

    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                Modifier
                    .size(width = SWATCH_WIDTH, height = 2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(text = "$year", style = legendStyle)
        }
        if (hasPrevious) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Canvas(Modifier.width(SWATCH_WIDTH).height(2.dp)) {
                    drawLine(
                        color = previousInk,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = size.height,
                        pathEffect =
                            PathEffect.dashPathEffect(
                                floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
                            ),
                    )
                }
                Text(text = "${year - 1}", style = legendStyle)
            }
        } else {
            Text(text = stringResource(Res.string.analytics_no_previous), style = legendStyle)
        }
    }
}

/**
 * The figure the archetype's own sentence asks for.
 *
 * Each description names a different quantity, so the value follows the wording rather than the
 * axis that chose the archetype: "the regular" is picked by the consistency axis but talks about
 * how much of the year had music in it, which is a share of days and not the axis's own score.
 */
@Composable
private fun archetypeFigure(wrapped: WrappedYear): String {
    val stats = wrapped.stats
    val activeShare = stats.activeDays.toFloat() / wrapped.daysInYear.coerceAtLeast(1)
    return when (wrapped.archetype) {
        WrappedArchetype.THE_REGULAR -> formatPercent(activeShare)
        WrappedArchetype.THE_EXPLORER -> formatPercent(stats.fingerprint.discovery)
        WrappedArchetype.THE_OMNIVORE -> formatCount(stats.distinctArtists)
        WrappedArchetype.THE_DEVOTEE -> formatPercent(stats.fingerprint.concentration)
        WrappedArchetype.THE_DEEP_DIVER -> formatPercent(stats.fingerprint.replay)
    }
}

private fun WrappedArchetype.nameRes(): StringResource =
    when (this) {
        WrappedArchetype.THE_REGULAR -> Res.string.wrapped_archetype_regular
        WrappedArchetype.THE_EXPLORER -> Res.string.wrapped_archetype_explorer
        WrappedArchetype.THE_OMNIVORE -> Res.string.wrapped_archetype_omnivore
        WrappedArchetype.THE_DEVOTEE -> Res.string.wrapped_archetype_devotee
        WrappedArchetype.THE_DEEP_DIVER -> Res.string.wrapped_archetype_deep_diver
    }

private fun WrappedArchetype.descRes(): StringResource =
    when (this) {
        WrappedArchetype.THE_REGULAR -> Res.string.wrapped_archetype_regular_desc
        WrappedArchetype.THE_EXPLORER -> Res.string.wrapped_archetype_explorer_desc
        WrappedArchetype.THE_OMNIVORE -> Res.string.wrapped_archetype_omnivore_desc
        WrappedArchetype.THE_DEVOTEE -> Res.string.wrapped_archetype_devotee_desc
        WrappedArchetype.THE_DEEP_DIVER -> Res.string.wrapped_archetype_deep_diver_desc
    }

/** Clockwise from straight up, so axis 0 is the top vertex. Matches the Analytics radar. */
private fun axisAngle(index: Int): Float =
    (index.toFloat() / AXES) * 2f * PI.toFloat() - PI.toFloat() / 2f

private fun axisPath(
    center: Offset,
    radius: Float,
    values: List<Float>,
): Path =
    Path().apply {
        values.forEachIndexed { index, value ->
            val angle = axisAngle(index)
            val distance = radius * value.coerceIn(0f, 1f)
            val x = center.x + cos(angle) * distance
            val y = center.y + sin(angle) * distance
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

private const val AXES = 5

/**
 * Fraction of the outer polygon, and how strongly that ring prints.
 *
 * A little heavier than the artboard's three white washes, because the ink is now
 * `onSurfaceVariant` — already a muted grey rather than pure white — so the same alpha over it
 * would come out fainter than the drawing was measured at.
 */
private val GRID_RINGS =
    listOf(
        0.348f to 0.10f,
        0.674f to 0.14f,
        1.000f to 0.20f,
    )

/** Last year's outline, at the weight `FingerprintChart` gives its own previous period. */
private const val PREVIOUS_STROKE_ALPHA = 0.55f

private const val PREVIOUS_FILL_ALPHA = 0.05f

/** This year's fill, at the weight `FingerprintChart` gives its own. */
private const val CURRENT_FILL_ALPHA = 0.22f

/** Leaves the outer third of the square to the labels, which sit outside the widest ring. */
private const val POLYGON_RADIUS_RATIO = 0.64f

/** Labels clear the outer ring by a fifth of its radius. */
private const val LABEL_RADIUS_RATIO = 1.22f

/**
 * How wide one axis label may get before it wraps.
 *
 * Solved against the widest vertex rather than guessed: the side axes sit at cos 18° of the label
 * ring, so a label centred there reaches `0.781 × 0.951 × radius` plus half its own width, and
 * this cap is what keeps that inside the square the radar was measured into.
 */
private const val LABEL_MAX_WIDTH_RATIO = 0.40f

private val SWATCH_WIDTH = 14.dp

/**
 * The one figure this card is built around.
 *
 * Named in sp because it is a font size, and fed to `titleLarge.copy(fontSize = …)` rather than
 * built into a `TextStyle` — the family, the weight and the colour still arrive from the theme.
 */
private val ARCHETYPE_SIZE = 42.sp

/** 0.98 of the size — "THE" and the name read as one stacked word. */
private val ARCHETYPE_LINE_HEIGHT = 0.98.em

/** Gap between the shell's header and this card's eyebrow. */
private val CARD_TOP_GAP = 14.dp

/** Gap between this card's last line and the shell's footer. */
private val CARD_BOTTOM_GAP = 22.dp
