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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.maxrave.domain.utils.connectArtists
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedTokens
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.viewModel.WrappedTrack
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped_plays
import simpmusic.composeapp.generated.resources.wrapped_tracks_title

/**
 * Card 03 — the five, counted down from one.
 *
 * The first track is set at more than twice the size of the other four and is the only one whose
 * rank is in the accent colour, so the ranking is legible before a single play count is read. That
 * asymmetry is the whole card: a list of five equal rows would say "here are five tracks", not
 * "this one, and then four others".
 *
 * Only #1's artwork is shown large — as the crop the card is built on — because it is the same
 * cover the reel has already used behind card 02, which is what ties the two together. The runners
 * up get 30dp squares, deliberately not circles: circles are card 04's language, for artists.
 *
 * The four rows below the rule are drawn entirely out of the app's type scale, without naming a
 * single colour: under `ForceDarkContent` a `titleSmall` is white and a `bodySmall` is muted, which
 * is exactly the rank / title / subtitle hierarchy the artboard drew by hand in three alphas.
 */
@Composable
fun WrappedTopTracksCard(
    wrapped: WrappedYear,
    modifier: Modifier = Modifier,
) {
    val leader = wrapped.topTracks.firstOrNull() ?: return
    val runnersUp = wrapped.topTracks.drop(1)
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .fillMaxHeight(HERO_FRACTION),
        ) {
            WrappedArtwork(
                url = leader.song.thumbnails,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(heroScrim(MaterialTheme.colorScheme.background)))
        }
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(14.dp))
            WrappedEyebrow(
                text = stringResource(Res.string.wrapped_tracks_title),
                modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
            )
            Spacer(Modifier.weight(SPACE_ABOVE_LEADER))
            Column(Modifier.padding(horizontal = WrappedTokens.ScreenPadding)) {
                LeaderRow(leader)
                if (runnersUp.isNotEmpty()) {
                    Spacer(Modifier.height(22.dp))
                    // The app's own rule rather than a 1dp Box with a hand-mixed alpha on it:
                    // `outlineVariant` is the role a divider is supposed to be drawn in, and it
                    // moves with the artwork-seeded scheme the way everything else on the card does.
                    HorizontalDivider()
                    Spacer(Modifier.height(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        runnersUp.forEach { track -> RunnerUpRow(track) }
                    }
                }
            }
            Spacer(Modifier.weight(SPACE_BELOW_LIST))
        }
    }
}

/**
 * The scrim over #1's crop, built from the card's own ground so it darkens *towards the card*
 * rather than towards black — on an artwork-seeded scheme those are not the same colour.
 *
 * Stops are the artboard's, shifted for the fact that a card's box starts 72dp down the screen: the
 * artwork bleeds under the shell's header, so what a card actually draws is the block's lower
 * five-sixths. Ported unshifted, the clear window at 10% opacity — the part meant to hold the
 * cover's face — would land some 75dp too low and the crop would read as murk with a bright strip
 * under it.
 */
private fun heroScrim(ground: Color): Brush =
    Brush.verticalGradient(
        0.000f to ground.copy(alpha = 0.37f),
        0.079f to ground.copy(alpha = 0.30f),
        0.268f to ground.copy(alpha = 0.10f),
        0.835f to ground.copy(alpha = 0.86f),
        1.000f to ground,
    )

/**
 * #1: the rank as display type, the title beside it, both sitting on the same baseline.
 *
 * The rank is the only accent-coloured thing on the card, and the accent is `primary` — so the
 * colour it prints in comes from the cover behind it rather than from a constant.
 */
@Composable
private fun LeaderRow(track: WrappedTrack) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = track.rank.toString(),
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontSize = LEADER_RANK_SIZE,
                    color = MaterialTheme.colorScheme.primary,
                    lineHeight = 0.78.em,
                ),
        )
        // Lifted off the numeral's baseline: the digit has no descender, so aligning the two blocks
        // flush would leave the title sitting visibly lower than the number it belongs to.
        Column(Modifier.padding(bottom = 4.dp)) {
            Text(
                // Straight off the scale: `titleLarge` is 25sp Bold in the title colour, which is
                // the artboard's 24px/700 white to within a rounding.
                text = track.song.title,
                style = MaterialTheme.typography.titleLarge.copy(lineHeight = 1.15.em),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = trackSubtitle(track),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Ranks 2–5: same three facts as #1, at a size that reads as a footnote to it. */
@Composable
private fun RunnerUpRow(track: WrappedTrack) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = track.rank.toString(),
            // Bold so a numeral reads as a rank rather than as more small print; still body colour,
            // so it stays behind the title it belongs to.
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(14.dp),
        )
        WrappedArtwork(
            url = track.song.thumbnails,
            modifier = Modifier.size(30.dp),
        )
        // Takes the remaining width so both lines have something to ellipsize against; without it
        // the row would size to its longest title and push the whole list off the card.
        Column(Modifier.weight(1f)) {
            Text(
                text = track.song.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = trackSubtitle(track),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * "ERIK · 168 plays", or just the plays where the row carries no artist.
 *
 * The separator is dropped rather than left hanging: a row reading "· 168 plays" tells the user
 * something is missing, which is worse than simply not mentioning it.
 */
@Composable
private fun trackSubtitle(track: WrappedTrack): String {
    val plays = stringResource(Res.string.wrapped_plays, formatCount(track.playCount))
    val artists = track.song.artistName?.connectArtists()?.takeIf { it.isNotBlank() }
    return if (artists == null) plays else "$artists · $plays"
}

/** #1's crop, as a share of the card rather than a dp: it is a backdrop, not a fixed object. */
private const val HERO_FRACTION = 0.57f

private const val SPACE_ABOVE_LEADER = 1.85f
private const val SPACE_BELOW_LIST = 1.00f

/** The card's one oversized figure — the size, and nothing else, is the card's to decide. */
private val LEADER_RANK_SIZE = 62.sp
