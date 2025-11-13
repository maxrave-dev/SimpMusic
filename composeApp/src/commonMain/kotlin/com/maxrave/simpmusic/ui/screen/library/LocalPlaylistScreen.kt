package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.sharp.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.kmpalette.rememberPaletteState
import com.maxrave.common.LOCAL_PLAYLIST_ID
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.PairSongLocalPlaylist
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.utils.FilterState
import com.maxrave.domain.utils.toTrack
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.displayNameRes
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DraggableItem
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.LoadingDialog
import com.maxrave.simpmusic.ui.component.LocalPlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.SortPlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.SuggestItems
import com.maxrave.simpmusic.ui.component.painterPlaylistThumbnail
import com.maxrave.simpmusic.ui.component.rememberDragDropState
import com.maxrave.simpmusic.ui.theme.md_theme_dark_background
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LocalPlaylistUIEvent
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.getString
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
import simpmusic.composeapp.generated.resources.baseline_shuffle_24
import simpmusic.composeapp.generated.resources.baseline_tips_and_updates_24
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.created_at
import simpmusic.composeapp.generated.resources.download_button
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.reload
import simpmusic.composeapp.generated.resources.sort_by
import simpmusic.composeapp.generated.resources.suggest
import simpmusic.composeapp.generated.resources.sync_playlist_warning
import simpmusic.composeapp.generated.resources.unsync_playlist_warning
import simpmusic.composeapp.generated.resources.warning
import simpmusic.composeapp.generated.resources.yes
import simpmusic.composeapp.generated.resources.your_playlist

private const val TAG = "LocalPlaylistScreen"

