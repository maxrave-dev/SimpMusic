package com.maxrave.simpmusic.ui.screen.home.wrapped.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedTokens
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped_opening_days
import simpmusic.composeapp.generated.resources.wrapped_opening_lead
import simpmusic.composeapp.generated.resources.wrapped_opening_title
import simpmusic.composeapp.generated.resources.wrapped_opening_tracks
import kotlin.math.ceil

/**
 * Card 01 — the year, over a wall of everything that played in it.
 *
 * The reel opens on its widest shot on purpose: a full-bleed mosaic of the user's own artwork with
 * one enormous numeral over it, so the first thing seen is *their* year rather than a chart. Every
 * card after this one narrows — a single figure, a countdown, one face — which is what makes the
 * reel read as a story instead of ten identical screens.
 *
 * Nothing here names a colour or a size of its own. The ground, the scrim and the one highlighted
 * clause are roles off `MaterialTheme.colorScheme`, which inside the reel is an artwork-seeded dark
 * scheme; the copy is the app's own type scale, which under `ForceDarkContent` already resolves to
 * a white title over muted body text. The single exception is the year's own font size, and it is
 * argued for on [YearFigure].
 */
@Composable
fun WrappedOpeningCard(
    wrapped: WrappedYear,
    modifier: Modifier = Modifier,
) {
    // The card paints its own ground because `Capturable` records the node's own drawing: a card
    // that let the shell's background show through would be captured transparent.
    val ground = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxSize().background(ground)) {
        WrappedArtworkMosaic(
            urls = wrapped.mosaicArtwork(),
            modifier = Modifier.matchParentSize(),
        )
        // Ramps to the ground colour rather than to transparent: the copy below sits on the mosaic,
        // and a scrim that stops short leaves the last line fighting an album cover for contrast.
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0.00f to ground.copy(alpha = 0.28f),
                    0.42f to ground.copy(alpha = 0.60f),
                    0.80f to ground.copy(alpha = 0.95f),
                    1.00f to ground,
                ),
            ),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = WrappedTokens.ScreenPadding, bottom = OPENING_BASELINE),
            verticalArrangement = Arrangement.Bottom,
        ) {
            WrappedEyebrow(stringResource(Res.string.wrapped_opening_title))
            Spacer(Modifier.height(10.dp))
            YearFigure(wrapped.year)
            Spacer(Modifier.height(16.dp))
            OpeningLead(wrapped)
        }
    }
}

/**
 * The year, set as large as the card can carry it.
 *
 * This is the one place the card steps outside the type scale, and it has to: the scale tops out at
 * 25sp, which is a list heading, not the object an opening card is built around. So it takes
 * `titleLarge` and copies a size onto it — the app's own idiom for an oversized figure, live at
 * `ListenTogetherScreen.kt:482`. `titleLarge` and not `displayLarge` because `titleLarge` is
 * already Bold in the title colour, so a numeral this size needs neither a weight nor a colour
 * argument; `displayLarge` in this app is 20sp Normal in *body* colour, and copying a size onto it
 * would give a grey, light hero that has to be argued back to white by hand.
 *
 * It autosizes down because "2026" at this size is only barely inside a 390dp phone and a narrower
 * window would clip it — a hero figure shrinks, it never gets cut.
 */
@Composable
private fun YearFigure(year: Int) {
    BasicText(
        text = year.toString(),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = YEAR_FIGURE_MIN, maxFontSize = YEAR_FIGURE_SIZE),
        style =
            MaterialTheme.typography.titleLarge.copy(
                fontSize = YEAR_FIGURE_SIZE,
                // In em, so a year that had to shrink keeps the same optical leading.
                lineHeight = 0.80.em,
                letterSpacing = (-0.055).em,
            ),
        // Pulls the numeral's left sidebearing back onto the gutter. Optical, so it is a fixed
        // nudge rather than a fraction of anything.
        modifier = Modifier.offset(x = (-10).dp),
    )
}

/**
 * "You opened SimpMusic on 287 of 365 days. 1,204 tracks went past."
 *
 * Three resources rather than one sentence, so the middle clause can be lifted from body colour to
 * `onSurface` — it is the only figure on this card and the surrounding prose is there to frame it,
 * not to compete. The role is read before the builder runs: `buildAnnotatedString` takes an
 * ordinary lambda, so nothing composable can be read inside it.
 */
@Composable
private fun OpeningLead(wrapped: WrappedYear) {
    val lead = stringResource(Res.string.wrapped_opening_lead)
    val days =
        stringResource(
            Res.string.wrapped_opening_days,
            formatCount(wrapped.stats.activeDays),
            formatCount(wrapped.daysInYear),
        )
    val tracks = stringResource(Res.string.wrapped_opening_tracks, formatCount(wrapped.stats.distinctTracks))
    val highlight = MaterialTheme.colorScheme.onSurface
    Text(
        text =
            buildAnnotatedString {
                append(lead)
                append(' ')
                withStyle(SpanStyle(color = highlight)) {
                    append(days)
                }
                append(". ")
                append(tracks)
            },
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.5.em),
        modifier = Modifier.widthIn(max = 280.dp),
    )
}

/**
 * The wall behind the opening: four columns of artwork, tiled until the card is full.
 *
 * Cycles a pool small enough that repeats are guaranteed — that is the intent, not a shortfall. A
 * grid of twelve covers repeating twice reads as wallpaper, which is what a scrim this heavy needs
 * underneath it; twenty-four distinct covers would read as a contact sheet and pull attention off
 * the year.
 */
@Composable
private fun WrappedArtworkMosaic(
    urls: List<String>,
    modifier: Modifier = Modifier,
) {
    if (urls.isEmpty()) return
    BoxWithConstraints(modifier) {
        val tileWidth = maxWidth / MOSAIC_COLUMNS
        val tileHeight = tileWidth * MOSAIC_TILE_ASPECT
        val rows = ceil(maxHeight / tileHeight).toInt()
        Column {
            repeat(rows) { row ->
                Row {
                    repeat(MOSAIC_COLUMNS) { column ->
                        val index = row * MOSAIC_COLUMNS + column
                        WrappedArtwork(
                            url = urls[index % urls.size],
                            modifier = Modifier.width(tileWidth).height(tileHeight),
                        )
                    }
                }
            }
        }
    }
}

/** Tracks first, then albums, then artists — the order the reel itself introduces them in. */
private fun WrappedYear.mosaicArtwork(): List<String> {
    val covers =
        topTracks.map { it.song.thumbnails } +
            topAlbums.map { it.album.thumbnails } +
            topArtists.map { it.artist.thumbnails }
    return covers.mapNotNull { url -> url?.takeIf { it.isNotBlank() } }.distinctByArtwork()
}

/**
 * The card's one figure, and the floor it may shrink to.
 *
 * Sizes, not a style: everything else about this numeral — family, colour, weight — arrives from
 * the theme, and these two numbers are only the range the autosizer works in.
 */
private val YEAR_FIGURE_SIZE = 152.sp

private val YEAR_FIGURE_MIN = 64.sp

/** Clear of the footer the shell draws, without floating so high the block detaches from it. */
private val OPENING_BASELINE = 22.dp

private const val MOSAIC_COLUMNS = 4

/** 97.5 : 140.7 from the artboard — taller than square, so four columns still read as a wall. */
private const val MOSAIC_TILE_ASPECT = 1.443f
