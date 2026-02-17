package com.maxrave.simpmusic.wear.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.common.Config
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.home.HomeItem
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.wear.ui.components.PlaybackControlIcon
import com.maxrave.simpmusic.wear.ui.components.QuickActionChip
import com.maxrave.simpmusic.wear.ui.components.WearList
import com.maxrave.simpmusic.wear.ui.components.WearErrorState
import com.maxrave.simpmusic.wear.ui.components.WearLoadingState
import com.maxrave.simpmusic.wear.ui.util.friendlyNetworkError
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

private const val LOADING = "__loading__"

@Composable
fun DiscoverScreen(
    mediaPlayerHandler: MediaPlayerHandler,
    openSearch: () -> Unit,
    openPlaylistsDirectory: () -> Unit,
    openDownloads: () -> Unit,
    openNowPlaying: () -> Unit,
    openQueue: () -> Unit,
    openLibrary: () -> Unit,
    openAccounts: () -> Unit,
    openYtPlaylist: (String) -> Unit,
    openAlbum: (String) -> Unit,
    openArtist: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dataStoreManager: DataStoreManager = remember { GlobalContext.get().get() }
    val homeRepository: HomeRepository = remember { GlobalContext.get().get() }
    var refreshNonce by remember { mutableIntStateOf(0) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val loggedIn = dataStoreManager.loggedIn.collectAsStateWithLifecycle(initialValue = DataStoreManager.FALSE).value == DataStoreManager.TRUE
    val pageId = dataStoreManager.pageId.collectAsStateWithLifecycle(initialValue = "").value
    val sessionKey = if (loggedIn) pageId else "guest"

    val homeRes by
        remember(refreshNonce, sessionKey) { homeRepository.getHomeData(context) }
            .collectAsStateWithLifecycle(initialValue = Resource.Error(LOADING))
    val control = mediaPlayerHandler.controlState.collectAsStateWithLifecycle().value
    val simpleState = mediaPlayerHandler.simpleMediaState.collectAsStateWithLifecycle().value
    val isMiniLoading =
        when (simpleState) {
            is SimpleMediaState.Loading -> control.isPlaying || simpleState.duration <= 0L
            is SimpleMediaState.Buffering -> control.isPlaying
            else -> false
        }
    val nowPlayingState = mediaPlayerHandler.nowPlayingState.collectAsStateWithLifecycle().value

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

    WearList(state = listState) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (loggedIn) "Discover" else "Discover (Guest)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = openSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = openAccounts) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Accounts")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickActionChip(
                    label = "Playlists",
                    onClick = openPlaylistsDirectory,
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    modifier = Modifier.weight(1f),
                )
                QuickActionChip(
                    label = "Downloads",
                    onClick = openDownloads,
                    icon = Icons.Filled.Download,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            MiniPlayerHeader(
                title =
                    nowPlayingState.songEntity?.title
                        ?: nowPlayingState.track?.title
                        ?: nowPlayingState.mediaItem.metadata.title
                        ?: "Nothing playing",
                artist =
                    nowPlayingState.songEntity?.artistName?.joinToString()
                        ?: nowPlayingState.track?.artists?.joinToString { it.name }
                        ?: nowPlayingState.mediaItem.metadata.artist
                        ?: "",
                isPlaying = control.isPlaying,
                isLoading = isMiniLoading,
                hasNext = control.isNextAvailable,
                hasPrev = control.isPreviousAvailable,
                onPrev = { scope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.Previous) } },
                onPlayPause = { scope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause) } },
                onNext = { scope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next) } },
                onOpenNowPlaying = openNowPlaying,
                onOpenQueue = openQueue,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.labelLarge,
                    modifier =
                        Modifier
                            .clickable(onClick = openLibrary)
                            .padding(vertical = 6.dp),
                )
                IconButton(
                    onClick = {
                        refreshNonce++
                        Toast.makeText(context, "Refreshing...", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        }

        when (homeRes) {
            is Resource.Success -> {
                val items = (homeRes as Resource.Success<List<HomeItem>>).data.orEmpty()
                items.forEach { section ->
                    val sectionTitle = section.title
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!section.subtitle.isNullOrBlank()) {
                            Text(
                                text = section.subtitle.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    val contents =
                        section.contents
                            .filterNotNull()
                            .take(6)

                    contents.forEach { content ->
                        item {
                            ContentRow(
                                content = content,
                                onClick = {
                                    when {
                                        !content.videoId.isNullOrBlank() -> {
                                            selectedTrack = content.toTrack()
                                        }
                                        !content.playlistId.isNullOrBlank() -> openYtPlaylist(content.playlistId!!)
                                        !content.browseId.isNullOrBlank() -> {
                                            val browseId = content.browseId!!
                                            val shouldOpenArtist =
                                                sectionTitle.contains("artist", ignoreCase = true) ||
                                                    browseId.isArtistBrowseId()
                                            if (shouldOpenArtist) {
                                                openArtist(browseId)
                                            } else {
                                                openAlbum(browseId)
                                            }
                                        }
                                        else -> Unit
                                    }
                                },
                            )
                        }
                    }
                }
            }
            is Resource.Error -> {
                val msg = (homeRes as Resource.Error<List<HomeItem>>).message
                if (msg == LOADING) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        WearLoadingState("Loading recommendations...")
                    }
                } else {
                    val friendly = context.friendlyNetworkError(msg)
                    item {
                        Spacer(Modifier.height(12.dp))
                        WearErrorState(message = friendly)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tip: try logging in from Accounts if this keeps failing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerHeader(
    title: String,
    artist: String,
    isPlaying: Boolean,
    isLoading: Boolean,
    hasNext: Boolean,
    hasPrev: Boolean,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = onOpenNowPlaying),
        )
        if (artist.isNotBlank()) {
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev, enabled = hasPrev) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(onClick = onPlayPause) {
                PlaybackControlIcon(isPlaying = isPlaying, isLoading = isLoading && !isPlaying)
            }
            IconButton(onClick = onNext, enabled = hasNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next")
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Open queue",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .clickable(onClick = onOpenQueue)
                    .padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun ContentRow(
    content: Content,
    onClick: () -> Unit,
) {
    val artist = content.artists?.joinToString { it.name }.orEmpty()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
    ) {
        Text(
            text = content.title,
            style = MaterialTheme.typography.bodyMedium,
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

private fun Content.toTrack(): Track =
    Track(
        album = this.album,
        artists = this.artists,
        duration = null,
        durationSeconds = this.durationSeconds,
        isAvailable = true,
        isExplicit = this.isExplicit ?: false,
        likeStatus = null,
        thumbnails = this.thumbnails,
        title = this.title,
        videoId = this.videoId ?: "",
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = null,
    )

private fun String.isArtistBrowseId(): Boolean =
    this.startsWith("UC") ||
        this.startsWith("MPLAUC") ||
        this.startsWith("FEmusic_library_privately_owned_artist")
