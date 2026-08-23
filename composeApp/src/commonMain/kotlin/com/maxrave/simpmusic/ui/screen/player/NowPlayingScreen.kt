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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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
import coil3.toBitmap
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
import com.maxrave.simpmusic.extension.smoothScrimBrush
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
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.AddCircleOutline
import com.maxrave.simpmusic.ui.icon.CheckCircle
import com.maxrave.simpmusic.ui.icon.Forward5
import com.maxrave.simpmusic.ui.icon.Fullscreen
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.KeyboardArrowDown
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.PlaylistAdd
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.Replay5
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Subtitles
import com.maxrave.simpmusic.ui.icon.SubtitlesOff
import com.maxrave.simpmusic.ui.icon.ThumbsUpDown
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.navigation.destination.player.FullscreenDestination
import com.maxrave.simpmusic.ui.theme.blackMoreOverlay
import com.maxrave.simpmusic.ui.theme.overlay
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LyricsProvider
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetUIEvent
import com.maxrave.simpmusic.viewModel.NowPlayingBottomSheetViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.artists
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.description
import simpmusic.composeapp.generated.resources.like_and_dislike
import simpmusic.composeapp.generated.resources.line_synced
import simpmusic.composeapp.generated.resources.lyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_betterlyrics
import simpmusic.composeapp.generated.resources.lyrics_provider_lrc
import simpmusic.composeapp.generated.resources.lyrics_provider_simpmusic
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
private val WHITESPACE_REGEX = Regex("""\s+""")

// Word-by-word lyrics carry a timestamp per word; replace each with a space
// (not ""), then collapse — otherwise the words run together.
private fun String.stripRichSyncTimestamps(): String =
    replace(RICH_SYNC_TIMESTAMP_REGEX, " ")
        .replace(WHITESPACE_REGEX, " ")
        .trim()

// Backdrop behind the player. A dark surface rather than pure black: #000000 reads as a hole
// next to the artwork-tinted gradient and cards, which is why Spotify sits its player on a
// near-black surface instead. Used for the gradient's end colour, the fade-to target and the
// area below the gradient so all three match exactly and leave no seam.
private val PlayerBackdropColor = Color(0xFF121212)

