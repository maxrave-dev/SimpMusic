package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.kmpalette.rememberPaletteState
import com.maxrave.common.MERGING_DATA_TYPE
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.extension.isVideo
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.utils.YouTubeLikeStatus
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.extension.GradientAngle
import com.maxrave.simpmusic.extension.GradientOffset
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.hsvToColor
import com.maxrave.simpmusic.ui.component.FullscreenLyricsContent
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherDestination
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentActions
import com.maxrave.simpmusic.ui.screen.player.content.NowPlayingContentState
import com.maxrave.simpmusic.ui.screen.player.content.PlayerBackdropColor
import com.maxrave.simpmusic.ui.screen.player.content.fullscreenCanvas
import com.maxrave.simpmusic.ui.screen.player.content.toAudioQualityLabel
import com.maxrave.simpmusic.ui.theme.AppTheme
import com.maxrave.simpmusic.ui.theme.ForceDarkContent
import com.maxrave.simpmusic.ui.theme.parseThemeColorHex
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.circle_app_icon

/**
 * Separate undecorated Fullscreen Window for Now Playing lyrics/artwork view.
 *
 * Implements Step 2:
 * - Window initialization and position binding to main window's display monitor.
 * - Explicit [LocalLifecycleOwner] provider inside [Window] so all [collectAsStateWithLifecycle]
 *   calls in content and nested components have a valid lifecycle.
 * - Reuses real [FullscreenLyricsContent] with full state and actions.
 * - Wraps content with [AppTheme] and [ForceDarkContent] to ensure theme,
 *   Koin injection, and all CompositionLocals propagate properly.
 * - Sourced from [SharedViewModel] and [MediaPlayerHandler].
 * - Handles Esc and F11 to close cleanly.
 */
@Composable
fun FullscreenPlayerWindow(
    sharedViewModel: SharedViewModel,
    mainAwtWindow: java.awt.Window?,
    onCloseRequest: () -> Unit,
) {
    // Determine the origin of the monitor currently displaying the main window.
    val initialPosition = remember(mainAwtWindow) {
        val awt = mainAwtWindow
        if (awt != null && awt.isDisplayable) {
            val bounds = awt.graphicsConfiguration.bounds
            Logger.d(
                "FullscreenPlayerWindow",
                "Binding to main window monitor bounds: x=${bounds.x}, y=${bounds.y}, w=${bounds.width}, h=${bounds.height}",
            )
            WindowPosition(bounds.x.dp, bounds.y.dp)
        } else {
            WindowPosition.PlatformDefault
        }
    }

    val windowState = rememberWindowState(
        placement = WindowPlacement.Fullscreen,
        position = initialPosition,
    )

    Window(
        onCloseRequest = onCloseRequest,
        title = "SimpMusic - Fullscreen Player",
        icon = painterResource(Res.drawable.circle_app_icon),
        undecorated = true,
        transparent = false,
        alwaysOnTop = false,
        state = windowState,
        onKeyEvent = { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown &&
                (keyEvent.key == Key.Escape || keyEvent.key == Key.F11)
            ) {
                Logger.d("FullscreenPlayerWindow", "Key ${keyEvent.key} pressed -> exiting fullscreen")
                onCloseRequest()
                true
            } else {
                false
            }
        },
    ) {
        // Guarantee keyboard focus to the fullscreen window and restore on dispose
        DisposableEffect(window) {
            window.toFront()
            window.requestFocus()
            onDispose {
                mainAwtWindow?.toFront()
                mainAwtWindow?.requestFocus()
            }
        }

        // Dedicated LifecycleOwner for this secondary window so collectAsStateWithLifecycle works reliably
        val lifecycleOwner = remember {
            object : LifecycleOwner {
                override val lifecycle: Lifecycle = LifecycleRegistry(this).apply {
                    currentState = Lifecycle.State.RESUMED
                }
            }
        }
        DisposableEffect(lifecycleOwner) {
            onDispose {
                (lifecycleOwner.lifecycle as? LifecycleRegistry)?.currentState = Lifecycle.State.DESTROYED
            }
        }

        CompositionLocalProvider(
            LocalLifecycleOwner provides lifecycleOwner,
        ) {
            FullscreenPlayerContent(
                sharedViewModel = sharedViewModel,
                onCloseRequest = onCloseRequest,
            )
        }
    }
}

