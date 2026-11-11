package com.maxrave.simpmusic.ui.screen.library

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.maxrave.common.Config.LOCAL_MEDIA_ID_PREFIX
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.data.player.GenericMediaMetadata
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.QueueData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

private data class DeviceAudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val artworkUri: String?,
)

@Composable
actual fun DeviceAudioScreen() {
    val context = LocalContext.current
    val playerHandler: MediaPlayerHandler = koinInject()
    val permission = remember {
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    var tracks by remember { mutableStateOf<List<DeviceAudioTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var refreshKey by remember { mutableStateOf(0) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
            if (granted) refreshKey++
        }

    LaunchedEffect(hasPermission, refreshKey) {
        if (hasPermission) {
            loading = true
            tracks = withContext(Dispatchers.IO) { queryDeviceAudio(context) }
            loading = false
        }
    }

    val visibleTracks = remember(tracks, search) {
        if (search.isBlank()) tracks
        else tracks.filter {
            it.title.contains(search, ignoreCase = true) ||
                it.artist.contains(search, ignoreCase = true) ||
                it.album.contains(search, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("device audio", style = MaterialTheme.typography.headlineSmall)
        if (!hasPermission) {
            Text(
                "allow audio access to browse music stored on this device. only audio files are read.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { permissionLauncher.launch(permission) }) {
                Text("allow access")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("search songs") },
                )
                Button(onClick = { refreshKey++ }, enabled = !loading) {
                    Text("refresh")
                }
            }
            Text("${tracks.size} songs found", style = MaterialTheme.typography.labelMedium)
            when {
                loading -> CircularProgressIndicator()
                visibleTracks.isEmpty() -> Text(
                    if (tracks.isEmpty()) "no music files found on this device"
                    else "no songs match your search",
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(visibleTracks, key = { it.id }) { track ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val queue = visibleTracks.map { it.toMediaItem() }
                                    val selectedIndex = visibleTracks.indexOfFirst { it.id == track.id }
                                    playerHandler.clearMediaItems()
                                    playerHandler.setQueueData(QueueData.Data())
                                    // Put the selected song first. The handler prepares and starts
                                    // the first item while preserving the rest as the upcoming queue.
                                    playerHandler.addMediaItemList(queue.drop(selectedIndex) + queue.take(selectedIndex))
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(
                                "${track.artist} • ${formatTrackDuration(track.durationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DeviceAudioTrack.toMediaItem() =
    GenericMediaItem(
        mediaId = LOCAL_MEDIA_ID_PREFIX + id,
        uri = uri,
        metadata = GenericMediaMetadata(
            title = title,
            artist = artist,
            albumTitle = album,
            artworkUri = artworkUri,
            description = "device audio",
        ),
    )

private fun queryDeviceAudio(context: Context): List<DeviceAudioTrack> {
    val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID,
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
    val result = mutableListOf<DeviceAudioTrack>()
    context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val albumId = cursor.getLong(albumIdColumn)
            result += DeviceAudioTrack(
                id = id,
                title = cursor.getString(titleColumn)?.takeIf(String::isNotBlank) ?: "unknown title",
                artist = cursor.getString(artistColumn)?.takeIf(String::isNotBlank) ?: "unknown artist",
                album = cursor.getString(albumColumn)?.takeIf(String::isNotBlank) ?: "",
                durationMs = cursor.getLong(durationColumn),
                uri = ContentUris.withAppendedId(collection, id).toString(),
                artworkUri = if (albumId > 0) "content://media/external/audio/albumart/$albumId" else null,
            )
        }
    }
    return result
}

private fun formatTrackDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
