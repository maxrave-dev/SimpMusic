package com.maxrave.simpmusic.ui.screen.player

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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.common.Config.MAIN_PLAYER
import com.maxrave.simpmusic.expect.ui.MediaPlayerViewWithSubtitle
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.rememberIsInPipMode
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.theme.overlay
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import simpmusic.composeapp.generated.resources.baseline_more_vert_24
import simpmusic.composeapp.generated.resources.five_seconds
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenPlayer(
    navController: NavController,
    sharedViewModel: SharedViewModel = koinInject(),
    hideNavBar: () -> Unit = {},
    showNavBar: () -> Unit = {},
) {
    var isFullScreen by remember { mutableStateOf(true) }
    val isInPipMode = rememberIsInPipMode()

    FullScreenRotationImmersive(
        onLaunch = {
            hideNavBar()
            isFullScreen = true
            sharedViewModel.isFullScreen = true
        },
        onDispose = {
            sharedViewModel.isFullScreen = false
            isFullScreen = false
            showNavBar()
        },
    )

    val nowPlayingState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()

    var showBottom by rememberSaveable { mutableStateOf(false) }
    var isSliding by rememberSaveable {
        mutableStateOf(false)
    }
    var sliderValue by rememberSaveable {
        mutableFloatStateOf(0f)
    }
    var showHideFullscreenOverlay by rememberSaveable {
        mutableStateOf(false)
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

    LaunchedEffect(key1 = showHideFullscreenOverlay, key2 = isSliding) {
        if (showHideFullscreenOverlay && !isSliding) {
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

    var shouldShowSubtitle by rememberSaveable {
        mutableStateOf(true)
    }

    Box {
        MediaPlayerViewWithSubtitle(
            playerName = MAIN_PLAYER,
            modifier =
                Modifier
                    .fillMaxSize(),
            shouldPip = true,
            shouldShowSubtitle = shouldShowSubtitle,
            timelineState = timelineState,
            lyricsData = nowPlayingState.lyricsData?.lyrics,
            translatedLyricsData = nowPlayingState.lyricsData?.translatedLyrics?.first,
            isInPipMode = isInPipMode,
            mainTextStyle = typo().bodyLarge,
            translatedTextStyle = typo().bodyMedium,
        )
        if (!isInPipMode) {
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
                                    stringResource(Res.string.five_seconds),
                                    color = Color.White,
                                    style = typo().bodyMedium,
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
                                    stringResource(Res.string.five_seconds),
                                    color = Color.White,
                                    style = typo().bodyMedium,
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
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black),
                                    ),
                                ),
                        )
                        TopAppBar(
                            modifier =
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Black, Color.Transparent),
                                        ),
                                    ).padding(horizontal = 12.dp)
                                    .fillMaxWidth(),
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            colors =
                                TopAppBarDefaults.topAppBarColors().copy(
                                    containerColor = Color.Transparent,
                                ),
                            title = {
                                Text(
                                    text = nowPlayingState.nowPlayingTitle,
                                    style = typo().titleMedium,
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
                                RippleIconButton(
                                    Res.drawable.baseline_more_vert_24,
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
                                        onValueChange = { value ->
                                            isSliding = true
                                            sliderValue = value
                                        },
                                        onValueChangeFinished = {
                                            isSliding = false
                                            sharedViewModel.onUIEvent(
                                                UIEvent.UpdateProgress(sliderValue),
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
                            // /
                            Box(
                                modifier =
                                    Modifier
                                        .height(48.dp)
                                        .fillMaxWidth(),
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .wrapContentSize()
                                            .align(Alignment.TopStart),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = formatDuration((timelineState.total * (sliderValue / 100f)).roundToLong()),
                                        style = typo().labelSmall,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = " / ${formatDuration(timelineState.total)}",
                                        style = typo().bodySmall,
                                    )
                                }
                                Row(
                                    Modifier
                                        .fillMaxHeight()
                                        .wrapContentWidth()
                                        .align(Alignment.CenterEnd),
                                    verticalAlignment = Alignment.Top,
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
}

@Composable
expect fun FullScreenRotationImmersive(
    onLaunch: () -> Unit = {},
    onDispose: () -> Unit = {},
)