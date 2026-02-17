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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.common.Config
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.wear.ui.components.WearList
import com.maxrave.simpmusic.wear.ui.components.WearEmptyState
import com.maxrave.simpmusic.wear.ui.components.WearErrorState
import com.maxrave.simpmusic.wear.ui.components.WearLoadingState
import com.maxrave.simpmusic.wear.ui.util.friendlyNetworkError
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

private const val LOADING = "__loading__"

@Composable
fun YtPlaylistScreen(
    playlistId: String,
    mediaPlayerHandler: MediaPlayerHandler,
    onBack: () -> Unit,
    openNowPlaying: () -> Unit,
) {
    val repo: PlaylistRepository = remember { GlobalContext.get().get() }
    val scope = rememberCoroutineScope()
    var refreshNonce by remember(playlistId) { mutableIntStateOf(0) }
    var selectedTrackIndex by remember(playlistId) { mutableStateOf<Int?>(null) }
    val listState = rememberSaveable(playlistId, saver = LazyListState.Saver) { LazyListState() }

    val playlistRes by
        remember(playlistId, refreshNonce) { repo.getFullPlaylistData(playlistId) }
            .collectAsStateWithLifecycle(initialValue = Resource.Error(LOADING))

    val playlist = (playlistRes as? Resource.Success)?.data
    val title = playlist?.title ?: "Playlist"
    val tracks = playlist?.tracks.orEmpty()

    fun play(
        index: Int,
        openPlayer: Boolean = false,
    ) {
        val track = tracks.getOrNull(index) ?: return
        scope.launch {
            mediaPlayerHandler.resetSongAndQueue()
            mediaPlayerHandler.setQueueData(
                QueueData.Data(
                    listTracks = tracks,
                    firstPlayedTrack = tracks.firstOrNull(),
                    playlistId = playlist?.id ?: playlistId,
                    playlistName = title,
                    playlistType = PlaylistType.PLAYLIST,
                    continuation = null,
                ),
            )
            mediaPlayerHandler.loadMediaItem(
                anyTrack = track,
                type = Config.PLAYLIST_CLICK,
                index = index,
            )
            if (openPlayer) openNowPlaying()
        }
    }

    val selectedTrack = selectedTrackIndex?.let { tracks.getOrNull(it) }
    if (selectedTrack != null) {
        SongDetailsScreen(
            track = selectedTrack,
            mediaPlayerHandler = mediaPlayerHandler,
            onBack = { selectedTrackIndex = null },
            onPlayRequested = { selectedTrackIndex?.let { play(it) } },
            onOpenNowPlaying = openNowPlaying,
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
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { play(0, openPlayer = true) },
                    enabled = tracks.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                }
            }
        }

        when (playlistRes) {
            is Resource.Success -> {
                if (tracks.isEmpty()) {
                    item {
                        Spacer(Modifier.height(10.dp))
                        WearEmptyState("No tracks in this playlist.")
                    }
                } else {
                    items(tracks.size) { index ->
                        val track = tracks[index]
                        val artist = track.artists?.joinToString { it.name }.orEmpty()
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTrackIndex = index }
                                    .padding(vertical = 8.dp),
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal,
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
                    }
                }
            }
            is Resource.Error -> {
                val msg = (playlistRes as Resource.Error).message
                item {
                    Spacer(Modifier.height(10.dp))
                    if (msg == LOADING) {
                        WearLoadingState("Loading playlist...")
                    } else {
                        WearErrorState(
                            message =
                                // The data layer often passes raw exception messages; make them actionable on Wear.
                                androidx.compose.ui.platform.LocalContext.current.friendlyNetworkError(msg),
                            actionLabel = "Retry",
                            onAction = { refreshNonce++ },
                        )
                    }
                }
            }
        }
    }
}
