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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
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
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.getStringBlocking
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
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ListState
import com.maxrave.simpmusic.viewModel.PlaylistUIEvent
import com.maxrave.simpmusic.viewModel.PlaylistUIState
import com.maxrave.simpmusic.viewModel.PlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.album_length
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import simpmusic.composeapp.generated.resources.baseline_downloaded
import simpmusic.composeapp.generated.resources.baseline_more_vert_24
import simpmusic.composeapp.generated.resources.baseline_pause_circle_24
import simpmusic.composeapp.generated.resources.baseline_play_circle_24
import simpmusic.composeapp.generated.resources.baseline_sensors_24
import simpmusic.composeapp.generated.resources.baseline_shuffle_24
import simpmusic.composeapp.generated.resources.download_button
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.holder
import simpmusic.composeapp.generated.resources.no_description
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.search
import simpmusic.composeapp.generated.resources.unlimited

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    playlistId: String,
    isYourYouTubePlaylist: Boolean,
    navController: NavController,
) {
    val tag = "PlaylistScreen"

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/downloading_animation.json").decodeToString(),
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()
    val listColors by viewModel.listColors.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val liked by viewModel.liked.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val tracksListState by viewModel.tracksListState.collectAsStateWithLifecycle()

    var showSearchBar by rememberSaveable { mutableStateOf(false) }

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val filteredTrack by remember {
        derivedStateOf {
            if (query.isEmpty() || !showSearchBar) {
                tracks
            } else {
                tracks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.artists?.joinToString(", ")?.contains(query, ignoreCase = true) == true
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        Logger.d(tag, "uiState hash: ${uiState.hashCode()}")
        Logger.d(tag, "uiState data: ${uiState.data}")
    }

    LaunchedEffect(showSearchBar) {
        if (showSearchBar) {
            viewModel.getFullTracks {}
            lazyState.animateScrollToItem(0)
        }
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
        Logger.d(tag, "shouldStartPaginate: ${shouldStartPaginate.value}")
        Logger.d(tag, "tracksListState: $tracksListState")
        Logger.d(tag, "Continuation: $continuation")
        if (shouldStartPaginate.value && tracksListState == ListState.IDLE) {
            viewModel.getContinuationTrack(
                playlistId,
                continuation,
            )
        }
    }

    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingPlaylistId by remember {
        derivedStateOf {
            queueData?.data?.playlistId
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
            Logger.w(tag, "new id: $playlistId")
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
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()
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
        Logger.w(tag, "State hash: ${state.hashCode()}")
        when (state) {
            is PlaylistUIState.Success -> {
                val data = state.data
                Logger.d(tag, "data: $data")
                if (data == null) return@Crossfade
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(Color.Black),
                    state = lazyState,
                ) {
                    if (!showSearchBar) {
                        item(contentType = "header") {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .background(Color.Transparent)
                                        .animateItem(),
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
                                            resId = Res.drawable.baseline_arrow_back_ios_new_24,
                                        ) {
                                            navController.navigateUp()
                                        }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                showSearchBar = !showSearchBar
                                            },
                                        ) {
                                            Icon(Icons.Rounded.Search, null, tint = Color.White)
                                        }
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.Start,
                                    ) {
                                        AsyncImage(
                                            model =
                                                ImageRequest
                                                    .Builder(LocalPlatformContext.current)
                                                    .data(data.thumbnail)
                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                    .diskCacheKey(data.thumbnail)
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
                                                    text = data.title,
                                                    style = typo().titleMedium,
                                                    color = Color.White,
                                                    maxLines = 2,
                                                )
                                                Column(
                                                    modifier = Modifier.padding(vertical = 4.dp),
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
                                                                style = typo().labelSmall,
                                                                color = Color.White,
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.size(4.dp))
                                                    Text(
                                                        text = "${
                                                            if (data.isRadio) {
                                                                stringResource(Res.string.radio)
                                                            } else {
                                                                stringResource(Res.string.playlist)
                                                            }
                                                        } • ${data.year}",
                                                        style = typo().bodyMedium,
                                                    )
                                                }
                                                Row(
                                                    modifier =
                                                        Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Crossfade(isPlaying && playingPlaylistId == data.id) { isThisPlaying ->
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
                                                                viewModel.onUIEvent(PlaylistUIEvent.PlayAll)
                                                            }
                                                        }
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
                                                                                    viewModel.makeToast(getStringBlocking(Res.string.downloaded))
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
                                                                                    viewModel.makeToast(getStringBlocking(Res.string.downloading))
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
                                                                        Logger.w("PlaylistScreen", "downloadState: $downloadState")
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
                                                            resId = Res.drawable.baseline_sensors_24,
                                                            fillMaxSize = true,
                                                        ) {
                                                            viewModel.onUIEvent(PlaylistUIEvent.StartRadio)
                                                        }
                                                        Spacer(Modifier.size(5.dp))
                                                        RippleIconButton(
                                                            modifier =
                                                                Modifier.size(36.dp),
                                                            resId = Res.drawable.baseline_shuffle_24,
                                                            fillMaxSize = true,
                                                        ) {
                                                            viewModel.onUIEvent(PlaylistUIEvent.Shuffle)
                                                        }
                                                        Spacer(Modifier.size(5.dp))
                                                    }
                                                    RippleIconButton(
                                                        modifier =
                                                            Modifier.size(36.dp),
                                                        resId = Res.drawable.baseline_more_vert_24,
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
                                                                stringResource(Res.string.no_description)
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
                                                            stringResource(Res.string.unlimited)
                                                        } else {
                                                            stringResource(
                                                                Res.string.album_length,
                                                                (data.trackCount).toString(),
                                                                "",
                                                            )
                                                        },
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
                    } else {
                        stickyHeader {
                            Box(Modifier.background(Color.Black)) {
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
                                    SearchBar(
                                        modifier =
                                            Modifier
                                                .height(50.dp)
                                                .padding(horizontal = 12.dp)
                                                .weight(1f),
                                        colors =
                                            SearchBarDefaults.colors().copy(
                                                containerColor = Color.Transparent,
                                            ),
                                        inputField = {
                                            CompositionLocalProvider(LocalTextStyle provides typo().bodySmall) {
                                                SearchBarDefaults.InputField(
                                                    query = query,
                                                    onQueryChange = { query = it },
                                                    onSearch = { showSearchBar = false },
                                                    expanded = showSearchBar,
                                                    onExpandedChange = { showSearchBar = it },
                                                    placeholder = {
                                                        Text(
                                                            stringResource(Res.string.search),
                                                            style = typo().bodyMedium,
                                                        )
                                                    },
                                                )
                                            }
                                        },
                                        expanded = false,
                                        onExpandedChange = {},
                                        windowInsets = WindowInsets(0, 0, 0, 0),
                                    ) {
                                    }
                                    IconButton(
                                        onClick = {
                                            showSearchBar = !showSearchBar
                                        },
                                    ) {
                                        Icon(Icons.Rounded.Close, null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    items(count = filteredTrack.size, key = { index ->
                        val item = filteredTrack.getOrNull(index)
                        (item?.videoId ?: "") + "item_$index"
                    }) { index ->
                        val item = filteredTrack.getOrNull(index)
                        if (item != null) {
                            if (playingTrack?.videoId == item.videoId && isPlaying) {
                                SongFullWidthItems(
                                    isPlaying = true,
                                    track = item,
                                    onMoreClickListener = { onItemMoreClick(it) },
                                    onClickListener = {
                                        Logger.w("PlaylistScreen", "index: $index")
                                        onPlaylistItemClick(it)
                                    },
                                    onAddToQueue = {
                                        sharedViewModel.addListToQueue(
                                            arrayListOf(item),
                                        )
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            } else {
                                SongFullWidthItems(
                                    isPlaying = false,
                                    track = item,
                                    onMoreClickListener = { onItemMoreClick(it) },
                                    onClickListener = {
                                        Logger.w("PlaylistScreen", "index: $index")
                                        onPlaylistItemClick(it)
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
                                            .fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CenterLoadingBox(
                                        modifier = Modifier.size(80.dp),
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
                                        text = stringResource(Res.string.error),
                                        style = typo().bodyMedium,
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
                    Logger.w("PlaylistScreen", "PlaylistBottomSheet")
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
                        playlistName = data.title,
                        isYourYouTubePlaylist = isYourYouTubePlaylist && !data.isRadio,
                        onSaveToLocal = {
                            viewModel.getFullTracks { track ->
                                viewModel.saveToLocal(track)
                            }
                        },
                        onEditTitle = { newTitle ->
                            viewModel.updatePlaylistTitle(newTitle, data.id)
                        },
                        onAddToQueue = if (data.isRadio) null else addToQueue,
                    )
                }
                AnimatedVisibility(
                    visible = shouldHideTopBar && !showSearchBar,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    TopAppBar(
                        windowInsets =
                            TopAppBarDefaults.windowInsets.exclude(
                                TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start),
                            ),
                        title = {
                            Text(
                                text = data.title,
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
                        actions = {
                            IconButton(
                                onClick = {
                                    showSearchBar = !showSearchBar
                                },
                            ) {
                                Icon(Icons.Rounded.Search, null, tint = Color.White)
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
                        modifier = Modifier.size(80.dp),
                    )
                }
            }

            is PlaylistUIState.Error -> {
                viewModel.makeToast("Error: ${state.message}")
                navController.navigateUp()
            }
        }
    }
}