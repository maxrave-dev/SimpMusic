@file:OptIn(ExperimentalMaterial3Api::class)

package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material.icons.rounded.ThumbsUpDown
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.rememberPaletteState
import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.RepeatState
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.toggleMiniPlayer
import com.maxrave.simpmusic.expect.ui.MediaPlayerView
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.expect.ui.PlatformCastButton
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.GradientAngle
import com.maxrave.simpmusic.extension.GradientOffset
import com.maxrave.simpmusic.extension.KeepScreenOn
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.getScreenSizeInfo
import com.maxrave.simpmusic.extension.hsvToColor
import com.maxrave.simpmusic.extension.isElementVisible
import com.maxrave.simpmusic.extension.parseTimestampToMilliseconds
import com.maxrave.simpmusic.extension.rememberIsInPipMode
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AIBadge
import com.maxrave.simpmusic.ui.component.AddToPlaylistModalBottomSheet
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.FullscreenLyricsSheet
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.InfoPlayerBottomSheet
import com.maxrave.simpmusic.ui.component.LyricsView
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PlayPauseButton
import com.maxrave.simpmusic.ui.component.PlayerControlLayout
import com.maxrave.simpmusic.ui.component.QueueBottomSheet
import com.maxrave.simpmusic.ui.component.VoteLyricsDialog
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.player.FullscreenDestination
import com.maxrave.simpmusic.ui.screen.other.getITunesCover
import com.maxrave.simpmusic.ui.theme.blackMoreOverlay
import com.maxrave.simpmusic.ui.theme.md_theme_dark_background
import com.maxrave.simpmusic.ui.theme.overlay
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetUIEvent
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.KuroMusic_lyrics
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.baseline_fullscreen_24
import simpmusic.composeapp.generated.resources.baseline_more_vert_24
import simpmusic.composeapp.generated.resources.baseline_playlist_add_24
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.description
import simpmusic.composeapp.generated.resources.holder
import simpmusic.composeapp.generated.resources.holder_video
import simpmusic.composeapp.generated.resources.like_and_dislike
import simpmusic.composeapp.generated.resources.line_synced
import simpmusic.composeapp.generated.resources.lyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_betterlyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_lrc
import simpmusic.composeapp.generated.resources.lyrics_provider_youtube
import simpmusic.composeapp.generated.resources.now_playing_upper
import simpmusic.composeapp.generated.resources.offline_mode
import simpmusic.composeapp.generated.resources.playing_on_device
import simpmusic.composeapp.generated.resources.published_at
import simpmusic.composeapp.generated.resources.rate_lyrics
import simpmusic.composeapp.generated.resources.rich_synced
import simpmusic.composeapp.generated.resources.show
import simpmusic.composeapp.generated.resources.spotify_lyrics_provider
import simpmusic.composeapp.generated.resources.unsynced
import simpmusic.composeapp.generated.resources.view_count
import kotlin.math.roundToLong

private const val TAG = "NowPlayingScreen"
private val RICH_SYNC_TIMESTAMP_REGEX = Regex("""<\d{2}:\d{2}\.\d{2,3}>\s*""")

