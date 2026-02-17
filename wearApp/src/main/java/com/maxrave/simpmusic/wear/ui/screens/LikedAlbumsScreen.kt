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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.simpmusic.wear.ui.components.WearEmptyState
import com.maxrave.simpmusic.wear.ui.components.WearList
import org.koin.core.context.GlobalContext

@Composable
fun LikedAlbumsScreen(
    onBack: () -> Unit,
    openAlbum: (String) -> Unit,
) {
    val albumRepository: AlbumRepository = remember { GlobalContext.get().get() }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val albums by albumRepository.getLikedAlbums().collectAsStateWithLifecycle(initialValue = emptyList())

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
                    text = "Liked albums",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (albums.isEmpty()) {
            item { WearEmptyState("No liked albums yet.") }
            return@WearList
        }

        items(albums.size) { index ->
            val album = albums[index]
            AlbumRow(
                album = album,
                onClick = { openAlbum(album.browseId) },
            )
        }
    }
}

@Composable
private fun AlbumRow(
    album: AlbumEntity,
    onClick: () -> Unit,
) {
    val subtitleParts = mutableListOf<String>()
    album.artistName?.takeIf { it.isNotEmpty() }?.let { names ->
        subtitleParts.add(names.joinToString())
    }
    album.year?.let { year ->
        if (year.isNotBlank()) subtitleParts.add(year)
    }
    subtitleParts.add("${album.trackCount} tracks")
    val subtitle = subtitleParts.joinToString(" • ")

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
    ) {
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
