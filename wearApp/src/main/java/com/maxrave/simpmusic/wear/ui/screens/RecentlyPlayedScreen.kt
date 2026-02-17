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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.wear.ui.components.WearEmptyState
import com.maxrave.simpmusic.wear.ui.components.WearList
import com.maxrave.simpmusic.wear.ui.components.WearLoadingState
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

private const val RECENT_PAGE_SIZE = 30

@Composable
fun RecentlyPlayedScreen(
    mediaPlayerHandler: MediaPlayerHandler,
    onBack: () -> Unit,
    openNowPlaying: () -> Unit,
    openArtist: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val songRepository: SongRepository = remember { GlobalContext.get().get() }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var offset by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    suspend fun loadMore(reset: Boolean) {
        if (loading) return
        loading = true
        val currentOffset = if (reset) 0 else offset
        val chunk =
            runCatching {
                songRepository.getRecentSong(RECENT_PAGE_SIZE, currentOffset)
            }.getOrElse { emptyList() }
        val mapped = chunk.map { it.toTrack() }
        tracks = if (reset) mapped else tracks + mapped
        offset = if (reset) mapped.size else offset + mapped.size
        hasMore = mapped.size >= RECENT_PAGE_SIZE
        loading = false
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        loadMore(reset = true)
    }

    fun playTracks(
        list: List<Track>,
        index: Int,
    ) {
        val firstTrack = list.getOrNull(index) ?: return
        scope.launch {
            mediaPlayerHandler.resetSongAndQueue()
            mediaPlayerHandler.setQueueData(
                QueueData.Data(
                    listTracks = list,
                    firstPlayedTrack = firstTrack,
                    playlistId = "wear_recent_tracks",
                    playlistName = "Recent plays",
                    playlistType = PlaylistType.PLAYLIST,
                    continuation = null,
                ),
            )
            mediaPlayerHandler.loadMediaItem(
                anyTrack = firstTrack,
                type = Config.PLAYLIST_CLICK,
                index = index,
            )
            openNowPlaying()
        }
    }

    val detailTrack = selectedTrack
    if (detailTrack != null) {
        val index = tracks.indexOfFirst { it.videoId == detailTrack.videoId }.takeIf { it >= 0 } ?: 0
        SongDetailsScreen(
            track = detailTrack,
            mediaPlayerHandler = mediaPlayerHandler,
            onBack = { selectedTrack = null },
            onPlayRequested = { playTracks(tracks, index) },
            onOpenNowPlaying = openNowPlaying,
            onOpenArtist = openArtist,
        )
        return
    }

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
                    text = "Recent plays",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (tracks.isEmpty()) {
            item {
                if (loading) {
                    WearLoadingState("Loading recent songs...")
                } else {
                    WearEmptyState("No recent plays yet.")
                }
            }
            return@WearList
        }

        item {
            FilledTonalButton(
                onClick = {
                    val shuffled = tracks.shuffled()
                    playTracks(shuffled, 0)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Shuffle recent")
            }
        }

        items(tracks.size) { index ->
            val track = tracks[index]
            TrackRow(
                title = track.title,
                subtitle = track.artists?.joinToString { it.name }.orEmpty(),
                onClick = { selectedTrack = track },
            )
        }

        if (hasMore) {
            item {
                Button(
                    onClick = { scope.launch { loadMore(reset = false) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                ) {
                    Text(if (loading) "Loading..." else "Load more")
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
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
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
