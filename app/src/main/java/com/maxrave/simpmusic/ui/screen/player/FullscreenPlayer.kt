package com.maxrave.simpmusic.ui.screen.player

import android.content.pm.ActivityInfo
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config.MAIN_PLAYER
import com.maxrave.simpmusic.extension.findActivity
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.ui.component.MediaPlayerView
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.theme.overlay
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@UnstableApi
fun FullscreenPlayer(
    navController: NavController,
    player: ExoPlayer = koinInject(named(MAIN_PLAYER)),
    sharedViewModel: SharedViewModel = koinInject(),
    hideNavBar: () -> Unit = {},
    showNavBar: () -> Unit = {},
) {
    val context = LocalContext.current
    var isFullScreen by remember { mutableStateOf(true) }

    var shouldEnterPipMode by rememberSaveable { mutableStateOf(false) }
    val currentShouldEnterPipMode by rememberUpdatedState(newValue = shouldEnterPipMode)

    navController.addOnDestinationChangedListener { _, des, _ ->
        shouldEnterPipMode =
            if (des.label == "FullscreenFragment") {
                Log.w("FullscreenPlayer", "shouldEnterPipMode = true")
                true
            } else {
                Log.w("FullscreenPlayer", "shouldEnterPipMode = false")
                false
            }
    }

    DisposableEffect(true) {
        hideNavBar()
        isFullScreen = true
        sharedViewModel.isFullScreen = true
        val activity = context.findActivity()
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val window = context.findActivity().window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            insetsController.apply {
                show(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            // restore original orientation when view disappears
            activity.requestedOrientation = originalOrientation
            sharedViewModel.isFullScreen = false
            isFullScreen = false
            showNavBar()
        }
    }

    LaunchedEffect(true) {
        val activity = context.findActivity()
        val window = activity.window

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    val nowPlayingState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()

    var showBottom by rememberSaveable { mutableStateOf(false) }
    var sliderValue by rememberSaveable {
        mutableFloatStateOf(0f)
    }
    var showHideFullscreenOverlay by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = timelineState) {
        sliderValue =
            if (timelineState.total > 0L) {
                timelineState.current.toFloat() * 100 / timelineState.total.toFloat()
            } else {
                0f
            }
    }

    LaunchedEffect(key1 = showHideFullscreenOverlay) {
        if (showHideFullscreenOverlay) {
            delay(3000)
            showHideFullscreenOverlay = false
        }
    }

    // For double tap effect
    val coroutineScope = rememberCoroutineScope()

    var doubleBackwardTapped by remember { mutableStateOf(false) }
    val interactionSourceBackward = remember { MutableInteractionSource() }

    var doubleForwardTapped by remember { mutableStateOf(false) }
    val interactionSourceForward = remember { MutableInteractionSource() }

    var showBackwardText by remember { mutableStateOf(false) }
    var showForwardText by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = showBackwardText) {
        if (showBackwardText) {
            delay(2000)
            showBackwardText = false
        }
    }
    LaunchedEffect(key1 = showForwardText) {
        if (showForwardText) {
            delay(2000)
            showForwardText = false
        }
    }

    var currentLineIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }
    var shouldShowSubtitle by rememberSaveable {
        mutableStateOf(true)
    }
    LaunchedEffect(key1 = timelineState) {
        val lines = nowPlayingState.lyricsData?.lyrics?.lines ?: return@LaunchedEffect
        if (timelineState.current > 0L) {
            lines.indices.forEach { i ->
                val sentence = lines[i]
                val startTimeMs = sentence.startTimeMs.toLong()

                // estimate the end time of the current sentence based on the start time of the next sentence
                val endTimeMs =
                    if (i < lines.size - 1) {
                        lines[i + 1].startTimeMs.toLong()
                    } else {
                        // if this is the last sentence, set the end time to be some default value (e.g., 1 minute after the start time)
                        startTimeMs + 60000
                    }
                if (timelineState.current in startTimeMs..endTimeMs) {
                    currentLineIndex = i
                }
            }
            if (lines.isNotEmpty() &&
                (
                    timelineState.current in (
                        0..(
                            lines.getOrNull(0)?.startTimeMs
                                ?: "0"
                        ).toLong()
                    )
                )
            ) {
                currentLineIndex = -1
            }
        } else {
            currentLineIndex = -1
        }
    }

    Box {
        MediaPlayerView(
            player = player,
            modifier = Modifier.fillMaxSize(),
        )
        if (nowPlayingState.lyricsData != null && !context.findActivity().isInPictureInPictureMode && shouldShowSubtitle) {
            Crossfade(currentLineIndex != -1) {
                val lines = nowPlayingState.lyricsData?.lyrics?.lines ?: return@Crossfade
                if (it) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(bottom = 40.dp)
                            .align(Alignment.BottomCenter),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(Modifier.fillMaxWidth(0.7f)) {
                            Column(
                                Modifier.align(Alignment.BottomCenter),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = lines.getOrNull(currentLineIndex)?.words ?: return@Crossfade,
                                    style = typo.bodyLarge,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier =
                                        Modifier
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .wrapContentWidth(),
                                )
                                Crossfade(nowPlayingState.lyricsData?.translatedLyrics?.lines != null, label = "") { translate ->
                                    val translateLines = nowPlayingState.lyricsData?.translatedLyrics?.lines ?: return@Crossfade
                                    if (translate) {
                                        Text(
                                            text = translateLines.getOrNull(currentLineIndex)?.words ?: return@Crossfade,
                                            style = typo.bodyMedium,
                                            color = Color.Yellow,
                                            textAlign = TextAlign.Center,
                                            modifier =
                                                Modifier
                                                    .background(Color.Black.copy(alpha = 0.5f))
                                                    .wrapContentWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxSize()) {
            // Left side
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clip(
                        RoundedCornerShape(
                            topEndPercent = 10,
                            bottomEndPercent = 10,
                        ),
                    ).indication(
                        interactionSource = interactionSourceBackward,
                        indication = ripple(),
                    ).pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showHideFullscreenOverlay = !showHideFullscreenOverlay
                            },
                            onDoubleTap = { offset ->
                                coroutineScope.launch {
                                    doubleBackwardTapped = true
                                    val press = PressInteraction.Press(offset)
                                    interactionSourceBackward.emit(press)
                                    sharedViewModel.onUIEvent(UIEvent.Backward)
                                    showBackwardText = true
                                    interactionSourceBackward.emit(PressInteraction.Release(press))
                                    doubleBackwardTapped = false
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(showBackwardText) {
                    if (it) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.KeyboardDoubleArrowLeft,
                                "",
                                tint = Color.White,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.five_seconds),
                                color = Color.White,
                                style = typo.bodyMedium,
                            )
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clip(
                        RoundedCornerShape(
                            topStartPercent = 10,
                            bottomStartPercent = 10,
                        ),
                    ).indication(
                        interactionSource = interactionSourceForward,
                        indication = ripple(),
                    ).pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showHideFullscreenOverlay = !showHideFullscreenOverlay
                            },
                            onDoubleTap = { offset ->
                                coroutineScope.launch {
                                    doubleForwardTapped = true
                                    val press = PressInteraction.Press(offset)
                                    interactionSourceForward.emit(press)
                                    sharedViewModel.onUIEvent(UIEvent.Forward)
                                    showForwardText = true
                                    interactionSourceForward.emit(PressInteraction.Release(press))
                                    doubleForwardTapped = false
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(showForwardText) {
                    if (it) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.five_seconds),
                                color = Color.White,
                                style = typo.bodyMedium,
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.KeyboardDoubleArrowRight,
                                "",
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
        }
        Crossfade(showHideFullscreenOverlay) {
            if (it) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(overlay),
                ) {
                    TopAppBar(
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth(),
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        colors =
                            TopAppBarDefaults.topAppBarColors().copy(
                                containerColor = Color.Transparent,
                            ),
                        title = {
                            Text(
                                text = nowPlayingState.nowPlayingTitle,
                                style = typo.titleMedium,
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
                        actions = {
                            RippleIconButton(
                                R.drawable.baseline_more_vert_24,
                            ) {
                                showBottom = true
                            }
                        },
                    )
                    Row(
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.3f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
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
                            enabled = controllerState.isPreviousAvailable,
                            onClick = {
                                sharedViewModel.onUIEvent(UIEvent.Previous)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                tint = if (controllerState.isPreviousAvailable) Color.White else Color.DarkGray,
                                contentDescription = "",
                                modifier =
                                    Modifier
                                        .size(36.dp),
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
                                sharedViewModel.onUIEvent(UIEvent.Backward)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay5,
                                tint = Color.White,
                                contentDescription = "",
                                modifier =
                                    Modifier
                                        .size(36.dp),
                            )
                        }
                        FilledTonalIconButton(
                            colors =
                                IconButtonDefaults.iconButtonColors().copy(
                                    containerColor = Color.Transparent,
                                ),
                            modifier =
                                Modifier
                                    .size(64.dp)
                                    .aspectRatio(1f)
                                    .clip(
                                        CircleShape,
                                    ),
                            onClick = {
                                sharedViewModel.onUIEvent(UIEvent.PlayPause)
                            },
                        ) {
                            Crossfade(controllerState.isPlaying) {
                                if (it) {
                                    Icon(
                                        imageVector = Icons.Filled.Pause,
                                        tint = Color.White,
                                        contentDescription = "",
                                        modifier =
                                            Modifier
                                                .size(48.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        tint = Color.White,
                                        contentDescription = "",
                                        modifier =
                                            Modifier
                                                .size(48.dp),
                                    )
                                }
                            }
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
                                imageVector = Icons.Filled.Forward5,
                                tint = Color.White,
                                contentDescription = "",
                                modifier =
                                    Modifier
                                        .size(36.dp),
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
                            enabled = controllerState.isNextAvailable,
                            onClick = {
                                sharedViewModel.onUIEvent(UIEvent.Next)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                tint = if (controllerState.isNextAvailable) Color.White else Color.DarkGray,
                                contentDescription = "",
                                modifier =
                                    Modifier
                                        .size(36.dp),
                            )
                        }
                    }
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(0.3f)
                                .padding(horizontal = 40.dp),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .height(32.dp)
                                    .fillMaxWidth(),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxHeight()
                                        .wrapContentWidth()
                                        .align(Alignment.CenterStart),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = formatDuration(timelineState.current, context),
                                    style = typo.labelSmall,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = " / ${formatDuration(timelineState.total, context)}",
                                    style = typo.bodySmall,
                                )
                            }
                            Row(
                                Modifier
                                    .align(Alignment.CenterEnd),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FilledTonalIconButton(
                                    colors =
                                        IconButtonDefaults.iconButtonColors().copy(
                                            containerColor = Color.Transparent,
                                        ),
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .aspectRatio(1f)
                                            .clip(
                                                CircleShape,
                                            ),
                                    onClick = {
                                        shouldShowSubtitle = !shouldShowSubtitle
                                    },
                                ) {
                                    Crossfade(shouldShowSubtitle) {
                                        if (it) {
                                            Icon(
                                                imageVector = Icons.Filled.SubtitlesOff,
                                                tint = Color.White,
                                                contentDescription = "",
                                                modifier =
                                                    Modifier
                                                        .size(24.dp),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Subtitles,
                                                tint = Color.White,
                                                contentDescription = "",
                                                modifier =
                                                    Modifier
                                                        .size(24.dp),
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                FilledTonalIconButton(
                                    colors =
                                        IconButtonDefaults.iconButtonColors().copy(
                                            containerColor = Color.Transparent,
                                        ),
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .aspectRatio(1f)
                                            .clip(
                                                CircleShape,
                                            ),
                                    onClick = {
                                        navController.navigateUp()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FullscreenExit,
                                        tint = Color.White,
                                        contentDescription = "",
                                        modifier =
                                            Modifier
                                                .size(24.dp),
                                    )
                                }
                            }
                        }
                        Box(
                            Modifier
                                .padding(
                                    vertical = 5.dp,
                                ),
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
                                                trackColor = Color.DarkGray,
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
                                    onValueChange = {
                                        sharedViewModel.onUIEvent(
                                            UIEvent.UpdateProgress(it),
                                        )
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
                                                    thumbColor = Color.White,
                                                    activeTrackColor = Color.White,
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
                                                    thumbColor = Color.White,
                                                    activeTrackColor = Color.White,
                                                    inactiveTrackColor = Color.Transparent,
                                                ),
                                            enabled = true,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showBottom) {
            NowPlayingBottomSheet(
                onDismiss = { showBottom = false },
                navController = navController,
                setSleepTimerEnable = true,
                changeMainLyricsProviderEnable = true,
                song = null,
            )
        }
    }
}