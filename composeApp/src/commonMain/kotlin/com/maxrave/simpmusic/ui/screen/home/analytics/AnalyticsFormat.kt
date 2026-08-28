package com.maxrave.simpmusic.ui.screen.home.analytics

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.listening_time_hours_minutes
import simpmusic.composeapp.generated.resources.listening_time_minutes
import simpmusic.composeapp.generated.resources.listening_time_seconds
import simpmusic.composeapp.generated.resources.month_full_apr
import simpmusic.composeapp.generated.resources.month_full_aug
import simpmusic.composeapp.generated.resources.month_full_dec
import simpmusic.composeapp.generated.resources.month_full_feb
import simpmusic.composeapp.generated.resources.month_full_jan
import simpmusic.composeapp.generated.resources.month_full_jul
import simpmusic.composeapp.generated.resources.month_full_jun
import simpmusic.composeapp.generated.resources.month_full_mar
import simpmusic.composeapp.generated.resources.month_full_may
import simpmusic.composeapp.generated.resources.month_full_nov
import simpmusic.composeapp.generated.resources.month_full_oct
import simpmusic.composeapp.generated.resources.month_full_sep
import simpmusic.composeapp.generated.resources.month_short_apr
import simpmusic.composeapp.generated.resources.month_short_aug
import simpmusic.composeapp.generated.resources.month_short_dec
import simpmusic.composeapp.generated.resources.month_short_feb
import simpmusic.composeapp.generated.resources.month_short_jan
import simpmusic.composeapp.generated.resources.month_short_jul
import simpmusic.composeapp.generated.resources.month_short_jun
import simpmusic.composeapp.generated.resources.month_short_mar
import simpmusic.composeapp.generated.resources.month_short_may
import simpmusic.composeapp.generated.resources.month_short_nov
import simpmusic.composeapp.generated.resources.month_short_oct
import simpmusic.composeapp.generated.resources.month_short_sep

/**
 * A listening total in units a person reads, rather than the raw second count.
 *
 * The screen used to print `"$seconds seconds"` straight from the database — a number like
 * 47231, which nobody can turn into "just over thirteen hours" while glancing at a chart. The
 * unit is dropped as soon as it stops carrying information: past an hour the seconds are noise,
 * and past a minute they are close to it.
 */
@Composable
fun formatListeningTime(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    return when {
        hours > 0 -> stringResource(Res.string.listening_time_hours_minutes, hours, minutes)
        minutes > 0 -> stringResource(Res.string.listening_time_minutes, minutes)
        else -> stringResource(Res.string.listening_time_seconds, safe)
    }
}

/**
 * Abbreviated month name for the reader's language.
 *
 * `kotlinx.datetime` ships `MonthNames.ENGLISH_FULL` and `ENGLISH_ABBREVIATED` and nothing else —
 * they are constants, not locale lookups — so every date on this screen read "August" no matter
 * what language the app was in. There is no localized alternative in the library, so the twelve
 * names come from string resources like everything else the user reads.
 */
@Composable
fun monthShortName(month: Month): String =
    stringResource(
        when (month) {
            Month.JANUARY -> Res.string.month_short_jan
            Month.FEBRUARY -> Res.string.month_short_feb
            Month.MARCH -> Res.string.month_short_mar
            Month.APRIL -> Res.string.month_short_apr
            Month.MAY -> Res.string.month_short_may
            Month.JUNE -> Res.string.month_short_jun
            Month.JULY -> Res.string.month_short_jul
            Month.AUGUST -> Res.string.month_short_aug
            Month.SEPTEMBER -> Res.string.month_short_sep
            Month.OCTOBER -> Res.string.month_short_oct
            Month.NOVEMBER -> Res.string.month_short_nov
            Month.DECEMBER -> Res.string.month_short_dec
            else -> Res.string.month_short_jan
        },
    )

