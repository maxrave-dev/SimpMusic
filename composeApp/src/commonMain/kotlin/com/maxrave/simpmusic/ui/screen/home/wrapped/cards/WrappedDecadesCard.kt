package com.maxrave.simpmusic.ui.screen.home.wrapped.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedTokens
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatPercent
import com.maxrave.simpmusic.viewModel.WrappedDecadeShare
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.analytics_decade_pre
import simpmusic.composeapp.generated.resources.wrapped_decades_caption
import simpmusic.composeapp.generated.resources.wrapped_decades_title

/**
 * Card 09 — how far back the year reached, as blocks whose height IS the share.
 *
 * The card is not gated here: [WrappedYear.cards] already drops it below the coverage floor, so
 * reaching this composable means the chart is worth drawing. The caveat line stays regardless —
 * `playback_event.albumBrowseId` is nullable, radio and standalone videos carry no album and so no
 * year, and a distribution that silently omits an unknown share of its input is not a distribution.
 *
 * Decade names are formed the way the Analytics decade chart forms them, down to the "Before 1960"
 * bucket, so the two screens cannot end up labelling the same row differently.
 */
@Composable
fun WrappedDecadesCard(
    wrapped: WrappedYear,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Spacer(Modifier.height(CARD_TOP_GAP))
        WrappedEyebrow(
            text = stringResource(Res.string.wrapped_decades_title),
            modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
        )

        Spacer(Modifier.height(30.dp))
        Column(Modifier.fillMaxWidth().weight(1f)) {
            wrapped.decades.forEachIndexed { index, decade ->
                DecadeBar(decade = decade, rank = index)
            }
        }

        Spacer(Modifier.height(36.dp))
        Text(
            text =
                stringResource(
                    Res.string.wrapped_decades_caption,
                    formatPercent(wrapped.decadeCoverage),
                ),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 1.5.em),
            modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
        )
        Spacer(Modifier.height(CARD_BOTTOM_GAP))
    }
}

/**
 * One decade, sized by weight rather than by a computed height, so the stack always fits.
 *
 * The floor added to every share is what stops a 3% decade collapsing into a hairline nobody can
 * read a label off. A twelfth reproduces the artboard's four heights almost exactly — 61/27/9/3
 * lands on 52%/26%/13%/9% of the block against its drawn 52%/26%/13%/9% — and, being a share of
 * the stack rather than a fixed dp, it keeps working when a year spans eight decades instead of
 * four.
 *
 * The winner is the only row that has to name a colour, and it names two: `primary` for the fill
 * and `onPrimary` for the text on it. That pairing is not decoration — the scheme is seeded from
 * the user's own covers, so `primary` can land anywhere from near-black to near-white, and the
 * scale's own white title colour would disappear on half of those.
 */
@Composable
private fun ColumnScope.DecadeBar(
    decade: WrappedDecadeShare,
    rank: Int,
) {
    val share = decade.share.coerceIn(0f, 1f)
    val isWinner = rank == 0
    // Only the accent bar overrides the ink. Every other row is a dark container, which is exactly
    // what the scale's title colour is already correct against, so it passes nothing.
    val onBar = if (isWinner) MaterialTheme.colorScheme.onPrimary else Color.Unspecified

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .weight(share + BAR_WEIGHT_FLOOR)
                .background(barFill(rank))
                .padding(horizontal = WrappedTokens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = decadeLabel(decade.decade),
            style = labelStyle(rank),
            color = onBar,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatPercent(share),
            style = shareStyle(rank),
            color = onBar,
            maxLines = 1,
        )
    }
}

/** "2020s", or the Analytics chart's own catch-all for anything older than its first bucket. */
@Composable
private fun decadeLabel(decade: Int): String =
    if (decade < EARLIEST_NAMED_DECADE) {
        stringResource(Res.string.analytics_decade_pre, EARLIEST_NAMED_DECADE)
    } else {
        "${decade}s"
    }

/**
 * The winner takes the accent; everything under it steps down the M3 container ladder.
 *
 * The artboard washes the trailing bars in white at 12.5/9/5.5/4 percent. These are solid blocks
 * sitting on the page, not veils over artwork, so the honest equivalent is the tone ladder the
 * scheme already publishes for exactly this — four surfaces that are each a step nearer the ground
 * than the last, and that keep stepping correctly whatever the artwork seeds them from. A wash at
 * a hand-picked alpha would have to be re-tuned per cover; these do not.
 */
@Composable
private fun barFill(rank: Int): Color {
    val scheme = MaterialTheme.colorScheme
    return when (rank) {
        0 -> scheme.primary
        1 -> scheme.surfaceContainerHighest
        2 -> scheme.surfaceContainerHigh
        3 -> scheme.surfaceContainer
        else -> scheme.surfaceContainerLow
    }
}

/**
 * The decade's name, ranked.
 *
 * Only the winner is given a size of its own — it is this card's hero, the way the date is card
 * 07's. Second and third step down through `titleMedium` and `titleSmall`, which is the scale's
 * own ladder rather than three more numbers to keep in sync.
 */
@Composable
private fun labelStyle(rank: Int): TextStyle =
    when (rank) {
        0 ->
            MaterialTheme.typography.titleLarge.copy(
                fontSize = WINNER_LABEL_SIZE,
                letterSpacing = (-0.02).em,
            )

        1 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleSmall
    }

/** The share, one tier below its own label so the name leads and the number confirms. */
@Composable
private fun shareStyle(rank: Int): TextStyle =
    when (rank) {
        0 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.titleSmall
    }

/**
 * Added to every share before it becomes a weight.
 *
 * Solved from the artboard rather than guessed: its four blocks are 262/132/66/44dp for shares of
 * 61/27/9/3 percent, and `(share + 1/12) / (1 + n/12)` reproduces all four to within a dp.
 */
private const val BAR_WEIGHT_FLOOR = 1f / 12f

/**
 * The one figure this card is built around.
 *
 * Named in sp because it is a font size, and fed to `titleLarge.copy(fontSize = …)` rather than
 * built into a `TextStyle` — the family, the weight and the colour still arrive from the theme.
 */
private val WINNER_LABEL_SIZE = 40.sp

/** Matches the Analytics decade chart's own first bucket. */
private const val EARLIEST_NAMED_DECADE = 1960

/** Gap between the shell's header and this card's eyebrow. */
private val CARD_TOP_GAP = 14.dp

/** Gap between this card's last line and the shell's footer. */
private val CARD_BOTTOM_GAP = 22.dp
