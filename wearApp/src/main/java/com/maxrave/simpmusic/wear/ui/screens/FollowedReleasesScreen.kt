package com.maxrave.simpmusic.wear.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.maxrave.domain.data.entities.FollowedArtistSingleAndAlbum
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.simpmusic.wear.ui.components.WearEmptyState
import com.maxrave.simpmusic.wear.ui.components.WearList
import com.maxrave.simpmusic.wear.ui.components.WearLoadingState
import org.koin.core.context.GlobalContext

@Composable
fun FollowedReleasesScreen(
    onBack: () -> Unit,
    openAlbum: (String) -> Unit,
    openArtist: (String) -> Unit,
) {
    val albumRepository: AlbumRepository = remember { GlobalContext.get().get() }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var followed by remember { mutableStateOf<List<FollowedArtistSingleAndAlbum>?>(null) }

    LaunchedEffect(albumRepository) {
        albumRepository.getAllFollowedArtistSingleAndAlbums().collect { followed = it.orEmpty() }
    }

    val artists = followed

    WearList(state = listState) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Followed releases",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when {
            artists == null -> {
                item { WearLoadingState("Loading followed releases...") }
            }
            artists.isEmpty() -> {
                item { WearEmptyState("No followed release data yet.") }
            }
            else -> {
                artists.forEach { artist ->
                    val albums = artist.album.toReleaseItems("Album")
                    val singles = artist.single.toReleaseItems("Single")

                    item {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { openArtist(artist.channelId) }
                                    .padding(vertical = 4.dp),
                        )
                    }

                    albums.forEach { release ->
                        item {
                            ReleaseRow(
                                title = release.title,
                                subtitle = release.typeLabel,
                                onClick = { openAlbum(release.browseId) },
                            )
                        }
                    }

                    singles.forEach { release ->
                        item {
                            ReleaseRow(
                                title = release.title,
                                subtitle = release.typeLabel,
                                onClick = { openAlbum(release.browseId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ReleaseItem(
    val browseId: String,
    val title: String,
    val typeLabel: String,
)

private fun List<Map<String, String>>.toReleaseItems(typeLabel: String): List<ReleaseItem> =
    mapNotNull { raw ->
        val browseId = raw["browseId"].orEmpty()
        val title = raw["title"].orEmpty()
        if (browseId.isBlank() || title.isBlank()) {
            null
        } else {
            ReleaseItem(
                browseId = browseId,
                title = title,
                typeLabel = typeLabel,
            )
        }
    }

@Composable
private fun ReleaseRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
