package com.maxrave.simpmusic.ui.screen.library

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.common.LibraryChipType
import com.maxrave.common.R
import com.maxrave.domain.utils.LocalResource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.ui.component.Chip
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.component.LibraryItem
import com.maxrave.simpmusic.ui.component.LibraryItemState
import com.maxrave.simpmusic.ui.component.LibraryItemType
import com.maxrave.simpmusic.ui.component.LibraryTilingBox
import com.maxrave.simpmusic.ui.theme.transparent
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun LibraryScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
) {
    val density = LocalDensity.current
    val context = LocalContext.current

    val loggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()
    val youTubePlaylist by viewModel.youTubePlaylist.collectAsStateWithLifecycle()
    val listCanvasSong by viewModel.listCanvasSong.collectAsStateWithLifecycle()
    val yourLocalPlaylist by viewModel.yourLocalPlaylist.collectAsStateWithLifecycle()
    val favoritePlaylist by viewModel.favoritePlaylist.collectAsStateWithLifecycle()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsStateWithLifecycle()
    val favoritePodcasts by viewModel.favoritePodcasts.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    var topAppBarHeight by remember {
        mutableStateOf(0.dp)
    }
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(nowPlaying) {
        Logger.w("LibraryScreen", "Check nowPlaying: $nowPlaying")
        viewModel.getRecentlyAdded()
    }

    val chipRowState = rememberScrollState()
    var currentFilter by rememberSaveable {
        mutableStateOf(LibraryChipType.YOUR_LIBRARY)
    }

    LaunchedEffect(currentFilter) {
        when (currentFilter) {
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                if (youTubePlaylist.data.isNullOrEmpty()) {
                    viewModel.getYouTubePlaylist()
                }
            }
            LibraryChipType.YOUR_LIBRARY -> {
                viewModel.getCanvasSong()
                viewModel.getRecentlyAdded()
            }
            LibraryChipType.LOCAL_PLAYLIST -> {
                viewModel.getLocalPlaylist()
            }
            LibraryChipType.FAVORITE_PLAYLIST -> {
                viewModel.getPlaylistFavorite()
            }
            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                viewModel.getDownloadedPlaylist()
            }
            LibraryChipType.FAVORITE_PODCAST -> {
                viewModel.getFavoritePodcasts()
            }
        }
    }

    Crossfade(
        modifier = Modifier.hazeSource(hazeState),
        targetState = currentFilter,
    ) { filter ->
        when (filter) {
            LibraryChipType.YOUR_LIBRARY -> {
                LazyColumn(
                    contentPadding =
                        innerPadding.copy(
                            top = topAppBarHeight,
                        ),
                ) {
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
            }
            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    youTubePlaylist,
                ) {
                    viewModel.getYouTubePlaylist()
                }
            }
            LibraryChipType.LOCAL_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    yourLocalPlaylist,
                    createNewPlaylist = {
                        showAddSheet = true
                    },
                ) {
                    viewModel.getLocalPlaylist()
                }
            }
            LibraryChipType.FAVORITE_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    favoritePlaylist,
                ) {
                    viewModel.getPlaylistFavorite()
                }
            }
            LibraryChipType.DOWNLOADED_PLAYLIST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    downloadedPlaylist,
                ) {
                    viewModel.getDownloadedPlaylist()
                }
            }
            LibraryChipType.FAVORITE_PODCAST -> {
                GridLibraryPlaylist(
                    navController,
                    innerPadding.copy(top = topAppBarHeight),
                    favoritePodcasts,
                ) {
                    viewModel.getFavoritePodcasts()
                }
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    if (showAddSheet) {
        var newTitle by remember { mutableStateOf("") }
        val showAddSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            {
                coroutineScope.launch {
                    showAddSheetState.hide()
                    showAddSheet = false
                }
            }
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = showAddSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = Color(0xFF474545),
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(id = R.string.playlist_name))
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    TextButton(
                        onClick = {
                            if (newTitle.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.playlist_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.createPlaylist(newTitle)
                                hideEditTitleBottomSheet()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(id = R.string.create))
                    }
                }
            }
        }
    }
    Column(
        Modifier
            .background(transparent)
            .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                blurEnabled = true
            }.onGloballyPositioned { coordinates ->
                topAppBarHeight = with(density) { coordinates.size.height.toDp() }
            },
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.library),
                    style = typo.titleMedium,
                    color = Color.White,
                )
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )
        Row(
            modifier =
                Modifier
                    .horizontalScroll(chipRowState)
                    .padding(horizontal = 15.dp)
                    .padding(bottom = 8.dp)
                    .background(Color.Transparent),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LibraryChipType.entries.forEach { type ->
                if (type == LibraryChipType.YOUTUBE_MUSIC_PLAYLIST && !loggedIn) {
                    return@forEach
                }
                Chip(
                    isAnimated = false,
                    isSelected = type == currentFilter,
                    text =
                        when (type) {
                            LibraryChipType.YOUR_LIBRARY -> "Your Library"
                            LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> stringResource(R.string.your_youtube_playlists)
                            LibraryChipType.LOCAL_PLAYLIST -> stringResource(R.string.your_playlists)
                            LibraryChipType.FAVORITE_PLAYLIST -> stringResource(R.string.favorite_playlists)
                            LibraryChipType.DOWNLOADED_PLAYLIST -> stringResource(R.string.downloaded_playlists)
                            LibraryChipType.FAVORITE_PODCAST -> stringResource(R.string.favorite_podcasts)
                        },
                ) {
                    currentFilter = type
                }
            }
        }
    }
}