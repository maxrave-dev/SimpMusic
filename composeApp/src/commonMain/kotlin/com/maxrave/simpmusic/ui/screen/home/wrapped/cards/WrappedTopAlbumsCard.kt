package com.maxrave.simpmusic.ui.screen.home.wrapped.cards

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.maxrave.domain.utils.connectArtists
import com.maxrave.simpmusic.ui.screen.home.wrapped.WrappedTokens
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.viewModel.WrappedAlbum
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped_albums_title
import simpmusic.composeapp.generated.resources.wrapped_plays

/**
 * Card 05 — the records, as one block of sleeve art.
 *
 * Albums are the one thing in the reel whose artwork *is* the object, so this card stops cropping
 * and stops listing: the top five are laid out flush as a single mosaic, #1 across the top at twice
 * the area of the rest, and only #1 is named. Tiles touch on purpose — gaps would turn one object
 * into five cards and throw away the ranking the sizes already carry.
 *
 * Fewer than five albums is normal and every count has its own hole-free shape, because a mosaic
 * with an empty rectangle in it reads as a failure rather than as a smaller collection. #1 always
 * takes the banner; whatever is left fills rows of two, and a row holding one tile spans the width.
 *
 * It is also the one card in the reel that names no size at all: the sleeves carry the hierarchy,
 * so the only type on it is a caption and a subtitle, and both come off the scale unchanged.
 */
@Composable
fun WrappedTopAlbumsCard(
    wrapped: WrappedYear,
    modifier: Modifier = Modifier,
) {
    val leader = wrapped.topAlbums.firstOrNull() ?: return
    val runnersUp = wrapped.topAlbums.drop(1)
    val ground = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxSize().background(ground)) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(SPACE_ABOVE_MOSAIC))
            Column(Modifier.weight(MOSAIC_BANNER_WEIGHT + runnersUp.mosaicRowWeight())) {
                LeaderBanner(leader, Modifier.weight(MOSAIC_BANNER_WEIGHT))
                runnersUp.chunked(MOSAIC_ROW_TILES).forEach { row ->
                    Row(Modifier.fillMaxWidth().weight(MOSAIC_ROW_WEIGHT)) {
                        row.forEach { album ->
                            WrappedArtwork(
                                url = album.album.thumbnails,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(SPACE_BELOW_MOSAIC))
        }
        // Kills the mosaic's bottom edge before it meets the shell's footer. A fixed height rather
        // than a fraction: this fade exists to clear a line of text, and text does not get taller
        // on a taller window.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.62f to ground,
                        1.00f to ground,
                    ),
                ),
        )
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(14.dp))
            WrappedEyebrow(
                text = stringResource(Res.string.wrapped_albums_title),
                modifier = Modifier.padding(horizontal = WrappedTokens.ScreenPadding),
            )
        }
    }
}

/**
 * #1's sleeve, with its name written into the lower third of it.
 *
 * The title sits inside the banner rather than under the mosaic so it is unmistakably a caption for
 * *this* sleeve — the four below it are deliberately anonymous. It stops short of the banner's edge
 * by [TITLE_INSET] so the type has sleeve under it rather than a seam.
 *
 * `titleMedium` at Bold is the artboard's 19px/700 to within a rounding, so the caption needs a
 * weight and nothing else — no size, no colour. The sleeve's own scrim is built from the card's
 * ground, which is what lets a bright cover and a nearly black one both hand the caption the same
 * contrast.
 */
@Composable
private fun LeaderBanner(
    album: WrappedAlbum,
    modifier: Modifier,
) {
    val ground = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxWidth()) {
        WrappedArtwork(
            url = album.album.thumbnails,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.26f to Color.Transparent,
                    0.58f to ground.copy(alpha = 0.58f),
                    1.00f to ground.copy(alpha = 0.94f),
                ),
            ),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = WrappedTokens.ScreenPadding)
                    .padding(bottom = TITLE_INSET),
        ) {
            Text(
                text = album.album.title,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 1.22.em,
                    ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = albumSubtitle(album),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "HIEUTHUHAI · 186 plays", or the plays alone where the album carries no artist. */
@Composable
private fun albumSubtitle(album: WrappedAlbum): String {
    val plays = stringResource(Res.string.wrapped_plays, formatCount(album.playCount))
    val artists = album.album.artistName?.connectArtists()?.takeIf { it.isNotBlank() }
    return if (artists == null) plays else "$artists · $plays"
}

/**
 * What the runner-up rows are worth against the banner.
 *
 * Zero when there are none, which hands the banner the whole mosaic — one album that filled the
 * year deserves to fill the card, and it is the arrangement a single-album year should get anyway.
 */
private fun List<WrappedAlbum>.mosaicRowWeight(): Float = chunked(MOSAIC_ROW_TILES).size * MOSAIC_ROW_WEIGHT

private const val MOSAIC_ROW_TILES = 2

/** 390 x 250 against 195 x 195 in the artboard — held as weights so the block flexes with the card. */
private const val MOSAIC_BANNER_WEIGHT = 250f
private const val MOSAIC_ROW_WEIGHT = 195f

private const val SPACE_ABOVE_MOSAIC = 46f
private const val SPACE_BELOW_MOSAIC = 12f

/** Sleeve left under the caption, so the type is on the record rather than on its edge. */
private val TITLE_INSET = 31.dp
