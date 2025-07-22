package com.maxrave.simpmusic.ui.screen.home

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.PlaylistType
import com.maxrave.simpmusic.service.QueueData
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
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@UnstableApi
@Composable
fun RecentlySongsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: RecentlySongsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val context = LocalContext.current
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
                        SongFullWidthItems(
                            songEntity = item,
                            isPlaying = playingTrack?.videoId == item.videoId && isPlaying,
                            onClickListener = { videoId ->
                                val firstQueue = item.toTrack()
                                viewModel.setQueueData(
                                    QueueData(
                                        listTracks = arrayListOf(firstQueue),
                                        firstPlayedTrack = firstQueue,
                                        playlistId = "RDAMVM$videoId",
                                        playlistName = context.getString(R.string.recently_added),
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
                                    ?: context.getString(R.string.error)
                            LaunchedEffect(error) {
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
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
                    text = stringResource(id = R.string.recently_added),
                    style = typo.titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        R.drawable.baseline_arrow_back_ios_new_24,
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