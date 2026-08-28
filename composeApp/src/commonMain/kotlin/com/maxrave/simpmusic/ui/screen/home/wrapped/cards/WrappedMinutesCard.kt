package com.maxrave.simpmusic.ui.screen.home.wrapped.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedTokens
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.ui.screen.home.wrapped.wholeDays
import com.maxrave.simpmusic.ui.screen.home.wrapped.wholeMinutes
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped_minutes_caption
import simpmusic.composeapp.generated.resources.wrapped_minutes_title
import simpmusic.composeapp.generated.resources.wrapped_minutes_unit
import simpmusic.composeapp.generated.resources.wrapped_minutes_whole_days

/**
 * Card 02 — one number, as big as the card will hold it.
 *
 * After the opening's wall of artwork this card is almost empty on purpose: the reel's second beat
 * is a single statistic, and giving it the whole screen is what makes it land. The artwork does not
 * disappear so much as recede — it survives as one horizontal band behind the figure, which keeps
 * the card recognisably part of the same reel without competing with the digits.
 *
 * The band's scrim stops at 78% rather than ramping to the ground, so its lower edge stays a hard
 * cut. That edge is the point: it reads as a strip laid on the card rather than as a photograph
 * fading out, and it is what separates this card's silhouette from cards 03 and 04, whose artwork
 * genuinely does dissolve into the ground.
 */
@Composable
fun WrappedMinutesCard(
    wrapped: WrappedYear,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Spacer(Modifier.weight(SPACE_ABOVE_EYEBROW))
        WrappedEyebrow(
            text = stringResource(Res.string.wrapped_minutes_title),
            modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
        )
        Spacer(Modifier.height(24.dp))
        MinutesBand(wrapped)
        Spacer(Modifier.weight(SPACE_BAND_TO_CAPTION))
        MinutesCaption(wrapped)
        Spacer(Modifier.weight(SPACE_BELOW_CAPTION))
    }
}

/**
 * The figure, and the strip of artwork it sits on.
 *
 * The band takes its height from the figure rather than from a fixed dp, so the two can never drift
 * apart: whatever the type does — autosizing down for a six-digit total, or growing on a tablet —
 * the strip follows it. [BAND_UNDERHANG] is the only free number, and it is what gives the figure
 * somewhere to sit rather than balancing on the band's edge.
 *
 * The unit label is the one thing on this card that names a colour, and it names a role: it is the
 * reel's accent, which under the shell's artwork-seeded scheme is `primary` — so it is the user's
 * own record that decides what colour "MINUTES" prints in.
 */
@Composable
private fun MinutesBand(wrapped: WrappedYear) {
    val ground = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth()) {
        WrappedArtwork(
            url = wrapped.topTracks.firstOrNull()?.song?.thumbnails,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0.00f to ground.copy(alpha = 0.40f),
                    0.46f to ground.copy(alpha = 0.62f),
                    1.00f to ground.copy(alpha = 0.78f),
                ),
            ),
        )
        Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = BAND_UNDERHANG)) {
            BasicText(
                text = formatCount(wholeMinutes(wrapped.stats.listenedSeconds)),
                maxLines = 1,
                autoSize =
                    TextAutoSize.StepBased(
                        minFontSize = MINUTES_FIGURE_MIN,
                        maxFontSize = MINUTES_FIGURE_SIZE,
                    ),
                // `titleLarge` and not `displayLarge`: it is already Bold in the title colour, so
                // the one thing this figure needs from the card is its size. See the same note on
                // card 01's YearFigure.
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = MINUTES_FIGURE_SIZE,
                        lineHeight = MINUTES_FIGURE_LINE_HEIGHT,
                        letterSpacing = (-0.045).em,
                    ),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.wrapped_minutes_unit),
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.34.em,
                    ),
                // Brings the tracked-out capitals back onto the gutter the figure sits on: wide
                // letter-spacing pads the left of the first glyph as well as the right of the last.
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

/** The figure said again in a unit a person can picture, then the caveat that makes it honest. */
@Composable
private fun MinutesCaption(wrapped: WrappedYear) {
    val days = stringResource(Res.string.wrapped_minutes_whole_days, formatCount(wholeDays(wrapped.stats.listenedSeconds)))
    val caveat = stringResource(Res.string.wrapped_minutes_caption)
    Text(
        text = "$days $caveat",
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.55.em),
        modifier = Modifier.padding(start = WrappedTokens.ScreenPadding, end = 24.dp),
    )
}

// Proportions of the artboard's leftover space, not dp: the card has to hold its shape on a short
// phone and on a desktop window, and only the gaps are free to absorb the difference.
private const val SPACE_ABOVE_EYEBROW = 1.10f
private const val SPACE_BAND_TO_CAPTION = 0.32f
private const val SPACE_BELOW_CAPTION = 1.00f

/** How far the band runs on past the unit label. */
private val BAND_UNDERHANG = 68.dp

/** The card's one figure, and the floor it may shrink to. */
private val MINUTES_FIGURE_SIZE = 106.sp

private val MINUTES_FIGURE_MIN = 44.sp

/**
 * Derived from the figure's size and NOT from the size it autosized to — which is the whole point.
 * An `em` leading would shrink with a six-digit total and take the band's height down with it, so a
 * long year and a short one would draw two differently shaped cards.
 */
private val MINUTES_FIGURE_LINE_HEIGHT = MINUTES_FIGURE_SIZE * 0.92f
