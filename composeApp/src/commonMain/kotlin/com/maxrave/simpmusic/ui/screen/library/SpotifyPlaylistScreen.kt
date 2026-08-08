package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.spotify.SpotifyTrack
import com.maxrave.domain.utils.LocalResource
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SpotifyPlaylistViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.play_all
import simpmusic.composeapp.generated.resources.spotify_track_unavailable

/**
 * Shows one Spotify playlist's tracks (title/artist/album art come straight from Spotify),
 * resolving each to a matching YouTube Music track in the background
 * ([SpotifyPlaylistViewModel.resolvedTracks]) and playing them through SimpMusic's own
 * player/queue ([SharedViewModel.playResolvedTrackList]) - once a track starts playing it's
 * indistinguishable from any other SimpMusic song.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlaylistScreen(
    playlistId: String,
    playlistName: String,
    navController: NavController,
    viewModel: SpotifyPlaylistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val resolvedTracks by viewModel.resolvedTracks.collectAsStateWithLifecycle()

    LaunchedEffect(playlistId) {
        viewModel.loadTracks(playlistId)
    }

    val trackList = (tracks as? LocalResource.Success)?.data.orEmpty()

    // Only tracks that resolved to an actual YouTube Music match are playable; unresolved
    // (still loading, or no match found) ones are skipped when building the play queue.
    fun play(fromSpotifyTrackId: String?) {
        val playableInOrder = trackList.mapNotNull { resolvedTracks[it.id] }
        if (playableInOrder.isEmpty()) return
        val startIndex =
            fromSpotifyTrackId
                ?.let { id -> trackList.takeWhile { it.id != id }.count { resolvedTracks[it.id] != null } }
                ?: 0
        sharedViewModel.playResolvedTrackList(
            tracks = playableInOrder,
            startIndex = startIndex.coerceIn(0, playableInOrder.lastIndex),
            playlistId = "spotify:playlist:$playlistId",
            playlistName = playlistName,
        )
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Text(text = playlistName, style = typo().titleMedium, maxLines = 1)
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(SimpIcons.ArrowBackIosNew, Modifier.size(32.dp), true) {
                        navController.navigateUp()
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        if (tracks is LocalResource.Loading && trackList.isEmpty()) {
            CenterLoadingBox(Modifier.fillMaxSize())
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${trackList.size} tracks",
                            style = typo().bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { play(null) },
                            enabled = resolvedTracks.values.any { it != null },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                        ) {
                            Icon(SimpIcons.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(stringResource(Res.string.play_all), modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }

                items(trackList, key = { it.id }) { track ->
                    val isResolving = !resolvedTracks.containsKey(track.id)
                    val isUnavailable = resolvedTracks[track.id] == null && !isResolving
                    SpotifyTrackRow(
                        track = track,
                        isResolving = isResolving,
                        isUnavailable = isUnavailable,
                        onClick = { play(track.id) },
                    )
                }

                item { EndOfPage() }
            }
        }
    }
}

@Composable
private fun SpotifyTrackRow(
    track: SpotifyTrack,
    isResolving: Boolean,
    isUnavailable: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !isResolving && !isUnavailable, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalPlatformContext.current)
                    .data(track.albumImageUrl)
                    .crossfade(200)
                    .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
        )
        Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
            Text(
                text = track.title,
                style = typo().bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
            Text(
                text = track.artists.joinToString(", "),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (isUnavailable) {
                Text(
                    text = stringResource(Res.string.spotify_track_unavailable),
                    style = typo().bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                )
            }
        }
        if (isResolving) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else if (!isUnavailable) {
            RippleIconButton(SimpIcons.PlayArrow, Modifier.size(28.dp), true, onClick = onClick)
        }
    }
}
