package com.maxrave.simpmusic.ui.screen.home.wrapped.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.simpmusic.ui.screen.home.analytics.monthShortName
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedTokens
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.viewModel.WrappedBiggestDay
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped_biggest_day_caption
import simpmusic.composeapp.generated.resources.wrapped_biggest_day_plays
import simpmusic.composeapp.generated.resources.wrapped_biggest_day_title

/**
 * Card 07 — the one day of the year that beat every other one.
 *
 * The month comes from [monthShortName], which reads the twelve abbreviations out of string
 * resources; `MonthNames.ENGLISH_ABBREVIATED` is a constant rather than a locale lookup and would
 * print "Feb" into every language the app ships. It is also left in the casing the translator
 * chose rather than uppercased to match the artboard: machine-uppercasing translated text is how
 * a Turkish dotted i turns into the wrong letter on a card that leaves the app.
 *
 * The date is the only thing on the card given a size of its own. Everything under it — the play
 * count, the comparison caption, the closing sentence — is a plain style off
 * `MaterialTheme.typography`, and takes its colour from that style rather than from an argument.
 */
@Composable
fun WrappedBiggestDayCard(
    wrapped: WrappedYear,
    modifier: Modifier = Modifier,
) {
    // Unreachable in the reel — WrappedYear.cards already drops this card when the day is
    // absent — but the parameter is the whole year, so the card answers for it rather than
    // assuming it away.
    val day = wrapped.biggestDay ?: return

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Spacer(Modifier.height(CARD_TOP_GAP))
        WrappedEyebrow(
            text = stringResource(Res.string.wrapped_biggest_day_title),
            modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
        )

        Spacer(Modifier.weight(1.60f))
        Text(
            text = "${day.date.day}\n${monthShortName(day.date.month)}",
            // The card's hero, and its one named size. `titleLarge` already carries Bold and the
            // scale's title colour, so blowing it up needs no other argument.
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontSize = DATE_FIGURE_SIZE,
                    lineHeight = DATE_FIGURE_LINE_HEIGHT,
                    letterSpacing = HERO_LETTER_SPACING,
                ),
            // Four dp tighter than every other block: at this size the numerals' own side bearing
            // is already doing the job of the margin, and matching it optically means undercutting
            // it metrically.
            modifier = Modifier.padding(start = WrappedTokens.ScreenPadding - 4.dp),
        )

        Spacer(Modifier.weight(0.70f))
        PlayCountLine(day)

        Spacer(Modifier.weight(0.60f))
        DayAgainstTypical(day)

        Spacer(Modifier.weight(1.00f))
        day.topTrack?.let { TopTrackLine(it, day.topTrackPlays) }
        Spacer(Modifier.height(CARD_BOTTOM_GAP))
    }
}

/** "41 · plays — against a typical 9", the two figures sharing one baseline. */
@Composable
private fun PlayCountLine(day: WrappedBiggestDay) {
    Row(
        modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = formatCount(day.plays),
            // Straight off the scale at its own size: the date above is what carries the card, and
            // a second enlarged figure would compete with it rather than rank under it. The accent
            // is the one thing named here, and it is the artwork's own — the same `primary` the
            // bar below is filled with, so the number and its picture read as one statement.
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            // alignByBaseline rather than Alignment.Bottom: these two sit at very different sizes,
            // and bottom-aligning them would hang the small one off the big one's descender.
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text =
                stringResource(
                    Res.string.wrapped_biggest_day_plays,
                    formatCount(day.typicalPlays),
                ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

/**
 * The day beside an ordinary one, full-bleed, as two bars.
 *
 * The artboard draws fourteen bars with the seventh towering over its neighbours, which reads as
 * "the days around it". There is no daily series in [WrappedYear] to fill the other thirteen from,
 * and this card is captured to a PNG that leaves the app, so thirteen invented neighbours would be
 * a fabricated chart on a shareable image. The two bars the card genuinely knows about say the
 * same thing — the line above names both numbers, and this is the picture of that sentence.
 *
 * The ordinary day is a `surfaceContainerHighest` block rather than a wash of white at some alpha:
 * it is a solid object standing next to another solid object, which is exactly what the M3
 * container ladder is for, and it steps with the scheme instead of having to be re-tuned per
 * artwork.
 */
@Composable
private fun DayAgainstTypical(day: WrappedBiggestDay) {
    val typicalFraction =
        if (day.plays <= 0) {
            0f
        } else {
            (day.typicalPlays.toFloat() / day.plays).coerceIn(TYPICAL_BAR_FLOOR, 1f)
        }

    Row(
        modifier = Modifier.fillMaxWidth().height(COMPARISON_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight(typicalFraction)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
    }
}

/** "You played <song> eleven times that day." The song, and only the song, at full strength. */
@Composable
private fun TopTrackLine(
    song: SongEntity,
    plays: Int,
) {
    val sentence =
        stringResource(
            Res.string.wrapped_biggest_day_caption,
            song.title,
            formatCount(plays),
        )
    val titleStart = sentence.indexOf(song.title)
    val emphasis = MaterialTheme.colorScheme.onSurface

    Text(
        text =
            buildAnnotatedString {
                append(sentence)
                // The title is substituted verbatim, so this find is exact; when a translation ever
                // reshapes it past recognition the line simply stays one weight, never breaks.
                if (song.title.isNotEmpty() && titleStart >= 0) {
                    addStyle(SpanStyle(color = emphasis), titleStart, titleStart + song.title.length)
                }
            },
        // The sentence is body copy and takes the scale's muted body colour; the span above lifts
        // the title out of it. Those two roles are the whole point of the line, so `onSurface` is
        // named once, for the half that has to stand out.
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = CAPTION_LINE_HEIGHT),
        modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
    )
}

/**
 * The one figure this card is built around.
 *
 * Named in sp because it is a font size, and fed to `titleLarge.copy(fontSize = …)` rather than
 * built into a `TextStyle` — the family, the weight and the colour still arrive from the theme.
 */
private val DATE_FIGURE_SIZE = 88.sp

/** 0.86 of the size — the two lines of the date read as one block, not as two words. */
private val DATE_FIGURE_LINE_HEIGHT = 0.86.em

/** Numerals this large need their tracking pulled back in, or the block reads as spaced-out. */
private val HERO_LETTER_SPACING = (-0.03).em

private val CAPTION_LINE_HEIGHT = 1.6.em

private val COMPARISON_HEIGHT = 116.dp

/** A typical day is never drawn as nothing, however lopsided the year's best day was. */
private const val TYPICAL_BAR_FLOOR = 0.04f

/** Gap between the shell's header and this card's eyebrow. */
private val CARD_TOP_GAP = 14.dp

/** Gap between this card's last line and the shell's footer. */
private val CARD_BOTTOM_GAP = 22.dp
