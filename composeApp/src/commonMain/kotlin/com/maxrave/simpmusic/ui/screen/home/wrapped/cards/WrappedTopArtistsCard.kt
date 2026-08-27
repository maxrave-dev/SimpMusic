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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.screen.home.analytics.formatListeningTime
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedTokens
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.viewModel.WrappedArtist
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped_artists_title
import simpmusic.composeapp.generated.resources.wrapped_artists_two_years
import simpmusic.composeapp.generated.resources.wrapped_plays

/**
 * Card 04 — a face, and the name written across it.
 *
 * The crop runs deeper here than on card 03 (seven tenths of the card against just over a half) and
 * the name is set as display type rather than as a list title, so the beat lands as a portrait
 * rather than as another chart. Card 03 counted five things down; this one shows one person and
 * files the other four underneath. Two adjacent cards about "your top five" have to differ in shape
 * or the reel stalls.
 *
 * Artist artwork is round wherever it appears, which is the only thing separating a 26dp artist
 * from a 30dp album on the card before it.
 */
@Composable
fun WrappedTopArtistsCard(
    wrapped: WrappedYear,
    modifier: Modifier = Modifier,
) {
    val leader = wrapped.topArtists.firstOrNull() ?: return
    val runnersUp = wrapped.topArtists.drop(1)
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .fillMaxHeight(PORTRAIT_FRACTION),
        ) {
            WrappedArtwork(
                url = leader.artist.thumbnails,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(portraitScrim(MaterialTheme.colorScheme.background)))
        }
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(14.dp))
            WrappedEyebrow(
                text = stringResource(Res.string.wrapped_artists_title),
                modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
            )
            Spacer(Modifier.weight(SPACE_ABOVE_NAME))
            BasicText(
                text = leader.artist.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // A name is not a number: it can be two characters or twenty-five, and there is no
                // shortening it that is not a mistake. So it shrinks instead, all the way down to a
                // size where the longest names on record still fit one line.
                autoSize = TextAutoSize.StepBased(minFontSize = NAME_MIN, maxFontSize = NAME_SIZE),
                // `titleLarge` carries the weight and the title colour; the card supplies only the
                // size and the tracking a name at this scale needs. See card 01's YearFigure.
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = NAME_SIZE,
                        lineHeight = 0.98.em,
                        letterSpacing = (-0.025).em,
                    ),
                modifier = Modifier.padding(start = 16.dp, end = WrappedTokens.ScreenPadding),
            )
            Spacer(Modifier.height(12.dp))
            LeaderMeta(leader)
            Spacer(Modifier.weight(SPACE_NAME_TO_LIST))
            Column(
                modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                runnersUp.forEach { artist -> RunnerUpRow(artist) }
            }
            Spacer(Modifier.weight(SPACE_BELOW_LIST))
        }
    }
}

/**
 * "642 plays · 34h 20m · top artist two years running".
 *
 * The badge is the only accent-coloured text on the card and it appears only for an artist who
 * topped both years — the claim [WrappedArtist.wasTopArtistLastYear] is documented to make. Showing
 * it on any other reading would be a lie the user has no way to check.
 *
 * The accent is read before the builder runs: `buildAnnotatedString` takes an ordinary lambda, so
 * nothing composable can be read inside it.
 */
@Composable
private fun LeaderMeta(artist: WrappedArtist) {
    val plays = stringResource(Res.string.wrapped_plays, formatCount(artist.playCount))
    val listened = formatListeningTime(artist.listenedSeconds)
    val badge =
        if (artist.wasTopArtistLastYear) stringResource(Res.string.wrapped_artists_two_years) else null
    val accent = MaterialTheme.colorScheme.primary
    Text(
        text =
            buildAnnotatedString {
                append(plays)
                append(META_SEPARATOR)
                append(listened)
                if (badge != null) {
                    append(META_SEPARATOR)
                    withStyle(SpanStyle(color = accent)) { append(badge) }
                }
            },
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.4.em),
        modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
    )
}

/** Ranks 2–5. Name only: the play counts belong to #1's paragraph, not to a column of figures. */
@Composable
private fun RunnerUpRow(artist: WrappedArtist) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(
            text = artist.rank.toString(),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(12.dp),
        )
        WrappedArtwork(
            url = artist.artist.thumbnails,
            modifier = Modifier.size(26.dp).clip(CircleShape),
        )
        Text(
            text = artist.artist.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Built from the card's ground, so the portrait sinks into the card rather than into black — which
 * on an artwork-seeded scheme is a different colour.
 *
 * Shifted from the artboard's stops for the 72dp of artwork that bleeds under the shell's header —
 * see the same note on card 03. The clear window is wider and later here than there, because what
 * has to stay legible is a face rather than a record sleeve.
 */
private fun portraitScrim(ground: Color): Brush =
    Brush.verticalGradient(
        0.000f to ground.copy(alpha = 0.40f),
        0.082f to ground.copy(alpha = 0.32f),
        0.243f to ground.copy(alpha = 0.10f),
        0.816f to ground.copy(alpha = 0.88f),
        1.000f to ground,
    )

/** The name's range: the card decides how big it may be, the theme decides everything else. */
private val NAME_SIZE = 47.sp

private val NAME_MIN = 22.sp

private const val META_SEPARATOR = " · "

/** Deeper than card 03's crop: the difference between the two silhouettes is the point. */
private const val PORTRAIT_FRACTION = 0.70f

private const val SPACE_ABOVE_NAME = 3.50f
private const val SPACE_NAME_TO_LIST = 0.85f
private const val SPACE_BELOW_LIST = 1.00f
