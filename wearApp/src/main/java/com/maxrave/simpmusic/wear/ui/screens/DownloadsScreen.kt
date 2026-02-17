package com.maxrave.simpmusic.wear.ui.screens

import android.content.Context
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.mediaservice.handler.DownloadHandler
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.wear.ui.components.WearEmptyState
import com.maxrave.simpmusic.wear.ui.components.WearList
import com.maxrave.simpmusic.wear.ui.components.WearLoadingState
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.util.Locale

@Composable
fun DownloadsScreen(
    mediaPlayerHandler: MediaPlayerHandler,
    onBack: () -> Unit,
    openNowPlaying: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val songRepository: SongRepository = remember { GlobalContext.get().get() }
    val cacheRepository: CacheRepository = remember { GlobalContext.get().get() }
    val downloadHandler: DownloadHandler = remember { GlobalContext.get().get() }
    val downloaded by songRepository.getDownloadedSongs().collectAsStateWithLifecycle(initialValue = null)
    val downloading by songRepository.getDownloadingSongs().collectAsStateWithLifecycle(initialValue = null)
    val preparing by songRepository.getPreparingSongs().collectAsStateWithLifecycle(initialValue = emptyList())
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var selectedSong by remember { mutableStateOf<SongEntity?>(null) }
    var refreshNonce by remember { mutableIntStateOf(0) }
    var storage by remember { mutableStateOf(context.readStorageSnapshot()) }
    var downloadCacheBytes by remember { mutableLongStateOf(0L) }

    val active = (preparing + downloading.orEmpty()).distinctBy { it.videoId }
    val finished = downloaded.orEmpty().distinctBy { it.videoId }
    val allKnownSongs = (active + finished).distinctBy { it.videoId }

    LaunchedEffect(active.size, finished.size, refreshNonce) {
        storage = context.readStorageSnapshot()
        downloadCacheBytes = cacheRepository.getCacheSize(Config.DOWNLOAD_CACHE)
    }

    val selected = selectedSong
    if (selected != null) {
        SongDetailsScreen(
            track = selected.toTrack(),
            mediaPlayerHandler = mediaPlayerHandler,
            onBack = { selectedSong = null },
            onPlayRequested = {
                mediaPlayerHandler.loadMediaItem(
                    anyTrack = selected.toTrack(),
                    type = com.maxrave.common.Config.SONG_CLICK,
                    index = null,
                )
            },
            onOpenNowPlaying = openNowPlaying,
        )
        return
    }

    val lowStorage = storage.freeBytes in 1 until LOW_STORAGE_THRESHOLD_BYTES

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
                    text = "Downloads",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Text(
                text = "Free ${storage.freeBytes.readableBytes()} / ${storage.totalBytes.readableBytes()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Text(
                text = "Download cache ${downloadCacheBytes.readableBytes()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (lowStorage) {
            item {
                Text(
                    text = "Low free space. Clear downloads before adding more.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        item {
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        downloadHandler.removeAllDownloads()
                        cacheRepository.clearCache(Config.DOWNLOAD_CACHE)
                        allKnownSongs.forEach { song ->
                            songRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                        }
                        refreshNonce++
                        Toast.makeText(context, "Downloads cleared", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = allKnownSongs.isNotEmpty(),
            ) {
                Text("Clear all downloads")
            }
        }

        if (downloaded == null && downloading == null) {
            item { WearLoadingState("Loading downloads...") }
            return@WearList
        }

        if (active.isEmpty() && finished.isEmpty()) {
            item {
                WearEmptyState(
                    title = "No downloads yet.",
                    hint = "Use Song details to start downloading tracks.",
                )
            }
            return@WearList
        }

        if (active.isNotEmpty()) {
            item {
                Text(
                    text = "In progress",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(active.size) { index ->
                val song = active[index]
                SongRow(
                    title = song.title,
                    artist = song.artistName?.joinToString().orEmpty(),
                    subtitle = "Downloading...",
                    onClick = { selectedSong = song },
                )
            }
        }

        if (finished.isNotEmpty()) {
            item {
                Text(
                    text = "Downloaded",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(finished.size) { index ->
                val song = finished[index]
                SongRow(
                    title = song.title,
                    artist = song.artistName?.joinToString().orEmpty(),
                    subtitle = "Available offline",
                    onClick = { selectedSong = song },
                )
            }
        }
    }
}

private data class StorageSnapshot(
    val totalBytes: Long,
    val freeBytes: Long,
)

private fun Context.readStorageSnapshot(): StorageSnapshot {
    val statFs = StatFs(filesDir.absolutePath)
    val total = statFs.totalBytes.coerceAtLeast(0L)
    val free = statFs.availableBytes.coerceAtLeast(0L)
    return StorageSnapshot(totalBytes = total, freeBytes = free)
}

private fun Long.readableBytes(): String {
    if (this <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        this >= gb.toLong() -> String.format(Locale.US, "%.1f GB", this / gb)
        this >= mb.toLong() -> String.format(Locale.US, "%.1f MB", this / mb)
        this >= kb.toLong() -> String.format(Locale.US, "%.1f KB", this / kb)
        else -> "$this B"
    }
}

private const val LOW_STORAGE_THRESHOLD_BYTES = 512L * 1024L * 1024L

@Composable
private fun SongRow(
    title: String,
    artist: String,
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
        if (artist.isNotBlank()) {
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