/**
 * The full month name's resource, resolved by whoever needs it.
 *
 * Returned unresolved rather than as a `String` because the two callers read it from opposite
 * sides: the recap header resolves it with `stringResource` inside composition, while
 * [com.maxrave.simpmusic.viewModel.LibraryDynamicPlaylistViewModel] resolves the same twelve names
 * with a suspending `getString` when it names a queue. One table, two ways in — a `@Composable`
 * spelling alone would leave the view model with a second copy of the mapping to drift from.
 */
fun monthFullNameResource(month: Month): StringResource =
    when (month) {
        Month.JANUARY -> Res.string.month_full_jan
        Month.FEBRUARY -> Res.string.month_full_feb
        Month.MARCH -> Res.string.month_full_mar
        Month.APRIL -> Res.string.month_full_apr
        Month.MAY -> Res.string.month_full_may
        Month.JUNE -> Res.string.month_full_jun
        Month.JULY -> Res.string.month_full_jul
        Month.AUGUST -> Res.string.month_full_aug
        Month.SEPTEMBER -> Res.string.month_full_sep
        Month.OCTOBER -> Res.string.month_full_oct
        Month.NOVEMBER -> Res.string.month_full_nov
        Month.DECEMBER -> Res.string.month_full_dec
        else -> Res.string.month_full_jan
    }

/**
 * `January` — the register a playlist title needs.
 *
 * [monthShortName] is the abbreviation the charts' label columns are sized for; "Recap Jan" reads
 * like a column header rather than the name of something you press play on.
 */
@Composable
fun monthFullName(month: Month): String = stringResource(monthFullNameResource(month))

/** `22 Aug 2026` — the chart's day bucket. */
@Composable
fun formatChartDay(day: LocalDate): String = "${day.day} ${monthShortName(day.month)} ${day.year}"

/**
 * `16 – 22 Aug` for a week inside one month, `28 Jul – 3 Aug` when it straddles two.
 *
 * The month is printed once where both ends share it — repeating it costs width the label column
 * does not have, and says nothing.
 */
@Composable
fun formatChartWeek(
    start: LocalDate,
    end: LocalDate,
): String =
    if (start.month == end.month) {
        "${start.day} – ${end.day} ${monthShortName(end.month)}"
    } else {
        "${start.day} ${monthShortName(start.month)} – ${end.day} ${monthShortName(end.month)}"
    }

/** `22 Aug` — a date where the year is already obvious from its surroundings. */
@Composable
fun formatChartDayShort(day: LocalDate): String = "${day.day} ${monthShortName(day.month)}"

/**
 * The span the period navigator names: `16 – 22 Aug`, `28 Jul – 3 Aug`, or just `2026`.
 *
 * A whole calendar year is written as the year alone — "1 Jan – 31 Dec 2026" says the same thing
 * three times as wide, and the arrows either side already make it clear this is one step.
 */
@Composable
fun formatPeriodSpan(
    start: LocalDate,
    end: LocalDate,
): String =
    when {
        start.month == Month.JANUARY && start.day == 1 && end.month == Month.DECEMBER && end.day == 31 ->
            "${start.year}"
        start.month == end.month && start.year == end.year ->
            "${start.day} – ${end.day} ${monthShortName(end.month)} ${end.year}"
        start.year == end.year ->
            "${start.day} ${monthShortName(start.month)} – ${end.day} ${monthShortName(end.month)} ${end.year}"
        else ->
            "${start.day} ${monthShortName(start.month)} ${start.year} – ${end.day} ${monthShortName(end.month)} ${end.year}"
    }

/** `Aug 2026` — the chart's month bucket, which used to render the raw enum name. */
@Composable
fun formatChartMonth(
    month: Month,
    year: Int,
): String = "${monthShortName(month)} $year"

/**
 * `21:40 · 22 Aug` — when a play happened.
 *
 * The year is left off deliberately: this list only ever shows recent plays, so the year is the
 * current one on every row and spends width saying nothing.
 */
@Composable
fun formatPlayedAt(timestamp: LocalDateTime): String {
    val hour = timestamp.hour.toString().padStart(2, '0')
    val minute = timestamp.minute.toString().padStart(2, '0')
    return "$hour:$minute · ${timestamp.day} ${monthShortName(timestamp.month)}"
}
