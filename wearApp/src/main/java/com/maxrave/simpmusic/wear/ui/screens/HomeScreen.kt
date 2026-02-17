package com.maxrave.simpmusic.wear.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.simpmusic.wear.ui.components.PlaybackControlIcon
import com.maxrave.simpmusic.wear.ui.components.WearList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    mediaPlayerHandler: MediaPlayerHandler,
    openNowPlaying: () -> Unit,
    openQueue: () -> Unit,
    openLibrary: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val nowPlayingState = mediaPlayerHandler.nowPlayingState.collectAsStateWithLifecycle().value
    val controlState = mediaPlayerHandler.controlState.collectAsStateWithLifecycle().value
    val isLoading by
        remember(mediaPlayerHandler) {
            mediaPlayerHandler.simpleMediaState
                .map { it is SimpleMediaState.Loading || it is SimpleMediaState.Buffering }
                .distinctUntilChanged()
        }.collectAsStateWithLifecycle(initialValue = false)
    val queueData = mediaPlayerHandler.queueData.collectAsStateWithLifecycle().value ?: QueueData()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val title =
        nowPlayingState.songEntity?.title
            ?: nowPlayingState.track?.title
            ?: nowPlayingState.mediaItem.metadata.title
            ?: "Nothing playing"
    val artistFromSong = nowPlayingState.songEntity?.artistName?.joinToString()
    val artist =
        artistFromSong
            ?: nowPlayingState.track?.artists?.joinToString { it.name }
            ?: nowPlayingState.mediaItem.metadata.artist
            ?: ""

    WearList(state = listState) {
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (artist.isNotBlank()) {
            item {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { Spacer(Modifier.height(10.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(
                    onClick = { scope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.Previous) } },
                    enabled = controlState.isPreviousAvailable,
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(
                    onClick = { scope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause) } },
                ) {
                    PlaybackControlIcon(isPlaying = controlState.isPlaying, isLoading = isLoading && !controlState.isPlaying)
                }
                IconButton(
                    onClick = { scope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next) } },
                    enabled = controlState.isNextAvailable,
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }

        item {
            FilledTonalButton(
                onClick = openNowPlaying,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Now playing") }
        }
        item { Spacer(Modifier.height(6.dp)) }
        item {
            FilledTonalButton(
                onClick = openLibrary,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Library") }
        }
        item { Spacer(Modifier.height(6.dp)) }
        item {
            Button(
                onClick = openQueue,
                modifier = Modifier.fillMaxWidth(),
                enabled = queueData.data.listTracks.isNotEmpty(),
            ) { Text("Queue (${queueData.data.listTracks.size})") }
        }
    }
}
