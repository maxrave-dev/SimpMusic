package com.maxrave.simpmusic.wear.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.wear.ui.components.WearEmptyState
import com.maxrave.simpmusic.wear.ui.components.WearErrorState
import com.maxrave.simpmusic.wear.ui.components.WearList
import com.maxrave.simpmusic.wear.ui.components.WearLoadingState
import com.maxrave.simpmusic.wear.ui.util.friendlyNetworkError
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

private const val LOADING = "__loading__"

@Composable
fun AlbumBrowseScreen(
    browseId: String,
    mediaPlayerHandler: MediaPlayerHandler,
    onBack: () -> Unit,
    openNowPlaying: () -> Unit,
    openAlbum: (String) -> Unit,
    openArtist: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val albumRepository: AlbumRepository = remember { GlobalContext.get().get() }
    val listState = rememberSaveable(browseId, saver = LazyListState.Saver) { LazyListState() }
    var selectedTrack by remember(browseId) { mutableStateOf<Track?>(null) }

    val albumRes by
        remember(browseId) { albumRepository.getAlbumData(browseId) }
            .collectAsStateWithLifecycle(initialValue = Resource.Error(LOADING))

    val albumData = (albumRes as? Resource.Success)?.data
    val trackForDetails = selectedTrack
    if (trackForDetails != null && albumData != null) {
        val trackIndex = albumData.tracks.indexOfFirst { it.videoId == trackForDetails.videoId }.takeIf { it >= 0 }
        SongDetailsScreen(
            track = trackForDetails,
            mediaPlayerHandler = mediaPlayerHandler,
            onBack = { selectedTrack = null },
            onPlayRequested = {
                mediaPlayerHandler.loadMediaItem(
                    anyTrack = trackForDetails,
                    type = Config.ALBUM_CLICK,
                    index = trackIndex,
                )
            },
            onOpenNowPlaying = openNowPlaying,
            onOpenArtist = { artistId -> openArtist(artistId) },
        )
        return
    }

    fun playTracks(
        tracks: List<Track>,
        startIndex: Int,
    ) {
        val firstTrack = tracks.getOrNull(startIndex) ?: return
        scope.launch {
            mediaPlayerHandler.resetSongAndQueue()
            mediaPlayerHandler.setQueueData(
                QueueData.Data(
                    listTracks = tracks,
                    firstPlayedTrack = firstTrack,
                    playlistId = browseId,
                    playlistName = albumData?.title ?: "Album",
                    playlistType = PlaylistType.PLAYLIST,
                    continuation = null,
                ),
            )
            mediaPlayerHandler.loadMediaItem(
                anyTrack = firstTrack,
                type = Config.ALBUM_CLICK,
                index = startIndex,
            )
            openNowPlaying()
        }
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
                    text = albumData?.title ?: "Album",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when (albumRes) {
            is Resource.Error -> {
                val msg = (albumRes as Resource.Error).message
                item {
                    if (msg == LOADING) {
                        WearLoadingState("Loading album...")
                    } else {
                        WearErrorState(message = androidx.compose.ui.platform.LocalContext.current.friendlyNetworkError(msg))
                    }
                }
            }
            is Resource.Success -> {
                val album = albumData
                if (album == null) {
                    item { WearEmptyState("Album data unavailable.") }
                    return@WearList
                }

                val artistName = album.artists.joinToString { it.name }
                val artistId = album.artists.firstOrNull()?.id
                if (artistName.isNotBlank()) {
                    item {
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                if (!artistId.isNullOrBlank()) {
                                    Modifier.clickable { openArtist(artistId) }
                                } else {
                                    Modifier
                                },
                        )
                    }
                }
                item {
                    Text(
                        text = listOf(album.year.orEmpty(), "${album.trackCount} tracks").filter { it.isNotBlank() }.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                item {
                    Button(
                        onClick = { playTracks(album.tracks, 0) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = album.tracks.isNotEmpty(),
                    ) {
                        Text("Play album")
                    }
                }

                item {
                    FilledTonalButton(
                        onClick = {
                            val shuffled = album.tracks.shuffled()
                            playTracks(shuffled, 0)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = album.tracks.isNotEmpty(),
                    ) {
                        Text("Shuffle")
                    }
                }

                if (!album.description.isNullOrBlank()) {
                    item {
                        Text(
                            text = album.description.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (album.tracks.isEmpty()) {
                    item { WearEmptyState("No tracks found.") }
                } else {
                    item {
                        Text(
                            text = "Tracks",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(album.tracks.size) { index ->
                        val track = album.tracks[index]
                        TrackItemRow(
                            title = track.title,
                            subtitle = track.artists?.joinToString { it.name }.orEmpty(),
                            onClick = { selectedTrack = track },
                        )
                    }
                }

                if (album.otherVersion.isNotEmpty()) {
                    item {
                        Spacer(Modifier.padding(top = 4.dp))
                        Text(
                            text = "Other versions",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(album.otherVersion.size) { index ->
                        val item = album.otherVersion[index]
                        TrackItemRow(
                            title = item.title,
                            subtitle = item.year,
                            onClick = { openAlbum(item.browseId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackItemRow(
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