@OptIn(ExperimentalFoundationApi::class)
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
        containerColor = Color.Black,
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = .5f),
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        NowPlayingScreenContent(
            sharedViewModel = sharedViewModel,
            navController = navController,
            isExpanded = sheetState.currentValue == SheetValue.Expanded,
            dismissIcon = SimpIcons.KeyboardArrowDown,
            onDismiss = {
                hideSheet()
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
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

    // ViewModel State
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val likeStatus by sharedViewModel.likeStatus.collectAsStateWithLifecycle()
    val castState by sharedViewModel.castState.collectAsStateWithLifecycle()

    val shouldShowVideo by sharedViewModel.getVideo.collectAsStateWithLifecycle()
    val translatedVoteState by sharedViewModel.translatedVoteState.collectAsStateWithLifecycle()
    val lyricsVoteState by sharedViewModel.lyricsVoteState.collectAsStateWithLifecycle()

    // Artwork Pager state — Spotify-style horizontal swipe between queue tracks.
    // The pager wraps the Canvas + Thumbnail layers. Controller layout below stays fixed.
    val nowPlayingState by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val queueDataState by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val artworkQueue by remember {
        derivedStateOf { queueDataState?.data?.listTracks ?: emptyList() }
    }
    // ⚠️ Use track.videoId (already prefix-stripped at MediaServiceHandlerImpl.kt:386).
    // Do NOT use mediaItem.mediaId — it carries the "Video" prefix for video items.
    val nowPlayingVideoId: String? = nowPlayingState?.track?.videoId
    val currentOrderIndex by remember(artworkQueue, nowPlayingVideoId) {
        derivedStateOf { deriveOrderIndex(artworkQueue, nowPlayingVideoId) }
    }
    val isRepeatOne = controllerState.repeatState is RepeatState.One
    // Single PagerState — the unified ArtworkPager renders BOTH the fullscreen canvas
    // background and the centered square thumbnail in each page, so we don't need two
    // pagers + state mirroring.
    val artworkPagerState =
        rememberPagerState(
            initialPage = currentOrderIndex.coerceAtLeast(0),
            pageCount = { artworkQueue.size.coerceAtLeast(1) },
        )
    var isAnimatingFromPlayer by remember { mutableStateOf(false) }
    var isUserDraggingActive by remember { mutableStateOf(false) }

    // Drag detection — `isScrollInProgress` is `true` for both user drags (forwarded
    // by the outer Modifier.scrollable on the Column) and programmatic
    // `animateScrollToPage`. We disambiguate via `isAnimatingFromPlayer`, which we
    // set explicitly around the player → pager animation (try/finally).
    LaunchedEffect(artworkPagerState) {
        snapshotFlow {
            artworkPagerState.isScrollInProgress to isAnimatingFromPlayer
        }.collect { (scrolling, animating) ->
            isUserDraggingActive = scrolling && !animating
        }
    }

    // ① Player → Pager: animate to new track when player advances.
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

    // ② Pager → Player: seek when user settles on a different page.
    // Adjacent (±1) → Next/Previous (preserves crossfade flow on Android).
    // Far skip → playMediaItemInMediaSource (handles unshuffling internally).
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
                        // Use SkipToPrevious so a swipe always goes to the previous track —
                        // UIEvent.Previous would seek to 0 of the current track once the
                        // playhead has passed the 3-second mark.
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

    // ③ Queue mutation guard — when queue shrinks below currentPage, scroll to last index
    // to avoid IndexOutOfBoundsException during recomposition.
    LaunchedEffect(artworkQueue.size) {
        if (artworkQueue.isNotEmpty() && artworkPagerState.currentPage >= artworkQueue.size) {
            runCatching { artworkPagerState.scrollToPage(artworkQueue.lastIndex) }
        }
    }

    // State
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

    // NEW: Add to Playlist state
    var showAddToPlaylistDirectly by rememberSaveable {
        mutableStateOf(false)
    }

    var shouldShowToolbar by remember {
        mutableStateOf(false)
    }

    // Palette state
    val paletteState = rememberPaletteState()

    val startColor =
        remember {
            Animatable(Color.Black)
        }
    val endColor =
        remember {
            Animatable(Color.Black)
        }
    val gradientOffset by remember {
        mutableStateOf(GradientOffset(GradientAngle.CW135))
    }

    var spotShadowColor by remember {
        mutableStateOf(Color.White)
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

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                spotShadowColor = it.getColorFromPalette()
                startColor.animateTo(it.getColorFromPalette())
                // Lands on the same backdrop colour the fade and the area below the gradient
                // use, so the palette ramp resolves into the surface instead of a black patch.
                endColor.animateTo(PlayerBackdropColor)
            }
    }

    LaunchedEffect(spotShadowColor) {
        Logger.d(TAG, "spotShadowColor: $spotShadowColor")
    }
    // Height
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

    // Crossfade: RGB rainbow color cycling when transitioning between tracks
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
        targetValue = if (timelineState.isCrossfading) rainbowColor else Color.White,
        animationSpec = tween(300),
        label = "sliderCrossfadeColor",
    )

    // Show ControlLayout Or Show Artist Badge
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

    // Fullscreen overlay
    var showHideFullscreenOverlay by rememberSaveable {
        mutableStateOf(false)
    }

    var currentLyricLineIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }

    LaunchedEffect(key1 = showHideFullscreenOverlay) {
        if (showHideFullscreenOverlay) {
            delay(3000)
            showHideFullscreenOverlay = false
        }
    }

    // Canvas subtitle sync
    LaunchedEffect(timelineState, screenDataState.lyricsData?.lyrics) {
        val lyrics = screenDataState.lyricsData?.lyrics
        if (lyrics == null || lyrics.syncType == "UNSYNCED" || lyrics.syncType == null) {
            currentLyricLineIndex = -1
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
                    currentLyricLineIndex = i
                }
            }
            if (lines.isNotEmpty() &&
                timelineState.current in 0..(lines.getOrNull(0)?.startTimeMs?.toLongOrNull() ?: 0L)
            ) {
                currentLyricLineIndex = -1
            }
        } else {
            currentLyricLineIndex = -1
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
            song = null, // Auto set now playing
            setSleepTimerEnable = true,
            changeMainLyricsProviderEnable = true,
        )
    }

    if (showFullscreenLyrics) {
        FullscreenLyricsSheet(
            sharedViewModel = sharedViewModel,
            navController = navController, // <-- ADD THIS LINE
            color = startColor.value,
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

    // NEW: Add to Playlist Bottom Sheet
    if (showAddToPlaylistDirectly) {
        val viewModel: NowPlayingBottomSheetViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.resetPlaylists()
            viewModel.setSongEntity(null) // Uses current playing song
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

    // Vote Dialog
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

    if (screenDataState.lyricsData != null && controllerState.isPlaying) {
        KeepScreenOn()
    }
    Box {
        Column(
            Modifier
                .verticalScroll(
                    mainScrollState,
                    enabled = isExpanded,
                )
                // Horizontal swipe is handled by the unified ArtworkPager below.
                // Spacers in this Column have no pointer input and don't block hits, so
                // drags fall through to the Pager.
                .then(
                    if (showHideMiddleLayout) {
                        Modifier
                            // The backdrop fills the whole scrollable content, then the gradient
                            // is drawn over just the first screen height. Using background() for
                            // the gradient instead would stretch it across the entire content,
                            // which is what made it run on forever while scrolling.
                            .background(PlayerBackdropColor)
                            .drawBehind {
                                val gradientHeight = screenInfo.hPX.toFloat()
                                val area = Size(size.width, gradientHeight)
                                // Palette gradient, keeping its diagonal angle.
                                drawRect(
                                    brush =
                                        Brush.linearGradient(
                                            colors =
                                                listOf(
                                                    startColor.value,
                                                    endColor.value,
                                                ),
                                            start = gradientOffset.start,
                                            end = gradientOffset.end,
                                        ),
                                    size = area,
                                )
                                // Vertical fade to the backdrop colour, fully opaque from 90%
                                // down, so the bottom edge meets the area underneath seamlessly
                                // across the whole width. Adding the same colour as a stop to
                                // the diagonal gradient above could not do that — it would only
                                // arrive in one corner and leave a visible diagonal seam.
                                drawRect(
                                    brush =
                                        smoothScrimBrush(
                                            from = PlayerBackdropColor.copy(alpha = 0f),
                                            to = PlayerBackdropColor,
                                            startY = 0f,
                                            // Reaches full opacity at 95% and is held there by Clamp,
                                            // same as the old `0.95f to PlayerBackdropColor` stop.
                                            endY = gradientHeight * 0.95f,
                                        ),
                                    size = area,
                                )
                            }
                    } else {
                        Modifier.background(Color.Black)
                    },
                ),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // === Unified ArtworkPager (Spotify-style swipe) ===
                // ONE HorizontalPager wraps both the fullscreen canvas backdrop AND the
                // centered square thumbnail. Both layers slide together as a single page
                // so when the user swipes during canvas mode, they see the next track's
                // thumbnail enter and the canvas exit in lockstep.
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

                    // Per-page palette state for the gradient backdrop.
                    // The bitmap is fed in by Layer 2's adjacent-thumbnail AsyncImage
                    // (onSuccess), so we use the SAME bitmap that's painted on screen —
                    // matches the outer Column's palette extraction characteristics.
                    val pagePaletteState = rememberPaletteState()
                    val pageStartColor =
                        remember(pageTrack?.videoId) {
                            Animatable(Color.Black)
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
                                // Prevent the canvas video (9:16 aspect, can be wider than the
                                // page) and any other content from bleeding into adjacent pages.
                                .clipToBounds()
                                // Tap toggles controls only when the canvas is covering this page;
                                // otherwise no-op (matches the legacy behaviour where the touch
                                // overlay only appeared in canvas mode).
                                .pointerInput(pageHasCanvas) {
                                    detectTapGestures(
                                        onTap = if (pageHasCanvas) {
                                            {
                                                if (mainScrollState.value == 0) {
                                                    showHideJob = true
                                                    showHideControlLayout = !showHideControlLayout
                                                }
                                            }
                                        } else null
                                    )
                                },
                    ) {
                        // ── Layer 0: per-page backdrop (adjacent pages only) ──
                        // Palette gradient (startColor → endColor) so the adjacent page never
                        // falls back to a flat dark void during a swipe.
                        // The CURRENT page deliberately skips this layer so the existing
                        // gradient / canvas on the Column stays visible.
                        if (!isCurrentArtworkPage && pageTrack != null) {
                            // Palette is fed by Layer 2's adjacent-thumbnail AsyncImage
                            // (see below) so the gradient color stays consistent with
                            // the bitmap actually painted for that page.
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors =
                                                    listOf(
                                                        pageStartColor.value,
                                                        Color.Black,
                                                    ),
                                                start = gradientOffset.start,
                                                end = gradientOffset.end,
                                            ),
                                        ),
                            )
                        }

                        // ── Layer 1: fullscreen canvas backdrop (current track + canvas data) ──
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
                            // Bottom gradient overlay — different intensity per state:
                            // - Focus: original full-height heavy gradient (controls readability)
                            // - Unfocus: 50% height + 50% lighter colors (just enough for metadata,
                            //   lets more canvas show through)
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
                                                    smoothScrimBrush(
                                                        from = overlay,
                                                        to = Color.Black,
                                                        startFraction = 0.2f,
                                                    ),
                                                ),
                                    )
                                } else {
                                    // Box fullscreen — gradient stops control where darkening starts.
                                    // Note: pager content can extend past visible viewport bottom due
                                    // to parent offsets. We span the FULL pager height (instead of a
                                    // fixed 120.dp BottomCenter Box) so the colorStops are anchored
                                    // to pager height — guaranteeing the visible viewport bottom
                                    // always falls inside the held-Black region (>=0.85f).
                                    // Unfocused gradient — compact dark coverage at the very bottom only:
                                    //   - 0%-92%: fully Transparent (canvas clear)
                                    //   - 92%-97%: quick fade to Black
                                    //   - 97%-100%: held solid Black (avoids canvas bleed-through
                                    //     at visible viewport bottom — alpha must reach 0xFF before
                                    //     the visible bottom, which sits at ~94-95% of pager).
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(
                                                    smoothScrimBrush(
                                                        from = Color.Black.copy(alpha = 0f),
                                                        to = Color.Black,
                                                        startFraction = 0.92f,
                                                        endFraction = 0.97f,
                                                    ),
                                                ),
                                    )
                                }
                            }
                        }

                        // ── Layer 2: centered square thumbnail ──
                        // Positioned at the same Y as the original middle layout
                        // (TopAppBar height + middleLayoutPaddingDp from the top of the page).
                        // alpha=0 when the canvas is covering this page; otherwise visible so
                        // adjacent pages always show the upcoming/previous track artwork.
                        Column(modifier = Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.height(topAppBarHeightDp.dp))
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
                                    // Live artwork (drives palette extraction via setBitmap).
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
                                                    .data(screenDataState.thumbnailURL)
                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                    .diskCacheKey(screenDataState.thumbnailURL + "BIGGER")
                                                    .crossfade(550)
                                                    .build(),
                                            contentDescription = "",
                                            onSuccess = {
                                                sharedViewModel.setBitmap(
                                                    it.result.image.toImageBitmap(),
                                                )
                                            },
                                            contentScale = ContentScale.Crop,
                                            placeholder = rememberHolderPainter(),
                                            error = rememberHolderPainter(),
                                            modifier =
                                                Modifier
                                                    .align(Alignment.Center)
                                                    .padding(3.dp)
                                                    .fillMaxWidth()
                                                    .background(Color.Transparent)
                                                    .aspectRatio(
                                                        if (!screenDataState.isVideo) 1f else 16f / 9,
                                                    ).clip(
                                                        RoundedCornerShape(8.dp),
                                                    ).alpha(
                                                        if (!screenDataState.isVideo || !shouldShowVideo) 1f else 0f,
                                                    ),
                                        )
                                    }

                                    // Inline video player (current page + isVideo + shouldShowVideo).
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
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.Black),
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
                                                        .pointerInput(Unit) {
                                                            detectTapGestures(
                                                                onTap = { showHideFullscreenOverlay = !showHideFullscreenOverlay }
                                                            )
                                                        },
                                            ) {
                                                Crossfade(targetState = showHideFullscreenOverlay) {
                                                    if (it) {
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .fillMaxSize()
                                                                    .background(
                                                                        // The old middle stop (0.15f to overlay)
                                                                        // hand-approximated a convex falloff;
                                                                        // smoothstep does that on its own.
                                                                        smoothScrimBrush(
                                                                            from = blackMoreOverlay,
                                                                            to = overlay.copy(alpha = 0f),
                                                                            startFraction = 0.03f,
                                                                            endFraction = 0.8f,
                                                                        ),
                                                                    ),
                                                        ) {
                                                            IconButton(
                                                                onClick = {
                                                                    onDismiss()
                                                                    navController.navigate(FullscreenDestination)
                                                                },
                                                                Modifier.align(Alignment.TopEnd),
                                                            ) {
                                                                Icon(
                                                                    imageVector = SimpIcons.Fullscreen,
                                                                    contentDescription = "",
                                                                    tint = Color.White,
                                                                )
                                                            }
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
                                                                            .clip(CircleShape),
                                                                    onClick = {
                                                                        sharedViewModel.onUIEvent(UIEvent.Backward)
                                                                    },
                                                                ) {
                                                                    Icon(
                                                                        imageVector = SimpIcons.Replay5,
                                                                        tint = Color.White,
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
                                                                            .clip(CircleShape),
                                                                    onClick = {
                                                                        sharedViewModel.onUIEvent(UIEvent.Forward)
                                                                    },
                                                                ) {
                                                                    Icon(
                                                                        imageVector = SimpIcons.Forward5,
                                                                        tint = Color.White,
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
                                                                                SimpIcons.SubtitlesOff
                                                                            } else {
                                                                                SimpIcons.Subtitles
                                                                            },
                                                                        contentDescription = "",
                                                                        tint = Color.White,
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (pageTrack != null) {
                                    // Adjacent page — static thumbnail from Track.thumbnails.
                                    val staticThumb =
                                        pageTrack.thumbnails
                                            ?.maxByOrNull { it.width * it.height }
                                            ?.url
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
                                                    .data(staticThumb)
                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                    .diskCacheKey(staticThumb)
                                                    .crossfade(300)
                                                    .build(),
                                            contentDescription = pageTrack.title,
                                            contentScale = ContentScale.Crop,
                                            placeholder = rememberHolderPainter(),
                                            error = rememberHolderPainter(),
                                            // Feed the per-page palette using the SAME bitmap
                                            // we just rendered so the Layer 0 gradient backdrop
                                            // matches what the user sees on screen.
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
                            }.padding(
                                top = with(localDensity) { WindowInsets.statusBars.getTop(localDensity).toDp() },
                            ),
                    colors =
                        TopAppBarDefaults.topAppBarColors().copy(
                            containerColor = Color.Transparent,
                        ),
                    // Position-aware insets shrink per frame while the sheet is dragged (pinned
                    // bar + layout jitter) — status-bar space is static padding on the modifier.
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.now_playing_upper),
                                style = typo().bodyMedium,
                                color = Color.White,
                            )
                            Text(
                                text = screenDataState.playlistName,
                                style = typo().labelMedium,
                                color = Color.White,
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
                                tint = Color.White,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showSheet = true
                        }) {
                            Icon(
                                imageVector = SimpIcons.MoreVert,
                                contentDescription = "",
                                tint = Color.White,
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

                            // Artwork is rendered by the unified ArtworkPager above (which lives in the
                            // outer Box). Reserve the same vertical space here so the Info Layout below
                            // stays at its original Y position. Spacer has no pointer input so it does
                            // not block the pager swipe gesture beneath it.
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

                            // Spotify-style current lyric line — vertically centered in the gap
                            // between the artwork and the info layout below. This Box replaces the
                            // plain gap Spacer at the same middleLayoutPaddingDp height, so the
                            // info layout position never moves; the line just lives inside the gap.
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                    Modifier
                                        .animateContentSize()
                                        .height(
                                            middleLayoutPaddingDp.dp,
                                        ).fillMaxWidth(),
                            ) {
                                val inlineLyrics = screenDataState.lyricsData?.lyrics
                                val hasSyncedLyrics =
                                    inlineLyrics != null &&
                                        inlineLyrics.syncType != null &&
                                        inlineLyrics.syncType != "UNSYNCED" &&
                                        inlineLyrics.lines != null
                                // Canvas mode has its own subtitle overlay — never show both.
                                val currentLyricLineText =
                                    if (!hasSyncedLyrics ||
                                        screenDataState.canvasData != null ||
                                        currentLyricLineIndex < 0
                                    ) {
                                        ""
                                    } else {
                                        inlineLyrics
                                            ?.lines
                                            ?.getOrNull(currentLyricLineIndex)
                                            ?.words
                                            ?.stripRichSyncTimestamps()
                                            .orEmpty()
                                    }
                                Crossfade(
                                    targetState = currentLyricLineText,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "inlineLyricLine",
                                ) { lineText ->
                                    Text(
                                        text = lineText,
                                        style = typo().labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp)
                                                .basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    animationMode = MarqueeAnimationMode.Immediately,
                                                ).focusable(),
                                    )
                                }
                            }

                            // Info Layout
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
                                                        .data(screenDataState.thumbnailURL)
                                                        .diskCachePolicy(CachePolicy.ENABLED)
                                                        .diskCacheKey(screenDataState.thumbnailURL + "BIGGER")
                                                        .crossfade(true)
                                                        .build(),
                                                placeholder = rememberHolderPainter(),
                                                error = rememberHolderPainter(),
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
                                                style = typo().titleMedium,
                                                maxLines = 1,
                                                color = Color.White,
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
                                                        style = typo().bodyMedium,
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
                                                        Icon(imageVector = SimpIcons.CheckCircle, tint = Color.White, contentDescription = "")
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
                                                            imageVector = SimpIcons.AddCircleOutline,
                                                            tint = Color.White,
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
                                    if (getPlatform() == Platform.Android) {
                                        // Real Slider
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
                                                                color = Color.Gray,
                                                                trackColor = Color.DarkGray,
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
                                                                color = Color.Gray,
                                                                trackColor =
                                                                    Color.Gray.copy(
                                                                        alpha = 0.6f,
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
                                                    // material3 1.5.0-alpha25 keeps a
                                                    // binary-compatibility overload of Slider that
                                                    // accepts valueRange and then forwards without
                                                    // it, so the slider silently runs on the
                                                    // default 0f..1f and anything larger is clamped
                                                    // to a full track. Hand it a fraction instead;
                                                    // sliderValue stays on the 0..100 scale that
                                                    // UIEvent.UpdateProgress and the time labels
                                                    // are built around.
                                                    value = sliderValue / 100f,
                                                    onValueChangeFinished = {
                                                        isSliding = false
                                                        sharedViewModel.onUIEvent(
                                                            UIEvent.UpdateProgress(sliderValue),
                                                        )
                                                    },
                                                    onValueChange = {
                                                        isSliding = true
                                                        sliderValue = it * 100f
                                                    },
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
                                        // Time Layout
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp),
                                        ) {
                                            Text(
                                                text = formatDuration((timelineState.total * (sliderValue / 100f)).roundToLong()),
                                                style = typo().bodyMedium,
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
                                                    style = typo().bodyMedium,
                                                    modifier = Modifier.weight(1f),
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                            Text(
                                                text = formatDuration(timelineState.total),
                                                style = typo().bodyMedium,
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
                                        // Control Button Layout
                                        PlayerControlLayout(
                                            controllerState,
                                        ) {
                                            sharedViewModel.onUIEvent(it)
                                        }
                                    } else {
                                        Spacer(Modifier.height(16.dp))
                                    }
                                    // List Bottom Buttons - MODIFIED TO ADD PLAYLIST BUTTON
                                    Row(
                                        modifier =
                                            Modifier
                                                .height(32.dp)
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Info + Cast Buttons (Left)
                                        // weight(fill = false) keeps a long device name from shoving the
                                        // playlist/queue buttons off the end of this SpaceBetween row.
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
                                                Icon(imageVector = SimpIcons.Info, tint = Color.White, contentDescription = "")
                                            }
                                            // Cyan rather than colorScheme.primary: this screen is force-dark whatever
                                            // the app theme is, so a light-theme primary would sink into the black
                                            // backdrop. Mirrors the `if (forceDark) Color.Cyan` rule in FullWidthItems.
                                            PlatformCastButton(
                                                modifier = Modifier.size(24.dp),
                                                tint = if (castState.isRemote) Color.Cyan else Color.White,
                                            )
                                            AnimatedVisibility(visible = castState.isRemote) {
                                                Text(
                                                    text =
                                                        stringResource(
                                                            Res.string.playing_on_device,
                                                            castState.deviceName ?: "Cast",
                                                        ),
                                                    style = typo().bodySmall,
                                                    color = Color.Cyan,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            // NEW: Add to Playlist Button (Center-Right)
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
                                                    imageVector = SimpIcons.PlaylistAdd,
                                                    tint = Color.White,
                                                    contentDescription = "Add to Playlist",
                                                )
                                            }

                                            // Queue Button (Right)
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
                                                    imageVector = SimpIcons.QueueMusic,
                                                    tint = Color.White,
                                                    contentDescription = "",
                                                )
                                            }
                                        }
                                    }
                                }
                                this@Column.AnimatedVisibility(
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
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onTap = {
                                                            if (mainScrollState.value == 0) {
                                                                showHideJob = true
                                                                showHideControlLayout = !showHideControlLayout
                                                            }
                                                        }
                                                    )
                                                },
                                        contentAlignment = Alignment.BottomStart,
                                    ) {
                                        // Gradient backdrop — transparent at top so Canvas shows
                                        // through, fading to dark at the bottom for a Spotify-like
                                        // backdrop under the metadata row.
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        smoothScrimBrush(
                                                            from = Color.Black.copy(alpha = 0f),
                                                            to = Color.Black.copy(alpha = 0.85f),
                                                        ),
                                                    ),
                                        )

                                        Column(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .animateContentSize(),
                                        ) {
                                            this@Column.AnimatedVisibility(
                                                visible = currentLyricLineIndex > -1,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically(),
                                            ) {
                                                // Canvas subtitle - Spotify-style: lyrics line above metadata row
                                                val lineText =
                                                    screenDataState.lyricsData
                                                        ?.lyrics
                                                        ?.lines
                                                        ?.getOrNull(currentLyricLineIndex)
                                                        ?.words
                                                        ?.stripRichSyncTimestamps()
                                                if (!lineText.isNullOrBlank()) {
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
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
                                                            style = typo().bodyMedium,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                        )
                                                        val translatedLineText =
                                                            screenDataState.lyricsData
                                                                ?.translatedLyrics
                                                                ?.first
                                                                ?.lines
                                                                ?.getOrNull(currentLyricLineIndex)
                                                                ?.words
                                                                ?.stripRichSyncTimestamps()
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
                                                                .data(screenDataState.thumbnailURL)
                                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                                .diskCacheKey(screenDataState.thumbnailURL + "BIGGER")
                                                                .crossfade(true)
                                                                .build(),
                                                        placeholder = rememberHolderPainter(),
                                                        error = rememberHolderPainter(),
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
                                                        style = typo().titleMedium,
                                                        maxLines = 1,
                                                        color = Color.White,
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
                                                                style = typo().bodyMedium,
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
                                                                    imageVector = SimpIcons.CheckCircle,
                                                                    tint = Color.White,
                                                                    contentDescription = "",
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
                                                                    imageVector = SimpIcons.AddCircleOutline,
                                                                    tint = Color.White,
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
                        // The original Touch Area overlay was removed: tap-to-toggle is now
                        // wired directly onto each ArtworkPager page (canvas + middle), so
                        // drag gestures reach HorizontalPager without competing with a
                        // sibling clickable.
                    }
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        // Lyrics Layout
                        AnimatedVisibility(
                            visible = screenDataState.lyricsData != null,
                            modifier = Modifier.padding(top = 10.dp),
                        ) {
                            ElevatedCard(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                colors =
                                    CardDefaults.elevatedCardColors().copy(
                                        containerColor = startColor.value,
                                    ),
                            ) {
                                Column(modifier = Modifier.padding(15.dp)) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(Res.string.lyrics),
                                            style = typo().labelMedium,
                                            color = Color.White,
                                        )
                                        if (screenDataState.lyricsData?.translatedLyrics?.second == LyricsProvider.AI) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            AIBadge()
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        // Vote button - only show if lyrics or translated lyrics from SimpMusic
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
                                                        imageVector = SimpIcons.ThumbsUpDown,
                                                        contentDescription = stringResource(Res.string.rate_lyrics),
                                                        tint = Color.White,
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
                                                Text(text = stringResource(Res.string.show), color = Color.White)
                                            }
                                        }
                                    }
                                    // Lyrics Layout
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
                                                    sharedViewModel.onUIEvent(UIEvent.UpdateProgress(f))
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
                                            style = typo().bodySmall,
                                            textAlign = TextAlign.End,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 10.dp),
                                        )
                                        Text(
                                            text =
                                                when (screenDataState.lyricsData?.lyricsProvider) {
                                                    LyricsProvider.SIMPMUSIC -> {
                                                        stringResource(Res.string.lyrics_provider_simpmusic)
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
                                            style = typo().bodySmall,
                                            textAlign = TextAlign.End,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth(),
                                        )
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
                                        containerColor = Color(0xFF212121),
                                    ),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Artwork occupies the top of the card; only the section
                                    // label sits on top of it. Name and subscriber count moved
                                    // below onto the solid card surface so they stay readable
                                    // regardless of how bright the artist photo is.
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
                                            placeholder = rememberHolderPainter(isVideo = true),
                                            error = rememberHolderPainter(isVideo = true),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            // No explicit clip: the ElevatedCard already clips to
                                            // its 8.dp shape, so only the card's top corners round
                                            // and the image meets the panel below flush.
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                        // Scrim behind the label: artist photos are often bright
                                        // at the top, which swallowed the white text.
                                        Box(
                                            modifier =
                                                Modifier
                                                    .matchParentSize()
                                                    .background(
                                                        smoothScrimBrush(
                                                            from = Color.Black.copy(alpha = 0.6f),
                                                            to = Color.Black.copy(alpha = 0f),
                                                            endFraction = 0.4f,
                                                        ),
                                                    ),
                                        )
                                        Text(
                                            text = stringResource(Res.string.artists),
                                            style = typo().labelMedium,
                                            color = Color.White,
                                            modifier =
                                                Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(15.dp),
                                        )
                                    }
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 15.dp, vertical = 12.dp),
                                    ) {
                                        Text(
                                            text = screenDataState.songInfoData?.author ?: "",
                                            style = typo().titleMedium,
                                            color = Color.White,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = screenDataState.songInfoData?.subscribers ?: "",
                                            style = typo().bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
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
                                Column(
                                    Modifier
                                        .padding(15.dp)
                                        .fillMaxWidth(),
                                ) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = stringResource(Res.string.published_at, screenDataState.songInfoData?.uploadDate ?: ""),
                                        style = typo().labelSmall,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text =
                                            stringResource(
                                                Res.string.view_count,
                                                "%,d".format(screenDataState.songInfoData?.viewCount),
                                            ),
                                        style = typo().labelMedium,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text =
                                            stringResource(
                                                Res.string.like_and_dislike,
                                                screenDataState.songInfoData?.like ?: 0,
                                                screenDataState.songInfoData?.dislike ?: 0,
                                            ),
                                        style = typo().bodyMedium,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(Res.string.description),
                                        style = typo().labelSmall,
                                        color = Color.White,
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
        AnimatedVisibility(
            visible = shouldShowToolbar && isExpanded,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(10.dp),
                shape = RectangleShape,
                colors =
                    CardDefaults.elevatedCardColors(
                        containerColor =
                            startColor.value
                                .copy(
                                    red = startColor.value.red - 0.05f,
                                    green = startColor.value.green - 0.05f,
                                    blue = startColor.value.blue - 0.05f,
                                ),
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
                                    style = typo().bodyMedium,
                                    color = Color.White,
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
                                            style = typo().bodySmall,
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
                        HeartCheckBox(checked = controllerState.isLiked, size = 30) {
                            sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                        }
                        Spacer(modifier = Modifier.width(15.dp))
                        Crossfade(targetState = timelineState.loading, label = "") {
                            if (it) {
                                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.LightGray,
                                        strokeWidth = 3.dp,
                                    )
                                }
                            } else {
                                PlayPauseButton(isPlaying = controllerState.isPlaying, modifier = Modifier.size(48.dp)) {
                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
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
                            color = Color.White,
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