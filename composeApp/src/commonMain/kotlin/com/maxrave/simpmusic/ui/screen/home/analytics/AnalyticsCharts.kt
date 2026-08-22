package com.maxrave.simpmusic.ui.screen.home.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxrave.domain.data.model.analytics.AnalyticsPeriodStats
import com.maxrave.domain.data.model.analytics.ListeningFingerprint
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.albums
import simpmusic.composeapp.generated.resources.analytics_axis_concentration
import simpmusic.composeapp.generated.resources.analytics_axis_consistency
import simpmusic.composeapp.generated.resources.analytics_axis_discovery
import simpmusic.composeapp.generated.resources.analytics_axis_diversity
import simpmusic.composeapp.generated.resources.analytics_axis_replay
import simpmusic.composeapp.generated.resources.analytics_busiest_hour
import simpmusic.composeapp.generated.resources.analytics_decade_coverage
import simpmusic.composeapp.generated.resources.analytics_decade_pre
import simpmusic.composeapp.generated.resources.analytics_hour_span
import simpmusic.composeapp.generated.resources.analytics_no_previous
import simpmusic.composeapp.generated.resources.analytics_plays_in_hour
import simpmusic.composeapp.generated.resources.analytics_previous_period
import simpmusic.composeapp.generated.resources.analytics_previous_was
import simpmusic.composeapp.generated.resources.analytics_songs
import simpmusic.composeapp.generated.resources.analytics_this_period
import simpmusic.composeapp.generated.resources.artists
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The three arc colours, outermost first. Distinct hues, matched lightness. */
private val RATIO_COLORS = listOf(Color(0xFF5AB0F0), Color(0xFF5BE0B0), Color(0xFFB98BF0))

/**
 * Three counts as concentric arcs — Last.fm's "Music ratio".
 *
 * Arcs, not a donut: songs, albums and artists measure three different things and do not add up to
 * any whole, so slicing one circle between them would claim a share of something that does not
 * exist. Concentric rings put them on a common scale without implying they are parts of each other.
 */
