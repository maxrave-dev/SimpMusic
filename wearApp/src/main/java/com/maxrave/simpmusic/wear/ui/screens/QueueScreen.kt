package com.maxrave.simpmusic.wear.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.simpmusic.wear.ui.components.WearList
import com.maxrave.simpmusic.wear.ui.components.WearEmptyState
import com.maxrave.simpmusic.wear.ui.components.WearLoadingState

@Composable
fun QueueScreen(
    mediaPlayerHandler: MediaPlayerHandler,
    onBack: () -> Unit,
) {
    val queueData = mediaPlayerHandler.queueData.collectAsStateWithLifecycle().value ?: QueueData()
    val currentIndex = mediaPlayerHandler.currentSongIndex.collectAsStateWithLifecycle().value
    val tracks = queueData.data.listTracks
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

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
                    text = "Queue",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { Spacer(Modifier.height(6.dp)) }

        if (queueData.queueState == QueueData.StateSource.STATE_INITIALIZING) {
            item { WearLoadingState("Loading queue...") }
            return@WearList
        }

        if (tracks.isEmpty()) {
            item {
                WearEmptyState(
                    title = "Queue is empty.",
                    hint = "Add a song from Discover or Library.",
                )
            }
            return@WearList
        }

        items(tracks.size) { index ->
            val track = tracks[index]
            QueueRow(
                track = track,
                isCurrent = index == currentIndex,
                onClick = { mediaPlayerHandler.playMediaItemInMediaSource(index) },
                onRemove = { mediaPlayerHandler.removeMediaItem(index) },
            )
        }
    }
}

@Composable
private fun QueueRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val artist = track.artists?.joinToString { it.name }.orEmpty()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(vertical = 4.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Remove from queue",
            )
        }
    }
}
