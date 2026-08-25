package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.ui.theme.typo

/**
 * The top five, as one mosaic rather than a row of equal cards.
 *
 * A shelf gives every entry the same size, which throws away the one thing a "top five" is about.
 * Here the first entry gets a tile twice the area of the rest, so the ranking is legible before a
 * single number is read. Tiles are flush by design — the block reads as one object, not as five.
 *
 * Two arrangements, both 5 tiles and both exactly half as tall as they are wide:
 * - **portrait**: a 2:1 banner across the top, then two rows of two squares.
 * - **landscape**: the banner would be 616dp tall on a 1280dp window, so #1 becomes a square
 *   taking the left half and the other four sit beside it as a 2x2.
 */
@Composable
fun FiveImagesComponent(
    modifier: Modifier,
    images: List<ImageData>,
    landscape: Boolean = false,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    if (images.isEmpty()) return
    // Rounded on the OUTSIDE of the block only. The tiles stay flush with each other on purpose —
    // rounding them individually would break the mosaic back into five separate cards, which is
    // the arrangement this component exists to avoid.
    val clipped = modifier.clip(shape)
    if (landscape) LandscapeMosaic(clipped, images) else PortraitMosaic(clipped, images)
}

@Composable
private fun PortraitMosaic(
    modifier: Modifier,
    images: List<ImageData>,
) {
    Column(modifier) {
        MosaicTile(
            image = images.first(),
            modifier = Modifier.fillMaxWidth().aspectRatio(2f),
        )
        images.drop(1).chunked(2).forEach { row ->
            if (row.size == 2) {
                Row(Modifier.fillMaxWidth()) { row.forEach { SquareTile(it) } }
            } else {
                // A lone trailing tile spans the full width instead of sitting beside a gap. The
                // old code returned early on an odd count, which silently DROPPED that entry — a
                // four-artist week only ever showed three.
                MosaicTile(row.first(), Modifier.fillMaxWidth().aspectRatio(2f))
            }
        }
    }
}

/**
 * The landscape arrangement has to stay filled at every count, unlike portrait.
 *
 * Its right-hand column stands beside a tile as tall as half the block's width, so a row the column
 * does not have is an empty rectangle in plain sight. In portrait the same missing row only makes
 * the block shorter, which is why the original early-returns were never noticed.
 *
 * Every case below keeps the column exactly as tall as that first tile: two 2:1 stripes, or a 2:1
 * stripe over a pair of squares, or two pairs of squares.
 */
@Composable
private fun LandscapeMosaic(
    modifier: Modifier,
    images: List<ImageData>,
) {
    if (images.size == 1) {
        MosaicTile(images.first(), modifier.fillMaxWidth().aspectRatio(2f))
        return
    }
    Row(modifier) {
        // Half the width, square — so it ends up exactly as tall as whatever fills the column
        // beside it, whose tiles are each a quarter of the width.
        MosaicTile(
            image = images.first(),
            modifier = Modifier.weight(1f).aspectRatio(1f),
        )
        val rest = images.drop(1)
        if (rest.size == 1) {
            MosaicTile(rest.first(), Modifier.weight(1f).aspectRatio(1f))
            return@Row
        }
        Column(Modifier.weight(1f)) {
            when (rest.size) {
                2 -> rest.forEach { MosaicTile(it, Modifier.fillMaxWidth().aspectRatio(2f)) }
                // Rank 2 gets the wide stripe, so the ordering still reads top-down.
                3 -> {
                    MosaicTile(rest[0], Modifier.fillMaxWidth().aspectRatio(2f))
                    Row(Modifier.fillMaxWidth()) { rest.subList(1, 3).forEach { SquareTile(it) } }
                }
                else -> {
                    Row(Modifier.fillMaxWidth()) { rest.subList(0, 2).forEach { SquareTile(it) } }
                    Row(Modifier.fillMaxWidth()) { rest.subList(2, 4).forEach { SquareTile(it) } }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SquareTile(image: ImageData) {
    MosaicTile(
        image = image,
        modifier = Modifier.weight(1f).aspectRatio(1f),
    )
}

/**
 * One tile: artwork cropped to fill, a scrim, and the labels stacked in the bottom-left corner.
 */
@Composable
private fun MosaicTile(
    image: ImageData,
    modifier: Modifier,
) {
    Box(
        modifier.clickable { image.onClick() },
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(image.imageUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(image.imageUrl)
                    .crossfade(550)
                    .build(),
            contentDescription = "",
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(artworkScrimBrush(Color.Black)),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Styles kept exactly as they were: title labelSmall in white, subtitle bodySmall
            // in white, and the optional third line bodySmall in whatever the theme gives it.
            MarqueeLine(image.title, typo().labelSmall, Color.White)
            MarqueeLine(image.subtitle, typo().bodySmall, Color.White)
            image.thirdTitle?.let { MarqueeLine(it, typo().bodySmall, null) }
        }
    }
}

@Composable
private fun MarqueeLine(
    text: String,
    style: TextStyle,
    color: Color?,
) {
    Text(
        text = text,
        style = style,
        color = color ?: Color.Unspecified,
        maxLines = 1,
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.CenterVertically)
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    animationMode = MarqueeAnimationMode.Immediately,
                ).focusable(),
    )
}

data class ImageData(
    val imageUrl: String,
    val title: String,
    val subtitle: String,
    val thirdTitle: String? = null,
    val onClick: () -> Unit,
)
