package com.maxrave.simpmusic.wear.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.AsyncImage
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.simpmusic.wear.ui.components.WearList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

@Composable
fun SongDetailsScreen(
    track: Track,
    mediaPlayerHandler: MediaPlayerHandler,
    onBack: () -> Unit,
    onPlayRequested: suspend () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenArtist: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadHandler: DownloadHandler = remember { GlobalContext.get().get() }
    val controlState = mediaPlayerHandler.controlState.collectAsStateWithLifecycle().value
    val nowPlayingState = mediaPlayerHandler.nowPlayingState.collectAsStateWithLifecycle().value
    val downloadTaskState = downloadHandler.downloadTask.collectAsStateWithLifecycle().value
    val simpleState = mediaPlayerHandler.simpleMediaState.collectAsStateWithLifecycle().value
    val queueTracks = mediaPlayerHandler.queueData.collectAsStateWithLifecycle().value?.data?.listTracks.orEmpty()

    val nowPlayingVideoId =
        remember(nowPlayingState) {
            nowPlayingState.track?.videoId
                ?: nowPlayingState.songEntity?.videoId
                ?: nowPlayingState.mediaItem.mediaId.normalizedMediaId()
        }
    val isCurrentTrack = nowPlayingVideoId == track.videoId

    var pendingResolve by remember(track.videoId) { mutableStateOf(false) }
    var showRetryHint by remember(track.videoId) { mutableStateOf(false) }
    var resolveStartedAtMs by remember(track.videoId) { mutableLongStateOf(0L) }

    val isResolving = pendingResolve
    var resolvingSeconds by remember(track.videoId) { mutableIntStateOf(0) }

    LaunchedEffect(pendingResolve, isCurrentTrack, controlState.isPlaying, simpleState, track.videoId) {
        if (!pendingResolve) return@LaunchedEffect
        val acceptedByPlayer =
            isCurrentTrack &&
                (
                    controlState.isPlaying ||
                        simpleState is SimpleMediaState.Ready ||
                        simpleState is SimpleMediaState.Progress ||
                        simpleState is SimpleMediaState.Buffering
                )
        if (acceptedByPlayer) {
            val elapsed = System.currentTimeMillis() - resolveStartedAtMs
            if (elapsed in 0..349L) {
                delay(350L - elapsed)
            }
            pendingResolve = false
            showRetryHint = false
        }
    }

    LaunchedEffect(track.videoId, pendingResolve) {
        if (!pendingResolve) {
            resolvingSeconds = 0
            return@LaunchedEffect
        }
        resolvingSeconds = 0
        while (pendingResolve) {
            delay(1000L)
            resolvingSeconds++
            if (resolvingSeconds >= 18) {
                pendingResolve = false
                showRetryHint = true
            }
        }
    }

    val artworkUrl = track.thumbnails?.lastOrNull()?.url
    val artist = track.artists?.joinToString { it.name }.orEmpty()
    val isPlayingCurrent = isCurrentTrack && controlState.isPlaying
    val currentDownloadState = downloadTaskState[track.videoId] ?: DownloadState.STATE_NOT_DOWNLOADED
    val downloadLabel =
        when (currentDownloadState) {
            DownloadState.STATE_DOWNLOADED -> "Remove download"
            DownloadState.STATE_PREPARING -> "Preparing download..."
            DownloadState.STATE_DOWNLOADING -> "Downloading..."
            else -> "Download"
        }
    val canTriggerDownload = currentDownloadState == DownloadState.STATE_NOT_DOWNLOADED
    val canRemoveDownload = currentDownloadState == DownloadState.STATE_DOWNLOADED
    val queueIndex = queueTracks.indexOfFirst { it.videoId == track.videoId }
    val canRemoveFromQueue = queueIndex >= 0
    val primaryArtistId = track.artists?.firstOrNull { !it.id.isNullOrBlank() }?.id

    WearList {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Song details",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { Spacer(Modifier.height(4.dp)) }

        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (artworkUrl.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 30.dp),
                    )
                } else {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(92.dp),
                    )
                }
            }
        }

        item {
            Text(
                text = track.title,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        val artistId = primaryArtistId
        if (!artistId.isNullOrBlank() && onOpenArtist != null) {
            item {
                FilledTonalButton(
                    onClick = { onOpenArtist(artistId) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open artist")
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }

        item {
            Button(
                onClick = {
                    scope.launch {
                        if (isCurrentTrack && !isResolving && !showRetryHint) {
                            mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause)
                        } else {
                            pendingResolve = true
                            resolveStartedAtMs = System.currentTimeMillis()
                            showRetryHint = false
                            runCatching {
                                onPlayRequested()
                            }.onFailure {
                                pendingResolve = false
                                showRetryHint = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isResolving || showRetryHint,
            ) {
                if (isResolving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    val label =
                        if (resolvingSeconds > 0) {
                            "Resolving... ${resolvingSeconds}s"
                        } else {
                            "Resolving..."
                        }
                    Text(label)
                } else {
                    Text(
                        when {
                            showRetryHint -> "Retry"
                            isPlayingCurrent -> "Pause"
                            else -> "Play"
                        },
                    )
                }
            }
        }

        item {
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        mediaPlayerHandler.playNext(track)
                        Toast.makeText(context, "Added as next track", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Play next")
            }
        }

        item {
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        mediaPlayerHandler.loadMoreCatalog(arrayListOf(track), isAddToQueue = true)
                        Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add to queue")
            }
        }

        if (canRemoveFromQueue) {
            item {
                FilledTonalButton(
                    onClick = {
                        if (queueIndex >= 0) {
                            mediaPlayerHandler.removeMediaItem(queueIndex)
                            Toast.makeText(context, "Removed from queue", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Remove from queue")
                }
            }
        }

        item {
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        if (canRemoveDownload) {
                            downloadHandler.removeDownload(track.videoId)
                            Toast.makeText(context, "Download removed", Toast.LENGTH_SHORT).show()
                        } else if (canTriggerDownload) {
                            val thumb = artworkUrl ?: "https://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
                            downloadHandler.downloadTrack(track.videoId, track.title, thumb)
                            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canTriggerDownload || canRemoveDownload,
            ) {
                Text(downloadLabel)
            }
        }

        item {
            FilledTonalButton(
                onClick = onOpenNowPlaying,
                modifier = Modifier.fillMaxWidth(),
                enabled = nowPlayingState.isNotEmpty(),
            ) {
                Text("Open player")
            }
        }

        if (showRetryHint) {
            item {
                Text(
                    text = "That took too long. Tap Retry to try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun String?.normalizedMediaId(): String? {
    if (this.isNullOrBlank()) return null
    return this.removePrefix("Video")
}
