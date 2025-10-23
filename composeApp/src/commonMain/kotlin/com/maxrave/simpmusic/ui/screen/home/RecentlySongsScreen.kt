package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.AlbumEntity
import com.maxrave.domain.data.entities.ArtistEntity
import com.maxrave.domain.data.entities.PlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.mediaservice.handler.PlaylistType
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.ui.component.ArtistFullWidthItems
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.PlaylistFullWidthItems
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.PlaylistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.RecentlySongsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.recently_added

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun RecentlySongsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: RecentlySongsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val hazeState = rememberHazeState()

    val recentlyItems = viewModel.recentlySongs.collectAsLazyPagingItems()
    val playingTrack by sharedViewModel.nowPlayingState.map { it?.songEntity }.collectAsState(initial = null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsState(initial = false)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
        ) {
            item {
                Spacer(
                    Modifier.size(
                        innerPadding.calculateTopPadding() + 64.dp,
                    ),
                )
            }

            items(
                count = recentlyItems.itemCount,
                key = { index ->
                    val item = recentlyItems[index]
                    when (item) {
                        is SongEntity -> "song_${item.videoId}_$index"
                        is AlbumEntity -> "album_${item.browseId}_$index"
                        is PlaylistEntity -> "playlist_${item.id}_$index"
                        is ArtistEntity -> "artist_${item.channelId}_$index"
                        else -> index.toString()
                    }
                },
            ) { index ->
                val item = recentlyItems[index]
                when (item) {
                    is SongEntity -> {
                        val recentlyAddedString = stringResource(Res.string.recently_added)
                        SongFullWidthItems(
                            songEntity = item,
                            isPlaying = playingTrack?.videoId == item.videoId && isPlaying,
                            onClickListener = { videoId ->
                                val firstQueue = item.toTrack()
                                viewModel.setQueueData(
                                    QueueData.Data(
                                        listTracks = arrayListOf(firstQueue),
                                        firstPlayedTrack = firstQueue,
                                        playlistId = "RDAMVM$videoId",
                                        playlistName = recentlyAddedString,
                                        playlistType = PlaylistType.RADIO,
                                        continuation = null,
                                    ),
                                )
                                viewModel.loadMediaItem(
                                    firstQueue,
                                    Config.SONG_CLICK,
                                    0,
                                )
                            },
                            onAddToQueue = {
                                sharedViewModel.addListToQueue(
                                    arrayListOf(item.toTrack()),
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    is AlbumEntity -> {
                        PlaylistFullWidthItems(
                            data = item,
                            onClickListener = {
                                navController.navigate(
                                    AlbumDestination(
                                        item.browseId,
                                    ),
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    is PlaylistEntity -> {
                        PlaylistFullWidthItems(
                            data = item,
                            onClickListener = {
                                navController.navigate(
                                    PlaylistDestination(
                                        item.id,
                                    ),
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    is ArtistEntity -> {
                        ArtistFullWidthItems(
                            data = item,
                            onClickListener = {
                                navController.navigate(
                                    ArtistDestination(
                                        item.channelId,
                                    ),
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            // Loading state
            recentlyItems.apply {
                when {
                    loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading -> {
                        item {
                            CenterLoadingBox(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                            )
                        }
                    }

                    loadState.refresh is LoadState.Error || loadState.append is LoadState.Error -> {
                        item {
                            val error =
                                (loadState.refresh as? LoadState.Error)?.error?.message
                                    ?: (loadState.append as? LoadState.Error)?.error?.message
                                    ?: stringResource(Res.string.error)
                            LaunchedEffect(error) {
                                viewModel.makeToast(error)
                            }
                        }
                    }
                }
            }

            item {
                EndOfPage()
            }
        }

        // Top App Bar with haze effect
        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    },
            title = {
                Text(
                    text = stringResource(Res.string.recently_added),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        Res.drawable.baseline_arrow_back_ios_new_24,
                        Modifier.size(32.dp),
                        true,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )
    }
}