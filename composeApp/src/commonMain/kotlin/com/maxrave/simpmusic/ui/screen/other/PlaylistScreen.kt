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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
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
import com.kyant.backdrop.highlight.Highlight
import com.kmpalette.rememberPaletteState
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.ui.component.SearchBarExit
import com.maxrave.simpmusic.ui.component.SearchBarEnter
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.getStringBlocking
import com.maxrave.simpmusic.extension.toImmersiveBackground
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.LoadingDialog
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.liquidGlass
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.Search
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ListState
import com.maxrave.simpmusic.viewModel.PlaylistUIEvent
import com.maxrave.simpmusic.viewModel.PlaylistUIState
import com.maxrave.simpmusic.viewModel.PlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
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
import simpmusic.composeapp.generated.resources.baseline_downloaded
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
import simpmusic.composeapp.generated.resources.error
import simpmusic.composeapp.generated.resources.no_description
import simpmusic.composeapp.generated.resources.playlist
import simpmusic.composeapp.generated.resources.radio
import simpmusic.composeapp.generated.resources.search
import simpmusic.composeapp.generated.resources.unlimited

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
    playlistId: String,
    isYourYouTubePlaylist: Boolean,
    navController: NavController,
) {
    // Home shelves navigate with the browseEndpoint id, which is "VL" + the playlist id
    // (HomeParser reads title.runs[0].navigationEndpoint.browseEndpoint.browseId). Every radio
    // prefix check and the watch endpoint expect the bare id, so normalise once on the way in
    // rather than stripping "VL" again at each consumer.
    val id = playlistId.removePrefix("VL")
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
    var searchBarHeightPx by remember { mutableStateOf(0) }

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }

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
                id,
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

    LaunchedEffect(key1 = id) {
        if (id != uiState.data?.id) {
            Logger.w(tag, "new id: $id")
            viewModel.getData(id)
        }
    }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    val paletteState = rememberPaletteState()
    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )
    var bitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }
    // Track which thumbnail URL we've already extracted a palette from.
    // Prevents palette flash when LazyColumn recycles the header item on scroll —
    // AsyncImage re-mount fires onSuccess again, but we skip the regenerate.
    var paletteGeneratedFor by remember {
        mutableStateOf<String?>(null)
    }
    val currentThumbnail = (uiState as? PlaylistUIState.Success)?.data?.thumbnail

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null && currentThumbnail != null && paletteGeneratedFor != currentThumbnail) {
            paletteState.generate(bm)
            paletteGeneratedFor = currentThumbnail
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                viewModel.setBrush(listOf(it.getColorFromPalette(), Color.Black))
            }
    }

    // Apple Music-inspired immersive treatment. Which header is used depends on the window's
    // aspect ratio alone, not on the platform: a portrait window (a phone held upright, or a
    // narrow desktop window) gets the edge-to-edge artwork header, a landscape one gets the
    // side-by-side header. Everything else on the page — the palette background, the row
    // dividers, the blurred top bar — is shared by both.
    val screenInfo = getScreenSizeInfo()
    val isPortrait = screenInfo.wDP < screenInfo.hDP
    val dominantColor = listColors.firstOrNull() ?: Color.Black
    // Apple Music-style page background from the artwork's dominant tone (see UIExt.toImmersiveBackground).
    val mutedPaletteBg = paletteState.palette.toImmersiveBackground()

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
                val hazeState =
                    rememberHazeState(
                        blurEnabled = true,
                    )
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(mutedPaletteBg)
                            .hazeSource(hazeState),
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
                                Column(
                                    Modifier
                                        .background(Color.Transparent),
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.Start,
                                    ) {
                                        if (isPortrait) {
                                            // Apple Music-style: edge-to-edge artwork + liquid glass buttons.
                                            // Glass buttons MUST be siblings of the backdrop source (not children)
                                            // to avoid render feedback loop / RuntimeShader crash.
                                            val artworkBackdrop = rememberBackdrop(Color.Black)
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height((screenInfo.hDP / 2).dp),
                                            ) {
                                                // Inner Box — backdrop SOURCE (artwork + overlays only, NO glass)
                                                Box(modifier = Modifier.fillMaxSize().layerBackdrop(artworkBackdrop)) {
                                                    AsyncImage(
                                                        model =
                                                            ImageRequest
                                                                .Builder(LocalPlatformContext.current)
                                                                .data(data.thumbnail)
                                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                                .diskCacheKey(data.thumbnail)
                                                                .memoryCacheKey(data.thumbnail)
                                                                .crossfade(false)
                                                                .build(),
                                                        placeholder = rememberHolderPainter(),
                                                        error = rememberHolderPainter(),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        onSuccess = {
                                                            bitmap = it.result.image.toImageBitmap()
                                                        },
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                    // Scrim spans 70% of the artwork (not a fixed 200dp): the
                                                    // shorter the ramp, the steeper the alpha, and a steep ramp
                                                    // is what makes the fade read as an edge. See
                                                    // artworkScrimBrush for the curve itself.
                                                    Box(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .height((screenInfo.hDP * 0.35f).dp)
                                                                .align(Alignment.BottomCenter)
                                                                .background(artworkScrimBrush(mutedPaletteBg)),
                                                    )
                                                    Column(
                                                        modifier =
                                                            Modifier
                                                                .align(Alignment.BottomCenter)
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 20.dp)
                                                                .padding(bottom = 16.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                    ) {
                                                        Text(
                                                            text = data.title,
                                                            style = typo().titleLarge,
                                                            color = Color.White,
                                                            maxLines = 2,
                                                            textAlign = TextAlign.Center,
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
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
                                                                    style = typo().titleSmall,
                                                                    color = Color.White,
                                                                    textAlign = TextAlign.Center,
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "${
                                                                if (data.isRadio) {
                                                                    stringResource(Res.string.radio)
                                                                } else {
                                                                    stringResource(Res.string.playlist)
                                                                }
                                                            } • ${data.year}",
                                                            style = typo().bodyMedium,
                                                            color = Color(0xC4FFFFFF),
                                                            textAlign = TextAlign.Center,
                                                        )
                                                    }
                                                }
                                                // Back + Heart + Search button overlays on artwork top — liquid glass
                                                Row(
                                                    modifier =
                                                        Modifier
                                                            .align(Alignment.TopCenter)
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                                            .windowInsetsPadding(WindowInsets.statusBars),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    LiquidGlassIconButton(
                                                        backdrop = artworkBackdrop,
                                                        imageVector = SimpIcons.ArrowBackIosNew,
                                                        modifier =
                                                            Modifier
                                                                .size(48.dp),
                                                    ) {
                                                        navController.navigateUp()
                                                    }
                                                    Spacer(Modifier.weight(1f))
                                                    Row(
                                                        modifier =
                                                            Modifier
                                                                .height(48.dp)
                                                                .liquidGlass(artworkBackdrop, RoundedCornerShape(24.dp)),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        if (!data.isRadio) {
                                                            Box(
                                                                modifier = Modifier.size(48.dp),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                HeartCheckBox(
                                                                    size = 28,
                                                                    checked = liked,
                                                                    onStateChange = {
                                                                        viewModel.onUIEvent(PlaylistUIEvent.Favorite)
                                                                    },
                                                                )
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                showSearchBar = !showSearchBar
                                                            },
                                                        ) {
                                                            Icon(SimpIcons.Search, null, tint = Color.White)
                                                        }
                                                        IconButton(
                                                            onClick = onPlaylistMoreClick,
                                                        ) {
                                                            Icon(
                                                                imageVector = SimpIcons.MoreVert,
                                                                contentDescription = "More",
                                                                tint = Color.White,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Apple Music desktop header: back and the overlay actions on
                                            // their own top row, then a square artwork with the text column
                                            // and the action cluster laid out beside it rather than under it.
                                            // Built exactly like the portrait branch, which renders correctly on both
                                            // platforms: the backdrop SOURCE is the content column — artwork included,
                                            // so the recorded layer holds something to refract — and the glass buttons
                                            // are SIBLINGS placed with align(), never children of the source (that
                                            // nesting is the render-feedback loop that kills the RuntimeShader).
                                            val headerBackdrop = rememberBackdrop(mutedPaletteBg)
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                Column(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .layerBackdrop(headerBackdrop)
                                                            .windowInsetsPadding(WindowInsets.statusBars)
                                                            .padding(horizontal = 32.dp, vertical = 16.dp),
                                                ) {
                                                    // Reserves the strip the sibling glass buttons are drawn over.
                                                    Spacer(modifier = Modifier.height(48.dp))
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                                        verticalAlignment = Alignment.Top,
                                                    ) {
                                                        AsyncImage(
                                                            model =
                                                                ImageRequest
                                                                    .Builder(LocalPlatformContext.current)
                                                                    .data(data.thumbnail)
                                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                                                    .diskCacheKey(data.thumbnail)
                                                                    .memoryCacheKey(data.thumbnail)
                                                                    .crossfade(false)
                                                                    .build(),
                                                            placeholder = rememberHolderPainter(),
                                                            error = rememberHolderPainter(),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            onSuccess = {
                                                                bitmap = it.result.image.toImageBitmap()
                                                            },
                                                            modifier =
                                                                Modifier
                                                                    .size(280.dp)
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                        )
                                                        Column(
                                                            modifier = Modifier.weight(1f),
                                                        ) {
                                                            Text(
                                                                text = data.title,
                                                                style = typo().headlineSmall,
                                                                color = Color.White,
                                                                maxLines = 2,
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = data.author.name,
                                                                style = typo().titleMedium,
                                                                // The app accent, standing in for the brand red
                                                                // Apple uses on this line.
                                                                color = seed,
                                                                modifier =
                                                                    Modifier.clickable {
                                                                        if (data.author.id.isNotEmpty()) {
                                                                            navController.navigate(
                                                                                ArtistDestination(
                                                                                    data.author.id,
                                                                                ),
                                                                            )
                                                                        }
                                                                    },
                                                            )
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            Text(
                                                                text = "${
                                                                    if (data.isRadio) {
                                                                        stringResource(Res.string.radio)
                                                                    } else {
                                                                        stringResource(Res.string.playlist)
                                                                    }
                                                                } • ${data.year}",
                                                                style = typo().labelMedium,
                                                                color = Color(0xC4FFFFFF),
                                                            )
                                                            Spacer(modifier = Modifier.height(20.dp))
                                                            // Apple Music-style action row:
                                                            // [Shuffle][Play pill][Download/More] (cluster centered, all 48dp matching size)
                                                            val isThisPlaying = isPlaying && playingPlaylistId == data.id
                                                            Row(
                                                                modifier =
                                                                    Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(vertical = 8.dp),
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                            ) {
                                                                if (!data.isRadio) {
                                                                    Box(
                                                                        modifier =
                                                                            Modifier
                                                                                .size(48.dp)
                                                                                .clip(CircleShape)
                                                                                .background(Color.White.copy(alpha = 0.12f))
                                                                                .clickable {
                                                                                    viewModel.onUIEvent(PlaylistUIEvent.Shuffle)
                                                                                },
                                                                        contentAlignment = Alignment.Center,
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = SimpIcons.Shuffle,
                                                                            contentDescription = "Shuffle",
                                                                            tint = Color.White,
                                                                            modifier = Modifier.size(22.dp),
                                                                        )
                                                                    }
                                                                }
                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .height(48.dp)
                                                                            .widthIn(min = 110.dp)
                                                                            .clip(CircleShape)
                                                                            .background(Color.White)
                                                                            .clickable {
                                                                                if (isThisPlaying) {
                                                                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                                                                } else {
                                                                                    viewModel.onUIEvent(PlaylistUIEvent.PlayAll)
                                                                                }
                                                                            }.padding(horizontal = 20.dp),
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Icon(
                                                                            imageVector =
                                                                                if (isThisPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                                                                            contentDescription = null,
                                                                            tint = Color.Black,
                                                                            modifier = Modifier.size(22.dp),
                                                                        )
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text(
                                                                            text = if (isThisPlaying) "Pause" else "Play",
                                                                            color = Color.Black,
                                                                            style = typo().labelLarge,
                                                                        )
                                                                    }
                                                                }
                                                                if (!data.isRadio) {
                                                                    Box(
                                                                        modifier =
                                                                            Modifier
                                                                                .size(48.dp)
                                                                                .clip(CircleShape)
                                                                                .background(Color.White.copy(alpha = 0.12f)),
                                                                        contentAlignment = Alignment.Center,
                                                                    ) {
                                                                        Crossfade(targetState = downloadState) { state ->
                                                                            when (state) {
                                                                                DownloadState.STATE_DOWNLOADED -> {
                                                                                    Box(
                                                                                        modifier =
                                                                                            Modifier
                                                                                                .fillMaxSize()
                                                                                                .clickable {
                                                                                                    viewModel.makeToast(
                                                                                                        getStringBlocking(Res.string.downloaded),
                                                                                                    )
                                                                                                },
                                                                                        contentAlignment = Alignment.Center,
                                                                                    ) {
                                                                                        Icon(
                                                                                            painter = painterResource(Res.drawable.baseline_downloaded),
                                                                                            tint = Color(0xFF00A0CB),
                                                                                            contentDescription = "",
                                                                                            modifier = Modifier.size(22.dp),
                                                                                        )
                                                                                    }
                                                                                }

                                                                                DownloadState.STATE_DOWNLOADING -> {
                                                                                    Box(
                                                                                        modifier =
                                                                                            Modifier
                                                                                                .fillMaxSize()
                                                                                                .clickable {
                                                                                                    viewModel.makeToast(
                                                                                                        getStringBlocking(Res.string.downloading),
                                                                                                    )
                                                                                                },
                                                                                        contentAlignment = Alignment.Center,
                                                                                    ) {
                                                                                        Image(
                                                                                            painter =
                                                                                                rememberLottiePainter(
                                                                                                    composition = composition,
                                                                                                    iterations = Compottie.IterateForever,
                                                                                                ),
                                                                                            contentDescription = "Lottie animation",
                                                                                            modifier = Modifier.size(28.dp),
                                                                                        )
                                                                                    }
                                                                                }

                                                                                else -> {
                                                                                    Box(
                                                                                        modifier =
                                                                                            Modifier
                                                                                                .fillMaxSize()
                                                                                                .clickable {
                                                                                                    Logger.w(
                                                                                                        "PlaylistScreen",
                                                                                                        "downloadState: $downloadState",
                                                                                                    )
                                                                                                    viewModel.onUIEvent(PlaylistUIEvent.Download)
                                                                                                },
                                                                                        contentAlignment = Alignment.Center,
                                                                                    ) {
                                                                                        Icon(
                                                                                            imageVector = SimpIcons.DownloadForOffline,
                                                                                            tint = Color.White,
                                                                                            contentDescription = "Download",
                                                                                            modifier = Modifier.size(22.dp),
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
                                                }
                                                // Glass overlays — siblings of the source, over the strip the column reserved.
                                                Box(Modifier.padding(start = 12.dp)) {
                                                    LiquidGlassIconButton(
                                                        backdrop = headerBackdrop,
                                                        imageVector = SimpIcons.ArrowBackIosNew,
                                                        shape = RoundedCornerShape(24.dp),
                                                        // Same directional style as the like/⋯ pill, a touch thicker. The default
                                                        // width of 0.5.dp becomes a ~2px stroke (HighlightModifier: ceil(width.toPx()) * 2),
                                                        // which reads along the pill's long edge but vanishes around a 48dp circle. 1.dp
                                                        // is the smallest step up that stays visible without looking like a border.
                                                        highlight = Highlight(width = 1.dp),
                                                        modifier =
                                                            Modifier
                                                                .align(Alignment.TopStart)
                                                                .padding(12.dp)
                                                                .windowInsetsPadding(WindowInsets.statusBars)
                                                                .size(48.dp),
                                                    ) {
                                                        navController.navigateUp()
                                                    }
                                                }
                                                Row(
                                                    modifier =
                                                        Modifier
                                                            .align(Alignment.TopEnd)
                                                            .windowInsetsPadding(WindowInsets.statusBars)
                                                            .padding(end = 32.dp, top = 16.dp)
                                                            .height(48.dp)
                                                            .liquidGlass(headerBackdrop, RoundedCornerShape(24.dp)),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    if (!data.isRadio) {
                                                        Box(
                                                            modifier = Modifier.size(48.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            HeartCheckBox(
                                                                size = 28,
                                                                checked = liked,
                                                                onStateChange = {
                                                                    viewModel.onUIEvent(PlaylistUIEvent.Favorite)
                                                                },
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            showSearchBar = !showSearchBar
                                                        },
                                                    ) {
                                                        Icon(SimpIcons.Search, null, tint = Color.White)
                                                    }
                                                    IconButton(
                                                        onClick = onPlaylistMoreClick,
                                                    ) {
                                                        Icon(
                                                            imageVector = SimpIcons.MoreVert,
                                                            contentDescription = "More",
                                                            tint = Color.White,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(),
                                        ) {
                                            Column(Modifier.padding(horizontal = 32.dp)) {
                                                if (isPortrait) {
                                                    // Apple Music-style action row:
                                                    // [Shuffle][Play pill][Download/More] (cluster centered, all 48dp matching size)
                                                    val isThisPlaying = isPlaying && playingPlaylistId == data.id
                                                    Row(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        if (!data.isRadio) {
                                                            Box(
                                                                modifier =
                                                                    Modifier
                                                                        .size(48.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color.White.copy(alpha = 0.12f))
                                                                        .clickable {
                                                                            viewModel.onUIEvent(PlaylistUIEvent.Shuffle)
                                                                        },
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Icon(
                                                                    imageVector = SimpIcons.Shuffle,
                                                                    contentDescription = "Shuffle",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(22.dp),
                                                                )
                                                            }
                                                        }
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .height(48.dp)
                                                                    .widthIn(min = 110.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color.White)
                                                                    .clickable {
                                                                        if (isThisPlaying) {
                                                                            sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                                                        } else {
                                                                            viewModel.onUIEvent(PlaylistUIEvent.PlayAll)
                                                                        }
                                                                    }.padding(horizontal = 20.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector =
                                                                        if (isThisPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                                                                    contentDescription = null,
                                                                    tint = Color.Black,
                                                                    modifier = Modifier.size(22.dp),
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = if (isThisPlaying) "Pause" else "Play",
                                                                    color = Color.Black,
                                                                    style = typo().labelLarge,
                                                                )
                                                            }
                                                        }
                                                        if (!data.isRadio) {
                                                            Box(
                                                                modifier =
                                                                    Modifier
                                                                        .size(48.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color.White.copy(alpha = 0.12f)),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Crossfade(targetState = downloadState) { state ->
                                                                    when (state) {
                                                                        DownloadState.STATE_DOWNLOADED -> {
                                                                            Box(
                                                                                modifier =
                                                                                    Modifier
                                                                                        .fillMaxSize()
                                                                                        .clickable {
                                                                                            viewModel.makeToast(
                                                                                                getStringBlocking(Res.string.downloaded),
                                                                                            )
                                                                                        },
                                                                                contentAlignment = Alignment.Center,
                                                                            ) {
                                                                                Icon(
                                                                                    painter = painterResource(Res.drawable.baseline_downloaded),
                                                                                    tint = Color(0xFF00A0CB),
                                                                                    contentDescription = "",
                                                                                    modifier = Modifier.size(22.dp),
                                                                                )
                                                                            }
                                                                        }

                                                                        DownloadState.STATE_DOWNLOADING -> {
                                                                            Box(
                                                                                modifier =
                                                                                    Modifier
                                                                                        .fillMaxSize()
                                                                                        .clickable {
                                                                                            viewModel.makeToast(
                                                                                                getStringBlocking(Res.string.downloading),
                                                                                            )
                                                                                        },
                                                                                contentAlignment = Alignment.Center,
                                                                            ) {
                                                                                Image(
                                                                                    painter =
                                                                                        rememberLottiePainter(
                                                                                            composition = composition,
                                                                                            iterations = Compottie.IterateForever,
                                                                                        ),
                                                                                    contentDescription = "Lottie animation",
                                                                                    modifier = Modifier.size(28.dp),
                                                                                )
                                                                            }
                                                                        }

                                                                        else -> {
                                                                            Box(
                                                                                modifier =
                                                                                    Modifier
                                                                                        .fillMaxSize()
                                                                                        .clickable {
                                                                                            Logger.w(
                                                                                                "PlaylistScreen",
                                                                                                "downloadState: $downloadState",
                                                                                            )
                                                                                            viewModel.onUIEvent(PlaylistUIEvent.Download)
                                                                                        },
                                                                                contentAlignment = Alignment.Center,
                                                                            ) {
                                                                                Icon(
                                                                                    imageVector = SimpIcons.DownloadForOffline,
                                                                                    tint = Color.White,
                                                                                    contentDescription = "Download",
                                                                                    modifier = Modifier.size(22.dp),
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
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
                        item {
                            val density = LocalDensity.current
                            Spacer(
                                Modifier.height(
                                    with(density) { searchBarHeightPx.toDp() },
                                ),
                            )
                        }
                    }
                    items(count = filteredTrack.size, key = { index ->
                        val item = filteredTrack.getOrNull(index)
                        (item?.videoId ?: "") + "item_$index"
                    }) { index ->
                        val item = filteredTrack.getOrNull(index)
                        if (item != null) {
                            Column(modifier = Modifier.animateItem()) {
                                if (playingTrack?.videoId == item.videoId && isPlaying) {
                                    SongFullWidthItems(
                                        forceDark = true,
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
                                        selectionMode = selectionState.isActive,
                                        isSelected = selectionState.isSelected(item.videoId),
                                        onLongClick = { selectionState.start(it) },
                                        onSelectToggle = { selectionState.toggle(it) },
                                        modifier = Modifier,
                                    )
                                } else {
                                    SongFullWidthItems(
                                        forceDark = true,
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
                                        selectionMode = selectionState.isActive,
                                        isSelected = selectionState.isSelected(item.videoId),
                                        onLongClick = { selectionState.start(it) },
                                        onSelectToggle = { selectionState.toggle(it) },
                                        modifier = Modifier,
                                    )
                                }
                                if (index < filteredTrack.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                        thickness = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.12f),
                                    )
                                }
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

                AnimatedVisibility(
                    visible = showSearchBar,
                    enter = SearchBarEnter,
                    exit = SearchBarExit,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { searchBarHeightPx = it.size.height }
                            .hazeEffect(hazeState) {
                                blurEnabled = true
                                blurRadius = 24.dp
                                backgroundColor = mutedPaletteBg
                                tints = listOf(HazeTint(mutedPaletteBg.copy(alpha = 0.55f)))
                            },
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .windowInsetsPadding(WindowInsets.statusBars),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RippleIconButton(
                                imageVector = SimpIcons.ArrowBackIosNew,
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
                                Icon(SimpIcons.Close, null, tint = Color.White)
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectionState.isActive,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    SongSelectionTopAppBar(
                        state = selectionState,
                        onSelectAll = {
                            selectionState.toggleSelectAll(filteredTrack.map { it.videoId })
                        },
                        onOpenActions = { showSelectionSheet = true },
                        modifier =
                            Modifier.hazeEffect(hazeState) {
                                blurEnabled = true
                                blurRadius = 24.dp
                                backgroundColor = mutedPaletteBg
                                tints = listOf(HazeTint(mutedPaletteBg.copy(alpha = 0.55f)))
                            },
                    )
                }
                if (showSelectionSheet) {
                    val selectedIds = selectionState.selected.toList()
                    SelectedSongsBottomSheet(
                        count = selectedIds.size,
                        onDismiss = { showSelectionSheet = false },
                        onPlayNext = {
                            selectionViewModel.playNext(selectedIds)
                            selectionState.exit()
                        },
                        onAddToQueue = {
                            selectionViewModel.addToQueue(selectedIds)
                            selectionState.exit()
                        },
                        onAddToPlaylist = { showSelectionAddToPlaylist = true },
                        onDownload = {
                            selectionViewModel.download(selectedIds)
                            selectionState.exit()
                        },
                        onAddToFavorite = {
                            selectionViewModel.addToFavorite(selectedIds)
                            selectionState.exit()
                        },
                    )
                }
                if (showSelectionAddToPlaylist) {
                    val selectedIds = selectionState.selected.toList()
                    val localPlaylists by selectionViewModel.listLocalPlaylist.collectAsStateWithLifecycle()
                    AddToPlaylistModalBottomSheet(
                        isBottomSheetVisible = true,
                        listLocalPlaylist = localPlaylists,
                        listYouTubePlaylist = emptyList(),
                        onDismiss = { showSelectionAddToPlaylist = false },
                        onClick = { playlist ->
                            selectionViewModel.addToPlaylist(playlist.id, selectedIds)
                            selectionState.exit()
                        },
                        onYTPlaylistClick = {},
                    )
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
                    visible = shouldHideTopBar && !showSearchBar && !selectionState.isActive,
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
                                    SimpIcons.ArrowBackIosNew,
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
                                Icon(SimpIcons.Search, null, tint = Color.White)
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                        modifier =
                            Modifier.hazeEffect(hazeState) {
                                blurEnabled = true
                                blurRadius = 24.dp
                                backgroundColor = mutedPaletteBg
                                tints = listOf(HazeTint(mutedPaletteBg.copy(alpha = 0.55f)))
                            },
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