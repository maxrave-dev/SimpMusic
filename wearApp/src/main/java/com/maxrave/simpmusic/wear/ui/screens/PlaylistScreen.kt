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
import androidx.wear.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import com.maxrave.common.LOCAL_PLAYLIST_ID
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.LocalPlaylistRepository
import com.maxrave.domain.utils.LocalResource
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.wear.ui.components.WearList
import com.maxrave.simpmusic.wear.ui.components.WearEmptyState
import com.maxrave.simpmusic.wear.ui.components.WearLoadingState
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

@Composable
fun PlaylistScreen(
    playlistId: Long,
    mediaPlayerHandler: MediaPlayerHandler,
    onBack: () -> Unit,
    openNowPlaying: () -> Unit,
) {
    val repo: LocalPlaylistRepository = remember { GlobalContext.get().get() }
    val scope = rememberCoroutineScope()
    var selectedTrackIndex by remember(playlistId) { mutableStateOf<Int?>(null) }
    val listState = rememberSaveable(playlistId, saver = LazyListState.Saver) { LazyListState() }

    val playlistRes: LocalResource<LocalPlaylistEntity?> by
        repo
            .getLocalPlaylist(playlistId)
            .collectAsStateWithLifecycle(initialValue = LocalResource.Loading())

    val title = playlistRes.data?.title ?: "Playlist"

    val tracks: List<Track> by
        produceState(initialValue = emptyList(), key1 = playlistId, key2 = playlistRes.data?.id) {
            value =
                withContext(Dispatchers.IO) {
                    repo.getFullPlaylistTracks(playlistId).map { it.toTrack() }
                }
        }

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
                    playlistId = LOCAL_PLAYLIST_ID + playlistId,
                    playlistName = title,
                    playlistType = PlaylistType.LOCAL_PLAYLIST,
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

        item { Spacer(Modifier.height(6.dp)) }

        if (playlistRes is LocalResource.Loading) {
            item {
                WearLoadingState("Loading playlist...")
            }
            return@WearList
        }

        if (tracks.isEmpty()) {
            item {
                WearEmptyState("No tracks in this playlist.")
            }
            return@WearList
        }

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