@ExperimentalFoundationApi
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalCoroutinesApi::class,
)
@Composable
fun LocalPlaylistScreen(
    id: Long,
    sharedViewModel: SharedViewModel = koinInject(),
    viewModel: LocalPlaylistViewModel = koinViewModel(),
    navController: NavController,
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/downloading_animation.json").decodeToString(),
        )
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val aiPainter = painterResource(Res.drawable.baseline_tips_and_updates_24)
    val limit = 1.5f
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progressAnimated by transition.animateFloat(
        initialValue = -limit,
        targetValue = limit,
        animationSpec =
            infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "shimmer",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(5000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "rotation",
    )

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    val downloadState by viewModel.uiState.map { it.downloadState }.collectAsState(
        initial = DownloadState.STATE_NOT_DOWNLOADED,
    )
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestions by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestButton by rememberSaveable { mutableStateOf(false) }

    val playingTrack by sharedViewModel.nowPlayingState
        .mapLatest {
            it?.songEntity
        }.collectAsState(initial = null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsState(initial = false)

    val queueData by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val playingPlaylistId by remember {
        derivedStateOf {
            queueData?.data?.playlistId
        }
    }

    val suggestedTracks by viewModel.uiState.map { it.suggestions?.songs ?: emptyList() }.collectAsState(initial = emptyList())
    val suggestionsLoading by viewModel.loading.collectAsStateWithLifecycle()
    var showSyncAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showUnsyncAlertDialog by rememberSaveable { mutableStateOf(false) }
    var firstTimeGetLocalPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var currentItem by remember {
        mutableStateOf<SongEntity?>(null)
    }

    var itemBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var playlistBottomSheetShow by remember {
        mutableStateOf(false)
    }

    var sortBottomSheetShow by remember {
        mutableStateOf(false)
    }

    var changingOrder by remember {
        mutableStateOf(false)
    }

    val trackPagingItems: LazyPagingItems<Pair<SongEntity, PairSongLocalPlaylist>> = viewModel.tracksPagingState.collectAsLazyPagingItems()
    LaunchedEffect(Unit) {
        snapshotFlow {
            trackPagingItems.loadState
        }.collectLatest {
            Logger.d("PlaylistScreen", "loadState: ${trackPagingItems.loadState}")
            viewModel.setLazyTrackPagingItems(trackPagingItems)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            firstTimeGetLocalPlaylist = true
        }
    }

    val onPlaylistItemClick: (videoId: String) -> Unit = { videoId ->
        viewModel.onUIEvent(
            LocalPlaylistUIEvent.ItemClick(
                videoId = videoId,
            ),
        )
    }
    val onItemMoreClick: (videoId: String) -> Unit = { videoId ->
        currentItem = trackPagingItems.itemSnapshotList.findLast { it?.first?.videoId == videoId }?.first
        if (currentItem != null) {
            itemBottomSheetShow = true
        }
    }
    val onPlaylistMoreClick: () -> Unit = {
        playlistBottomSheetShow = true
    }

    LaunchedEffect(key1 = shouldShowSuggestions) {
        if (suggestedTracks.isEmpty() && uiState.syncState != LocalPlaylistEntity.YouTubeSyncState.NotSynced) {
            viewModel.getSuggestions(uiState.id)
        }
    }

    LaunchedEffect(key1 = id) {
        if (id != uiState.id) {
            Logger.w("PlaylistScreen", "new id: $id")
            viewModel.setOffset(0)
            viewModel.removeListSuggestion()
            viewModel.updatePlaylistState(id, true)
            delay(100)
            firstTimeGetLocalPlaylist = true
        }
    }
    LaunchedEffect(key1 = uiState) {
        shouldShowSuggestButton =
            !uiState.ytPlaylistId.isNullOrEmpty() &&
            uiState.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced
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
    val coroutineScope = rememberCoroutineScope()
    val dragDropState =
        rememberDragDropState(lazyState) { from, to ->
            coroutineScope.launch {
                Logger.d(TAG, "onMove from $from to $to")
                viewModel.changeLocalPlaylistItemPosition(from - 1, to - 1)
                trackPagingItems.refresh()
            }
        }
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
//    Box {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDrag = { change, offset ->
                            Logger.d(TAG, "onDrag $offset")
                            change.consume()
                            dragDropState.onDrag(offset = offset)

                            if (overscrollJob?.isActive == true) {
                                return@detectDragGesturesAfterLongPress
                            }

                            dragDropState
                                .checkForOverScroll()
                                .takeIf { it != 0f }
                                ?.let {
                                    overscrollJob =
                                        coroutineScope.launch {
                                            dragDropState.state.animateScrollBy(
                                                it * 1.3f,
                                                tween(easing = FastOutLinearInEasing),
                                            )
                                        }
                                }
                                ?: run { overscrollJob?.cancel() }
                        },
                        onDragStart = { offset ->
                            Logger.d(TAG, "onDragStart $offset")
                            dragDropState.onDragStart(offset)
                        },
                        onDragEnd = {
                            Logger.d(TAG, "onDragEnd")
                            dragDropState.onDragInterrupted(true)
                            overscrollJob?.cancel()
                        },
                        onDragCancel = {
                            Logger.d(TAG, "onDragCancel")
                            dragDropState.onDragInterrupted()
                            overscrollJob?.cancel()
                        },
                    )
                },
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
//                                .haze(
//                                    hazeState,
//                                    style = HazeMaterials.regular(),
//                                ),
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
//                            .hazeChild(hazeState, style = HazeMaterials.regular()),
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
                                    .crossfade(550)
                                    .build(),
                            placeholder = painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp),
                            error = painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp),
                            fallback = painterPlaylistThumbnail(uiState.title, style = typo().labelMedium, 250.dp to 250.dp),
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
                                    style = typo().titleMedium,
                                    color = Color.White,
                                )
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.your_playlist),
                                        style = typo().titleSmall,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text =
                                            stringResource(
                                                Res.string.created_at,
                                                uiState.inLibrary?.format(
                                                    LocalDateTime.Format {
                                                        hour()
                                                        char(':')
                                                        minute()
                                                        chars(" - ")
                                                        day()
                                                        char(' ')
                                                        monthName(
                                                            MonthNames.ENGLISH_FULL,
                                                        )
                                                        char(' ')
                                                        year()
                                                    },
                                                ) ?: "",
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
                                    Crossfade(isPlaying && playingPlaylistId == LOCAL_PLAYLIST_ID + uiState.id) { isThisPlaying ->
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
                                                viewModel.onUIEvent(LocalPlaylistUIEvent.PlayClick)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.size(5.dp))
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
                                                                viewModel.makeToast(runBlocking { getString(Res.string.downloaded) })
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
                                                                viewModel.makeToast(runBlocking { getString(Res.string.downloading) })
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
                                                    viewModel.downloadFullPlaylist()
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Spacer(Modifier.size(5.dp))
                                    AnimatedVisibility(visible = shouldShowSuggestButton) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .graphicsLayer {
                                                        compositingStrategy =
                                                            CompositingStrategy.Offscreen
                                                    }.clickable {
                                                        shouldShowSuggestions = !shouldShowSuggestions
                                                    }.drawWithCache {
                                                        val width = size.width
                                                        val height = size.height

                                                        val offsetDraw = width * progressAnimated
                                                        val gradientColors =
                                                            listOf(
                                                                Color(0xFF4C82EF),
                                                                Color(0xFFD96570),
                                                            )
                                                        val brush =
                                                            Brush.linearGradient(
                                                                colors = gradientColors,
                                                                start = Offset(offsetDraw, 0f),
                                                                end =
                                                                    Offset(
                                                                        offsetDraw + width,
                                                                        height,
                                                                    ),
                                                            )

                                                        onDrawBehind {
                                                            // Destination
                                                            with(aiPainter) {
                                                                translate(
                                                                    left = (size.width - width / 1.5f) / 2,
                                                                    top = (size.height - width / 1.5f) / 2,
                                                                ) {
                                                                    draw(
                                                                        size = Size(width / 1.5f, width / 1.5f),
                                                                    )
                                                                }
                                                            }

                                                            // Source
                                                            drawRect(
                                                                brush = brush,
                                                                blendMode = BlendMode.SrcIn,
                                                            )
                                                        }
                                                    },
                                        )
                                    }
                                    RippleIconButton(
                                        modifier =
                                            Modifier.size(36.dp),
                                        resId = Res.drawable.baseline_shuffle_24,
                                        fillMaxSize = true,
                                    ) {
                                        viewModel.onUIEvent(LocalPlaylistUIEvent.ShuffleClick)
                                    }
                                    Spacer(Modifier.size(5.dp))
                                    RippleIconButton(
                                        modifier =
                                            Modifier.size(36.dp),
                                        resId = Res.drawable.baseline_more_vert_24,
                                        fillMaxSize = true,
                                    ) {
                                        onPlaylistMoreClick()
                                    }
                                }
                                Text(
                                    text =
                                        stringResource(
                                            Res.string.album_length,
                                            (uiState.trackCount).toString(),
                                            "",
                                        ),
                                    color = Color.White,
                                    style = typo().bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                                AnimatedVisibility(visible = shouldShowSuggestions) {
                                    Column(
                                        modifier = Modifier.animateContentSize(),
                                    ) {
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.suggest,
                                                    ),
                                                color = Color.White,
                                                modifier =
                                                    Modifier
                                                        .padding(vertical = 8.dp)
                                                        .weight(1f),
                                            )
                                            TextButton(
                                                onClick = { viewModel.reloadSuggestion() },
                                                modifier =
                                                    Modifier
                                                        .padding(horizontal = 8.dp),
                                            ) {
                                                Text(
                                                    text = stringResource(Res.string.reload),
                                                    color = Color.White,
                                                    modifier =
                                                        Modifier.align(
                                                            Alignment.CenterVertically,
                                                        ),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Crossfade(targetState = suggestionsLoading) {
                                            if (it) {
                                                CenterLoadingBox(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .height(200.dp)
                                                            .align(Alignment.CenterHorizontally),
                                                )
                                            } else {
                                                Column {
                                                    suggestedTracks.forEachIndexed { index, track ->
                                                        SuggestItems(
                                                            track = track,
                                                            isPlaying = playingTrack?.videoId == track.videoId,
                                                            onAddClickListener = {
                                                                viewModel.addSuggestTrackToListTrack(
                                                                    track,
                                                                )
                                                            },
                                                            onClickListener = {
                                                                viewModel.onUIEvent(LocalPlaylistUIEvent.SuggestionsItemClick(track.videoId))
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.size(12.dp))
                                        HorizontalDivider(
                                            color = Color.Gray,
                                            thickness = 0.5.dp,
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                    ElevatedButton(
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                                        modifier =
                                            Modifier
                                                .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                                        onClick = {
                                            sortBottomSheetShow = true
                                        },
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Sharp.Sort,
                                                contentDescription = "Sort playlist",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp),
                                            )
                                            Spacer(modifier = Modifier.size(4.dp))
                                            Text(
                                                text =
                                                    stringResource(Res.string.sort_by) + ": " +
                                                        stringResource(
                                                            uiState.filterState.displayNameRes(),
                                                        ),
                                                style = typo().bodySmall,
                                                color = Color.Gray,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    AnimatedVisibility(
                                        visible =
                                            uiState.filterState == FilterState.CustomOrder &&
                                                uiState.syncState != LocalPlaylistEntity.YouTubeSyncState.Synced,
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                    ) {
                                        TextButton(
                                            onClick = {
                                                changingOrder = !changingOrder
                                            },
                                        ) {
                                            Text(
                                                text =
                                                    if (changingOrder) {
                                                        "Done"
                                                    } else {
                                                        "Change order"
                                                    },
                                                style = typo().bodySmall,
                                                color = Color.Gray,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        items(count = trackPagingItems.itemCount, key = { index ->
            val item = trackPagingItems[index]
            (item?.first?.videoId ?: "") + "item_$index" + item?.second?.inPlaylist + item?.second?.position
        }) { index ->
            val item = trackPagingItems[index]?.first
            if (item != null) {
                val content = @Composable {
                    if (playingTrack?.videoId == item.videoId && isPlaying) {
                        SongFullWidthItems(
                            isPlaying = true,
                            shouldShowDragHandle = changingOrder,
                            songEntity = item,
                            onMoreClickListener = { onItemMoreClick(it) },
                            onClickListener = {
                                Logger.w("PlaylistScreen", "index: $index")
                                onPlaylistItemClick(it)
                            },
                            onAddToQueue = {
                                sharedViewModel.addListToQueue(
                                    arrayListOf(item.toTrack()),
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )
                    } else {
                        SongFullWidthItems(
                            isPlaying = false,
                            shouldShowDragHandle = changingOrder,
                            songEntity = item,
                            onMoreClickListener = { onItemMoreClick(it) },
                            onClickListener = {
                                Logger.w("PlaylistScreen", "index: $index")
                                onPlaylistItemClick(it)
                            },
                            onAddToQueue = {
                                sharedViewModel.addListToQueue(
                                    arrayListOf(item.toTrack()),
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                if (changingOrder) {
                    DraggableItem(
                        dragDropState,
                        index + 1,
                        Modifier.animateItem(),
                    ) {
                        content()
                    }
                } else {
                    content()
                }
            }
        }
        trackPagingItems.apply {
            when {
                loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading -> {
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(15.dp))
                            CenterLoadingBox(modifier = Modifier.size(80.dp))
                            Spacer(modifier = Modifier.height(15.dp))
                        }
                    }
                }
            }
        }
        item {
            EndOfPage()
        }
    }
    if (itemBottomSheetShow && currentItem != null) {
        val track = currentItem ?: return
        NowPlayingBottomSheet(
            onDelete = { viewModel.deleteItem(uiState.id, track) },
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
        LocalPlaylistBottomSheet(
            isBottomSheetVisible = playlistBottomSheetShow,
            onDismiss = { playlistBottomSheetShow = false },
            title = uiState.title,
            ytPlaylistId = uiState.ytPlaylistId,
            onEditTitle =
                { newTitle ->
                    viewModel.updatePlaylistTitle(newTitle, uiState.id)
                },
            onEditThumbnail =
                { thumbUri ->
                    viewModel.updatePlaylistThumbnail(thumbUri, uiState.id)
                },
            onAddToQueue = {
                viewModel.addAllToQueue()
            },
            onSync = {
                if (uiState.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
                    showUnsyncAlertDialog = true
                } else {
                    showSyncAlertDialog = true
                }
            },
            onUpdatePlaylist = {
                viewModel.updateListTrackSynced(uiState.id)
            },
            onDelete = {
                viewModel.deletePlaylist(uiState.id)
                navController.navigateUp()
            },
        )
    }
    if (showSyncAlertDialog) {
        AlertDialog(
            title = { Text(text = stringResource(Res.string.warning)) },
            text = { Text(text = stringResource(Res.string.sync_playlist_warning)) },
            onDismissRequest = { showSyncAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.syncPlaylistWithYouTubePlaylist(uiState.id)
                    showSyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }
    if (showUnsyncAlertDialog) {
        AlertDialog(
            title = { Text(text = stringResource(Res.string.warning)) },
            text = { Text(text = stringResource(Res.string.unsync_playlist_warning)) },
            onDismissRequest = { showUnsyncAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unsyncPlaylistWithYouTubePlaylist(uiState.id)
                    showUnsyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsyncAlertDialog = false
                }) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }
    if (sortBottomSheetShow) {
        SortPlaylistBottomSheet(
            selectedState = uiState.filterState,
            onDismiss = { sortBottomSheetShow = false },
            onSortChanged = {
                viewModel.onUIEvent(LocalPlaylistUIEvent.ChangeFilter(it))
                sortBottomSheetShow = false
            },
        )
    }
    AnimatedVisibility(
        visible = shouldHideTopBar,
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
                    text = uiState.title,
                    style = typo().titleMedium,
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
}