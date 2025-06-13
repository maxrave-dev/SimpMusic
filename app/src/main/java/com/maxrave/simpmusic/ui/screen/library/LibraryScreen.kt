package com.maxrave.simpmusic.ui.screen.library

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.LibraryItem
import com.maxrave.simpmusic.ui.component.LibraryItemState
import com.maxrave.simpmusic.ui.component.LibraryItemType
import com.maxrave.simpmusic.ui.component.LibraryTilingBox
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.LocalResource
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
) {
    val loggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsState()
    val youTubePlaylist by viewModel.youTubePlaylist.collectAsState()
    val listCanvasSong by viewModel.listCanvasSong.collectAsState()
    val yourLocalPlaylist by viewModel.yourLocalPlaylist.collectAsState()
    val favoritePlaylist by viewModel.favoritePlaylist.collectAsState()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsState()
    val favoritePodcasts by viewModel.favoritePodcasts.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    LaunchedEffect(true) {
        Log.w("LibraryScreen", "Check youtubePlaylist: ${youTubePlaylist.data}")
        if (youTubePlaylist.data.isNullOrEmpty()) {
            viewModel.getYouTubePlaylist()
        }
        viewModel.getCanvasSong()
        viewModel.getLocalPlaylist()
        viewModel.getPlaylistFavorite()
        viewModel.getDownloadedPlaylist()
        viewModel.getFavoritePodcasts()
        viewModel.getRecentlyAdded()
    }
    LaunchedEffect(nowPlaying) {
        Log.w("LibraryScreen", "Check nowPlaying: $nowPlaying")
        viewModel.getRecentlyAdded()
    }

    LazyColumn(
        contentPadding = innerPadding,
    ) {
        item {
            Spacer(Modifier.height(64.dp))
        }
        item {
            LibraryTilingBox(navController)
        }
        item {
            AnimatedVisibility(!listCanvasSong.data.isNullOrEmpty()) {
                LibraryItem(
                    state =
                        LibraryItemState(
                            type = LibraryItemType.CanvasSong,
                            data = listCanvasSong.data ?: emptyList(),
                            isLoading = listCanvasSong is LocalResource.Loading,
                        ),
                    navController = navController,
                )
            }
        }
        item {
            LibraryItem(
                state =
                    LibraryItemState(
                        type =
                            LibraryItemType.YouTubePlaylist(loggedIn) {
                                viewModel.getYouTubePlaylist()
                            },
                        data = youTubePlaylist.data ?: emptyList(),
                        isLoading = youTubePlaylist is LocalResource.Loading,
                    ),
                navController = navController,
            )
        }
        item {
            LibraryItem(
                state =
                    LibraryItemState(
                        type =
                            LibraryItemType.LocalPlaylist { newTitle ->
                                viewModel.createPlaylist(newTitle)
                            },
                        data = yourLocalPlaylist.data ?: emptyList(),
                        isLoading = yourLocalPlaylist is LocalResource.Loading,
                    ),
                navController = navController,
            )
        }
        item {
            LibraryItem(
                state =
                    LibraryItemState(
                        type = LibraryItemType.FavoritePlaylist,
                        data = favoritePlaylist.data ?: emptyList(),
                        isLoading = favoritePlaylist is LocalResource.Loading,
                    ),
                navController = navController,
            )
        }
        item {
            LibraryItem(
                state =
                    LibraryItemState(
                        type = LibraryItemType.DownloadedPlaylist,
                        data = downloadedPlaylist.data ?: emptyList(),
                        isLoading = downloadedPlaylist is LocalResource.Loading,
                    ),
                navController = navController,
            )
        }
        item {
            LibraryItem(
                state =
                    LibraryItemState(
                        type = LibraryItemType.FavoritePodcasts,
                        data = favoritePodcasts.data ?: emptyList(),
                        isLoading = favoritePodcasts is LocalResource.Loading,
                    ),
                navController = navController,
            )
        }
        item {
            LibraryItem(
                state =
                    LibraryItemState(
                        type =
                            LibraryItemType.RecentlyAdded(
                                playingVideoId = nowPlaying,
                            ),
                        data = recentlyAdded.data ?: emptyList(),
                        isLoading = recentlyAdded is LocalResource.Loading,
                    ),
                navController = navController,
            )
        }
        item {
            EndOfPage()
        }
    }
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.library),
                style = typo.titleMedium,
            )
        },
    )
}