@Composable
private fun FullscreenPlayerContent(
    sharedViewModel: SharedViewModel,
    onCloseRequest: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val mediaPlayerHandler: MediaPlayerHandler = koinInject()
    val navController = rememberNavController()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // ViewModel State collections
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val lyricsOffsetMs by sharedViewModel.getLyricsOffsetMs().collectAsStateWithLifecycle(0)
    val likeStatus by sharedViewModel.likeStatus.collectAsStateWithLifecycle()
    val castState by sharedViewModel.castState.collectAsStateWithLifecycle()
    val formatState by sharedViewModel.format.collectAsStateWithLifecycle(initialValue = null)
    val shouldShowVideo by sharedViewModel.getVideo.collectAsStateWithLifecycle()
    val isUserLoggedIn by sharedViewModel
        .isUserLoggedInFlow()
        .collectAsStateWithLifecycle(initialValue = false)
    val nowPlayingStyle by sharedViewModel
        .getNowPlayingStyle()
        .collectAsStateWithLifecycle(initialValue = DataStoreManager.NOW_PLAYING_STYLE_SPOTIFY)

    // Theme & styling state
    val themeMode by sharedViewModel
        .getThemeMode()
        .collectAsStateWithLifecycle(DataStoreManager.THEME_MODE_DARK)
    val themeColorSource by sharedViewModel
        .getThemeColorSource()
        .collectAsStateWithLifecycle(DataStoreManager.THEME_COLOR_DEFAULT)
    val customThemeColorHex by sharedViewModel
        .getCustomThemeColor()
        .collectAsStateWithLifecycle(DataStoreManager.DEFAULT_THEME_COLOR_HEX)
    val isLiquidGlassEnabled by sharedViewModel
        .getEnableLiquidGlass()
        .collectAsStateWithLifecycle(DataStoreManager.FALSE)

    // Artwork & Queue State
    val nowPlayingState by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val queueDataState by sharedViewModel.getQueueDataState().collectAsStateWithLifecycle()
    val artworkQueue by remember {
        derivedStateOf { queueDataState?.data?.listTracks ?: emptyList() }
    }
    val nowPlayingVideoId: String? = nowPlayingState?.track?.videoId
    val commentsVideoId: String? =
        nowPlayingState
            ?.mediaItem
            ?.let { if (it.isVideo()) it.mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO) else it.mediaId }
            ?.takeIf { it.isNotEmpty() }

    val currentOrderIndex by remember(artworkQueue, nowPlayingVideoId) {
        derivedStateOf {
            deriveOrderIndex(
                queue = artworkQueue,
                nowPlayingVideoId = nowPlayingVideoId,
                playerOrderIndex = mediaPlayerHandler.currentOrderIndex(),
            )
        }
    }

    val artworkPagerState =
        rememberPagerState(
            initialPage = currentOrderIndex.coerceAtLeast(0),
            pageCount = { artworkQueue.size.coerceAtLeast(1) },
        )

    // Palette State
    val paletteState = rememberPaletteState()
    val startColor = remember { Animatable(Color.Black) }
    val endColor = remember { Animatable(Color.Black) }
    var spotShadowColor by remember { mutableStateOf(Color.White) }

    LaunchedEffect(Unit) {
        snapshotFlow { screenDataState.bitmap }
            .filterNotNull()
            .distinctUntilChanged()
            .collectLatest {
                paletteState.generate(it)
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                spotShadowColor = it.getColorFromPalette()
                startColor.animateTo(it.getColorFromPalette())
                endColor.animateTo(PlayerBackdropColor)
            }
    }

    // Slider State
    var isSliding by rememberSaveable { mutableStateOf(false) }
    var sliderValue by rememberSaveable { mutableFloatStateOf(0f) }
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

    // Slider crossfade rainbow color
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

    // Current lyric line sync
    var currentLyricLineIndex by rememberSaveable { mutableIntStateOf(-1) }
    LaunchedEffect(timelineState, screenDataState.lyricsData?.lyrics, lyricsOffsetMs) {
        val lyrics = screenDataState.lyricsData?.lyrics
        if (lyrics == null || lyrics.syncType == "UNSYNCED" || lyrics.syncType == null) {
            currentLyricLineIndex = -1
            return@LaunchedEffect
        }
        val lines = lyrics.lines ?: return@LaunchedEffect
        val nowMs = timelineState.current - lyricsOffsetMs
        if (nowMs > 0L) {
            lines.indices.forEach { i ->
                val startTimeMs = lines[i].startTimeMs.toLongOrNull() ?: 0L
                val endTimeMs =
                    if (i < lines.size - 1) {
                        lines[i + 1].startTimeMs.toLongOrNull() ?: 0L
                    } else {
                        startTimeMs + 60000
                    }
                if (nowMs in startTimeMs..endTimeMs) {
                    currentLyricLineIndex = i
                }
            }
            if (lines.isNotEmpty() &&
                nowMs in 0..(lines.getOrNull(0)?.startTimeMs?.toLongOrNull() ?: 0L)
            ) {
                currentLyricLineIndex = -1
            }
        } else {
            currentLyricLineIndex = -1
        }
    }

    // Assemble NowPlayingContentState
    val contentState =
        NowPlayingContentState(
            screenData = screenDataState,
            controllerState = controllerState,
            timelineState = timelineState,
            timelineFlow = sharedViewModel.timeline,
            likeStatus = likeStatus == YouTubeLikeStatus.LIKE,
            castState = castState,
            shouldShowVideo = shouldShowVideo,
            isUserLoggedIn = isUserLoggedIn,
            artworkQueue = artworkQueue,
            currentOrderIndex = currentOrderIndex,
            artworkPagerState = artworkPagerState,
            startColor = startColor,
            endColor = endColor,
            spotShadowColor = spotShadowColor,
            gradientOffset = GradientOffset(GradientAngle.CW135),
            sliderTrackColor = sliderTrackColor,
            sliderValue = sliderValue,
            currentLyricLineIndex = currentLyricLineIndex,
            showControlLayout = true,
            controlLayoutAlpha = 1f,
            showHideMiddleLayout = screenDataState.fullscreenCanvas() == null,
            shouldShowToolbar = false,
            isInPipMode = false,
            mainScrollState = rememberScrollState(),
            isExpanded = true,
            dismissIcon = SimpIcons.Close,
            audioQualityLabel = formatState.toAudioQualityLabel(),
            lyricsOffsetMs = lyricsOffsetMs.toLong(),
            videoAspectRatio = 16f / 9,
            videoId = commentsVideoId,
        )

    // Assemble NowPlayingContentActions
    val contentActions =
        NowPlayingContentActions(
            onUIEvent = { sharedViewModel.onUIEvent(it) },
            onSeekToQueueIndex = { index ->
                mediaPlayerHandler.playMediaItemInMediaSource(index)
            },
            onArtworkBitmap = { sharedViewModel.setBitmap(it) },
            onSliderChange = { newValue ->
                isSliding = true
                sliderValue = newValue
            },
            onSliderChangeFinished = {
                isSliding = false
                sharedViewModel.onUIEvent(
                    UIEvent.UpdateProgress(sliderValue),
                )
            },
            onToggleControls = {},
            onNavigateToArtist = {
                val song = sharedViewModel.nowPlayingState.value?.songEntity
                (
                    song?.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() }
                        ?: screenDataState.songInfoData?.authorId
                )?.let { channelId ->
                    onCloseRequest()
                    navController.navigate(
                        ArtistDestination(channelId = channelId),
                    )
                }
            },
            onOpenListenTogether = {
                onCloseRequest()
                navController.navigate(ListenTogetherDestination)
            },
            onAddToYouTubeLiked = { sharedViewModel.addToYouTubeLiked() },
            onShowMoreSheet = {},
            onShowQueue = {},
            onShowInfo = {},
            onShowAddToPlaylist = {},
            onShowComments = {},
            onWriteComment = {},
            onShowFullscreenLyrics = {},
            onShowVoteDialog = {},
            onLyricsOffsetChange = { sharedViewModel.setLyricsOffsetMs(it) },
            onEnterFullscreenVideo = {},
            onDismiss = onCloseRequest,
            onToolbarVisibilityChange = {},
            onMoveQueueItem = { from, to ->
                coroutineScope.launch {
                    mediaPlayerHandler.swap(from, to)
                }
            },
            onRemoveQueueItem = { index ->
                mediaPlayerHandler.removeMediaItem(index)
            },
            onQueueItemMore = { _, _ -> },
        )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        AppTheme(
            themeMode = themeMode,
            themeColorSource = themeColorSource,
            customThemeColor = parseThemeColorHex(customThemeColorHex),
            liquidGlassEnabled = isLiquidGlassEnabled == TRUE,
        ) {
            ForceDarkContent {
                FullscreenLyricsContent(
                    sharedViewModel = sharedViewModel,
                    navController = navController,
                    color = startColor.value,
                    state = contentState,
                    actions = contentActions,
                    nowPlayingStyle = nowPlayingStyle,
                    onDismiss = onCloseRequest,
                )
            }
        }
    }
}
