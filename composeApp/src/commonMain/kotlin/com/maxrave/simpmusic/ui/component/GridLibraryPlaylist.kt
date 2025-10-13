package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.PodcastsEntity
import com.maxrave.domain.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.utils.LocalResource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.isScrollingUp
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.LocalPlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PodcastDestination
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.ui.theme.white
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.create

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal inline fun <reified T> GridLibraryPlaylist(
    navController: NavController,
    contentPadding: PaddingValues,
    data: LocalResource<List<T>>,
    emptyText: StringResource,
    noinline onScrolling: (onTop: Boolean) -> Unit = { _ -> },
    noinline createNewPlaylist: (() -> Unit)? = null,
    noinline onReload: () -> Unit,
) {
    Logger.w("GridLibraryPlaylist", "Generic Type: ${T::class.java}")
    val state = rememberLazyGridState()
    val isScrollingUp by state.isScrollingUp()

    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    onScrolling.invoke(true)
                } else {
                    onScrolling.invoke(isScrollingUp)
                }
            }
    }
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
                        .padding(
                            top = contentPadding.calculateTopPadding(),
                        ),
                containerColor = PullToRefreshDefaults.indicatorContainerColor,
                color = PullToRefreshDefaults.indicatorColor,
                maxDistance = PullToRefreshDefaults.PositionalThreshold,
            )
        },
    ) {
        Crossfade(targetState = data) { data ->
            val list = (data as? LocalResource.Success)?.data ?: emptyList()
            if (data is LocalResource.Success && list.isNotEmpty() || createNewPlaylist != null) {
                LazyVerticalGrid(
                    columns = GridCells.FixedSize(size = 132.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    contentPadding = contentPadding,
                    state = state,
                ) {
                    if (createNewPlaylist != null) {
                        item {
                            Box(
                                modifier =
                                    Modifier.clickable {
                                        createNewPlaylist()
                                    },
                            ) {
                                Column(
                                    modifier =
                                        Modifier
                                            .padding(10.dp),
                                ) {
                                    Box(
                                        Modifier
                                            .size(132.dp)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .angledGradientBackground(
                                                colors =
                                                    listOf(
                                                        seed,
                                                        white.copy(alpha = 0.8f),
                                                    ),
                                                degrees = 45f,
                                            ),
                                        Alignment.Center,
                                    ) {
                                        Icon(
                                            modifier = Modifier.size(84.dp),
                                            imageVector = Icons.Rounded.Add,
                                            tint = white,
                                            contentDescription = null,
                                        )
                                    }
                                    Text(
                                        text = stringResource(Res.string.create),
                                        style = typo().titleSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier =
                                            Modifier
                                                .width(132.dp)
                                                .wrapContentHeight(align = Alignment.CenterVertically)
                                                .padding(top = 8.dp)
                                                .basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    animationMode = MarqueeAnimationMode.Immediately,
                                                ).focusable(),
                                    )
                                }
                            }
                        }
                    }
                    items(list) { item ->
                        if (item !is PlaylistType) {
                            return@items
                        }
                        HomeItemContentPlaylist(
                            onClick = {
                                when (item) {
                                    is LocalPlaylistEntity -> {
                                        navController.navigate(
                                            LocalPlaylistDestination(
                                                item.id,
                                            ),
                                        )
                                    }

                                    is PlaylistsResult -> {
                                        navController.navigate(
                                            PlaylistDestination(
                                                item.browseId,
                                                isYourYouTubePlaylist = true,
                                            ),
                                        )
                                    }

                                    is AlbumEntity -> {
                                        navController.navigate(
                                            AlbumDestination(
                                                item.browseId,
                                            ),
                                        )
                                    }

                                    is PlaylistEntity -> {
                                        navController.navigate(
                                            PlaylistDestination(
                                                item.id,
                                            ),
                                        )
                                    }

                                    is PodcastsEntity -> {
                                        navController.navigate(
                                            PodcastDestination(
                                                podcastId = item.podcastId,
                                            ),
                                        )
                                    }
                                }
                            },
                            data = item,
                            thumbSize = 132.dp,
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EndOfPage()
                    }
                }
            } else if (data is LocalResource.Loading) {
                CenterLoadingBox(
                    Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(emptyText),
                        style = typo().bodyMedium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}