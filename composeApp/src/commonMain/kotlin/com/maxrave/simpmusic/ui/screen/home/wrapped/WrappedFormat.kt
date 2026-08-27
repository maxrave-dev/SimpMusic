package com.maxrave.simpmusic.ui.screen.home.wrapped

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import com.maxrave.simpmusic.viewModel.WrappedListeningBand
import kotlin.math.roundToInt

/**
 * Formatting for the reel, kept out of the cards.
 *
 * Compose Resources understands `%1$s` and `%1$d` and nothing else — no width, no flags, no `%%` —
 * so every separator, unit symbol and rounding decision has to be made in Kotlin and handed to the
 * string as an already-finished piece. A string resource here only ever joins finished pieces,
 * which is also what spares translators from format specifiers.
 *
 * Anything the Analytics screen already formats is reused from
 * [com.maxrave.simpmusic.ui.screen.home.analytics.AnalyticsFormat] rather than reimplemented — a
 * duration printed two ways in one app is a bug the user can see.
 */

/**
 * Thousands-grouped, the way the design prints every large figure ("18,430").
 *
 * Grouping is done by hand because there is no `NumberFormat` in common Kotlin. The separator
 * follows the locale for the handful of languages the app ships where a comma would be wrong;
 * everything else gets the comma the design draws.
 */
@Composable
fun formatCount(value: Long): String = groupDigits(value, groupingSeparatorFor(Locale.current.language))

@Composable
fun formatCount(value: Int): String = formatCount(value.toLong())

/** A fraction 0..1 as whole percent — "41%". Rounded, never truncated: 0.999 is 100%, not 99%. */
fun formatPercent(fraction: Float): String = "${(fraction.coerceIn(0f, 1f) * 100).roundToInt()}%"

/**
 * The hour a card prints beside its meridiem — "9" next to "PM".
 *
 * Split rather than returned whole because card 06 sets the two at wildly different sizes. On a
 * 24-hour locale the meridiem comes back empty and the number carries the whole label.
 */
fun hourNumber(
    hour: Int,
    use24Hour: Boolean,
): String =
    when {
        use24Hour -> hour.toString()
        hour % 12 == 0 -> "12"
        else -> (hour % 12).toString()
    }

/** Empty on a 24-hour locale, where a meridiem would be wrong rather than merely redundant. */
fun hourMeridiem(
    hour: Int,
    use24Hour: Boolean,
): String =
    when {
        use24Hour -> ""
        hour < 12 -> "AM"
        else -> "PM"
    }

/** "9 PM", or "21" where a meridiem does not belong. Used inside sentences, not as a hero figure. */
fun hourLabel(
    hour: Int,
    use24Hour: Boolean,
): String =
    hourMeridiem(hour, use24Hour)
        .let { meridiem -> if (meridiem.isEmpty()) hourNumber(hour, true) else "${hourNumber(hour, false)} $meridiem" }

/** The two ends of a band, as card 06's caption names them. */
fun bandBounds(
    band: WrappedListeningBand,
    use24Hour: Boolean,
): Pair<String, String> = hourLabel(band.startHour, use24Hour) to hourLabel(band.endHour % 24, use24Hour)

/** Seconds to whole minutes — the figure card 02 is built around. */
fun wholeMinutes(seconds: Long): Long = seconds / 60

/** Seconds to whole days, for card 02's "12 whole days". Floored: claiming a day that did not finish would be a lie. */
fun wholeDays(seconds: Long): Long = seconds / 86_400

private fun groupDigits(
    value: Long,
    separator: Char,
): String {
    val digits = value.toString()
    val negative = digits.startsWith('-')
    val body = if (negative) digits.drop(1) else digits
    if (body.length <= 4) return digits
    val grouped =
        body
            .reversed()
            .chunked(3)
            .joinToString(separator.toString())
            .reversed()
    return if (negative) "-$grouped" else grouped
}

/**
 * A comma everywhere the design's comma is right, a full stop where it would misread.
 *
 * Deliberately a short list rather than a general rule: these are the languages the app ships where
 * a comma marks the DECIMAL point, so "18,430" would read as eighteen-point-four-three-zero.
 */
private fun groupingSeparatorFor(language: String): Char =
    when (language.lowercase()) {
        "de", "es", "it", "nl", "pt", "id", "tr", "vi", "da", "ca", "ro", "el", "sr", "hr", "sl", "az", "uk", "ru", "bg", "cs", "sk", "pl", "hu", "fi", "sv", "nb", "no", "lv", "lt", "et" -> '.'
        else -> ','
    }
