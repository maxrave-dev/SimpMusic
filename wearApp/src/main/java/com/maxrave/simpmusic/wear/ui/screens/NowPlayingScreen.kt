package com.maxrave.simpmusic.wear.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.domain.mediaservice.handler.SimpleMediaState
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.wear.ui.components.PlaybackControlIcon
import com.maxrave.simpmusic.wear.ui.components.ThinProgressBar
import com.maxrave.simpmusic.wear.ui.components.WearList
import com.maxrave.simpmusic.wear.ui.util.formatDurationMs
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

@Composable
fun NowPlayingScreen(
    mediaPlayerHandler: MediaPlayerHandler,
    onBack: () -> Unit,
    onOpenVolumeSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadHandler: DownloadHandler = remember { GlobalContext.get().get() }

    val nowPlayingState = mediaPlayerHandler.nowPlayingState.collectAsStateWithLifecycle().value
    val controlState = mediaPlayerHandler.controlState.collectAsStateWithLifecycle().value
    val downloadTaskState = downloadHandler.downloadTask.collectAsStateWithLifecycle().value
    val simpleState = mediaPlayerHandler.simpleMediaState.collectAsStateWithLifecycle().value
    val isLoading = simpleState is SimpleMediaState.Loading || simpleState is SimpleMediaState.Buffering
    val detailTrack = nowPlayingState.track ?: nowPlayingState.songEntity?.toTrack()
    var showDetails by rememberSaveable(nowPlayingState.mediaItem.mediaId) { mutableStateOf(false) }

    if (showDetails && detailTrack != null) {
        SongDetailsScreen(
            track = detailTrack,
            mediaPlayerHandler = mediaPlayerHandler,
            onBack = { showDetails = false },
            onPlayRequested = {
                mediaPlayerHandler.loadMediaItem(
                    anyTrack = detailTrack,
                    type = Config.SONG_CLICK,
                    index = null,
                )
            },
            onOpenNowPlaying = { showDetails = false },
        )
        return
    }

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
    val artworkUrl =
        nowPlayingState.track?.thumbnails?.lastOrNull()?.url
            ?: nowPlayingState.songEntity?.thumbnails
            ?: nowPlayingState.mediaItem.metadata.artworkUri

    val durationMs =
        when (simpleState) {
            is SimpleMediaState.Ready -> simpleState.duration
            is SimpleMediaState.Loading -> simpleState.duration
            else -> mediaPlayerHandler.getPlayerDuration()
        }

    val rawProgressMs =
        when (simpleState) {
            is SimpleMediaState.Progress -> simpleState.progress
            is SimpleMediaState.Buffering -> simpleState.position
            else -> mediaPlayerHandler.getProgress()
        }
    var progressMs by rememberSaveable { mutableLongStateOf(rawProgressMs) }
    LaunchedEffect(rawProgressMs, controlState.isPlaying, durationMs) {
        progressMs = rawProgressMs
        if (!controlState.isPlaying) return@LaunchedEffect
        while (controlState.isPlaying) {
            delay(1000L)
            val latest = mediaPlayerHandler.getProgress()
            progressMs =
                if (durationMs > 0L) {
                    latest.coerceIn(0L, durationMs)
                } else {
                    latest.coerceAtLeast(0L)
                }
        }
    }

    val progressFraction =
        if (durationMs > 0L) {
            progressMs.coerceIn(0L, durationMs).toFloat() / durationMs.toFloat()
        } else {
            0f
        }
    val isResolvingStream =
        nowPlayingState.isNotEmpty() &&
            !controlState.isPlaying &&
            durationMs <= 0L &&
            progressMs <= 0L
    val showLoadingUi = isLoading || isResolvingStream
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val currentDownloadState = detailTrack?.videoId?.let { downloadTaskState[it] } ?: DownloadState.STATE_NOT_DOWNLOADED
    val canTriggerDownload = detailTrack != null && currentDownloadState == DownloadState.STATE_NOT_DOWNLOADED
    val canRemoveDownload = detailTrack != null && currentDownloadState == DownloadState.STATE_DOWNLOADED
    val downloadEnabled = canTriggerDownload || canRemoveDownload
    val downloadIconTint =
        when (currentDownloadState) {
            DownloadState.STATE_NOT_DOWNLOADED -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.primary
        }
    val downloadContentDescription =
        when {
            detailTrack == null -> "Download unavailable"
            currentDownloadState == DownloadState.STATE_DOWNLOADED -> "Remove download"
            currentDownloadState == DownloadState.STATE_PREPARING -> "Preparing download"
            currentDownloadState == DownloadState.STATE_DOWNLOADING -> "Downloading"
            else -> "Download"
        }
    var retrySuggested by rememberSaveable(nowPlayingState.mediaItem.mediaId) { mutableStateOf(false) }
    LaunchedEffect(showLoadingUi, controlState.isPlaying, nowPlayingState.mediaItem.mediaId) {
        if (!showLoadingUi || controlState.isPlaying || !nowPlayingState.isNotEmpty()) {
            retrySuggested = false
            return@LaunchedEffect
        }
        retrySuggested = false
        delay(12_000L)
        if (showLoadingUi && !controlState.isPlaying && nowPlayingState.isNotEmpty()) {
            retrySuggested = true
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
                    text = "Now playing",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { Spacer(Modifier.height(6.dp)) }

        item {
            ArtworkThumb(
                title = title,
                artworkUrl = artworkUrl,
                onClick = if (detailTrack != null) ({ showDetails = true }) else null,
            )
        }

        item { Spacer(Modifier.height(2.dp)) }

        item {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color =
                    if (detailTrack != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                modifier =
                    if (detailTrack != null) {
                        Modifier
                            .fillMaxWidth()
                            .clickable { showDetails = true }
                    } else {
                        Modifier.fillMaxWidth()
                    },
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
        item {
            Text(
                text =
                    when {
                        controlState.isPlaying -> "Playing"
                        showLoadingUi -> "Connecting..."
                        else -> "Paused"
                    },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { Spacer(Modifier.height(8.dp)) }

        if (durationMs > 0L) {
            item {
                ThinProgressBar(progress = progressFraction)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = formatDurationMs(progressMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                    )
                    Text(
                        text = formatDurationMs(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                    )
                }
            }
        } else {
            item {
                Text(
                    text =
                        when {
                            retrySuggested -> "Stream timed out. Tap play to retry."
                            controlState.isPlaying -> "Buffering..."
                            else -> "Resolving stream..."
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        item { Spacer(Modifier.height(6.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { scope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.Previous) } },
                    enabled = controlState.isPreviousAvailable,
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            if (!controlState.isPlaying && nowPlayingState.isNotEmpty() && (retrySuggested || showLoadingUi)) {
                                mediaPlayerHandler.retryCurrentStream()
                            } else {
                                mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause)
                            }
                        }
                    },
                ) {
                    PlaybackControlIcon(
                        isPlaying = controlState.isPlaying,
                        isLoading = showLoadingUi && !controlState.isPlaying && !retrySuggested,
                    )
                }
                IconButton(
                    onClick = { scope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.Next) } },
                    enabled = controlState.isNextAvailable,
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
            }
        }

        item { Spacer(Modifier.height(6.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { scope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.Shuffle) } },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (controlState.isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = { scope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.Repeat) } },
                    modifier = Modifier.size(40.dp),
                ) {
                    val (icon, tint) =
                        when (controlState.repeatState) {
                            RepeatState.None -> Icons.Filled.Repeat to MaterialTheme.colorScheme.onSurface
                            RepeatState.All -> Icons.Filled.Repeat to MaterialTheme.colorScheme.primary
                            RepeatState.One -> Icons.Filled.RepeatOne to MaterialTheme.colorScheme.primary
                        }
                    Icon(icon, contentDescription = "Repeat", tint = tint)
                }
                IconButton(
                    onClick = { mediaPlayerHandler.toggleLike() },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        if (controlState.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (controlState.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            val track = detailTrack ?: return@launch
                            if (canRemoveDownload) {
                                downloadHandler.removeDownload(track.videoId)
                                Toast.makeText(context, "Download removed", Toast.LENGTH_SHORT).show()
                            } else if (canTriggerDownload) {
                                val thumb = track.thumbnails?.lastOrNull()?.url ?: "https://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
                                downloadHandler.downloadTrack(track.videoId, track.title, thumb)
                                Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    enabled = downloadEnabled,
                ) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = downloadContentDescription,
                        tint = downloadIconTint,
                    )
                }
                IconButton(
                    onClick = onOpenVolumeSettings,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Volume")
                }
            }
        }
    }
}

@Composable
private fun ArtworkThumb(
    title: String,
    artworkUrl: String?,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(86.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center,
    ) {
        val contentModifier =
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .then(contentModifier),
        contentAlignment = Alignment.Center,
        ) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(86.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }
    }
}
