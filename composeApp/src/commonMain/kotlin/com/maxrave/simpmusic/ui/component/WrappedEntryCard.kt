package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.screen.home.wrapped.formatCount
import com.maxrave.simpmusic.ui.screen.home.wrapped.wholeMinutes
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.WrappedYear
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.wrapped_entry_subtitle
import simpmusic.composeapp.generated.resources.wrapped_entry_title

/**
 * The one entry point into the Wrapped reel, shown on the Analytics screen and at the top of the
 * Library Wrapped tab.
 *
 * **One card, not two.** It shipped once as two separate cards saying two different things in two
 * places; there is exactly one now, and both hosts call it.
 *
 * It is a row, at row height: one cover, two lines, and a play disc. An earlier version led with a
 * five-cover mosaic and the year set as a display figure, which made the card taller than half a
 * phone screen — a banner competing with the page it sits on rather than an invitation into it.
 *
 * Built from the app's own vocabulary rather than a shape of its own: the shell is [ElevatedCard]
 * with `CardDefaults`, exactly as [LibraryTilingItem] draws a tappable card in Library; the cover
 * is a plain `AsyncImage` clipped the way every list row in the app clips one; colours are
 * `MaterialTheme.colorScheme` roles and type comes off [typo].
 *
 * The disc carries a play triangle, not a chevron: a chevron promises a page of details, and this
 * opens something that runs.
 */
@Composable
fun WrappedEntryCard(
    wrapped: WrappedYear,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(CARD_RADIUS),
        elevation = CardDefaults.elevatedCardElevation(),
        // `elevatedCardColors`' own container is `surfaceContainerLow`, which on the Analytics page
        // sits almost on top of the artwork-tinted background it is drawn over. One step up keeps
        // the card reading as an object there, and still as a card on the flat Library page.
        colors = CardDefaults.elevatedCardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(GUTTER),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = wrapped.entryCover(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(COVER_SIZE).clip(RoundedCornerShape(COVER_RADIUS)),
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.wrapped_entry_title, wrapped.year.toString()),
                    style = typo().titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        stringResource(
                            Res.string.wrapped_entry_subtitle,
                            formatCount(wholeMinutes(wrapped.stats.listenedSeconds)),
                            formatCount(wrapped.stats.distinctArtists),
                        ),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(DISC_SIZE),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = SimpIcons.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(DISC_GLYPH_SIZE),
                    )
                }
            }
        }
    }
}

/**
 * One cover, and the most-played one the year has.
 *
 * Falls through tracks → albums → artists because a year can be short on any one of them, and a
 * card with a blank square reads as broken rather than as sparse. `null` is still a possible
 * answer — [AsyncImage] then simply leaves the card's own container showing, which at this size is
 * a rounded blank the layout already accounts for.
 */
private fun WrappedYear.entryCover(): String? {
    val cover =
        topTracks.firstOrNull()?.song?.thumbnails
            ?: topAlbums.firstOrNull()?.album?.thumbnails
            ?: topArtists.firstOrNull()?.artist?.thumbnails
    return cover?.takeIf { it.isNotBlank() }
}

private val GUTTER = 14.dp

/** Row height, not banner height — the whole card lands near 96dp with the gutter. */
private val COVER_SIZE = 68.dp

private val COVER_RADIUS = 8.dp

private val CARD_RADIUS = 12.dp

private val DISC_SIZE = 44.dp

private val DISC_GLYPH_SIZE = 22.dp
