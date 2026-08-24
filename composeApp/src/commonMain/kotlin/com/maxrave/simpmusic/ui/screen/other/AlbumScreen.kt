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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.rememberPaletteState
import com.kyant.backdrop.highlight.Highlight
import com.maxrave.domain.data.entities.DownloadState
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.toImmersiveBackground
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.HomeItemContentPlaylist
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlaylistBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.component.liquidGlass
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.component.selection.SelectedSongsBottomSheet
import com.maxrave.simpmusic.ui.component.selection.SongSelectionTopAppBar
import com.maxrave.simpmusic.ui.component.selection.rememberSongSelectionState
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.DownloadForOffline
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.Pause
import com.maxrave.simpmusic.ui.icon.PlayArrow
import com.maxrave.simpmusic.ui.icon.Shuffle
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.AlbumDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.seed
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.AlbumViewModel
import com.maxrave.simpmusic.viewModel.LocalPlaylistState
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.SongSelectionViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
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
import simpmusic.composeapp.generated.resources.baseline_downloaded
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.downloading
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
    var albumBottomSheetShow by rememberSaveable { mutableStateOf(false) }
    var chosenSong: Track? by remember { mutableStateOf(null) }

    val selectionState = rememberSongSelectionState()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()
    var showSelectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSelectionAddToPlaylist by rememberSaveable { mutableStateOf(false) }

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

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null && paletteGeneratedFor != uiState.thumbnail) {
            paletteState.generate(bm)
            paletteGeneratedFor = uiState.thumbnail
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
    val dominantColor = uiState.colors.firstOrNull() ?: Color.Black
    // Apple Music-style page background from the artwork's dominant tone (see UIExt.toImmersiveBackground).
    val mutedPaletteBg = paletteState.palette.toImmersiveBackground()

    Crossfade(uiState.loadState) {
        when (it) {
            LocalPlaylistState.PlaylistLoadState.Success -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(mutedPaletteBg)
                            .hazeSource(hazeState),
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
                            Column(
                                Modifier
                                    .background(Color.Transparent),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    if (isPortrait) {
                                        // Apple Music-style: edge-to-edge artwork (taller than square,
                                        // ~half screen height) with title overlay + liquid glass buttons.
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
                                                            .data(uiState.thumbnail)
                                                            .diskCachePolicy(CachePolicy.ENABLED)
                                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                                            .diskCacheKey(uiState.thumbnail)
                                                            .memoryCacheKey(uiState.thumbnail)
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
                                                // Subtle bottom gradient — keeps artwork visible behind
                                                // the title text and blends artwork edge seamlessly into
                                                // the muted palette page background (Apple Music style).
                                                // Spans 70% of the artwork (not a fixed 200dp): the shorter
                                                // the ramp, the steeper the alpha, and a steep ramp is what
                                                // makes the fade read as an edge.
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .height((screenInfo.hDP * 0.35f).dp)
                                                            .align(Alignment.BottomCenter)
                                                            .background(artworkScrimBrush(mutedPaletteBg)),
                                                )
                                                // Title/artist/year overlay (centered horizontally like Apple Music)
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
                                                        text = uiState.title,
                                                        style = typo().titleLarge,
                                                        color = Color.White,
                                                        maxLines = 2,
                                                        textAlign = TextAlign.Center,
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = uiState.artist.name,
                                                        style = typo().titleSmall,
                                                        color = Color.White,
                                                        textAlign = TextAlign.Center,
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
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text =
                                                            stringResource(
                                                                Res.string.year_and_category,
                                                                uiState.year,
                                                                stringResource(Res.string.album),
                                                            ),
                                                        style = typo().bodyMedium,
                                                        color = Color(0xC4FFFFFF),
                                                        textAlign = TextAlign.Center,
                                                    )
                                                }
                                            }
                                            // Back button — liquid glass effect (Kyant backdrop)
                                            LiquidGlassIconButton(
                                                backdrop = artworkBackdrop,
                                                imageVector = SimpIcons.ArrowBackIosNew,
                                                modifier =
                                                    Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(12.dp)
                                                        .windowInsetsPadding(WindowInsets.statusBars)
                                                        .size(48.dp),
                                            ) {
                                                navController.navigateUp()
                                            }
                                            // Heart + More — liquid glass pill
                                            Row(
                                                modifier =
                                                    Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(12.dp)
                                                        .windowInsetsPadding(WindowInsets.statusBars)
                                                        .height(48.dp)
                                                        .liquidGlass(artworkBackdrop, RoundedCornerShape(24.dp)),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(48.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    HeartCheckBox(
                                                        size = 28,
                                                        checked = uiState.liked,
                                                        onStateChange = {
                                                            viewModel.setAlbumLike()
                                                        },
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { albumBottomSheetShow = true },
                                                ) {
                                                    Icon(
                                                        imageVector = SimpIcons.MoreVert,
                                                        contentDescription = "More",
                                                        tint = Color.White,
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Apple Music desktop header: back and the like/more pair on
                                        // their own top row, then a square artwork with the text column
                                        // and the action cluster laid out beside it rather than under it.
                                        // Built exactly like the portrait branch, which renders correctly on both
                                        // platforms: the backdrop SOURCE is the content column — artwork included,
                                        // so the recorded layer holds something to refract — and the glass buttons
                                        // are SIBLINGS placed with align(), never children of the source (that
                                        // nesting is the render-feedback loop that kills the RuntimeShader).
                                        val headerBackdrop = rememberBackdrop(Color.Black)
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
                                                                .data(uiState.thumbnail)
                                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                                .diskCacheKey(uiState.thumbnail)
                                                                .memoryCacheKey(uiState.thumbnail)
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
                                                            text = uiState.title,
                                                            style = typo().headlineSmall,
                                                            color = Color.White,
                                                            maxLines = 2,
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = uiState.artist.name,
                                                            style = typo().titleMedium,
                                                            // The app accent, standing in for the brand red
                                                            // Apple uses on this line.
                                                            color = seed,
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
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text =
                                                                stringResource(
                                                                    Res.string.year_and_category,
                                                                    uiState.year,
                                                                    stringResource(Res.string.album),
                                                                ),
                                                            style = typo().labelMedium,
                                                            color = Color(0xC4FFFFFF),
                                                        )
                                                        Spacer(modifier = Modifier.height(20.dp))
                                                        // Apple Music-style action row:
                                                        // [Shuffle][Play pill][Download] (cluster centered, all 48dp matching size)
                                                        val isThisPlaying =
                                                            playingVideoId.isNotEmpty() &&
                                                                playingPlaylistId == browseId.replaceFirst("VL", "")
                                                        Row(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Box(
                                                                modifier =
                                                                    Modifier
                                                                        .size(48.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color.White.copy(alpha = 0.12f))
                                                                        .clickable { viewModel.shuffle() },
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Icon(
                                                                    imageVector = SimpIcons.Shuffle,
                                                                    contentDescription = "Shuffle",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(22.dp),
                                                                )
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
                                                                                uiState.listTrack.firstOrNull()?.let {
                                                                                    viewModel.playTrack(it)
                                                                                }
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
                                                            Box(
                                                                modifier =
                                                                    Modifier
                                                                        .size(48.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color.White.copy(alpha = 0.12f)),
                                                                contentAlignment = Alignment.Center,
                                                            ) {
                                                                Crossfade(targetState = uiState.downloadState) { state ->
                                                                    when (state) {
                                                                        DownloadState.STATE_DOWNLOADED -> {
                                                                            Box(
                                                                                modifier =
                                                                                    Modifier
                                                                                        .fillMaxSize()
                                                                                        .clickable {
                                                                                            viewModel.makeToast(
                                                                                                runBlocking {
                                                                                                    getString(Res.string.downloaded)
                                                                                                },
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
                                                                                                runBlocking {
                                                                                                    getString(Res.string.downloading)
                                                                                                },
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
                                                                                        .clickable { viewModel.downloadFullAlbum() },
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
                                                Box(
                                                    modifier = Modifier.size(48.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    HeartCheckBox(
                                                        size = 28,
                                                        checked = uiState.liked,
                                                        onStateChange = {
                                                            viewModel.setAlbumLike()
                                                        },
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { albumBottomSheetShow = true },
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
                                            // Apple Music-style action row:
                                            // [Shuffle][Play pill][Download] (cluster centered, all 48dp matching size).
                                            // The landscape header carries its own copy of this cluster beside the
                                            // artwork, so it only belongs here in portrait.
                                            if (isPortrait) {
                                                val isThisPlaying =
                                                    playingVideoId.isNotEmpty() &&
                                                        playingPlaylistId == browseId.replaceFirst("VL", "")
                                                Row(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Box(
                                                        modifier =
                                                            Modifier
                                                                .size(48.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White.copy(alpha = 0.12f))
                                                                .clickable { viewModel.shuffle() },
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Icon(
                                                            imageVector = SimpIcons.Shuffle,
                                                            contentDescription = "Shuffle",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(22.dp),
                                                        )
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
                                                                        uiState.listTrack.firstOrNull()?.let {
                                                                            viewModel.playTrack(it)
                                                                        }
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
                                                    Box(
                                                        modifier =
                                                            Modifier
                                                                .size(48.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Crossfade(targetState = uiState.downloadState) { state ->
                                                            when (state) {
                                                                DownloadState.STATE_DOWNLOADED -> {
                                                                    Box(
                                                                        modifier =
                                                                            Modifier
                                                                                .fillMaxSize()
                                                                                .clickable {
                                                                                    viewModel.makeToast(
                                                                                        runBlocking {
                                                                                            getString(Res.string.downloaded)
                                                                                        },
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
                                                                                        runBlocking {
                                                                                            getString(Res.string.downloading)
                                                                                        },
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
                                                                                .clickable { viewModel.downloadFullAlbum() },
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
                            Column(modifier = Modifier.animateItem()) {
                                SongFullWidthItems(
                                    forceDark = true,
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
                                    selectionMode = selectionState.isActive,
                                    isSelected = selectionState.isSelected(item.videoId),
                                    onLongClick = { selectionState.start(it) },
                                    onSelectToggle = { selectionState.toggle(it) },
                                    modifier = Modifier,
                                )
                                if (index < uiState.trackCount - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                        thickness = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.12f),
                                    )
                                }
                            }
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
                                            forceDark = true,
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
                    visible = shouldHideTopBar && !selectionState.isActive,
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
                                    SimpIcons.ArrowBackIosNew,
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
                        modifier =
                            Modifier.hazeEffect(hazeState) {
                                blurEnabled = true
                                blurRadius = 24.dp
                                backgroundColor = mutedPaletteBg
                                tints = listOf(HazeTint(mutedPaletteBg.copy(alpha = 0.55f)))
                            },
                    )
                }
                AnimatedVisibility(
                    visible = selectionState.isActive,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    SongSelectionTopAppBar(
                        state = selectionState,
                        onSelectAll = {
                            selectionState.toggleSelectAll(uiState.listTrack.map { it.videoId })
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
                        // Deliberately does not exit yet: the playlist picker opens next and
                        // still needs the selection alive.
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
                if (albumBottomSheetShow) {
                    PlaylistBottomSheet(
                        onDismiss = { albumBottomSheetShow = false },
                        playlistId = browseId,
                        playlistName = uiState.title,
                        isYourYouTubePlaylist = false,
                        onSaveToLocal = {},
                        onAddToQueue = {
                            sharedViewModel.addListToQueue(
                                uiState.listTrack.toCollection(arrayListOf()),
                            )
                        },
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