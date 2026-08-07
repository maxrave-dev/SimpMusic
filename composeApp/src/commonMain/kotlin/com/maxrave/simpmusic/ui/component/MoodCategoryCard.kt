package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.monochrome

/**
 * A "Moods & Genres" browse category tile: the [playlistTitleGradient] and SimpMusic badge of an
 * artwork-less playlist tile, plus the tilted cover Spotify puts on its browse cards.
 *
 * [artworkUrl] is null until resolved — the category list carries no artwork at all, so the cover
 * costs a separate browse per category and arrives late. The tile is designed to look finished
 * without it.
 */
@Composable
fun MoodCategoryCard(
    title: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val badge = painterResource(Res.drawable.monochrome)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .clip(RoundedCornerShape(10.dp))
                .angledGradientBackground(
                    colors = playlistTitleGradient(title),
                    degrees = 45f,
                ).drawBehind {
                    // PlaylistThumbnailPainter sizes the badge off the width because its tile is
                    // square. This one is 2:1, so the same fractions would double it — anchor on
                    // the height instead to keep the badge the size the eye expects.
                    val radius = size.height * 0.09f
                    val centerX = size.width - radius * 2f
                    val centerY = radius * 2f
                    drawCircle(
                        center = Offset(centerX, centerY),
                        color = Color.White,
                        radius = radius,
                    )
                    val badgeSize = size.copy(width = radius * 3f, height = radius * 3f)
                    translate(
                        left = centerX - badgeSize.width / 2f,
                        top = centerY - badgeSize.height / 2f,
                    ) {
                        with(badge) {
                            draw(badgeSize, alpha = 0.2f)
                        }
                    }
                }.clickable(onClick = onClick),
    ) {
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        // Offset past the corner first, THEN rotate: the tile clips its children,
                        // so letting the cover run off the edge is what produces the cut-off
                        // diagonal instead of a square pasted inside the card.
                        .offset(x = 8.dp, y = 12.dp)
                        .size(64.dp)
                        .rotate(25f)
                        .clip(RoundedCornerShape(2.dp)),
            )
        }
        Text(
            text = title,
            style = typo().titleSmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    // Safe width ends at the LEFT edge of the tilted cover, not just clear of the
                    // badge. The cover is 64.dp rotated 25°, so its bounding box grows to
                    // 64*(cos25+sin25) ≈ 85.dp — about 10.dp wider on each side — and it is offset
                    // 8.dp past the right edge. That leaves ~66.dp of it inside the tile.
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 72.dp),
        )
    }
}
