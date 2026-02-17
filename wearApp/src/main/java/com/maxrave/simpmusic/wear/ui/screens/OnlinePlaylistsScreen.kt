package com.maxrave.simpmusic.wear.ui.screens

import android.app.Activity
import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.wear.input.RemoteInputIntentHelper
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.repository.PlaylistRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.wear.ui.components.WearEmptyState
import com.maxrave.simpmusic.wear.ui.components.WearErrorState
import com.maxrave.simpmusic.wear.ui.components.WearList
import com.maxrave.simpmusic.wear.ui.components.WearLoadingState
import com.maxrave.simpmusic.wear.ui.util.friendlyNetworkError
import kotlinx.coroutines.flow.collect
import org.koin.core.context.GlobalContext

private const val PLAYLIST_SEARCH_REMOTE_INPUT_KEY = "wear_playlist_search_query"

@Composable
fun OnlinePlaylistsScreen(
    onBack: () -> Unit,
    openPlaylist: (String) -> Unit,
) {
    val context = LocalContext.current
    val playlistRepository: PlaylistRepository = remember { GlobalContext.get().get() }
    val searchRepository: SearchRepository = remember { GlobalContext.get().get() }
    val libraryPlaylists by playlistRepository.getLibraryPlaylist().collectAsStateWithLifecycle(initialValue = null)
    val mixedPlaylists by playlistRepository.getMixedForYou().collectAsStateWithLifecycle(initialValue = null)

    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberSaveable(query, saver = LazyListState.Saver) { LazyListState() }
    var searchLoading by remember(query) { mutableStateOf(false) }
    var searchError by remember(query) { mutableStateOf<String?>(null) }
    var searchPlaylists by remember(query) { mutableStateOf<List<PlaylistsResult>>(emptyList()) }

    val keyboardLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
            val data = result.data ?: return@rememberLauncherForActivityResult
            val input = RemoteInput.getResultsFromIntent(data)
            val typed = input?.getCharSequence(PLAYLIST_SEARCH_REMOTE_INPUT_KEY)?.toString()?.trim().orEmpty()
            if (typed.isBlank()) return@rememberLauncherForActivityResult
            query = typed
        }

    fun openWearKeyboard() {
        val remoteInputs =
            listOf(
                RemoteInput
                    .Builder(PLAYLIST_SEARCH_REMOTE_INPUT_KEY)
                    .setLabel("Search playlists")
                    .build(),
            )
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
        keyboardLauncher.launch(intent)
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            searchLoading = false
            searchError = null
            searchPlaylists = emptyList()
            return@LaunchedEffect
        }

        searchLoading = true
        searchError = null
        searchPlaylists = emptyList()
        var lastError: String? = null

        var directResults = emptyList<PlaylistsResult>()
        searchRepository.getSearchDataPlaylist(query).collect { values ->
            when (values) {
                is Resource.Success -> directResults = values.data.orEmpty()
                is Resource.Error -> lastError = values.message
            }
        }

        var featuredResults = emptyList<PlaylistsResult>()
        searchRepository.getSearchDataFeaturedPlaylist(query).collect { values ->
            when (values) {
                is Resource.Success -> featuredResults = values.data.orEmpty()
                is Resource.Error -> if (lastError.isNullOrBlank()) lastError = values.message
            }
        }

        searchPlaylists = (directResults + featuredResults).distinctBy { it.browseId }
        searchLoading = false
        if (searchPlaylists.isEmpty() && !lastError.isNullOrBlank()) {
            searchError = lastError
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
                    text = "Playlists",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { openWearKeyboard() }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search playlists")
                }
            }
        }

        item {
            FilledTonalButton(
                onClick = { openWearKeyboard() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Search playlists")
            }
        }

        if (query.isNotBlank()) {
            item {
                Text(
                    text = "Search results for \"$query\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (searchLoading) {
                item { WearLoadingState("Searching playlists...") }
            } else if (searchPlaylists.isEmpty()) {
                item {
                    val msg = searchError
                    if (!msg.isNullOrBlank()) {
                        WearErrorState(
                            message = context.friendlyNetworkError(msg),
                            actionLabel = "Retry",
                            onAction = { openWearKeyboard() },
                        )
                    } else {
                        WearEmptyState("No playlists found.")
                    }
                }
            } else {
                items(searchPlaylists.size) { index ->
                    val playlist = searchPlaylists[index]
                    PlaylistRow(
                        playlist = playlist,
                        onClick = { openPlaylist(playlist.browseId) },
                    )
                }
            }
            return@WearList
        }

        val library = libraryPlaylists.orEmpty()
        val mixed = mixedPlaylists.orEmpty()

        if (libraryPlaylists == null && mixedPlaylists == null) {
            item { WearLoadingState("Loading online playlists...") }
            return@WearList
        }

        if (library.isEmpty() && mixed.isEmpty()) {
            item {
                WearEmptyState(
                    title = "No online playlists yet.",
                    hint = "Sign in and refresh library playlists from your phone account.",
                )
            }
            return@WearList
        }

        if (library.isNotEmpty()) {
            item {
                Text(
                    text = "Your library",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(library.size) { index ->
                val playlist = library[index]
                PlaylistRow(playlist = playlist, onClick = { openPlaylist(playlist.browseId) })
            }
        }

        if (mixed.isNotEmpty()) {
            item {
                Text(
                    text = "Mixed for you",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            items(mixed.size) { index ->
                val playlist = mixed[index]
                PlaylistRow(playlist = playlist, onClick = { openPlaylist(playlist.browseId) })
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistsResult,
    onClick: () -> Unit,
) {
    val subtitle = listOf(playlist.author, playlist.itemCount).filter { it.isNotBlank() }.joinToString(" • ")
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
    ) {
        Text(
            text = playlist.title,
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
