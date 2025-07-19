package com.maxrave.simpmusic.ui.screen.other

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants.IterateForever
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.android.material.snackbar.Snackbar
import com.kmpalette.rememberPaletteState
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.LoadingDialog
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.md_theme_dark_background
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ListState
import com.maxrave.simpmusic.viewModel.PlaylistUIEvent
import com.maxrave.simpmusic.viewModel.PlaylistUIState
import com.maxrave.simpmusic.viewModel.PlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@Composable
@UnstableApi
fun PlaylistScreen(
    viewModel: PlaylistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    playlistId: String,
    isYourYouTubePlaylist: Boolean,
    navController: NavController,
) {
    val context = LocalContext.current
    val tag = "PlaylistScreen"

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.downloading_animation),
    )

    val uiState by viewModel.uiState.collectAsState()
    val continuation by viewModel.continuation.collectAsState()
    val listColors by viewModel.listColors.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val liked by viewModel.liked.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val tracksListState by viewModel.tracksListState.collectAsState()

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        Log.d(tag, "uiState hash: ${uiState.hashCode()}")
        Log.d(tag, "uiState data: ${uiState.data}")
    }

    val shouldStartPaginate =
        remember {
            derivedStateOf {
                tracksListState != ListState.PAGINATION_EXHAUST &&
                    (
                        lazyState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: -9
                    ) >= (lazyState.layoutInfo.totalItemsCount - 6)
            }
        }

    LaunchedEffect(key1 = shouldStartPaginate.value) {
        Log.d(tag, "shouldStartPaginate: ${shouldStartPaginate.value}")
        Log.d(tag, "tracksListState: $tracksListState")
        Log.d(tag, "Continuation: $continuation")
        if (shouldStartPaginate.value && tracksListState == ListState.IDLE) {
            viewModel.getContinuationTrack(
                playlistId,
                continuation,
            )
        }
    }

    val playingTrack by sharedViewModel.nowPlayingState
        .mapLatest {
            it?.songEntity
        }.collectAsState(initial = null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsState(initial = false)

    var currentItem by remember {
        mutableStateOf<Track?>(null)
    }

    var itemBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var playlistBottomSheetShow by remember {
        mutableStateOf(false)
    }

    val onPlaylistItemClick: (videoId: String) -> Unit = { videoId ->
        viewModel.onUIEvent(
            PlaylistUIEvent.ItemClick(
                videoId = videoId,
            ),
        )
    }
    val onItemMoreClick: (videoId: String) -> Unit = { videoId ->
        currentItem = tracks.firstOrNull { it.videoId == videoId }
        if (currentItem != null) {
            itemBottomSheetShow = true
        }
    }
    val onPlaylistMoreClick: () -> Unit = {
        playlistBottomSheetShow = true
    }

    LaunchedEffect(key1 = playlistId) {
        if (playlistId != uiState.data?.id) {
            Log.w(tag, "new id: $playlistId")
            viewModel.getData(playlistId)
        }
    }
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

    // Loading dialog
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsState()
    if (showLoadingDialog.first) {
        LoadingDialog(
            true,
            showLoadingDialog.second,
        )
    }
//    Box {
    Crossfade(
        targetState = uiState,
    ) { state ->
        Log.w(tag, "State hash: ${state.hashCode()}")
        when (state) {
            is PlaylistUIState.Success -> {
                val data = state.data
                Log.d(tag, "data: $data")
                if (data == null) return@Crossfade
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
                                            .aspectRatio(1f)
                                            .clip(
                                                RoundedCornerShape(8.dp),
                                            ).angledGradientBackground(listColors, 25f),
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
                                        resId = R.drawable.baseline_arrow_back_ios_new_24,
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
                                                .Builder(LocalContext.current)
                                                .data(data.thumbnail)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(data.thumbnail)
                                                .crossfade(true)
                                                .build(),
                                        placeholder = painterResource(R.drawable.holder),
                                        error = painterResource(R.drawable.holder),
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
                                                text = data.title,
                                                style = typo.titleLarge,
                                                color = Color.White,
                                                maxLines = 2,
                                            )
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                            ) {
                                                CompositionLocalProvider(
                                                    LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                                                ) {
                                                    TextButton(
                                                        modifier =
                                                            Modifier
                                                                .wrapContentHeight()
                                                                .defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
                                                        contentPadding = PaddingValues(vertical = 1.dp),
                                                        onClick = {
                                                            if (data.author.id.isNotEmpty()) {
                                                                navController.navigate(
                                                                    ArtistDestination(
                                                                        data.author.id,
                                                                    ),
                                                                )
                                                            }
                                                        },
                                                    ) {
                                                        Text(
                                                            text = data.author.name,
                                                            style = typo.labelSmall,
                                                            color = Color.White,
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.size(4.dp))
                                                Text(
                                                    text = "${if (data.isRadio) {
                                                        stringResource(R.string.radio)
                                                    } else {
                                                        stringResource(R.string.playlist)
                                                    }} • ${data.year}",
                                                    style = typo.bodyMedium,
                                                )
                                            }
                                            Row(
                                                modifier =
                                                    Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                RippleIconButton(
                                                    resId = R.drawable.baseline_play_circle_24,
                                                    fillMaxSize = true,
                                                    modifier = Modifier.size(36.dp),
                                                ) {
                                                    viewModel.onUIEvent(PlaylistUIEvent.PlayAll)
                                                }
                                                if (!data.isRadio) {
                                                    HeartCheckBox(
                                                        size = 32,
                                                        checked = liked,
                                                        onStateChange = {
                                                            viewModel.onUIEvent(PlaylistUIEvent.Favorite)
                                                        },
                                                    )
                                                    Crossfade(targetState = downloadState) {
                                                        when (it) {
                                                            DownloadState.STATE_DOWNLOADED -> {
                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .size(36.dp)
                                                                            .clip(
                                                                                CircleShape,
                                                                            ).clickable {
                                                                                Toast
                                                                                    .makeText(
                                                                                        context,
                                                                                        context.getString(R.string.downloaded),
                                                                                        Toast.LENGTH_SHORT,
                                                                                    ).show()
                                                                            },
                                                                ) {
                                                                    Icon(
                                                                        painter = painterResource(id = R.drawable.baseline_downloaded),
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
                                                                                Toast
                                                                                    .makeText(
                                                                                        context,
                                                                                        context.getString(R.string.downloading),
                                                                                        Toast.LENGTH_SHORT,
                                                                                    ).show()
                                                                            },
                                                                ) {
                                                                    LottieAnimation(
                                                                        composition,
                                                                        iterations = IterateForever,
                                                                        modifier = Modifier.fillMaxSize(),
                                                                    )
                                                                }
                                                            }

                                                            else -> {
                                                                RippleIconButton(
                                                                    fillMaxSize = true,
                                                                    resId = R.drawable.download_button,
                                                                    modifier = Modifier.size(36.dp),
                                                                ) {
                                                                    Log.w("PlaylistScreen", "downloadState: $downloadState")
                                                                    viewModel.onUIEvent(PlaylistUIEvent.Download)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.weight(1f))
                                                if (!data.isRadio) {
                                                    RippleIconButton(
                                                        modifier =
                                                            Modifier.size(36.dp),
                                                        resId = R.drawable.baseline_sensors_24,
                                                        fillMaxSize = true,
                                                    ) {
                                                        viewModel.onUIEvent(PlaylistUIEvent.StartRadio)
                                                    }
                                                    Spacer(Modifier.size(5.dp))
                                                    RippleIconButton(
                                                        modifier =
                                                            Modifier.size(36.dp),
                                                        resId = R.drawable.baseline_shuffle_24,
                                                        fillMaxSize = true,
                                                    ) {
                                                        viewModel.onUIEvent(PlaylistUIEvent.Shuffle)
                                                    }
                                                    Spacer(Modifier.size(5.dp))
                                                }
                                                RippleIconButton(
                                                    modifier =
                                                        Modifier.size(36.dp),
                                                    resId = R.drawable.baseline_more_vert_24,
                                                    fillMaxSize = true,
                                                ) {
                                                    onPlaylistMoreClick()
                                                }
                                            }
                                            val uriHandler = LocalUriHandler.current
                                            DescriptionView(
                                                modifier =
                                                    Modifier
                                                        .padding(
                                                            top = 8.dp,
                                                        ),
                                                text =
                                                    state.data.description.let {
                                                        if (!it.isNullOrEmpty()) {
                                                            it
                                                        } else {
                                                            stringResource(R.string.no_description)
                                                        }
                                                    },
                                                limitLine = 3,
                                                onTimeClicked = {},
                                                onURLClicked = { url ->
                                                    uriHandler.openUri(url)
                                                },
                                            )
                                            Text(
                                                text =
                                                    if (data.isRadio) {
                                                        stringResource(R.string.unlimited)
                                                    } else {
                                                        stringResource(
                                                            id = R.string.album_length,
                                                            (data.trackCount).toString(),
                                                            "",
                                                        )
                                                    },
                                                color = Color.White,
                                                style = typo.bodyMedium,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    items(count = tracks.size, key = { index ->
                        val item = tracks.getOrNull(index)
                        (item?.videoId ?: "") + "item_$index"
                    }) { index ->
                        val item = tracks.getOrNull(index)
                        if (item != null) {
                            if (playingTrack?.videoId == item.videoId && isPlaying) {
                                SongFullWidthItems(
                                    isPlaying = true,
                                    track = item,
                                    onMoreClickListener = { onItemMoreClick(it) },
                                    onClickListener = {
                                        Log.w("PlaylistScreen", "index: $index")
                                        onPlaylistItemClick(it)
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            } else {
                                SongFullWidthItems(
                                    isPlaying = false,
                                    track = item,
                                    onMoreClickListener = { onItemMoreClick(it) },
                                    onClickListener = {
                                        Log.w("PlaylistScreen", "index: $index")
                                        onPlaylistItemClick(it)
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                    when (tracksListState) {
                        ListState.IDLE -> {
                            // DO NOTHING
                            item {
                                EndOfPage()
                            }
                        }
                        ListState.LOADING, ListState.PAGINATING -> {
                            item {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(64.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CenterLoadingBox(
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                            item {
                                EndOfPage()
                            }
                        }
                        ListState.ERROR -> {
                            item {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(64.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.error),
                                        style = typo.bodyMedium,
                                    )
                                }
                            }
                            item {
                                EndOfPage()
                            }
                        }
                        ListState.PAGINATION_EXHAUST -> {
                            item {
                                EndOfPage()
                            }
                        }
                    }
                }

                if (itemBottomSheetShow && currentItem != null) {
                    val track = currentItem?.toSongEntity() ?: return@Crossfade
                    NowPlayingBottomSheet(
                        onDismiss = {
                            itemBottomSheetShow = false
                            currentItem = null
                        },
                        navController = navController,
                        song = track,
                    )
                }
                if (playlistBottomSheetShow) {
                    Log.w("PlaylistScreen", "PlaylistBottomSheet")
                    val addToQueue = {
                        viewModel.getFullTracks { track ->
                            sharedViewModel.addListToQueue(
                                track.toCollection(arrayListOf()),
                            )
                        }
                    }
                    PlaylistBottomSheet(
                        onDismiss = { playlistBottomSheetShow = false },
                        playlistId = data.id,
                        isYourYouTubePlaylist = isYourYouTubePlaylist && !data.isRadio,
                        onSaveToLocal = {
                            viewModel.getFullTracks { track ->
                                viewModel.saveToLocal(track)
                            }
                        },
                        onAddToQueue = if (data.isRadio) null else addToQueue,
                    )
                }
                AnimatedVisibility(
                    visible = shouldHideTopBar,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = data.title,
                                style = typo.titleMedium,
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
                                    R.drawable.baseline_arrow_back_ios_new_24,
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
                        modifier = Modifier.angledGradientBackground(listColors, 90f),
                    )
                }
            }

            is PlaylistUIState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CenterLoadingBox(
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            is PlaylistUIState.Error -> {
                Snackbar
                    .make(
                        context,
                        LocalView.current,
                        "Error: ${state.message}",
                        Snackbar.LENGTH_SHORT,
                    ).show()
                navController.navigateUp()
            }
        }
    }
}