@OptIn(ExperimentalFoundationApi::class, ExperimentalHazeMaterialsApi::class)
@ExperimentalMaterial3Api
@Composable
fun NowPlayingScreen(
    sharedViewModel: SharedViewModel = koinInject(),
    navController: NavController,
    onDismiss: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    val hideSheet: () -> Unit = {
        coroutineScope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        modifier =
            Modifier
                .fillMaxHeight(),
        onDismissRequest = {
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {},
        scrimColor = Color.Black,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        NowPlayingScreenContent(
            sharedViewModel = sharedViewModel,
            navController = navController,
            isExpanded = sheetState.currentValue == SheetValue.Expanded,
            dismissIcon = Icons.Rounded.KeyboardArrowDown,
            onDismiss = {
                hideSheet()
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun NowPlayingScreenContent(
    sharedViewModel: SharedViewModel = koinInject(),
    mediaPlayerHandler: MediaPlayerHandler = koinInject(),
    navController: NavController,
    isExpanded: Boolean,
    dismissIcon: ImageVector,
    onDismiss: () -> Unit = {},
) {
    val screenInfo = getScreenSizeInfo()

    val localDensity = LocalDensity.current
    val uriHandler = LocalUriHandler.current

    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val likeStatus by sharedViewModel.likeStatus.collectAsStateWithLifecycle()

    val shouldShowVideo by sharedViewModel.getVideo.collectAsStateWithLifecycle()
    val translatedVoteState by sharedViewModel.translatedVoteState.collectAsStateWithLifecycle()
    val lyricsVoteState by sharedViewModel.lyricsVoteState.collectAsStateWithLifecycle()

    val nowPlayingState by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val queueDataState by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val artworkQueue by remember {
        derivedStateOf { queueDataState?.data?.listTracks ?: emptyList() }
    }
    val nowPlayingVideoId: String? = nowPlayingState?.track?.videoId
    val currentOrderIndex by remember(artworkQueue, nowPlayingVideoId) {
        derivedStateOf { deriveOrderIndex(artworkQueue, nowPlayingVideoId) }
    }
    val isRepeatOne = controllerState.repeatState is RepeatState.One

    val artworkPagerState =
        rememberPagerState(
            initialPage = currentOrderIndex.coerceAtLeast(0),
            pageCount = { artworkQueue.size.coerceAtLeast(1) },
        )
    var isAnimatingFromPlayer by remember { mutableStateOf(false) }
    var isUserDraggingActive by remember { mutableStateOf(false) }

    LaunchedEffect(artworkPagerState) {
        snapshotFlow {
            artworkPagerState.isScrollInProgress to isAnimatingFromPlayer
        }.collect { (scrolling, animating) ->
            isUserDraggingActive = scrolling && !animating
        }
    }

    LaunchedEffect(currentOrderIndex, artworkQueue.size) {
        val target = currentOrderIndex
        if (!isUserDraggingActive &&
            artworkQueue.isNotEmpty() &&
            target in 0 until artworkQueue.size &&
            target != artworkPagerState.currentPage
        ) {
            isAnimatingFromPlayer = true
            try {
                artworkPagerState.animateScrollToPage(target)
            } finally {
                isAnimatingFromPlayer = false
            }
        }
    }

    LaunchedEffect(artworkPagerState, currentOrderIndex, artworkQueue.size) {
        snapshotFlow { artworkPagerState.settledPage }
            .distinctUntilChanged()
            .collect { settled ->
                if (isAnimatingFromPlayer) return@collect
                if (artworkQueue.isEmpty()) return@collect
                if (settled !in 0 until artworkQueue.size) return@collect
                if (settled == currentOrderIndex) return@collect

                runCatching {
                    when (val action = computeSeekAction(settled, currentOrderIndex)) {
                        ArtworkSeekAction.Next -> {
                            sharedViewModel.onUIEvent(UIEvent.Next)
                        }

                        ArtworkSeekAction.Previous -> {
                            sharedViewModel.onUIEvent(UIEvent.SkipToPrevious)
                        }

                        is ArtworkSeekAction.Skip -> {
                            mediaPlayerHandler.playMediaItemInMediaSource(action.index)
                        }

                        ArtworkSeekAction.NoOp -> {
                            Unit
                        }
                    }
                }.onFailure { error ->
                    Logger.w(TAG, "ArtworkPager seek failed: ${error.message}")
                }
            }
    }

    LaunchedEffect(artworkQueue.size) {
        if (artworkQueue.isNotEmpty() && artworkPagerState.currentPage >= artworkQueue.size) {
            runCatching { artworkPagerState.scrollToPage(artworkQueue.lastIndex) }
        }
    }

    val isInPipMode = rememberIsInPipMode()

    val mainScrollState = rememberScrollState()

    var showHideMiddleLayout by rememberSaveable {
        mutableStateOf(true)
    }

    var showSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showFullscreenLyrics by rememberSaveable {
        mutableStateOf(false)
    }

    var showQueueBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showInfoBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showVoteDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showAddToPlaylistDirectly by rememberSaveable {
        mutableStateOf(false)
    }

    var shouldShowToolbar by remember {
        mutableStateOf(false)
    }

    val paletteState = rememberPaletteState()

    val bgColor = MaterialTheme.colorScheme.background
    val startColor = remember { Animatable(bgColor) }
    val endColor = remember { Animatable(bgColor) }
    val gradientOffset by remember {
        mutableStateOf(GradientOffset(GradientAngle.CW135))
    }

    var spotShadowColor by remember {
        mutableStateOf(Color.White)
    }

    val blurBg by sharedViewModel.blurBg.collectAsStateWithLifecycle()

    var itunesCoverUrl by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(screenDataState.nowPlayingTitle, screenDataState.artistName) {
        itunesCoverUrl = null
        try {
            itunesCoverUrl = getITunesCover(screenDataState.nowPlayingTitle, screenDataState.artistName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(screenDataState) {
        Logger.d(TAG, "ScreenDataState: $screenDataState")
        showHideMiddleLayout = screenDataState.canvasData == null
        snapshotFlow { screenDataState.bitmap }.collectLatest {
            if (it != null) {
                paletteState.generate(it)
            }
        }
    }

    val isLightTopBg = startColor.value.luminance() > 0.5f
    val topContentColor = if (isLightTopBg) Color.Black else Color.White
    val topTextShadow = if (!isLightTopBg) Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 4f) else null

    LaunchedEffect(bgColor, isLightTopBg) {
        val targetEndColor = if (isLightTopBg) bgColor else md_theme_dark_background
        endColor.animateTo(targetEndColor, animationSpec = tween(1000))
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                if (it != null) {
                    spotShadowColor = it.getColorFromPalette()
                    startColor.animateTo(it.getColorFromPalette(), animationSpec = tween(1000))
                }
            }
    }

    var topAppBarHeightDp by rememberSaveable {
        mutableIntStateOf(0)
    }
    var middleLayoutHeightDp by rememberSaveable {
        mutableIntStateOf(0)
    }
    var infoLayoutHeightDp by rememberSaveable {
        mutableIntStateOf(0)
    }
    var middleLayoutPaddingDp by rememberSaveable {
        mutableIntStateOf(0)
    }
    val minimumPaddingDp by rememberSaveable {
        mutableIntStateOf(
            30,
        )
    }
    LaunchedEffect(
        topAppBarHeightDp,
        screenInfo,
        infoLayoutHeightDp,
        minimumPaddingDp,
    ) {
        if (topAppBarHeightDp > 0 && middleLayoutHeightDp > 0 && infoLayoutHeightDp > 0 && screenInfo.hDP > 0) {
            val result = (screenInfo.hDP - topAppBarHeightDp - middleLayoutHeightDp - infoLayoutHeightDp - minimumPaddingDp) / 2
            middleLayoutPaddingDp =
                if (result > minimumPaddingDp) {
                    result
                } else {
                    minimumPaddingDp
                }
        }
    }

    var isSliding by rememberSaveable {
        mutableStateOf(false)
    }
    var sliderValue by rememberSaveable {
        mutableFloatStateOf(0f)
    }
    LaunchedEffect(key1 = timelineState, key2 = isSliding) {
        if (!isSliding) {
            sliderValue =
                if (timelineState.total > 0L) {
                    timelineState.current.toFloat() * 100 / timelineState.total.toFloat()
                } else {
                    0f
                }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "crossfadeRainbow")
    val rainbowHue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "rainbowHue",
    )
    val rainbowColor = hsvToColor(rainbowHue, 1f, 1f)
    val sliderTrackColor by animateColorAsState(
        targetValue = if (timelineState.isCrossfading) rainbowColor else topContentColor,
        animationSpec = tween(300),
        label = "sliderCrossfadeColor",
    )

    var showHideControlLayout by rememberSaveable {
        mutableStateOf(true)
    }
    val controlLayoutAlpha: Float by animateFloatAsState(
        targetValue = if (showHideControlLayout) 1f else 0f,
        animationSpec =
            tween(
                durationMillis = 500,
                easing = LinearEasing,
            ),
        label = "ControlLayoutAlpha",
    )

    var showHideJob by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(key1 = showHideJob) {
        if (!showHideJob) {
            delay(5000)
            if (mainScrollState.value == 0) showHideControlLayout = false
            showHideJob = true
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            screenDataState
        }.distinctUntilChangedBy {
            it.canvasData?.url
        }.collectLatest {
            if (it.canvasData != null && mainScrollState.value == 0) {
                showHideJob = false
            } else {
                showHideJob = true
                showHideControlLayout = true
            }
        }
    }

    LaunchedEffect(key1 = showHideControlLayout) {
        if (showHideControlLayout && screenDataState.canvasData != null && mainScrollState.value == 0) {
            showHideJob = false
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { mainScrollState.value }
            .distinctUntilChanged()
            .collect {
                if (it > 0 && !showHideControlLayout && screenDataState.canvasData != null) {
                    showHideJob = true
                    showHideControlLayout = true
                } else if (showHideControlLayout && it == 0 && screenDataState.canvasData != null) {
                    showHideJob = false
                }
            }
    }

    var showHideFullscreenOverlay by rememberSaveable {
        mutableStateOf(false)
    }

    var canvasSubtitleLineIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }

    LaunchedEffect(key1 = showHideFullscreenOverlay) {
        if (showHideFullscreenOverlay) {
            delay(3000)
            showHideFullscreenOverlay = false
        }
    }

    LaunchedEffect(timelineState, screenDataState.lyricsData?.lyrics) {
        val lyrics = screenDataState.lyricsData?.lyrics
        if (lyrics == null || lyrics.syncType == "UNSYNCED" || lyrics.syncType == null) {
            canvasSubtitleLineIndex = -1
            return@LaunchedEffect
        }
        val lines = lyrics.lines ?: return@LaunchedEffect
        val translatedLines =
            screenDataState.lyricsData
                ?.translatedLyrics
                ?.first
                ?.lines
        if (timelineState.current > 0L) {
            lines.indices.forEach { i ->
                val startTimeMs = lines[i].startTimeMs.toLongOrNull() ?: 0L
                val endTimeMs =
                    if (i < lines.size - 1) {
                        lines[i + 1].startTimeMs.toLongOrNull() ?: 0L
                    } else {
                        startTimeMs + 60000
                    }
                if (timelineState.current in startTimeMs..endTimeMs) {
                    canvasSubtitleLineIndex = i
                }
            }
            if (lines.isNotEmpty() &&
                timelineState.current in 0..(lines.getOrNull(0)?.startTimeMs?.toLongOrNull() ?: 0L)
            ) {
                canvasSubtitleLineIndex = -1
            }
        } else {
            canvasSubtitleLineIndex = -1
        }
    }

    if (showSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showSheet = false
            },
            navController = navController,
            onNavigateToOtherScreen = {
                onDismiss()
            },
            song = null,
            setSleepTimerEnable = true,
            changeMainLyricsProviderEnable = true,
        )
    }

    if (showFullscreenLyrics) {
        FullscreenLyricsSheet(
            sharedViewModel = sharedViewModel,
            navController = navController,
            color = startColor.value,
            shouldHaze = sharedViewModel.blurFullscreenLyrics(),
        ) {
            showFullscreenLyrics = false
        }
    }

    if (showQueueBottomSheet) {
        QueueBottomSheet(
            onDismiss = {
                showQueueBottomSheet = false
            },
        )
    }

    if (showInfoBottomSheet) {
        InfoPlayerBottomSheet(
            onDismiss = {
                showInfoBottomSheet = false
            },
        )
    }

    if (showAddToPlaylistDirectly) {
        val viewModel: NowPlayingBottomSheetViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.resetPlaylists()
            viewModel.setSongEntity(null)
        }

        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist = uiState.listLocalPlaylist,
            listYouTubePlaylist = uiState.listYouTubePlaylist,
            onDismiss = { showAddToPlaylistDirectly = false },
            onClick = { playlist ->
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToPlaylist(playlist.id))
                showAddToPlaylistDirectly = false
            },
            onYTPlaylistClick = { playlist ->
                viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToYouTubePlaylist(playlist.browseId))
                showAddToPlaylistDirectly = false
            },
            videoId = uiState.songUIState.videoId,
        )
    }

    if (showVoteDialog) {
        val canVoteLyrics =
            screenDataState.lyricsData?.lyricsProvider == LyricsProvider.SIMPMUSIC &&
                screenDataState.lyricsData
                    ?.lyrics
                    ?.simpMusicLyrics != null
        val canVoteTranslatedLyrics =
            screenDataState.lyricsData?.translatedLyrics?.second == LyricsProvider.SIMPMUSIC &&
                screenDataState.lyricsData
                    ?.translatedLyrics
                    ?.first
                    ?.simpMusicLyrics != null

        VoteLyricsDialog(
            canVoteLyrics = canVoteLyrics,
            canVoteTranslatedLyrics = canVoteTranslatedLyrics,
            lyricsVoteState = lyricsVoteState,
            translatedLyricsVoteState = translatedVoteState,
            onVoteLyrics = { upvote ->
                sharedViewModel.voteLyrics(upvote)
            },
            onVoteTranslatedLyrics = { upvote ->
                sharedViewModel.voteTranslatedLyrics(upvote)
            },
            onDismiss = {
                showVoteDialog = false
            },
        )
    }

    val hazeState =
        rememberHazeState(
            blurEnabled = true,
        )

    if (screenDataState.lyricsData != null && controllerState.isPlaying) {
        KeepScreenOn()
    }
    Box {
        if (blurBg && screenDataState.canvasData == null) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(itunesCoverUrl ?: screenDataState.thumbnailURL)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey((itunesCoverUrl ?: screenDataState.thumbnailURL) + "BIGGER")
                        .crossfade(550)
                        .build(),
                contentDescription = "",
                contentScale = ContentScale.FillHeight,
                placeholder = painterResource(Res.drawable.holder),
                error = painterResource(Res.drawable.holder),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxSize()
                        .hazeSource(hazeState),
            )
        }
        Column(
            Modifier
                .verticalScroll(
                    mainScrollState,
                    enabled = isExpanded,
                )
                .pointerInput(Unit) {
                    var isSwipeHandled = false
                    detectHorizontalDragGestures(
                        onDragEnd = { isSwipeHandled = false },
                    ) { change, dragAmount ->
                        change.consume()
                        if (!isSwipeHandled) {
                            when {
                                dragAmount < -90 -> {
                                    if (controllerState.isNextAvailable) {
                                        sharedViewModel.onUIEvent(UIEvent.Next)
                                        isSwipeHandled = true
                                    }
                                }

                                dragAmount > 90 -> {
                                    if (controllerState.isPreviousAvailable) {
                                        sharedViewModel.onUIEvent(UIEvent.Previous)
                                        isSwipeHandled = true
                                    }
                                }
                            }
                        }
                    }
                }.then(
                    if (showHideMiddleLayout) {
                        if (blurBg && screenDataState.canvasData == null) {
                            Modifier
                                .background(Color.Transparent)
                                .hazeEffect(hazeState, style = if (isLightTopBg) CupertinoMaterials.thin() else CupertinoMaterials.regular()) {
                                    blurEnabled = true
                                }
                        } else {
                            Modifier.background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        startColor.value,
                                        startColor.value.copy(alpha = 0.5f),
                                        endColor.value
                                    )
                                )
                            )
                        }
                    } else {
                        Modifier.background(bgColor)
                    },
                ),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = artworkPagerState,
                    modifier =
                        Modifier
                            .height(screenInfo.hDP.dp)
                            .fillMaxWidth(),
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !isRepeatOne && artworkQueue.isNotEmpty(),
                    key = { idx ->
                        val vid = artworkQueue.getOrNull(idx)?.videoId.orEmpty()
                        "artwork_${vid}_$idx"
                    },
                ) { page ->
                    val pageTrack = artworkQueue.getOrNull(page)
                    val isCurrentArtworkPage = page == currentOrderIndex
                    val pageHasCanvas = isCurrentArtworkPage && screenDataState.canvasData != null

                    val pagePaletteState = rememberPaletteState()
                    val pageStartColor =
                        remember(pageTrack?.videoId) {
                            Animatable(md_theme_dark_background)
                        }
                    LaunchedEffect(pagePaletteState, pageTrack?.videoId) {
                        snapshotFlow { pagePaletteState.palette }
                            .distinctUntilChanged()
                            .collectLatest { palette ->
                                pageStartColor.animateTo(
                                    palette.getColorFromPalette(),
                                )
                            }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .clickable(
                                    enabled = pageHasCanvas,
                                    onClick = {
                                        if (mainScrollState.value == 0) {
                                            showHideJob = true
                                            showHideControlLayout = !showHideControlLayout
                                        }
                                    },
                                    indication = null,
                                    interactionSource =
                                        remember {
                                            MutableInteractionSource()
                                        },
                                ),
                    ) {
                        if (!isCurrentArtworkPage && pageTrack != null) {
                            var pageItunesCover by rememberSaveable(pageTrack.videoId) { mutableStateOf<String?>(null) }
                            LaunchedEffect(pageTrack.videoId) {
                                try {
                                    val artistName = pageTrack.artists?.joinToString(", ") { it.name } ?: ""
                                    pageItunesCover = getITunesCover(pageTrack.title ?: "", artistName)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            val staticThumb = pageTrack.thumbnails?.maxByOrNull { it.width * it.height }?.url
                            val displayThumb = pageItunesCover ?: staticThumb

                            if (blurBg) {
                                AsyncImage(
                                    model =
                                        ImageRequest
                                            .Builder(LocalPlatformContext.current)
                                            .data(displayThumb)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .diskCacheKey(displayThumb)
                                            .crossfade(300)
                                            .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(Res.drawable.holder),
                                    error = painterResource(Res.drawable.holder),
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .alpha(0.35f),
                                )
                            } else {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors =
                                                        listOf(
                                                            pageStartColor.value,
                                                            endColor.value,
                                                        ),
                                                    start = gradientOffset.start,
                                                    end = gradientOffset.end,
                                                ),
                                            ),
                                )
                            }
                        }

                        if (pageHasCanvas) {
                            Crossfade(targetState = screenDataState.canvasData?.isVideo) { isVideo ->
                                if (isVideo == true) {
                                    screenDataState.canvasData?.url?.let { url ->
                                        MediaPlayerView(
                                            url = url,
                                            modifier =
                                                Modifier
                                                    .fillMaxHeight()
                                                    .then(
                                                        if (getPlatform() == Platform.Desktop) {
                                                            Modifier
                                                        } else {
                                                            Modifier
                                                                .wrapContentWidth(unbounded = true, align = Alignment.CenterHorizontally)
                                                                .align(Alignment.Center)
                                                        },
                                                    ),
                                        )
                                    }
                                } else if (isVideo == false) {
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(LocalPlatformContext.current)
                                                .data(screenDataState.canvasData?.url)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(screenDataState.canvasData?.url)
                                                .crossfade(550)
                                                .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            Crossfade(
                                targetState = showHideControlLayout,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .align(Alignment.BottomCenter),
                            ) { focused ->
                                if (focused) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colorStops =
                                                            arrayOf(
                                                                0.2f to overlay,
                                                                1f to Color.Black,
                                                            ),
                                                    ),
                                                ),
                                    )
                                } else {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colorStops =
                                                            arrayOf(
                                                                0f to Color.Transparent,
                                                                0.92f to Color.Transparent,
                                                                0.97f to Color.Black,
                                                                1f to Color.Black,
                                                            ),
                                                    ),
                                                ),
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.height(topAppBarHeightDp.dp))
                            CenterAlignedTopAppBar(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .onGloballyPositioned {
                                            topAppBarHeightDp =
                                                with(localDensity) {
                                                    it.size.height
                                                        .toDp()
                                                        .value
                                                        .toInt()
                                                }
                                        },
                                colors =
                                    TopAppBarDefaults.topAppBarColors().copy(
                                        containerColor = Color.Transparent,
                                    ),
                                windowInsets =
                                    TopAppBarDefaults.windowInsets.only(
                                        WindowInsetsSides.Top,
                                    ),
                                title = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.now_playing_upper),
                                            style = typo().bodyMedium.copy(shadow = topTextShadow),
                                            color = topContentColor,
                                        )
                                        Text(
                                            text = screenDataState.playlistName,
                                            style = typo().labelMedium.copy(shadow = topTextShadow),
                                            color = topContentColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                                    .basicMarquee(
                                                        iterations = Int.MAX_VALUE,
                                                        animationMode = MarqueeAnimationMode.Immediately,
                                                    ).focusable(),
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        onDismiss()
                                    }) {
                                        Icon(
                                            imageVector = dismissIcon,
                                            contentDescription = "",
                                            tint = topContentColor,
                                        )
                                    }
                                },
                                actions = {
                                    if (getPlatform() == Platform.Desktop) {
                                        IconButton(onClick = { toggleMiniPlayer() }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                                contentDescription = "Mini Player",
                                                tint = topContentColor,
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        showSheet = true
                                    }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.baseline_more_vert_24),
                                            contentDescription = "",
                                            tint = topContentColor,
                                        )
                                    }
                                },
                            )
                            Column {
                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            topAppBarHeightDp.dp,
                                        ),
                                )
                                Box {
                                    Column(
                                        Modifier
                                            .fillMaxWidth(),
                                    ) {
                                        Spacer(
                                            modifier =
                                                Modifier
                                                    .animateContentSize()
                                                    .height(middleLayoutPaddingDp.dp)
                                                    .fillMaxWidth(),
                                        )

                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp)
                                                    .alpha(if (pageHasCanvas) 0f else 1f)
                                                    .aspectRatio(1f),
                                        ) {
                                            if (isCurrentArtworkPage) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier =
                                                        Modifier
                                                            .align(Alignment.Center)
                                                            .background(Color.Transparent)
                                                            .shadow(
                                                                elevation = 3.dp,
                                                                shape = RoundedCornerShape(8.dp),
                                                                spotColor =
                                                                    spotShadowColor.copy(
                                                                        alpha = 0.6f,
                                                                    ),
                                                                ambientColor = Color.Transparent,
                                                            ),
                                                ) {
                                                    AsyncImage(
                                                        model =
                                                            ImageRequest
                                                                .Builder(LocalPlatformContext.current)
                                                                .data(itunesCoverUrl ?: screenDataState.thumbnailURL)
                                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                                .diskCacheKey((itunesCoverUrl ?: screenDataState.thumbnailURL) + "BIGGER")
                                                                .crossfade(550)
                                                                .build(),
                                                        contentDescription = "",
                                                        onSuccess = {
                                                            sharedViewModel.setBitmap(
                                                                it.result.image
                                                                    .toImageBitmap(),
                                                            )
                                                        },
                                                        contentScale = ContentScale.Crop,
                                                        placeholder = painterResource(Res.drawable.holder),
                                                        modifier =
                                                            Modifier
                                                                .align(Alignment.Center)
                                                                .padding(3.dp)
                                                                .fillMaxWidth()
                                                                .background(Color.Transparent)
                                                                .aspectRatio(1f)
                                                                .clip(
                                                                    RoundedCornerShape(8.dp),
                                                                ).alpha(
                                                                    if (!screenDataState.isVideo || !shouldShowVideo) 1f else 0f,
                                                                ),
                                                    )
                                                }

                                                androidx.compose.animation.AnimatedVisibility(
                                                    visible = screenDataState.isVideo && shouldShowVideo,
                                                    modifier = Modifier.align(Alignment.Center),
                                                ) {
                                                    var internalShowSubtitle by rememberSaveable {
                                                        mutableStateOf(true)
                                                    }
                                                    Box(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .aspectRatio(16f / 9)
                                                                .clip(
                                                                    RoundedCornerShape(8.dp),
                                                                ).background(
                                                                    MaterialTheme.colorScheme.background,
                                                                ),
                                                    ) {
                                                        Box(Modifier.fillMaxSize()) {
                                                            MediaPlayerViewWithSubtitle(
                                                                playerName = MAIN_PLAYER,
                                                                modifier = Modifier.align(Alignment.Center),
                                                                shouldShowSubtitle = internalShowSubtitle,
                                                                shouldPip = false,
                                                                shouldScaleDownSubtitle = true,
                                                                timelineState = timelineState,
                                                                lyricsData = screenDataState.lyricsData?.lyrics,
                                                                translatedLyricsData = screenDataState.lyricsData?.translatedLyrics?.first,
                                                                isInPipMode = isInPipMode,
                                                                mainTextStyle = typo().bodyLarge,
                                                                translatedTextStyle = typo().bodyMedium,
                                                            )
                                                        }
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxSize()
                                                                    .clickable(
                                                                        onClick = { showHideFullscreenOverlay = !showHideFullscreenOverlay },
                                                                        indication = null,
                                                                        interactionSource =
                                                                            remember {
                                                                                MutableInteractionSource()
                                                                            },
                                                                    ),
                                                        ) {
                                                            Crossfade(
                                                                targetState = showHideFullscreenOverlay,
                                                            ) {
                                                                if (it) {
                                                                    Box(
                                                                        modifier =
                                                                            Modifier
                                                                                .fillMaxSize()
                                                                                .background(
                                                                                    Brush.verticalGradient(
                                                                                        colorStops =
                                                                                            arrayOf(
                                                                                                0.03f to blackMoreOverlay,
                                                                                                0.15f to overlay,
                                                                                                0.8f to Color.Transparent,
                                                                                            ),
                                                                                    ),
                                                                                ),
                                                                    ) {
                                                                        IconButton(onClick = {
                                                                            onDismiss()
                                                                            navController.navigate(
                                                                                FullscreenDestination,
                                                                            )
                                                                        }, Modifier.align(Alignment.TopEnd)) {
                                                                            Icon(
                                                                                painter = painterResource(Res.drawable.baseline_fullscreen_24),
                                                                                contentDescription = "",
                                                                                tint = Color.White,
                                                                            )
                                                                        }
                                                                        CompositionLocalProvider(LocalContentColor provides Color.White) {
                                                                            Row(
                                                                                Modifier
                                                                                    .align(Alignment.Center)
                                                                                    .fillMaxWidth(),
                                                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                                            ) {
                                                                                FilledTonalIconButton(
                                                                                    colors =
                                                                                        IconButtonDefaults.iconButtonColors().copy(
                                                                                            containerColor = Color.Transparent,
                                                                                        ),
                                                                                    modifier =
                                                                                        Modifier
                                                                                            .size(48.dp)
                                                                                            .aspectRatio(1f)
                                                                                            .clip(
                                                                                                CircleShape,
                                                                                            ),
                                                                                    onClick = {
                                                                                        sharedViewModel.onUIEvent(UIEvent.Backward)
                                                                                    },
                                                                                ) {
                                                                                    Icon(
                                                                                        imageVector = Icons.Rounded.Replay5,
                                                                                        contentDescription = "",
                                                                                        modifier =
                                                                                            Modifier
                                                                                                .size(36.dp)
                                                                                                .alpha(0.8f),
                                                                                    )
                                                                                }
                                                                                FilledTonalIconButton(
                                                                                    colors =
                                                                                        IconButtonDefaults.iconButtonColors().copy(
                                                                                            containerColor = Color.Transparent,
                                                                                        ),
                                                                                    modifier =
                                                                                        Modifier
                                                                                            .size(48.dp)
                                                                                            .aspectRatio(1f)
                                                                                            .clip(
                                                                                                CircleShape,
                                                                                            ),
                                                                                    onClick = {
                                                                                        sharedViewModel.onUIEvent(UIEvent.Forward)
                                                                                    },
                                                                                ) {
                                                                                    Icon(
                                                                                        imageVector = Icons.Rounded.Forward5,
                                                                                        contentDescription = "",
                                                                                        modifier =
                                                                                            Modifier
                                                                                                .size(36.dp)
                                                                                                .alpha(0.8f),
                                                                                    )
                                                                                }
                                                                            }
                                                                            if (screenDataState.lyricsData != null) {
                                                                                IconButton(
                                                                                    onClick = {
                                                                                        internalShowSubtitle = !internalShowSubtitle
                                                                                    },
                                                                                    Modifier.align(Alignment.BottomEnd),
                                                                                ) {
                                                                                    Icon(
                                                                                        imageVector =
                                                                                            if (internalShowSubtitle) {
                                                                                                Icons.Filled.SubtitlesOff
                                                                                            } else {
                                                                                                Icons.Filled.Subtitles
                                                                                            },
                                                                                        contentDescription = "",
                                                                                        tint = MaterialTheme.colorScheme.onBackground,
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
                                            } else if (pageTrack != null) {
                                                var pageItunesCover by rememberSaveable(pageTrack.videoId) { mutableStateOf<String?>(null) }
                                                LaunchedEffect(pageTrack.videoId) {
                                                    try {
                                                        val artistName = pageTrack.artists?.joinToString(", ") { it.name } ?: ""
                                                        pageItunesCover = getITunesCover(pageTrack.title ?: "", artistName)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }

                                                val staticThumb = pageTrack.thumbnails?.maxByOrNull { it.width * it.height }?.url
                                                val displayThumb = pageItunesCover ?: staticThumb

                                                val palettePageScope = rememberCoroutineScope()
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier =
                                                        Modifier
                                                            .align(Alignment.Center)
                                                            .background(Color.Transparent)
                                                            .shadow(
                                                                elevation = 3.dp,
                                                                shape = RoundedCornerShape(8.dp),
                                                                spotColor = Color.Black.copy(alpha = 0.4f),
                                                                ambientColor = Color.Transparent,
                                                            ),
                                                ) {
                                                    AsyncImage(
                                                        model =
                                                            ImageRequest
                                                                .Builder(LocalPlatformContext.current)
                                                                .data(displayThumb)
                                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                                .diskCacheKey(displayThumb)
                                                                .crossfade(300)
                                                                .build(),
                                                        contentDescription = pageTrack.title,
                                                        contentScale = ContentScale.Crop,
                                                        placeholder = painterResource(Res.drawable.holder),
                                                        error = painterResource(Res.drawable.holder),
                                                        onSuccess = { state ->
                                                            palettePageScope.launch {
                                                                pagePaletteState.generate(
                                                                    state.result.image.toImageBitmap(),
                                                                )
                                                            }
                                                        },
                                                        modifier =
                                                            Modifier
                                                                .align(Alignment.Center)
                                                                .padding(3.dp)
                                                                .fillMaxWidth()
                                                                .aspectRatio(1f)
                                                                .clip(RoundedCornerShape(8.dp)),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            CenterAlignedTopAppBar(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .onGloballyPositioned {
                                            topAppBarHeightDp =
                                                with(localDensity) {
                                                    it.size.height
                                                        .toDp()
                                                        .value
                                                        .toInt()
                                                }
                                        },
                                colors =
                                    TopAppBarDefaults.topAppBarColors().copy(
                                        containerColor = Color.Transparent,
                                    ),
                                windowInsets =
                                    TopAppBarDefaults.windowInsets.only(
                                        WindowInsetsSides.Top,
                                    ),
                                title = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.now_playing_upper),
                                            style = typo().bodyMedium.copy(shadow = topTextShadow),
                                            color = topContentColor,
                                        )
                                        Text(
                                            text = screenDataState.playlistName,
                                            style = typo().labelMedium.copy(shadow = topTextShadow),
                                            color = topContentColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                                    .basicMarquee(
                                                        iterations = Int.MAX_VALUE,
                                                        animationMode = MarqueeAnimationMode.Immediately,
                                                    ).focusable(),
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        onDismiss()
                                    }) {
                                        Icon(
                                            imageVector = dismissIcon,
                                            contentDescription = "",
                                            tint = topContentColor,
                                        )
                                    }
                                },
                                actions = {
                                    if (getPlatform() == Platform.Desktop) {
                                        IconButton(onClick = { toggleMiniPlayer() }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                                contentDescription = "Mini Player",
                                                tint = topContentColor,
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        showSheet = true
                                    }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.baseline_more_vert_24),
                                            contentDescription = "",
                                            tint = topContentColor,
                                        )
                                    }
                                },
                            )
                            Column {
                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            topAppBarHeightDp.dp,
                                        ),
                                )
                                Box {
                                    Column(
                                        Modifier
                                            .fillMaxWidth(),
                                    ) {
                                        Spacer(
                                            modifier =
                                                Modifier
                                                    .animateContentSize()
                                                    .height(
                                                        middleLayoutPaddingDp.dp,
                                                    ).fillMaxWidth(),
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp)
                                                    .onGloballyPositioned { coords ->
                                                        middleLayoutHeightDp =
                                                            with(localDensity) {
                                                                coords.size.height
                                                                    .toDp()
                                                                    .value
                                                                    .toInt()
                                                            }
                                                    }.aspectRatio(1f),
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier
                                                    .animateContentSize()
                                                    .height(
                                                        middleLayoutPaddingDp.dp,
                                                    ).fillMaxWidth(),
                                        )

                                        Box {
                                            Column(
                                                Modifier
                                                    .alpha(controlLayoutAlpha)
                                                    .onGloballyPositioned {
                                                        infoLayoutHeightDp =
                                                            with(localDensity) {
                                                                it.size.height
                                                                    .toDp()
                                                                    .value
                                                                    .toInt()
                                                            }
                                                    },
                                            ) {
                                                Row(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 20.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    AnimatedVisibility(screenDataState.canvasData != null) {
                                                        AsyncImage(
                                                            model =
                                                                ImageRequest
                                                                    .Builder(LocalPlatformContext.current)
                                                                    .data(itunesCoverUrl ?: screenDataState.thumbnailURL)
                                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                                    .diskCacheKey((itunesCoverUrl ?: screenDataState.thumbnailURL) + "BIGGER")
                                                                    .crossfade(true)
                                                                    .build(),
                                                            placeholder = painterResource(Res.drawable.holder),
                                                            error = painterResource(Res.drawable.holder),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.FillWidth,
                                                            modifier =
                                                                Modifier
                                                                    .heightIn(0.dp, 55.dp)
                                                                    .width(55.dp)
                                                                    .padding(end = 10.dp)
                                                                    .clip(
                                                                        RoundedCornerShape(4.dp),
                                                                    ).align(Alignment.CenterVertically),
                                                        )
                                                    }

                                                    Column(Modifier.weight(1f)) {
                                                        Text(
                                                            text = screenDataState.nowPlayingTitle,
                                                            style = typo().headlineMedium.copy(shadow = topTextShadow),
                                                            maxLines = 1,
                                                            color = topContentColor,
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxWidth()
                                                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                                                    .basicMarquee(
                                                                        iterations = Int.MAX_VALUE,
                                                                        animationMode = MarqueeAnimationMode.Immediately,
                                                                    ).focusable(),
                                                        )
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        LazyRow(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            item(screenDataState.isExplicit) {
                                                                AnimatedVisibility(visible = screenDataState.isExplicit) {
                                                                    ExplicitBadge(
                                                                        modifier =
                                                                            Modifier
                                                                                .size(20.dp)
                                                                                .padding(end = 4.dp)
                                                                                .weight(1f),
                                                                    )
                                                                }
                                                            }
                                                            item(screenDataState.artistName) {
                                                                Text(
                                                                    text = screenDataState.artistName,
                                                                    style = typo().bodyMedium.copy(shadow = topTextShadow),
                                                                    color = topContentColor,
                                                                    maxLines = 1,
                                                                    modifier =
                                                                        Modifier
                                                                            .fillMaxWidth()
                                                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                                                            .basicMarquee(
                                                                                iterations = Int.MAX_VALUE,
                                                                                animationMode = MarqueeAnimationMode.Immediately,
                                                                            ).focusable()
                                                                            .clickable {
                                                                                val song = sharedViewModel.nowPlayingState.value?.songEntity
                                                                                (
                                                                                    song?.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() }
                                                                                        ?: screenDataState.songInfoData?.authorId
                                                                                    )?.let { channelId ->
                                                                                        onDismiss()
                                                                                        navController.navigate(
                                                                                            ArtistDestination(
                                                                                                channelId = channelId,
                                                                                            ),
                                                                                        )
                                                                                    }
                                                                            },
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (sharedViewModel.isUserLoggedIn()) {
                                                        Spacer(modifier = Modifier.size(16.dp))
                                                        Crossfade(
                                                            targetState = likeStatus,
                                                        ) {
                                                            if (it) {
                                                                IconButton(
                                                                    modifier =
                                                                        Modifier
                                                                            .size(24.dp)
                                                                            .aspectRatio(1f)
                                                                            .clip(
                                                                                CircleShape,
                                                                            ),
                                                                    onClick = {
                                                                        sharedViewModel.addToYouTubeLiked()
                                                                    },
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Rounded.CheckCircle,
                                                                        tint = topContentColor,
                                                                        contentDescription = ""
                                                                    )
                                                                }
                                                            } else {
                                                                IconButton(
                                                                    modifier =
                                                                        Modifier
                                                                            .size(24.dp)
                                                                            .aspectRatio(1f)
                                                                            .clip(
                                                                                CircleShape,
                                                                            ),
                                                                    onClick = {
                                                                        sharedViewModel.addToYouTubeLiked()
                                                                    },
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Rounded.AddCircleOutline,
                                                                        tint = topContentColor,
                                                                        contentDescription = "",
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.size(12.dp))
                                                    CompositionLocalProvider(LocalContentColor provides topContentColor) {
                                                        HeartCheckBox(checked = controllerState.isLiked, size = 32) {
                                                            sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                                                        }
                                                    }
                                                }
                                                if (getPlatform() == Platform.Android) {
                                                    Box(
                                                        Modifier
                                                            .padding(
                                                                top = 15.dp,
                                                            ).padding(horizontal = 20.dp)
                                                            .isElementVisible {
                                                                shouldShowToolbar = !it && isExpanded && mainScrollState.value > 0
                                                            },
                                                    ) {
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxWidth()
                                                                    .height(24.dp),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Crossfade(timelineState.loading) {
                                                                if (it) {
                                                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                                        LinearProgressIndicator(
                                                                            modifier =
                                                                                Modifier
                                                                                    .fillMaxWidth()
                                                                                    .height(4.dp)
                                                                                    .padding(
                                                                                        horizontal = 3.dp,
                                                                                    ).clip(
                                                                                        RoundedCornerShape(8.dp),
                                                                                    ),
                                                                            color = topContentColor.copy(alpha = 0.5f),
                                                                            trackColor = topContentColor.copy(alpha = 0.2f),
                                                                            strokeCap = StrokeCap.Round,
                                                                        )
                                                                    }
                                                                } else {
                                                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                                        LinearProgressIndicator(
                                                                            progress = { timelineState.bufferedPercent.toFloat() / 100 },
                                                                            modifier =
                                                                                Modifier
                                                                                    .fillMaxWidth()
                                                                                    .height(4.dp)
                                                                                    .padding(
                                                                                        horizontal = 3.dp,
                                                                                    ).clip(
                                                                                        RoundedCornerShape(8.dp),
                                                                                    ),
                                                                            color = topContentColor.copy(alpha = 0.5f),
                                                                            trackColor =
                                                                                topContentColor.copy(
                                                                                    alpha = 0.2f,
                                                                                ),
                                                                            strokeCap = StrokeCap.Round,
                                                                            drawStopIndicator = {},
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                            Slider(
                                                                value = sliderValue,
                                                                onValueChangeFinished = {
                                                                    isSliding = false
                                                                    sharedViewModel.onUIEvent(
                                                                        UIEvent.UpdateProgress(sliderValue),
                                                                    )
                                                                },
                                                                onValueChange = {
                                                                    isSliding = true
                                                                    sliderValue = it
                                                                },
                                                                valueRange = 0f..100f,
                                                                modifier =
                                                                    Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(top = 3.dp)
                                                                        .align(
                                                                            Alignment.TopCenter,
                                                                        ),
                                                                track = { sliderState ->
                                                                    SliderDefaults.Track(
                                                                        modifier =
                                                                            Modifier
                                                                                .height(5.dp),
                                                                        enabled = true,
                                                                        sliderState = sliderState,
                                                                        colors =
                                                                            SliderDefaults.colors().copy(
                                                                                thumbColor = sliderTrackColor,
                                                                                activeTrackColor = sliderTrackColor,
                                                                                inactiveTrackColor = Color.Transparent,
                                                                            ),
                                                                        thumbTrackGapSize = 0.dp,
                                                                        drawTick = { _, _ -> },
                                                                        drawStopIndicator = null,
                                                                    )
                                                                },
                                                                thumb = {
                                                                    SliderDefaults.Thumb(
                                                                        modifier =
                                                                            Modifier
                                                                                .height(18.dp)
                                                                                .width(8.dp)
                                                                                .padding(
                                                                                    vertical = 4.dp,
                                                                                ),
                                                                        thumbSize = DpSize(8.dp, 8.dp),
                                                                        interactionSource =
                                                                            remember {
                                                                                MutableInteractionSource()
                                                                            },
                                                                        colors =
                                                                            SliderDefaults.colors().copy(
                                                                                thumbColor = sliderTrackColor,
                                                                                activeTrackColor = sliderTrackColor,
                                                                                inactiveTrackColor = Color.Transparent,
                                                                            ),
                                                                        enabled = true,
                                                                    )
                                                                },
                                                            )
                                                        }
                                                    }
                                                    Row(
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 20.dp),
                                                    ) {
                                                        Text(
                                                            text = formatDuration((timelineState.total * (sliderValue / 100f)).roundToLong()),
                                                            style = typo().bodyMedium.copy(shadow = topTextShadow),
                                                            color = topContentColor,
                                                            modifier = Modifier.weight(1f),
                                                            textAlign = TextAlign.Left,
                                                        )
                                                        AnimatedVisibility(
                                                            enter = fadeIn(),
                                                            exit = fadeOut(),
                                                            visible = timelineState.isCrossfading,
                                                        ) {
                                                            Text(
                                                                text = stringResource(Res.string.crossfading),
                                                                style = typo().bodyMedium.copy(shadow = topTextShadow),
                                                                color = topContentColor,
                                                                modifier = Modifier.weight(1f),
                                                                textAlign = TextAlign.Center,
                                                            )
                                                        }
                                                        Text(
                                                            text = formatDuration(timelineState.total),
                                                            style = typo().bodyMedium.copy(shadow = topTextShadow),
                                                            color = topContentColor,
                                                            modifier = Modifier.weight(1f),
                                                            textAlign = TextAlign.Right,
                                                        )
                                                    }

                                                    Spacer(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .height(5.dp),
                                                    )
                                                    CompositionLocalProvider(LocalContentColor provides topContentColor) {
                                                        PlayerControlLayout(controllerState) {
                                                            sharedViewModel.onUIEvent(it)
                                                        }
                                                    }
                                                } else {
                                                    Spacer(Modifier.height(16.dp))
                                                }
                                                Row(
                                                    modifier =
                                                        Modifier
                                                            .height(32.dp)
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 20.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f, fill = false),
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        IconButton(
                                                            modifier =
                                                                Modifier
                                                                    .size(24.dp)
                                                                    .aspectRatio(1f)
                                                                    .clip(CircleShape),
                                                            onClick = {
                                                                showInfoBottomSheet = true
                                                            },
                                                        ) {
                                                            Icon(imageVector = Icons.Outlined.Info, tint = topContentColor, contentDescription = "")
                                                        }
                                                        PlatformCastButton(
                                                            modifier = Modifier.size(24.dp),
                                                            tint = topContentColor,
                                                        )
                                                    }

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        IconButton(
                                                            modifier =
                                                                Modifier
                                                                    .size(24.dp)
                                                                    .aspectRatio(1f)
                                                                    .clip(CircleShape),
                                                            onClick = {
                                                                showAddToPlaylistDirectly = true
                                                            },
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(Res.drawable.baseline_playlist_add_24),
                                                                tint = topContentColor,
                                                                contentDescription = "Add to Playlist",
                                                            )
                                                        }

                                                        IconButton(
                                                            modifier =
                                                                Modifier
                                                                    .size(24.dp)
                                                                    .aspectRatio(1f)
                                                                    .clip(CircleShape),
                                                            onClick = {
                                                                showQueueBottomSheet = true
                                                            },
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                                                tint = topContentColor,
                                                                contentDescription = "",
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = !showHideControlLayout,
                                                enter = fadeIn(),
                                                exit = fadeOut(),
                                            ) {
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .height(
                                                                infoLayoutHeightDp.dp,
                                                            ).fillMaxWidth()
                                                            .clickable(
                                                                onClick = {
                                                                    if (mainScrollState.value == 0) {
                                                                        showHideJob = true
                                                                        showHideControlLayout = !showHideControlLayout
                                                                    }
                                                                },
                                                                indication = null,
                                                                interactionSource =
                                                                    remember {
                                                                        MutableInteractionSource()
                                                                    },
                                                            ),
                                                    contentAlignment = Alignment.BottomStart,
                                                ) {
                                                    Box(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxSize()
                                                                .background(
                                                                    Brush.verticalGradient(
                                                                        colorStops =
                                                                            arrayOf(
                                                                                0f to Color.Transparent,
                                                                                1f to Color.Black.copy(alpha = 0.85f),
                                                                            ),
                                                                    ),
                                                                ),
                                                    )

                                                    Column(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .animateContentSize(),
                                                    ) {
                                                        AnimatedVisibility(
                                                            visible = canvasSubtitleLineIndex > -1,
                                                            enter = fadeIn() + expandVertically(),
                                                            exit = fadeOut() + shrinkVertically(),
                                                        ) {
                                                            val lineText =
                                                                screenDataState.lyricsData
                                                                    ?.lyrics
                                                                    ?.lines
                                                                    ?.getOrNull(canvasSubtitleLineIndex)
                                                                    ?.words
                                                                    ?.replace(RICH_SYNC_TIMESTAMP_REGEX, "")
                                                                    ?.trim()
                                                            if (!lineText.isNullOrBlank()) {
                                                                Column(
                                                                    modifier = Modifier.fillMaxWidth().clickable { showFullscreenLyrics = true },
                                                                ) {
                                                                    Text(
                                                                        modifier =
                                                                            Modifier
                                                                                .fillMaxWidth()
                                                                                .padding(horizontal = 20.dp)
                                                                                .padding(bottom = 4.dp)
                                                                                .basicMarquee(
                                                                                    iterations = Int.MAX_VALUE,
                                                                                    animationMode = MarqueeAnimationMode.Immediately,
                                                                                ).focusable(),
                                                                        text = lineText,
                                                                        style = typo().bodyMedium.copy(shadow = topTextShadow),
                                                                        color = topContentColor,
                                                                        maxLines = 1,
                                                                    )
                                                                    val translatedLineText =
                                                                        screenDataState.lyricsData
                                                                            ?.translatedLyrics
                                                                            ?.first
                                                                            ?.lines
                                                                            ?.getOrNull(canvasSubtitleLineIndex)
                                                                            ?.words
                                                                            ?.replace(RICH_SYNC_TIMESTAMP_REGEX, "")
                                                                            ?.trim()
                                                                    if (!translatedLineText.isNullOrBlank()) {
                                                                        Text(
                                                                            modifier =
                                                                                Modifier
                                                                                    .fillMaxWidth()
                                                                                    .padding(horizontal = 20.dp)
                                                                                    .padding(bottom = 8.dp)
                                                                                    .basicMarquee(
                                                                                        iterations = Int.MAX_VALUE,
                                                                                        animationMode = MarqueeAnimationMode.Immediately,
                                                                                    ).focusable(),
                                                                            text = translatedLineText,
                                                                            style = typo().bodyMedium,
                                                                            color = Color.Yellow,
                                                                            maxLines = 1,
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        Row(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 20.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            AnimatedVisibility(screenDataState.canvasData != null) {
                                                                AsyncImage(
                                                                    model =
                                                                        ImageRequest
                                                                            .Builder(LocalPlatformContext.current)
                                                                            .data(itunesCoverUrl ?: screenDataState.thumbnailURL)
                                                                            .diskCachePolicy(CachePolicy.ENABLED)
                                                                            .diskCacheKey((itunesCoverUrl ?: screenDataState.thumbnailURL) + "BIGGER")
                                                                            .crossfade(true)
                                                                            .build(),
                                                                    placeholder = painterResource(Res.drawable.holder),
                                                                    error = painterResource(Res.drawable.holder),
                                                                    contentDescription = null,
                                                                    contentScale = ContentScale.FillWidth,
                                                                    modifier =
                                                                        Modifier
                                                                            .heightIn(0.dp, 55.dp)
                                                                            .width(55.dp)
                                                                            .padding(end = 10.dp)
                                                                            .clip(
                                                                                RoundedCornerShape(4.dp),
                                                                            ).align(Alignment.CenterVertically),
                                                                )
                                                            }

                                                            Column(Modifier.weight(1f)) {
                                                                Text(
                                                                    text = screenDataState.nowPlayingTitle,
                                                                    style = typo().titleMedium.copy(shadow = topTextShadow),
                                                                    maxLines = 1,
                                                                    color = topContentColor,
                                                                    modifier =
                                                                        Modifier
                                                                            .fillMaxWidth()
                                                                            .wrapContentHeight(align = Alignment.CenterVertically)
                                                                            .basicMarquee(
                                                                                iterations = Int.MAX_VALUE,
                                                                                animationMode = MarqueeAnimationMode.Immediately,
                                                                            ).focusable(),
                                                                )
                                                                Spacer(modifier = Modifier.height(3.dp))
                                                                LazyRow(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                ) {
                                                                    item(screenDataState.isExplicit) {
                                                                        AnimatedVisibility(visible = screenDataState.isExplicit) {
                                                                            ExplicitBadge(
                                                                                modifier =
                                                                                    Modifier
                                                                                        .size(20.dp)
                                                                                        .padding(end = 4.dp)
                                                                                        .weight(1f),
                                                                            )
                                                                        }
                                                                    }
                                                                    item(screenDataState.artistName) {
                                                                        Text(
                                                                            text = screenDataState.artistName,
                                                                            style = typo().bodyMedium.copy(shadow = topTextShadow),
                                                                            color = topContentColor,
                                                                            maxLines = 1,
                                                                            modifier =
                                                                                Modifier
                                                                                    .fillMaxWidth()
                                                                                    .wrapContentHeight(align = Alignment.CenterVertically)
                                                                                    .basicMarquee(
                                                                                        iterations = Int.MAX_VALUE,
                                                                                        animationMode = MarqueeAnimationMode.Immediately,
                                                                                    ).focusable()
                                                                                    .clickable {
                                                                                        val song = sharedViewModel.nowPlayingState.value?.songEntity
                                                                                        (
                                                                                            song?.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() }
                                                                                                ?: screenDataState.songInfoData?.authorId
                                                                                            )?.let { channelId ->
                                                                                                onDismiss()
                                                                                                navController.navigate(
                                                                                                    ArtistDestination(
                                                                                                        channelId = channelId,
                                                                                                    ),
                                                                                                )
                                                                                            }
                                                                                    },
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            if (sharedViewModel.isUserLoggedIn()) {
                                                                Spacer(modifier = Modifier.size(16.dp))
                                                                Crossfade(
                                                                    targetState = likeStatus,
                                                                ) {
                                                                    if (it) {
                                                                        IconButton(
                                                                            modifier =
                                                                                Modifier
                                                                                    .size(24.dp)
                                                                                    .aspectRatio(1f)
                                                                                    .clip(
                                                                                        CircleShape,
                                                                                    ),
                                                                            onClick = {
                                                                                sharedViewModel.addToYouTubeLiked()
                                                                            },
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Rounded.CheckCircle,
                                                                                tint = topContentColor,
                                                                                contentDescription = ""
                                                                            )
                                                                        }
                                                                    } else {
                                                                        IconButton(
                                                                            modifier =
                                                                                Modifier
                                                                                    .size(24.dp)
                                                                                    .aspectRatio(1f)
                                                                                    .clip(
                                                                                        CircleShape,
                                                                                    ),
                                                                            onClick = {
                                                                                sharedViewModel.addToYouTubeLiked()
                                                                            },
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Rounded.AddCircleOutline,
                                                                                tint = topContentColor,
                                                                                contentDescription = "",
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.size(12.dp))
                                                            HeartCheckBox(checked = controllerState.isLiked, size = 32) {
                                                                sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = screenDataState.canvasData != null,
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(
                                                        (middleLayoutPaddingDp * 2 + middleLayoutHeightDp).dp,
                                                    ).clickable(
                                                        onClick = {
                                                            if (mainScrollState.value == 0) {
                                                                showHideJob = true
                                                                showHideControlLayout = !showHideControlLayout
                                                            }
                                                        },
                                                        indication = null,
                                                        interactionSource =
                                                            remember {
                                                                MutableInteractionSource()
                                                            },
                                                    ),
                                        )
                                    }
                                }
                                Column(Modifier.padding(horizontal = 20.dp)) {
                                    AnimatedVisibility(
                                        visible = screenDataState.lyricsData != null,
                                        modifier = Modifier.padding(top = 10.dp),
                                    ) {
                                        ElevatedCard(
                                            onClick = { showFullscreenLyrics = true },
                                            shape = RoundedCornerShape(8.dp),
                                            colors =
                                                CardDefaults.elevatedCardColors().copy(
                                                    containerColor = startColor.value,
                                                ),
                                        ) {
                                            CompositionLocalProvider(
                                                LocalContentColor provides topContentColor,
                                                LocalTextStyle provides typo().bodyMedium.copy(shadow = topTextShadow)
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 15.dp)) {
                                                    Spacer(modifier = Modifier.height(5.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = stringResource(Res.string.lyrics),
                                                            style = typo().labelMedium.copy(shadow = topTextShadow),
                                                            color = topContentColor,
                                                        )
                                                        if (screenDataState.lyricsData?.translatedLyrics?.second == LyricsProvider.AI) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            AIBadge()
                                                        }
                                                        Spacer(modifier = Modifier.weight(1f))
                                                        val canVoteLyrics =
                                                            screenDataState.lyricsData?.lyricsProvider == LyricsProvider.SIMPMUSIC &&
                                                                screenDataState.lyricsData
                                                                    ?.lyrics
                                                                    ?.simpMusicLyrics != null
                                                        val canVoteTranslatedLyrics =
                                                            screenDataState.lyricsData?.translatedLyrics?.second == LyricsProvider.SIMPMUSIC &&
                                                                screenDataState.lyricsData
                                                                    ?.translatedLyrics
                                                                    ?.first
                                                                    ?.simpMusicLyrics != null
                                                        if (canVoteLyrics || canVoteTranslatedLyrics) {
                                                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                                IconButton(
                                                                    onClick = {
                                                                        showVoteDialog = true
                                                                    },
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Rounded.ThumbsUpDown,
                                                                        contentDescription = stringResource(Res.string.rate_lyrics),
                                                                        tint = topContentColor,
                                                                        modifier = Modifier.size(16.dp),
                                                                    )
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                        }
                                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                                            TextButton(
                                                                onClick = {
                                                                    showFullscreenLyrics = true
                                                                },
                                                                contentPadding = PaddingValues(0.dp),
                                                                modifier =
                                                                    Modifier
                                                                        .height(20.dp)
                                                                        .wrapContentWidth(),
                                                            ) {
                                                                Text(text = stringResource(Res.string.show), color = topContentColor)
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(18.dp))
                                                    Box(
                                                        modifier =
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .height(300.dp),
                                                    ) {
                                                        screenDataState.lyricsData?.let {
                                                            LyricsView(
                                                                lyricsData = it,
                                                                timeLine = sharedViewModel.timeline,
                                                                onLineClick = { f ->
                                                                    showFullscreenLyrics = true
                                                                },
                                                            )
                                                        }
                                                    }

                                                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text =
                                                                when (screenDataState.lyricsData?.lyrics?.syncType) {
                                                                    "LINE_SYNCED" -> stringResource(Res.string.line_synced)
                                                                    "RICH_SYNCED" -> stringResource(Res.string.rich_synced)
                                                                    else -> stringResource(Res.string.unsynced)
                                                                },
                                                            style = typo().bodySmall.copy(shadow = topTextShadow),
                                                            textAlign = TextAlign.End,
                                                            color = topContentColor.copy(alpha = 0.7f),
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(top = 10.dp),
                                                        )
                                                        Text(
                                                            text =
                                                                when (screenDataState.lyricsData?.lyricsProvider) {
                                                                    LyricsProvider.SIMPMUSIC -> {
                                                                        stringResource(Res.string.KuroMusic_lyrics)
                                                                    }

                                                                    LyricsProvider.LRCLIB -> {
                                                                        stringResource(Res.string.lyrics_provider_lrc)
                                                                    }

                                                                    LyricsProvider.YOUTUBE -> {
                                                                        stringResource(Res.string.lyrics_provider_youtube)
                                                                    }

                                                                    LyricsProvider.SPOTIFY -> {
                                                                        stringResource(Res.string.spotify_lyrics_provider)
                                                                    }

                                                                    LyricsProvider.OFFLINE -> {
                                                                        stringResource(Res.string.offline_mode)
                                                                    }

                                                                    LyricsProvider.BETTER_LYRICS -> {
                                                                        stringResource(Res.string.lyrics_provider_betterlyrics)
                                                                    }

                                                                    else -> {
                                                                        ""
                                                                    }
                                                                },
                                                            style = typo().bodySmall.copy(shadow = topTextShadow),
                                                            textAlign = TextAlign.End,
                                                            color = topContentColor.copy(alpha = 0.7f),
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxWidth(),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AnimatedVisibility(visible = screenDataState.songInfoData != null) {
                                        ElevatedCard(
                                            onClick = {
                                                val song = sharedViewModel.nowPlayingState.value?.songEntity
                                                (
                                                    song?.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() }
                                                        ?: screenDataState.songInfoData?.authorId
                                                    )?.let { channelId ->
                                                        onDismiss()
                                                        navController.navigate(
                                                            ArtistDestination(
                                                                channelId = channelId,
                                                            ),
                                                        )
                                                    }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors =
                                                CardDefaults.elevatedCardColors().copy(
                                                    containerColor = startColor.value,
                                                ),
                                        ) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(250.dp),
                                            ) {
                                                val thumb = screenDataState.songInfoData?.authorThumbnail
                                                AsyncImage(
                                                    model =
                                                        ImageRequest
                                                            .Builder(LocalPlatformContext.current)
                                                            .data(thumb)
                                                            .diskCachePolicy(CachePolicy.ENABLED)
                                                            .diskCacheKey(thumb)
                                                            .crossfade(550)
                                                            .build(),
                                                    placeholder = painterResource(Res.drawable.holder_video),
                                                    error = painterResource(Res.drawable.holder_video),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier =
                                                        Modifier
                                                            .fillMaxSize()
                                                            .alpha(0.8f)
                                                            .clip(
                                                                RoundedCornerShape(8.dp),
                                                            ),
                                                )
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .padding(12.dp)
                                                            .fillMaxSize(),
                                                ) {
                                                    Column(Modifier.align(Alignment.TopStart)) {
                                                        Spacer(modifier = Modifier.height(5.dp))
                                                        Text(
                                                            text = stringResource(Res.string.artists),
                                                            style = typo().labelMedium.copy(
                                                                shadow = Shadow(
                                                                    color = Color.Black.copy(alpha = 0.5f),
                                                                    offset = Offset(0f, 4f),
                                                                    blurRadius = 8f
                                                                )
                                                            ),
                                                            color = Color.White,
                                                        )
                                                    }
                                                    Column(Modifier.align(Alignment.BottomStart)) {
                                                        Text(
                                                            text = screenDataState.songInfoData?.author ?: "",
                                                            style = typo().labelMedium.copy(
                                                                shadow = Shadow(
                                                                    color = Color.Black.copy(alpha = 0.5f),
                                                                    offset = Offset(0f, 4f),
                                                                    blurRadius = 8f
                                                                )
                                                            ),
                                                            color = Color.White,
                                                        )
                                                        Spacer(modifier = Modifier.height(5.dp))
                                                        Text(
                                                            text = screenDataState.songInfoData?.subscribers ?: "",
                                                            style = typo().bodySmall.copy(
                                                                shadow = Shadow(
                                                                    color = Color.Black.copy(alpha = 0.5f),
                                                                    offset = Offset(0f, 4f),
                                                                    blurRadius = 8f
                                                                )
                                                            ),
                                                            color = Color.LightGray,
                                                            textAlign = TextAlign.End,
                                                        )
                                                        Spacer(modifier = Modifier.height(5.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    AnimatedVisibility(visible = screenDataState.songInfoData != null) {
                                        ElevatedCard(
                                            onClick = {},
                                            shape = RoundedCornerShape(8.dp),
                                            colors =
                                                CardDefaults.elevatedCardColors().copy(
                                                    containerColor = startColor.value,
                                                ),
                                        ) {
                                            CompositionLocalProvider(LocalContentColor provides topContentColor) {
                                                Column(
                                                    Modifier
                                                        .padding(horizontal = 12.dp, vertical = 15.dp)
                                                        .fillMaxWidth(),
                                                ) {
                                                    Spacer(modifier = Modifier.height(5.dp))
                                                    Text(
                                                        text = stringResource(
                                                            Res.string.published_at,
                                                            screenDataState.songInfoData?.uploadDate ?: ""
                                                        ),
                                                        style = typo().labelSmall.copy(shadow = topTextShadow),
                                                        color = topContentColor,
                                                    )
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Text(
                                                        text =
                                                            stringResource(
                                                                Res.string.view_count,
                                                                "%,d".format(screenDataState.songInfoData?.viewCount),
                                                            ),
                                                        style = typo().labelMedium.copy(shadow = topTextShadow),
                                                        color = topContentColor,
                                                    )
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Text(
                                                        text =
                                                            stringResource(
                                                                Res.string.like_and_dislike,
                                                                screenDataState.songInfoData?.like ?: 0,
                                                                screenDataState.songInfoData?.dislike ?: 0,
                                                            ),
                                                        style = typo().bodyMedium.copy(shadow = topTextShadow),
                                                        color = topContentColor.copy(alpha = 0.7f),
                                                    )
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Text(
                                                        text = stringResource(Res.string.description),
                                                        style = typo().labelSmall.copy(shadow = topTextShadow),
                                                        color = topContentColor,
                                                    )
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    DescriptionView(
                                                        text = screenDataState.songInfoData?.description ?: "",
                                                        onTimeClicked = { raw ->
                                                            val timestamp = parseTimestampToMilliseconds(raw)
                                                            if (timestamp != 0.0 && timestamp < timelineState.total) {
                                                                sharedViewModel.onUIEvent(
                                                                    UIEvent.UpdateProgress(
                                                                        ((timestamp * 100) / timelineState.total).toFloat(),
                                                                    ),
                                                                )
                                                            }
                                                        },
                                                        onURLClicked = { url ->
                                                            uriHandler.openUri(
                                                                url,
                                                            )
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(5.dp))
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Spacer(
                                        modifier =
                                            Modifier.height(
                                                with(localDensity) { WindowInsets.systemBars.getBottom(localDensity).toDp() },
                                            ),
                                    )
                                }
                            }
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = shouldShowToolbar && isExpanded,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                    ) {
                        ElevatedCard(
                            elevation = CardDefaults.elevatedCardElevation(10.dp),
                            shape = RectangleShape,
                            colors =
                                CardDefaults.elevatedCardColors(
                                    containerColor = startColor.value.copy(
                                        red = (startColor.value.red - 0.05f).coerceAtLeast(0f),
                                        green = (startColor.value.green - 0.05f).coerceAtLeast(0f),
                                        blue = (startColor.value.blue - 0.05f).coerceAtLeast(0f),
                                    )
                                ),
                            modifier =
                                Modifier
                                    .clipToBounds()
                                    .wrapContentHeight()
                                    .fillMaxWidth(),
                        ) {
                            Box(
                                modifier =
                                    Modifier.padding(
                                        top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() },
                                    ),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                        Modifier
                                            .padding(
                                                vertical = 8.dp,
                                                horizontal = 15.dp,
                                            ).fillMaxWidth(),
                                ) {
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Box(modifier = Modifier.weight(1F)) {
                                        Column(
                                            Modifier
                                                .wrapContentHeight(),
                                        ) {
                                            Text(
                                                text = screenDataState.nowPlayingTitle,
                                                style = typo().bodyMedium.copy(shadow = topTextShadow),
                                                color = topContentColor,
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
                                            LazyRow(verticalAlignment = Alignment.CenterVertically) {
                                                item {
                                                    AnimatedVisibility(visible = screenDataState.isExplicit) {
                                                        ExplicitBadge(
                                                            modifier =
                                                                Modifier
                                                                    .size(20.dp)
                                                                    .padding(end = 4.dp)
                                                                    .weight(1f),
                                                        )
                                                    }
                                                }
                                                item(
                                                    key = screenDataState.artistName,
                                                ) {
                                                    Text(
                                                        text = screenDataState.artistName,
                                                        style = typo().bodySmall.copy(shadow = topTextShadow),
                                                        color = topContentColor,
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
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(15.dp))
                                    CompositionLocalProvider(LocalContentColor provides topContentColor) {
                                        HeartCheckBox(checked = controllerState.isLiked, size = 30) {
                                            sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(15.dp))
                                    Crossfade(targetState = timelineState.loading, label = "") {
                                        if (it) {
                                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    color = topContentColor,
                                                    strokeWidth = 3.dp,
                                                )
                                            }
                                        } else {
                                            CompositionLocalProvider(LocalContentColor provides topContentColor) {
                                                PlayPauseButton(isPlaying = controllerState.isPlaying, modifier = Modifier.size(48.dp)) {
                                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                                }
                                            }
                                        }
                                    }
                                }
                                Box(
                                    modifier =
                                        Modifier
                                            .wrapContentSize(Alignment.Center)
                                            .align(Alignment.BottomCenter),
                                ) {
                                    LinearProgressIndicator(
                                        progress = { timelineState.current.toFloat() / timelineState.total },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(
                                                    color = Color.Transparent,
                                                    shape = RoundedCornerShape(4.dp),
                                                ),
                                        color = topContentColor,
                                        trackColor = Color.Gray.copy(alpha = 0.4f),
                                        strokeCap = StrokeCap.Round,
                                        drawStopIndicator = {},
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