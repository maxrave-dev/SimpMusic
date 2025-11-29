package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.kmpalette.rememberPaletteState
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.md_theme_dark_background
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.AlbumViewModel
import com.maxrave.simpmusic.viewModel.LocalPlaylistState
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.album
import simpmusic.composeapp.generated.resources.album_length
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import simpmusic.composeapp.generated.resources.baseline_downloaded
import simpmusic.composeapp.generated.resources.baseline_pause_circle_24
import simpmusic.composeapp.generated.resources.baseline_play_circle_24
import simpmusic.composeapp.generated.resources.baseline_shuffle_24
import simpmusic.composeapp.generated.resources.download_button
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.holder
import simpmusic.composeapp.generated.resources.no_description
import simpmusic.composeapp.generated.resources.other_version
import simpmusic.composeapp.generated.resources.year_and_category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    browseId: String,
    navController: NavController,
    viewModel: AlbumViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val uriHandler = LocalUriHandler.current

    val playingVideoId by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()

    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingPlaylistId by remember {
        derivedStateOf {
            queueData?.data?.playlistId
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var chosenSong: Track? by remember { mutableStateOf(null) }

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/downloading_animation.json").decodeToString(),
        )
    }

    LaunchedEffect(browseId) {
        viewModel.updateBrowseId(browseId)
    }

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    val paletteState = rememberPaletteState()
    var bitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null) {
            paletteState.generate(bm)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                viewModel.setBrush(listOf(it.getColorFromPalette(), md_theme_dark_background))
            }
    }

    Crossfade(uiState.loadState) {
        when (it) {
            LocalPlaylistState.PlaylistLoadState.Success -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(Color.Black),
                    state = lazyState,
                ) {
                    item(contentType = "header") {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .background(Color.Transparent),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(260.dp)
                                            .clip(
                                                RoundedCornerShape(8.dp),
                                            ).angledGradientBackground(uiState.colors, 25f),
                                )
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                brush =
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            Color.Transparent,
                                                            Color(0x75000000),
                                                            Color.Black,
                                                        ),
                                                    ),
                                            ),
                                )
                            }
                            Column(
                                Modifier
                                    .background(Color.Transparent),
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .wrapContentWidth()
                                            .padding(16.dp)
                                            .windowInsetsPadding(WindowInsets.statusBars),
                                ) {
                                    RippleIconButton(
                                        resId = Res.drawable.baseline_arrow_back_ios_new_24,
                                    ) {
                                        navController.navigateUp()
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(LocalPlatformContext.current)
                                                .data(uiState.thumbnail)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(uiState.thumbnail)
                                                .crossfade(true)
                                                .build(),
                                        placeholder = painterResource(Res.drawable.holder),
                                        error = painterResource(Res.drawable.holder),
                                        contentDescription = null,
                                        contentScale = ContentScale.FillHeight,
                                        onSuccess = {
                                            bitmap =
                                                it.result.image
                                                    .toBitmap()
                                                    .asImageBitmap()
                                        },
                                        modifier =
                                            Modifier
                                                .height(250.dp)
                                                .wrapContentWidth()
                                                .align(Alignment.CenterHorizontally)
                                                .clip(
                                                    RoundedCornerShape(8.dp),
                                                ),
                                    )
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight(),
                                    ) {
                                        Column(Modifier.padding(horizontal = 32.dp)) {
                                            Spacer(modifier = Modifier.size(25.dp))
                                            Text(
                                                text = uiState.title,
                                                style = typo().titleLarge,
                                                color = Color.White,
                                                maxLines = 2,
                                            )
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                            ) {
                                                // Author clickable
                                                Text(
                                                    text = uiState.artist.name,
                                                    style = typo().titleSmall,
                                                    color = Color.White,
                                                    modifier =
                                                        Modifier.clickable {
                                                            uiState.artist.id?.let { channelId ->
                                                                navController.navigate(
                                                                    ArtistDestination(
                                                                        channelId = channelId,
                                                                    ),
                                                                )
                                                            }
                                                        },
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text =
                                                        stringResource(
                                                            Res.string.year_and_category,
                                                            uiState.year,
                                                            stringResource(Res.string.album),
                                                        ),
                                                    style = typo().bodyMedium,
                                                    color = Color(0xC4FFFFFF),
                                                )
                                            }
                                            Row(
                                                modifier =
                                                    Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Crossfade(
                                                    playingVideoId.isNotEmpty() &&
                                                        playingPlaylistId == browseId.replaceFirst("VL", ""),
                                                ) { isThisPlaying ->
                                                    if (isThisPlaying) {
                                                        RippleIconButton(
                                                            resId = Res.drawable.baseline_pause_circle_24,
                                                            fillMaxSize = true,
                                                            tint = seed,
                                                            modifier = Modifier.size(48.dp),
                                                        ) {
                                                            sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                                        }
                                                    } else {
                                                        RippleIconButton(
                                                            resId = Res.drawable.baseline_play_circle_24,
                                                            fillMaxSize = true,
                                                            tint = seed,
                                                            modifier = Modifier.size(48.dp),
                                                        ) {
                                                            viewModel.playTrack(uiState.listTrack.firstOrNull() ?: return@RippleIconButton)
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.size(5.dp))
                                                Crossfade(targetState = uiState.downloadState) {
                                                    when (it) {
                                                        DownloadState.STATE_DOWNLOADED -> {
                                                            Box(
                                                                modifier =
                                                                    Modifier
                                                                        .size(36.dp)
                                                                        .clip(
                                                                            CircleShape,
                                                                        ).clickable {
                                                                            viewModel.makeToast(
                                                                                runBlocking {
                                                                                    getString(Res.string.downloaded)
                                                                                },
                                                                            )
                                                                        },
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(Res.drawable.baseline_downloaded),
                                                                    tint = Color(0xFF00A0CB),
                                                                    contentDescription = "",
                                                                    modifier =
                                                                        Modifier
                                                                            .size(36.dp)
                                                                            .padding(2.dp),
                                                                )
                                                            }
                                                        }

                                                        DownloadState.STATE_DOWNLOADING -> {
                                                            Box(
                                                                modifier =
                                                                    Modifier
                                                                        .size(36.dp)
                                                                        .clip(
                                                                            CircleShape,
                                                                        ).clickable {
                                                                            viewModel.makeToast(
                                                                                runBlocking {
                                                                                    getString(Res.string.downloading)
                                                                                },
                                                                            )
                                                                        },
                                                            ) {
                                                                Image(
                                                                    painter =
                                                                        rememberLottiePainter(
                                                                            composition = composition,
                                                                            iterations = Compottie.IterateForever,
                                                                        ),
                                                                    contentDescription = "Lottie animation",
                                                                    modifier = Modifier.fillMaxSize(),
                                                                )
                                                            }
                                                        }

                                                        else -> {
                                                            RippleIconButton(
                                                                fillMaxSize = true,
                                                                resId = Res.drawable.download_button,
                                                                modifier = Modifier.size(36.dp),
                                                            ) {
                                                                viewModel.downloadFullAlbum()
                                                            }
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.size(5.dp))
                                                HeartCheckBox(
                                                    size = 36,
                                                    checked = uiState.liked,
                                                    onStateChange = {
                                                        viewModel.setAlbumLike()
                                                    },
                                                )
                                                Spacer(Modifier.weight(1f))
                                                Spacer(Modifier.size(5.dp))
                                                RippleIconButton(
                                                    modifier =
                                                        Modifier.size(36.dp),
                                                    resId = Res.drawable.baseline_shuffle_24,
                                                    fillMaxSize = true,
                                                ) {
                                                    viewModel.shuffle()
                                                }
                                            }
                                            DescriptionView(
                                                text =
                                                    uiState.description?.let {
                                                        it.ifEmpty { null }
                                                    } ?: stringResource(Res.string.no_description),
                                                onTimeClicked = { raw ->
                                                    // Don't handle time click
                                                },
                                                onURLClicked = { url ->
                                                    uriHandler.openUri(
                                                        url,
                                                    )
                                                },
                                                modifier = Modifier.padding(vertical = 8.dp),
                                            )
                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.album_length,
                                                        (uiState.trackCount).toString(),
                                                        uiState.length,
                                                    ),
                                                color = Color.White,
                                                style = typo().bodyMedium,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    items(count = uiState.trackCount, key = { index ->
                        val item = uiState.listTrack.getOrNull(index)
                        item?.videoId + "item_$index"
                    }) { index ->
                        val item = uiState.listTrack.getOrNull(index)
                        if (item != null) {
                            SongFullWidthItems(
                                isPlaying = item.videoId == playingVideoId,
                                index = index,
                                track = item,
                                onMoreClickListener = {
                                    chosenSong = item
                                    showBottomSheet = true
                                },
                                onClickListener = {
                                    viewModel.playTrack(item)
                                },
                                onAddToQueue = {
                                    sharedViewModel.addListToQueue(
                                        arrayListOf(item),
                                    )
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    item(contentType = "other_version") {
                        AnimatedVisibility(uiState.otherVersion.isNotEmpty()) {
                            Column {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = stringResource(Res.string.other_version),
                                    style = typo().labelMedium,
                                    modifier =
                                        Modifier.padding(
                                            horizontal = 24.dp,
                                            vertical = 8.dp,
                                        ),
                                )
                                LazyRow(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                ) {
                                    items(uiState.otherVersion) { album ->
                                        HomeItemContentPlaylist(
                                            onClick = {
                                                navController.navigate(
                                                    AlbumDestination(
                                                        browseId = album.browseId,
                                                    ),
                                                )
                                            },
                                            data = album,
                                            thumbSize = 180.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        EndOfPage()
                    }
                }
                AnimatedVisibility(
                    visible = shouldHideTopBar,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = uiState.title,
                                style = typo().titleMedium,
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(
                                            align = Alignment.CenterVertically,
                                        ).basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                        },
                        navigationIcon = {
                            Box(Modifier.padding(horizontal = 5.dp)) {
                                RippleIconButton(
                                    Res.drawable.baseline_arrow_back_ios_new_24,
                                    Modifier
                                        .size(32.dp),
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
                        modifier = Modifier.angledGradientBackground(uiState.colors, 90f),
                    )
                }
                if (showBottomSheet) {
                    NowPlayingBottomSheet(
                        onDismiss = {
                            showBottomSheet = false
                            chosenSong = null
                        },
                        navController = navController,
                        song = chosenSong?.toSongEntity(),
                    )
                }
            }

            LocalPlaylistState.PlaylistLoadState.Error -> {
                navController.navigateUp()
            }

            LocalPlaylistState.PlaylistLoadState.Loading -> {
                CenterLoadingBox(
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}