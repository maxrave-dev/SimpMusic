package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.library.LibraryCollectionPreview
import com.maxrave.domain.data.model.library.LibraryOverview
import com.maxrave.simpmusic.ui.component.taste.libraryCardColor
import com.maxrave.simpmusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.maxrave.simpmusic.ui.screen.library.LibraryDynamicPlaylistType
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.favorite
import simpmusic.composeapp.generated.resources.followed
import simpmusic.composeapp.generated.resources.library_artist_count
import simpmusic.composeapp.generated.resources.library_favorite_empty_hint
import simpmusic.composeapp.generated.resources.liked_songs_count
import simpmusic.composeapp.generated.resources.most_played

/**
 * The four collections at the top of the Your library tab: Favorite as the wide card, then
 * Followed, Most played and Downloaded. Each card fans out the artwork of its newest items; with
 * nothing in it yet the fan is a set of empty dashed sleeves, so a new user sees the same shapes
 * waiting to be filled rather than a blank card. [overview] is null until the first read lands,
 * and the cards then draw their titles alone.
 */
@Composable
fun LibraryTilingBox(
    navController: NavController,
    overview: LibraryOverview?,
) {
    val container = libraryCardColor()
    val open = { type: LibraryDynamicPlaylistType ->
        navController.navigate(LibraryDynamicPlaylistDestination(type = type.toStringParams()))
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FavoriteCard(overview?.favorite, container) { open(LibraryDynamicPlaylistType.Favorite) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CollectionCard(
                title = Res.string.followed,
                preview = overview?.followed,
                countText = overview?.followed?.count?.let { pluralStringResource(Res.plurals.library_artist_count, it, it) },
                round = true,
                container = container,
                modifier = Modifier.weight(1f),
            ) { open(LibraryDynamicPlaylistType.Followed) }
            CollectionCard(
                title = Res.string.most_played,
                preview = overview?.mostPlayed,
                countText = overview?.mostPlayed?.count?.let { pluralStringResource(Res.plurals.liked_songs_count, it, it) },
                round = false,
                container = container,
                modifier = Modifier.weight(1f),
            ) { open(LibraryDynamicPlaylistType.MostPlayed) }
            CollectionCard(
                title = Res.string.downloaded,
                preview = overview?.downloaded,
                countText = overview?.downloaded?.count?.let { pluralStringResource(Res.plurals.liked_songs_count, it, it) },
                round = false,
                container = container,
                modifier = Modifier.weight(1f),
            ) { open(LibraryDynamicPlaylistType.Downloaded) }
        }
    }
}

@Composable
private fun FavoriteCard(
    preview: LibraryCollectionPreview?,
    container: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(132.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp).height(132.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 14.dp)) {
                Text(
                    text = stringResource(Res.string.favorite),
                    style = typo().titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (preview != null) {
                    Text(
                        text =
                            if (preview.count == 0) {
                                stringResource(Res.string.library_favorite_empty_hint)
                            } else {
                                pluralStringResource(Res.plurals.liked_songs_count, preview.count, preview.count)
                            },
                        style = typo().bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            SleeveFan(
                thumbnails = preview?.thumbnails,
                slots = 3,
                sleeve = 74.dp,
                spread = 40.dp,
                tilt = 11f,
                round = false,
                ring = container,
                modifier = Modifier.size(width = 150.dp, height = 110.dp),
            )
        }
    }
}

@Composable
private fun CollectionCard(
    title: StringResource,
    preview: LibraryCollectionPreview?,
    countText: String?,
    round: Boolean,
    container: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(118.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Box(Modifier.fillMaxWidth().height(118.dp)) {
            SleeveFan(
                thumbnails = preview?.thumbnails,
                slots = 2,
                sleeve = 40.dp,
                spread = 20.dp,
                tilt = 9f,
                round = round,
                ring = container,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 10.dp)
                        .size(width = 62.dp, height = 46.dp),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(
                    text = stringResource(title),
                    style = typo().labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (countText != null) {
                    Text(
                        text = countText,
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Up to [slots] sleeves fanned out from the centre, each [tilt] degrees and [spread] further out
 * than the last, the newest artwork on top. No artwork at all draws every slot as an empty dashed
 * sleeve; null (not loaded yet) draws nothing, so a library that has items never flashes as empty.
 */
@Composable
private fun SleeveFan(
    thumbnails: List<String>?,
    slots: Int,
    sleeve: Dp,
    spread: Dp,
    tilt: Float,
    round: Boolean,
    ring: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (thumbnails == null) return@Box
        val shape = if (round) CircleShape else RoundedCornerShape(8.dp)
        val covers = thumbnails.take(slots)
        val count = if (covers.isEmpty()) slots else covers.size
        for (i in 0 until count) {
            val step = i - (count - 1) / 2f
            val placed =
                Modifier.size(sleeve).graphicsLayer {
                    translationX = (spread * step).toPx()
                    rotationZ = tilt * step
                }
            if (covers.isEmpty()) {
                EmptySleeve(shape, placed)
            } else {
                // Oldest first, so the newest is drawn last and lands on top.
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(covers[count - 1 - i])
                            .crossfade(true)
                            .build(),
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = placed.clip(shape).border(2.dp, ring, shape),
                )
            }
        }
    }
}

@Composable
private fun EmptySleeve(
    shape: Shape,
    modifier: Modifier,
) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val fill = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
    Box(
        modifier.drawBehind {
            val sleeveOutline = shape.createOutline(size, layoutDirection, this)
            drawOutline(sleeveOutline, fill)
            drawOutline(
                outline = sleeveOutline,
                color = outline,
                style =
                    Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.5.dp.toPx())),
                    ),
            )
        },
    )
}

private val CardShape = RoundedCornerShape(20.dp)
