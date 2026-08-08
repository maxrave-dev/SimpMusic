package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.spotify.SpotifyPlaylist
import com.maxrave.domain.utils.LocalResource
import com.maxrave.simpmusic.ui.navigation.destination.list.SpotifyPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.SpotifyLoginDestination
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.log_in_to_spotify
import simpmusic.composeapp.generated.resources.no_spotify_playlists

/**
 * Live grid of the logged-in user's Spotify playlists, mirroring [GridLibraryPlaylist]'s look
 * without pulling in its [com.maxrave.domain.data.type.PlaylistType]-specific navigation logic
 * (Spotify playlists aren't a [com.maxrave.domain.data.type.PlaylistType] - they aren't
 * persisted, see `SpotifyLibraryRepository`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlaylistGrid(
    navController: NavController,
    contentPadding: PaddingValues,
    isLoggedIn: Boolean,
    data: LocalResource<List<SpotifyPlaylist>>,
    onReload: () -> Unit,
) {
    if (!isLoggedIn) {
        Box(
            Modifier.fillMaxSize().padding(contentPadding).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { navController.navigate(SpotifyLoginDestination) }) {
                    Text(stringResource(Res.string.log_in_to_spotify))
                }
            }
        }
        return
    }

    val state = rememberLazyGridState()
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        onRefresh = onReload,
        isRefreshing = data is LocalResource.Loading,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = data is LocalResource.Loading,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = contentPadding.calculateTopPadding()),
                containerColor = PullToRefreshDefaults.indicatorContainerColor,
                color = PullToRefreshDefaults.indicatorColor,
                maxDistance = PullToRefreshDefaults.PositionalThreshold,
            )
        },
    ) {
        val list = (data as? LocalResource.Success)?.data ?: emptyList()
        if (data is LocalResource.Success && list.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.no_spotify_playlists),
                    style = typo().bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (data is LocalResource.Loading && list.isEmpty()) {
            CenterLoadingBox(Modifier.fillMaxSize())
        } else {
            LazyVerticalGrid(
                columns = GridCells.FixedSize(size = 132.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                contentPadding = contentPadding,
                state = state,
            ) {
                items(list, key = { it.id }) { playlist ->
                    Box(
                        modifier =
                            Modifier.clickable {
                                navController.navigate(SpotifyPlaylistDestination(playlist.id, playlist.name))
                            },
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            AsyncImage(
                                model =
                                    ImageRequest
                                        .Builder(LocalPlatformContext.current)
                                        .data(playlist.imageUrl)
                                        .crossfade(300)
                                        .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .aspectRatio(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp)),
                            )
                            Text(
                                text = playlist.name,
                                style = typo().titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(align = Alignment.CenterVertically)
                                        .padding(top = 8.dp)
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                            playlist.ownerName?.let { owner ->
                                Text(
                                    text = owner,
                                    style = typo().bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    EndOfPage()
                }
            }
        }
    }
}