@Composable
fun MusicRatioChart(
    stats: AnalyticsPeriodStats,
    previous: AnalyticsPeriodStats?,
    modifier: Modifier = Modifier,
    diameter: Dp = 168.dp,
) {
    val rows =
        listOf(
            Triple(stringResource(Res.string.analytics_songs), stats.distinctTracks, previous?.distinctTracks),
            Triple(stringResource(Res.string.albums), stats.distinctAlbums, previous?.distinctAlbums),
            Triple(stringResource(Res.string.artists), stats.distinctArtists, previous?.distinctArtists),
        )
    val max = rows.maxOf { it.second }.coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(diameter)) {
            val stroke = size.minDimension * 0.085f
            rows.forEachIndexed { i, (_, value, _) ->
                // Radii step inwards by a little more than the stroke so the rings never touch.
                val radius = size.minDimension / 2f - stroke / 2f - i * stroke * 1.55f
                if (radius <= 0f) return@forEachIndexed
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                // Capped at 92% of the circle: a full ring and a nearly-full ring look identical,
                // and the largest value is the one that must stay readable as "the largest".
                drawArc(
                    color = RATIO_COLORS[i],
                    startAngle = -90f,
                    sweepAngle = 360f * 0.92f * (value.toFloat() / max),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEachIndexed { i, (label, value, prev) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(RATIO_COLORS[i]))
                        Text(label, style = typo().bodySmall, maxLines = 1)
                    }
                    Text("$value", style = typo().labelMedium, color = Color.White, maxLines = 1)
                    Text(
                        text =
                            if (prev == null) {
                                stringResource(Res.string.analytics_no_previous)
                            } else {
                                stringResource(Res.string.analytics_previous_was, "$prev")
                            },
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Five behaviour axes as a radar, this period filled and the previous one dashed over it.
 *
 * The second polygon is not decoration. Every axis is self-normalised 0..1, so a lone shape says
 * almost nothing — "consistency 0.62" is neither high nor low until there is something to be high
 * or low against. Last.fm compares against a global average; the only honest reference available
 * here is the listener's own previous period, which is arguably the more useful one anyway.
 */
@Composable
fun FingerprintChart(
    current: ListeningFingerprint,
    previous: ListeningFingerprint?,
    modifier: Modifier = Modifier,
    diameter: Dp = 200.dp,
) {
    val labels =
        listOf(
            stringResource(Res.string.analytics_axis_consistency),
            stringResource(Res.string.analytics_axis_discovery),
            stringResource(Res.string.analytics_axis_diversity),
            stringResource(Res.string.analytics_axis_concentration),
            stringResource(Res.string.analytics_axis_replay),
        )
    val grid = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    val prevColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(diameter)) {
            val radius = size.minDimension / 2f * 0.78f
            fun ring(fraction: Float) = polygonPath(center, radius * fraction, 5)
            listOf(0.33f, 0.66f, 1f).forEach { drawPath(ring(it), grid, style = Stroke(1f)) }
            repeat(5) { i ->
                drawLine(grid, center, axisPoint(center, radius, i, 5, 1f), strokeWidth = 1f)
            }
            previous?.let {
                drawPath(valuePath(center, radius, it.axes), prevColor, style = Stroke(1.5f))
            }
            val now = valuePath(center, radius, current.axes)
            drawPath(now, seed.copy(alpha = 0.22f))
            drawPath(now, seed, style = Stroke(2f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEachIndexed { i, label ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(label, style = typo().bodySmall, maxLines = 1, modifier = Modifier.width(96.dp))
                    Text(
                        "${(current.axes[i] * 100).toInt()}%",
                        style = typo().bodySmall,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDot(seed, stringResource(Res.string.analytics_this_period))
                if (previous != null) LegendDot(prevColor, stringResource(Res.string.analytics_previous_period))
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = typo().bodySmall, maxLines = 1)
    }
}

private fun axisPoint(
    center: Offset,
    radius: Float,
    index: Int,
    count: Int,
    fraction: Float,
): Offset {
    val angle = (index.toFloat() / count) * 2f * PI.toFloat() - PI.toFloat() / 2f
    return Offset(center.x + cos(angle) * radius * fraction, center.y + sin(angle) * radius * fraction)
}

private fun polygonPath(
    center: Offset,
    radius: Float,
    sides: Int,
): Path =
    Path().apply {
        repeat(sides) { i ->
            val p = axisPoint(center, radius, i, sides, 1f)
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }

private fun valuePath(
    center: Offset,
    radius: Float,
    values: List<Float>,
): Path =
    Path().apply {
        values.forEachIndexed { i, v ->
            val p = axisPoint(center, radius, i, values.size, v.coerceIn(0f, 1f))
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }

/**
 * Twenty-four filled wedges around a hole — Last.fm's "Listening clock".
 *
 * Wedges rather than spokes, and every hour keeps a dark wedge running the full ring behind its
 * value. Without that track an hour with one play and an hour with none look nearly the same: the
 * eye reads a short spoke as a missing tick rather than as "almost nothing here".
 */
@Composable
fun ListeningClockChart(
    playsByHour: List<Int>,
    modifier: Modifier = Modifier,
    diameter: Dp = 190.dp,
) {
    val hours = if (playsByHour.size == 24) playsByHour else List(24) { 0 }
    val peak = hours.indices.maxByOrNull { hours[it] } ?: 0
    val max = hours.maxOrNull()?.coerceAtLeast(1) ?: 1
    val track = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(diameter)) {
            val outer = size.minDimension / 2f
            val inner = outer * 0.46f
            val gap = 2.2f
            hours.forEachIndexed { hour, count ->
                val start = hour * 15f + gap / 2f - 90f
                val sweep = 15f - gap
                drawWedge(center, inner, outer, start, sweep, track)
                if (count > 0) {
                    val valueOuter = inner + (outer - inner) * (count.toFloat() / max)
                    drawWedge(
                        center, inner, valueOuter, start, sweep,
                        if (count > max * 0.55f) seed else seed.copy(alpha = 0.45f),
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(Res.string.analytics_busiest_hour), style = typo().bodySmall, maxLines = 1)
            Text(
                stringResource(Res.string.analytics_hour_span, hourLabel(peak), hourLabel((peak + 1) % 24)),
                style = typo().labelMedium,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                stringResource(Res.string.analytics_plays_in_hour),
                style = typo().bodySmall,
                maxLines = 1,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text("${hours[peak]}", style = typo().labelMedium, color = Color.White, maxLines = 1)
        }
    }
}

/** One annulus sector, built by hand — `drawArc` can only stroke a ring, not fill a slice of one. */
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
 * Plays by release decade, with the share of plays that could be dated stated underneath.
 *
 * The coverage line is required, not polite: `playback_event.albumBrowseId` is nullable, so radio
 * and standalone videos carry no album and therefore no year. A distribution that silently drops
 * an unknown share of its input is not a distribution.
 */
@Composable
fun DecadeChart(
    stats: AnalyticsPeriodStats,
    modifier: Modifier = Modifier,
) {
    if (stats.decades.isEmpty()) return
    val max = stats.decades.maxOf { it.plays }.coerceAtLeast(1)
    val coverage = if (stats.plays <= 0) 0 else ((stats.datedPlays * 100L) / stats.plays).toInt()
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        stats.decades.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text =
                        if (row.decade < 1960) {
                            stringResource(Res.string.analytics_decade_pre, 1960)
                        } else {
                            "${row.decade}s"
                        },
                    style = typo().bodySmall,
                    maxLines = 1,
                    modifier = Modifier.width(88.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(16.dp)
                        .clip(CircleShape)
                        .background(trackColor),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((row.plays.toFloat() / max).coerceIn(0.02f, 1f))
                            .height(16.dp)
                            .clip(CircleShape)
                            .background(if (row.plays == max) seed else seed.copy(alpha = 0.45f)),
                    )
                }
                Text(
                    "${row.plays}",
                    style = typo().bodySmall,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 10.dp).width(40.dp),
                )
            }
        }
        Text(
            stringResource(Res.string.analytics_decade_coverage, "$coverage%"),
            style = typo().bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * A change against the same span one period earlier, or null when there is nothing to compare to.
 *
 * Returning null rather than "+100%" is the whole point: on a first week every figure would
 * otherwise read as an infinite increase, which is noise dressed as insight.
 */
fun percentDelta(
    now: Long,
    before: Long?,
): Int? {
    if (before == null || before <= 0L) return null
    return (((now - before) * 100.0) / before).toInt()
}

/**
 * `08:00` — zero-padded here rather than in the string resource.
 *
 * Compose Resources substitutes `%1$s` and `%1$d` but does NOT understand the flag/width part of a
 * format specifier, so `%1$02d` is left in the output verbatim. Padding belongs in Kotlin; the
 * resource only ever joins already-formatted pieces.
 */
private fun hourLabel(hour: Int): String = "${hour.toString().padStart(2, '0')}:00"
