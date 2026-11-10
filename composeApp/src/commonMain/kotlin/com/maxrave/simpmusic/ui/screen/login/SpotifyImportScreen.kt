package com.maxrave.simpmusic.ui.screen.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.maxrave.domain.data.model.spotify.SpotifyLibraryError
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.login.SpotifyLoginDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SpotifyImportViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.log_in_to_spotify
import simpmusic.composeapp.generated.resources.ok
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.spotify_import_already
import simpmusic.composeapp.generated.resources.spotify_import_done
import simpmusic.composeapp.generated.resources.spotify_import_empty
import simpmusic.composeapp.generated.resources.spotify_import_error_expired
import simpmusic.composeapp.generated.resources.spotify_import_error_login
import simpmusic.composeapp.generated.resources.spotify_import_error_network
import simpmusic.composeapp.generated.resources.spotify_import_error_not_allowed
import simpmusic.composeapp.generated.resources.spotify_import_error_rate_limited
import simpmusic.composeapp.generated.resources.spotify_import_liked
import simpmusic.composeapp.generated.resources.spotify_import_progress
import simpmusic.composeapp.generated.resources.spotify_import_skipped
import simpmusic.composeapp.generated.resources.spotify_import_title
import simpmusic.composeapp.generated.resources.spotify_import_tracks
import simpmusic.composeapp.generated.resources.spotify_import_unmatched

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyImportScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SpotifyImportViewModel = koinViewModel(),
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        hideBottomNavigation()
        viewModel.refresh()
    }
    DisposableEffect(Unit) {
        onDispose { showBottomNavigation() }
    }

    val importing = state.progress != null

    Column(Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
        TopAppBar(
            title = { Text(stringResource(Res.string.spotify_import_title), style = typo().titleMedium) },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(SimpIcons.ArrowBackIosNew, contentDescription = null)
                }
            },
        )

        state.progress?.let { progress ->
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(Res.string.spotify_import_progress, progress.name, progress.processed, progress.total),
                    style = typo().bodyMedium,
                )
                if (progress.total > 0) {
                    LinearProgressIndicator(progress = { progress.processed.toFloat() / progress.total }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = { viewModel.cancelImport() }) { Text(stringResource(Res.string.cancel)) }
            }
        }

        state.error?.let { error ->
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(errorText(error), style = typo().bodyMedium, color = MaterialTheme.colorScheme.error)
                val needsLogin = error == SpotifyLibraryError.NOT_LOGGED_IN || error == SpotifyLibraryError.SESSION_EXPIRED
                Button(
                    onClick = {
                        if (needsLogin) navController.navigate(SpotifyLoginDestination) else viewModel.refresh()
                    },
                ) {
                    Text(stringResource(if (needsLogin) Res.string.log_in_to_spotify else Res.string.retry))
                }
            }
        }

        if (state.loadingPlaylists) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        }

        if (state.loggedIn && state.error == null && !state.loadingPlaylists) {
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "liked") {
                    ImportRow(
                        title = stringResource(Res.string.spotify_import_liked),
                        subtitle = null,
                        enabled = !importing,
                        onClick = { viewModel.importLikedSongs() },
                    )
                }
                if (state.playlists.isEmpty()) {
                    item(key = "empty") {
                        Text(stringResource(Res.string.spotify_import_empty), Modifier.padding(16.dp), style = typo().bodyMedium)
                    }
                }
                items(state.playlists, key = { it.id }) { playlist ->
                    ImportRow(
                        title = playlist.name,
                        subtitle = stringResource(Res.string.spotify_import_tracks, playlist.trackCount),
                        enabled = !importing,
                        onClick = { viewModel.importPlaylist(playlist) },
                    )
                }
            }
        }
    }

    state.report?.let { report ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissReport() },
            confirmButton = { TextButton(onClick = { viewModel.dismissReport() }) { Text(stringResource(Res.string.ok)) } },
            title = { Text(report.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (report.error != null) {
                        Text(errorText(report.error!!), color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(stringResource(Res.string.spotify_import_done, report.name, report.added, report.total))
                        if (report.alreadyThere > 0) Text(stringResource(Res.string.spotify_import_already, report.alreadyThere))
                        if (report.skippedUnavailable > 0) Text(stringResource(Res.string.spotify_import_skipped, report.skippedUnavailable))
                        if (report.unmatched.isNotEmpty()) {
                            Text(stringResource(Res.string.spotify_import_unmatched, report.unmatched.size))
                            // A long list would push the OK button off screen; the count above is the full number.
                            Text(report.unmatched.take(MAX_LISTED_UNMATCHED).joinToString("\n"), style = typo().bodySmall)
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun ImportRow(
    title: String,
    subtitle: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(title, style = typo().bodyLarge)
        if (subtitle != null) Text(subtitle, style = typo().bodySmall)
    }
}

@Composable
private fun errorText(error: SpotifyLibraryError): String =
    stringResource(
        when (error) {
            SpotifyLibraryError.NOT_LOGGED_IN -> Res.string.spotify_import_error_login
            SpotifyLibraryError.SESSION_EXPIRED -> Res.string.spotify_import_error_expired
            SpotifyLibraryError.NOT_ALLOWED -> Res.string.spotify_import_error_not_allowed
            SpotifyLibraryError.RATE_LIMITED -> Res.string.spotify_import_error_rate_limited
            SpotifyLibraryError.NETWORK -> Res.string.spotify_import_error_network
        },
    )

private const val MAX_LISTED_UNMATCHED = 30
