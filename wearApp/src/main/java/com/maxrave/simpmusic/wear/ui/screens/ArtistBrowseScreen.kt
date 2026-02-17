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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.maxrave.domain.repository.ArtistRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.wear.ui.components.WearEmptyState
import com.maxrave.simpmusic.wear.ui.components.WearErrorState
import com.maxrave.simpmusic.wear.ui.components.WearList
import com.maxrave.simpmusic.wear.ui.components.WearLoadingState
import com.maxrave.simpmusic.wear.ui.util.friendlyNetworkError
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

private const val LOADING = "__loading__"

@Composable
fun ArtistBrowseScreen(
    channelId: String,
    mediaPlayerHandler: MediaPlayerHandler,
    onBack: () -> Unit,
    openNowPlaying: () -> Unit,
    openAlbum: (String) -> Unit,
    openArtist: (String) -> Unit,
    openPlaylist: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val artistRepository: ArtistRepository = remember { GlobalContext.get().get() }
    val listState = rememberSaveable(channelId, saver = LazyListState.Saver) { LazyListState() }
    var selectedTrack by remember(channelId) { mutableStateOf<Track?>(null) }

    val artistRes by
        remember(channelId) { artistRepository.getArtistData(channelId) }
            .collectAsStateWithLifecycle(initialValue = Resource.Error(LOADING))

    val artistData = (artistRes as? Resource.Success)?.data
    val topSongs = artistData?.songs?.results.orEmpty().map { it.toTrack() }
    val videoSongs = artistData?.video.orEmpty().map { it.toTrack() }

    val trackForDetails = selectedTrack
    if (trackForDetails != null) {
        SongDetailsScreen(
            track = trackForDetails,
            mediaPlayerHandler = mediaPlayerHandler,
            onBack = { selectedTrack = null },
            onPlayRequested = {
                mediaPlayerHandler.loadMediaItem(
                    anyTrack = trackForDetails,
                    type = Config.SONG_CLICK,
                    index = null,
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
                    playlistId = "artist:$channelId",
                    playlistName = artistData?.name ?: "Artist",
                    playlistType = PlaylistType.PLAYLIST,
                    continuation = null,
                ),
            )
            mediaPlayerHandler.loadMediaItem(
                anyTrack = firstTrack,
                type = Config.PLAYLIST_CLICK,
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
                    text = artistData?.name ?: "Artist",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        when (artistRes) {
            is Resource.Error -> {
                val msg = (artistRes as Resource.Error).message
                item {
                    if (msg == LOADING) {
                        WearLoadingState("Loading artist...")
                    } else {
                        WearErrorState(message = context.friendlyNetworkError(msg))
                    }
                }
            }
            is Resource.Success -> {
                val artist = artistData
                if (artist == null) {
                    item { WearEmptyState("Artist data unavailable.") }
                    return@WearList
                }

                val summary = listOf(artist.subscribers, artist.views).filter { !it.isNullOrBlank() }.joinToString(" • ")
                if (summary.isNotBlank()) {
                    item {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (!artist.description.isNullOrBlank()) {
                    item {
                        Text(
                            text = artist.description.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                item {
                    Button(
                        onClick = { playTracks(topSongs, 0) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = topSongs.isNotEmpty(),
                    ) {
                        Text("Play top songs")
                    }
                }

                item {
                    FilledTonalButton(
                        onClick = {
                            val shuffled = topSongs.shuffled()
                            playTracks(shuffled, 0)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = topSongs.isNotEmpty(),
                    ) {
                        Text("Shuffle top songs")
                    }
                }

                if (topSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Top songs",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(topSongs.size) { index ->
                        val song = topSongs[index]
                        TrackRow(
                            title = song.title,
                            subtitle = song.artists?.joinToString { it.name }.orEmpty(),
                            onClick = { selectedTrack = song },
                        )
                    }
                }

                val albums = artist.albums?.results.orEmpty()
                if (albums.isNotEmpty()) {
                    item {
                        Text(
                            text = "Albums",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(albums.size) { index ->
                        val album = albums[index]
                        TrackRow(
                            title = album.title,
                            subtitle = album.year,
                            onClick = { openAlbum(album.browseId) },
                        )
                    }
                }

                val singles = artist.singles?.results.orEmpty()
                if (singles.isNotEmpty()) {
                    item {
                        Text(
                            text = "Singles",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(singles.size) { index ->
                        val single = singles[index]
                        TrackRow(
                            title = single.title,
                            subtitle = single.year,
                            onClick = { openAlbum(single.browseId) },
                        )
                    }
                }

                val featuredOn = artist.featuredOn.orEmpty()
                if (featuredOn.isNotEmpty()) {
                    item {
                        Text(
                            text = "Featured playlists",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(featuredOn.size) { index ->
                        val playlist = featuredOn[index]
                        TrackRow(
                            title = playlist.title,
                            subtitle = playlist.author,
                            onClick = { openPlaylist(playlist.id) },
                        )
                    }
                }

                val relatedArtists = artist.related?.results.orEmpty()
                if (relatedArtists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Related artists",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(relatedArtists.size) { index ->
                        val related = relatedArtists[index]
                        TrackRow(
                            title = related.title,
                            subtitle = related.subscribers,
                            onClick = { openArtist(related.browseId) },
                        )
                    }
                }

                if (videoSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Videos",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(videoSongs.size) { index ->
                        val video = videoSongs[index]
                        TrackRow(
                            title = video.title,
                            subtitle = video.artists?.joinToString { it.name }.orEmpty(),
                            onClick = { selectedTrack = video },
                        )
                    }
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
