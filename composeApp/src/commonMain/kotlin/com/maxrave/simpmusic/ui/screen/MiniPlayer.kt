package com.maxrave.simpmusic.ui.screen

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kmpalette.rememberPaletteState
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.utils.connectArtists
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.toggleMiniPlayer
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.toImageBitmap
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.extension.toResizedBitmap
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.ExplicitBadge
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.PlayPauseButton
import com.maxrave.simpmusic.ui.component.PlayerControlLayout
import com.maxrave.simpmusic.ui.component.QueueBottomSheet
import com.maxrave.simpmusic.ui.component.liquidGlass
import com.maxrave.simpmusic.ui.component.rememberHolderPainter
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.PictureInPictureAlt
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.VolumeOff
import com.maxrave.simpmusic.ui.icon.VolumeUp
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.crossfading
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

private const val TAG = "MiniPlayer"

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    modifier: Modifier,
    backdrop: PlatformBackdrop,
    sharedViewModel: SharedViewModel = koinInject(),
    onClose: () -> Unit,
    onClick: () -> Unit,
) {
    val isLiquidGlassEnabled by sharedViewModel.getEnableLiquidGlass().collectAsStateWithLifecycle(DataStoreManager.FALSE)
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()

    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0f) }

    // The Desktop capsule is always liquid glass, so it needs the glass code paths whatever the
    // setting says — both the luminance sampling loop that drives the glass and the theme-following
    // text colour. Leaving them gated left the capsule with luminance stuck at 0: a 2dp blur and a
    // 0.12 darken, which is why it looked like a smear rather than glass. The setting still governs
    // the Android card below.
    val useGlassSurface = isLiquidGlassEnabled == DataStoreManager.TRUE || getPlatform() == Platform.Desktop

    val isDarkTheme = LocalIsDarkTheme.current
    val textColor by animateColorAsState(
        // With liquid glass the surface follows the theme (light = frosted white → black text);
        // without it, the surface is the artwork colour, so follow the backdrop luminance.
        targetValue =
            if (useGlassSurface) {
                if (isDarkTheme) Color.White else Color.Black
            } else if (luminanceAnimation.value > 0.6f) {
                Color.Black
            } else {
                Color.White
            },
        label = "MiniPlayerTextColor",
        animationSpec = tween(500),
    )

    LaunchedEffect(layer, useGlassSurface) {
        val buffer = IntArray(25)
        while (isActive && useGlassSurface) {
            try {
                withContext(Dispatchers.Main) {
                    val imageBitmap = layer.toImageBitmap()
                    val thumbnail = imageBitmap.toResizedBitmap(5, 5)
                    thumbnail.readPixels(buffer)
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error getting pixels from layer: ${e.message}")
            }
            val averageLuminance =
                (0 until 25).sumOf { index ->
                    val color = buffer.get(index)
                    val r = (color shr 16 and 0xFF) / 255f
                    val g = (color shr 8 and 0xFF) / 255f
                    val b = (color and 0xFF) / 255f
                    0.2126 * r + 0.7152 * g + 0.0722 * b
                } / 25
            luminanceAnimation.animateTo(
                averageLuminance.coerceIn(0.3, 0.8).toFloat(),
                tween(500),
            )
            delay(1.seconds)
        }
    }

    val (songEntity, setSongEntity) =
        remember {
            mutableStateOf<SongEntity?>(null)
        }
    val (liked, setLiked) =
        remember {
            mutableStateOf(false)
        }
    val (isPlaying, setIsPlaying) =
        remember {
            mutableStateOf(false)
        }
    val (progress, setProgress) =
        remember {
            mutableFloatStateOf(0f)
        }
    val (isCrossfading, setIsCrossfading) =
        remember {
            mutableStateOf(false)
        }

    val coroutineScope = rememberCoroutineScope()

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "",
    )

    // Palette state
    val paletteState = rememberPaletteState()
    val background =
        remember {
            Animatable(Color.DarkGray)
        }

    val offsetX = remember { Animatable(initialValue = 0f) }
    val offsetY = remember { Animatable(0f) }

    var loading by rememberSaveable {
        mutableStateOf(true)
    }

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
                background.animateTo(it.getColorFromPalette())
            }
    }

    LaunchedEffect(key1 = true) {
        val job1 =
            launch {
                sharedViewModel.nowPlayingState.collect { item ->
                    if (item != null) {
                        setSongEntity(item.songEntity)
                    }
                }
            }
        val job2 =
            launch {
                sharedViewModel.controllerState.collectLatest { state ->
                    setLiked(state.isLiked)
                    setIsPlaying(state.isPlaying)
                    setIsCrossfading(state.isCrossfading)
                }
            }
        val job4 =
            launch {
                sharedViewModel.timeline.collect { timeline ->
                    loading = timeline.loading
                    val prog =
                        if (timeline.total > 0L && timeline.current >= 0L) {
                            timeline.current.toFloat() / timeline.total
                        } else {
                            0f
                        }
                    setProgress(prog)
                }
            }
        job1.join()
        job2.join()
        job4.join()
    }

    if (getPlatform() == Platform.Android) {
        // One shape for both the Card and the clip below. They must not diverge: the clip wraps
        // the Card's own background draw, so the larger radius wins and silently becomes the
        // visible one.
        val miniPlayerShape =
            if (isLiquidGlassEnabled == DataStoreManager.TRUE) CircleShape else RoundedCornerShape(12.dp)
        Card(
            shape = miniPlayerShape,
            colors =
                CardDefaults.cardColors(
                    containerColor = if (isLiquidGlassEnabled == DataStoreManager.TRUE) Color.Transparent else background.value,
                    disabledContainerColor = if (isLiquidGlassEnabled == DataStoreManager.TRUE) Color.Transparent else background.value,
                ),
            modifier =
                modifier
                    .then(
                        if (isLiquidGlassEnabled == DataStoreManager.TRUE) {
                            Modifier.liquidGlass(backdrop, layer, luminanceAnimation.value, RoundedCornerShape(16.dp))
                        } else {
                            Modifier
                        },
                    ).then(
                        Modifier
                            .clip(miniPlayerShape)
                            .offset { IntOffset(0, offsetY.value.roundToInt()) }
                            .clickable(
                                onClick = onClick,
                            ).pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                    },
                                    onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                                        if (offsetY.value + dragAmount > 0) {
                                            coroutineScope.launch {
                                                change.consume()
                                                offsetY.animateTo(offsetY.value + 2 * dragAmount)
                                                Logger.w("MiniPlayer", "Dragged ${offsetY.value}")
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            offsetY.animateTo(0f)
                                        }
                                    },
                                    onDragEnd = {
                                        Logger.w("MiniPlayer", "Drag Ended")
                                        coroutineScope.launch {
                                            if (offsetY.value > 70) {
                                                onClose()
                                            }
                                            offsetY.animateTo(0f)
                                        }
                                    },
                                )
                            },
                    ),
        ) {
            Box(modifier = Modifier.fillMaxHeight()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxSize(),
                ) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Box(modifier = Modifier.weight(1F)) {
                        Row(
                            modifier =
                                Modifier
                                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragStart = {
                                            },
                                            onHorizontalDrag = {
                                                change: PointerInputChange,
                                                dragAmount: Float,
                                                ->
                                                coroutineScope.launch {
                                                    change.consume()
                                                    offsetX.animateTo(offsetX.value + dragAmount * 2)
                                                    Logger.w("MiniPlayer", "Dragged ${offsetX.value}")
                                                }
                                            },
                                            onDragCancel = {
                                                Logger.w("MiniPlayer", "Drag Cancelled")
                                                coroutineScope.launch {
                                                    if (offsetX.value > 200) {
                                                        sharedViewModel.onUIEvent(UIEvent.Previous)
                                                    } else if (offsetX.value < -120) {
                                                        sharedViewModel.onUIEvent(UIEvent.Next)
                                                    }
                                                    offsetX.animateTo(0f)
                                                }
                                            },
                                            onDragEnd = {
                                                Logger.w("MiniPlayer", "Drag Ended")
                                                coroutineScope.launch {
                                                    if (offsetX.value > 200) {
                                                        sharedViewModel.onUIEvent(UIEvent.Previous)
                                                    } else if (offsetX.value < -120) {
                                                        sharedViewModel.onUIEvent(UIEvent.Next)
                                                    }
                                                    offsetX.animateTo(0f)
                                                }
                                            },
                                        )
                                    },
                        ) {
                            AsyncImage(
                                model =
                                    ImageRequest
                                        .Builder(LocalPlatformContext.current)
                                        .data(songEntity?.thumbnails)
                                        .crossfade(550)
                                        .build(),
                                placeholder = rememberHolderPainter(),
                                error = rememberHolderPainter(),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                onSuccess = {
                                    bitmap =
                                        it.result.image.toImageBitmap()
                                },
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .align(Alignment.CenterVertically)
                                        .clip(
                                            RoundedCornerShape(4.dp),
                                        ),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            AnimatedContent(
                                targetState = songEntity,
                                modifier = Modifier.weight(1F).fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart,
                                transitionSpec = {
                                    // Compare the incoming number with the previous number.
                                    if (targetState != initialState) {
                                        // If the target number is larger, it slides up and fades in
                                        // while the initial (smaller) number slides up and fades out.
                                        (
                                            slideInHorizontally { width ->
                                                width
                                            } + fadeIn()
                                        ).togetherWith(
                                            slideOutHorizontally { width -> +width } + fadeOut(),
                                        )
                                    } else {
                                        // If the target number is smaller, it slides down and fades in
                                        // while the initial number slides down and fades out.
                                        (
                                            slideInHorizontally { width ->
                                                +width
                                            } + fadeIn()
                                        ).togetherWith(
                                            slideOutHorizontally { width -> width } + fadeOut(),
                                        )
                                    }.using(
                                        // Disable clipping since the faded slide-in/out should
                                        // be displayed out of bounds.
                                        SizeTransform(clip = false),
                                    )
                                },
                            ) { target ->
                                if (target != null) {
                                    Column(
                                        Modifier
                                            .wrapContentHeight()
                                            .align(Alignment.CenterVertically),
                                    ) {
                                        Text(
                                            text = (songEntity?.title ?: "").toString(),
                                            style = typo().labelSmall,
                                            color = textColor,
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.animation.AnimatedVisibility(visible = songEntity?.isExplicit == true) {
                                                ExplicitBadge(
                                                    modifier =
                                                        Modifier
                                                            .size(20.dp)
                                                            .padding(end = 4.dp)
                                                            .weight(1f),
                                                )
                                            }
                                            Text(
                                                text = (songEntity?.artistName?.connectArtists() ?: ""),
                                                style = typo().bodySmall,
                                                maxLines = 1,
                                                color = textColor,
                                                modifier =
                                                    Modifier
                                                        .weight(1f)
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
                        }
                    }
                    Spacer(modifier = Modifier.width(15.dp))
                    HeartCheckBox(checked = liked, size = 30, tint = textColor) {
                        sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                    }
                    Spacer(modifier = Modifier.width(15.dp))
                    Crossfade(targetState = loading, label = "") {
                        if (it) {
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = textColor,
                                    strokeWidth = 3.dp,
                                )
                            }
                        } else {
                            PlayPauseButton(isPlaying = isPlaying, modifier = Modifier.size(48.dp), tint = textColor) {
                                sharedViewModel.onUIEvent(UIEvent.PlayPause)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(15.dp))
                }
                Box(
                    modifier =
                        Modifier
                            .wrapContentSize(Alignment.Center)
                            .padding(
                                horizontal = 10.dp,
                            ).align(Alignment.BottomCenter),
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(4.dp),
                                ),
                        color = textColor,
                        trackColor = Color.Transparent,
                        strokeCap = StrokeCap.Round,
                        drawStopIndicator = {},
                    )
                }
            }
        }
    } else {
        // Desktop bottom bar surface follows the theme (haze over content), so text and controls
        // use the theme foreground token instead of the artwork-luminance colour.
        val textColor = MaterialTheme.colorScheme.onBackground

        // Crossfade cue: a label on the artist line, nothing on the bar itself. The Now Playing
        // screen cycles the track through hues for this, which on a 2dp hairline reads as a
        // rendering fault rather than as a transition.
        // Head of the highlight that travels through the "Crossfading" label, 0..1. Runs
        // unconditionally: putting it behind the crossfade check would restart the animation from
        // zero each time the label appears, so the sweep would jump rather than continue.
        val sweepTransition = rememberInfiniteTransition(label = "miniPlayerCrossfadeSweep")
        val crossfadeSweep by sweepTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(3200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "miniPlayerSweepHead",
        )
        val progressColor = textColor

        var isSliding by rememberSaveable {
            mutableStateOf(false)
        }
        var sliderValue by rememberSaveable {
            mutableFloatStateOf(0f)
        }
        var showQueueBottomSheet by rememberSaveable {
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
        if (showQueueBottomSheet) {
            QueueBottomSheet(
                onDismiss = {
                    showQueueBottomSheet = false
                },
            )
        }
        // Apple Music-style floating capsule: transport on the left, the track and its slim
        // progress slider in the middle, the action cluster on the right. Size and placement come
        // from the caller (App.kt), so the capsule keeps a fixed width and floats over content.
        // Always liquid glass, not gated on the setting: the capsule IS the glass shape here, and
        // falling back to a haze blur gives a dark smear instead of a floating pill.
        val capsuleShape = RoundedCornerShape(50)
        val density = LocalDensity.current
        Box(
            modifier
                .liquidGlass(backdrop, layer, luminanceAnimation.value, capsuleShape, blurScale = 1.2f)
                .clip(capsuleShape)
                .clickable {
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .fillMaxHeight()
                    // No vertical padding: the progress line is bottom-aligned inside its own 16dp
                    // touch box, so the line already floats 8dp above whatever the bottom edge is.
                    // Padding on top of that pushed it back up against the artwork. Everything else
                    // in this row is centred, so losing the inset costs them nothing.
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // PlayerControlLayout is a fillMaxWidth Row with SpaceEvenly and weight(1f) on every
                // button, so it consumes whatever width it is handed. The old layout kept it in check
                // with Column(width = 600.dp); inside a capsule it must be boxed to a fixed width or
                // it spreads across the whole bar and pushes the other two clusters out of view.
                Box(Modifier.width(200.dp)) {
                    PlayerControlLayout(
                        controllerState,
                        isSmallSize = true,
                        plainPlayPause = true,
                        horizontalPadding = 0.dp,
                        // Dark keeps the familiar seed; light needs the darker seed-derived
                        // primary or the active state washes out on the light glass.
                        activeColor = if (isDarkTheme) com.maxrave.simpmusic.ui.theme.seed else MaterialTheme.colorScheme.primary,
                        contentColor = textColor,
                    ) {
                        sharedViewModel.onUIEvent(it)
                    }
                }
                VerticalDivider(
                    modifier = Modifier.height(28.dp).padding(horizontal = 14.dp),
                    color = textColor.copy(alpha = 0.2f),
                )
                // The whole track cluster is the hover target, not the progress line itself:
                // pointing anywhere near the title thickens the slider and reveals the
                // timestamps, so a 2dp line never has to be hit precisely. Apple hides the
                // times behind a much smaller hover area and it is the single most complained
                // about part of their Tahoe player.
                val trackInteraction = remember { MutableInteractionSource() }
                val isTrackHovered by trackInteraction.collectIsHoveredAsState()
                val showScrubber = isTrackHovered || isSliding
                // A Box, not a Column: the [artwork -> text] content is centred on the capsule's own
                // vertical axis and the progress line hangs off the bottom edge. Stacking them in a
                // Column instead centres the PAIR, which pushes the content above the axis by half
                // the slider's height. Across the 60dp capsule that lands the ~33dp content at 13-46
                // and the line at 52, clear of the content by ~6dp. The old stacked layout could not
                // fit its 56dp of children in a padded 52dp box at all, and overlapped by 4dp.
                Box(
                    modifier =
                        Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                            .hoverable(trackInteraction),
                ) {
                    // Trim.Both + Alignment.Center: the app font carries most of its slack under
                    // the baseline, so glyphs ride low inside their own line box — the box was
                    // centred all along, the DIGITS were not. This centres and hugs the glyphs.
                    val scrubberDigits =
                        typo().bodySmall.copy(
                            lineHeight = 11.sp,
                            lineHeightStyle =
                                LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both,
                                ),
                        )
                    // Hovering swaps the WHOLE content block — artwork included — out for the
                    // timestamps, the way Apple's capsule does. The two cross-fade through alpha
                    // rather than AnimatedVisibility, because an alpha of 0 is still MEASURED: the
                    // content keeps donating its height, so nothing around it reflows as the pointer
                    // arrives, and the swap area needs no hardcoded height.
                    val infoAlpha by animateFloatAsState(
                        targetValue = if (showScrubber) 0f else 1f,
                        animationSpec = tween(200),
                        label = "CapsuleInfoAlpha",
                    )
                    Row(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .graphicsLayer { alpha = infoAlpha },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(songEntity?.thumbnails)
                                    .crossfade(550)
                                    .build(),
                            placeholder = rememberHolderPainter(),
                            error = rememberHolderPainter(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            onSuccess = {
                                bitmap =
                                    it.result.image.toImageBitmap()
                            },
                            // 32dp, not 40: the artwork is the tallest thing in the content row, so it
                            // sets the floor under the capsule's own height once the progress box is
                            // hung below it. At 40 the shortest capsule that still cleared the line
                            // was 72dp, which read as a slab rather than a floating pill.
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .clip(
                                        RoundedCornerShape(6.dp),
                                    ),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = (songEntity?.title ?: "").toString(),
                                // labelSmall is 14sp — oversized against a 40dp artwork; keep its
                                // weight, drop the size a notch.
                                style = typo().labelSmall.copy(fontSize = 12.sp),
                                color = textColor,
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.animation.AnimatedVisibility(visible = songEntity?.isExplicit == true) {
                                    ExplicitBadge(
                                        modifier =
                                            Modifier
                                                .size(16.dp)
                                                .padding(end = 4.dp),
                                    )
                                }
                                Text(
                                    text = (songEntity?.artistName?.connectArtists() ?: ""),
                                    style = typo().bodySmall,
                                    color = textColor.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    // fill = false so the artist name gives the label room instead
                                    // of claiming the whole row and pushing it out of view.
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                // On the artist line rather than with the timestamps: that row
                                // rides `alpha = 1f - infoAlpha`, so it only exists while the
                                // pointer is over the capsule — a crossfade cue nobody sees
                                // unless they happen to be hovering is no cue at all.
                                AnimatedVisibility(
                                    visible = timelineState.isCrossfading,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    // The label is what travels: a highlight sweeping left to
                                    // right through the glyphs. TextStyle takes a brush directly,
                                    // so the gradient paints the text itself — no overlay, no
                                    // clipping, and it keeps working whatever the label's width.
                                    val shimmerSpan = 140f
                                    val shimmerHead = crossfadeSweep * (shimmerSpan * 3f) - shimmerSpan
                                    Text(
                                        text = " · " + stringResource(Res.string.crossfading),
                                        style =
                                            typo().bodySmall.copy(
                                                brush =
                                                    Brush.horizontalGradient(
                                                        0f to textColor.copy(alpha = 0.45f),
                                                        // The sweep head is PURE white, not the resting label colour — the label
                                                        // colour is an adaptive grey, and a grey gleam reads as no gleam at all.
                                                        0.5f to Color.White,
                                                        1f to textColor.copy(alpha = 0.45f),
                                                        startX = shimmerHead,
                                                        endX = shimmerHead + shimmerSpan,
                                                        tileMode = TileMode.Clamp,
                                                    ),
                                            ),
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    // The timestamps take the content's place on the same axis, spanning the whole
                    // cluster rather than only the text column — so the elapsed digit starts where the
                    // artwork was, which is what makes the swap read as one block being replaced.
                    Row(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .graphicsLayer { alpha = 1f - infoAlpha },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatDuration((timelineState.total * (sliderValue / 100f)).roundToLong()),
                            style = scrubberDigits,
                            color = textColor.copy(alpha = 0.7f),
                            maxLines = 1,
                        )
                        Text(
                            // Time REMAINING, signed, which is what Apple's capsule reports on the
                            // right — not the track's total length.
                            text =
                                "−" +
                                    formatDuration(
                                        (timelineState.total * (1f - sliderValue / 100f)).roundToLong(),
                                    ),
                            style = scrubberDigits,
                            color = textColor.copy(alpha = 0.7f),
                            maxLines = 1,
                        )
                    }
                    // The timestamps now sit on the content line, so the track no longer has to
                    // inset itself to keep clear of them.
                    CapsuleProgress(
                        sliderValue = sliderValue,
                        loading = loading,
                        trackHeight = if (showScrubber) 4.dp else 2.dp,
                        thumbSize = 0.dp,
                        textColor = textColor,
                        progressColor = progressColor,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                        onValueChange = {
                            isSliding = true
                            sliderValue = it * 100f
                        },
                        onValueChangeFinished = {
                            isSliding = false
                            sharedViewModel.onUIEvent(
                                UIEvent.UpdateProgress(sliderValue),
                            )
                        },
                    )
                }
                VerticalDivider(
                    modifier = Modifier.height(28.dp).padding(horizontal = 14.dp),
                    color = textColor.copy(alpha = 0.2f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 40dp cell to sit on the IconButton grid, and size 32 on purpose:
                    // HeartCheckBox pads 4dp per side internally, so 32 yields the same 24dp
                    // glyph the neighbouring icons draw at — 26 left an 18dp heart that read
                    // as extra padding around a smaller icon.
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        HeartCheckBox(checked = controllerState.isLiked, size = 32) {
                            sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                        }
                    }
                    IconButton(
                        onClick = {
                            showQueueBottomSheet = true
                        },
                    ) {
                        Icon(
                            imageVector = SimpIcons.QueueMusic,
                            tint = textColor,
                            contentDescription = "",
                        )
                    }
                    // Desktop mini player button (JVM only)
                    if (getPlatform() == Platform.Desktop) {
                        IconButton(onClick = { toggleMiniPlayer() }) {
                            Icon(
                                imageVector = SimpIcons.PictureInPictureAlt,
                                tint = textColor,
                                contentDescription = "Mini Player",
                            )
                        }
                    }
                    var isVolumeSliding by rememberSaveable {
                        mutableStateOf(false)
                    }
                    var volumeValue by rememberSaveable {
                        mutableFloatStateOf(0f)
                    }
                    LaunchedEffect(key1 = controllerState, key2 = isVolumeSliding) {
                        if (!isVolumeSliding) {
                            volumeValue = controllerState.volume
                        }
                    }
                    // Remembers the level to come back to when unmuting, so the button restores
                    // what the user was listening at instead of jumping to full volume.
                    // Starting muted leaves nothing to restore, so full volume stays the fallback.
                    var previousVolumeValue by rememberSaveable {
                        mutableFloatStateOf(controllerState.volume.takeIf { it > 0f } ?: 1f)
                    }
                    LaunchedEffect(controllerState.volume) {
                        if (controllerState.volume > 0f) {
                            previousVolumeValue = controllerState.volume
                        }
                    }
                    // Vertical volume popup anchored on the speaker icon, the way a context menu
                    // opens: a Popup draws outside the capsule's bounds, so the capsule keeps its
                    // width instead of expanding sideways as it used to. `hoverable` sits on the
                    // anchor Box AND on the popup body, otherwise the popup closes the moment the
                    // pointer leaves the icon and the slider becomes impossible to reach.
                    val volumeInteraction = remember { MutableInteractionSource() }
                    val isVolumeHovered by volumeInteraction.collectIsHoveredAsState()
                    val popupInteraction = remember { MutableInteractionSource() }
                    val isPopupHovered by popupInteraction.collectIsHoveredAsState()
                    Box(modifier = Modifier.hoverable(volumeInteraction)) {
                        IconButton(
                            onClick = {
                                // Toggle mute/unmute
                                if (controllerState.volume > 0f) {
                                    sharedViewModel.onUIEvent(UIEvent.UpdateVolume(0f))
                                } else {
                                    sharedViewModel.onUIEvent(
                                        UIEvent.UpdateVolume(previousVolumeValue.coerceIn(0.1f, 1f)),
                                    )
                                }
                            },
                        ) {
                            Icon(
                                imageVector =
                                    if (controllerState.volume > 0f) {
                                        SimpIcons.VolumeUp
                                    } else {
                                        SimpIcons.VolumeOff
                                    },
                                tint = textColor,
                                contentDescription = if (controllerState.volume > 0f) "Mute" else "Unmute",
                            )
                        }
                        // Releasing the mouse above the popup — which is what happens when you drag
                        // the thumb to the top — drops the hover and `isVolumeSliding` in the same
                        // frame, so the popup used to vanish right under the user's hand. Hold it
                        // open for a beat instead: moving back in cancels this effect before the
                        // delay elapses, so the popup stays.
                        var isVolumePopupVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(isVolumeHovered, isPopupHovered, isVolumeSliding) {
                            if (isVolumeHovered || isPopupHovered || isVolumeSliding) {
                                isVolumePopupVisible = true
                            } else {
                                delay(400)
                                isVolumePopupVisible = false
                            }
                        }
                        if (isVolumePopupVisible) {
                            Popup(
                                alignment = Alignment.TopCenter,
                                offset = IntOffset(0, with(density) { -(VOLUME_POPUP_HEIGHT + 4.dp).roundToPx() }),
                            ) {
                                Column(
                                    modifier =
                                        Modifier
                                            .hoverable(popupInteraction)
                                            .width(44.dp)
                                            .height(VOLUME_POPUP_HEIGHT)
                                            .clip(RoundedCornerShape(14.dp))
                                            // Theme surface, not `background` — that one animates to the
                                            // artwork's palette colour, which turned the popup olive green
                                            // for one cover and pink for the next.
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f))
                                            .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = (volumeValue * 100).roundToInt().toString(),
                                        style = typo().bodySmall,
                                        color = textColor.copy(alpha = 0.7f),
                                        maxLines = 1,
                                    )
                                    // Weighted box: whatever height is left after the label and the icon, the slider
                                    // centres inside it. A fixed-height popup used to overflow — 96dp of slider plus
                                    // label, icon, spacing and padding came to 164dp in a 148dp popup, so the icon
                                    // was swallowed and the track sat glued to the bottom edge.
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                            // A vertical Slider is a horizontal one rotated a quarter
                                            // turn: graphicsLayer rotates the drawing AND the pointer
                                            // input, so drag direction follows the visual.
                                            Slider(
                                                value = volumeValue,
                                                onValueChangeFinished = {
                                                    isVolumeSliding = false
                                                    sharedViewModel.onUIEvent(
                                                        UIEvent.UpdateVolume(volumeValue.coerceIn(0f, 1f)),
                                                    )
                                                },
                                                onValueChange = {
                                                    isVolumeSliding = true
                                                    volumeValue = it
                                                },
                                                valueRange = 0f..1f,
                                                modifier =
                                                    Modifier
                                                        .graphicsLayer {
                                                            // transformOrigin(0, 0) and place(-width, 0) below are
                                                            // a pair — they only work together. Dropping the origin
                                                            // and centring the rotation leaves the slider drawn
                                                            // outside its own node, where the popup's clip() eats
                                                            // it and nothing shows at all.
                                                            rotationZ = 270f
                                                            transformOrigin = TransformOrigin(0f, 0f)
                                                        }.layout { measurable, constraints ->
                                                            val placeable =
                                                                measurable.measure(
                                                                    Constraints(
                                                                        minWidth = constraints.minHeight,
                                                                        maxWidth = constraints.maxHeight,
                                                                        minHeight = constraints.minWidth,
                                                                        maxHeight = constraints.maxWidth,
                                                                    ),
                                                                )
                                                            layout(placeable.height, placeable.width) {
                                                                placeable.place(-placeable.width, 0)
                                                            }
                                                        }.width(VOLUME_SLIDER_LENGTH),
                                                track = { sliderState ->
                                                    SliderDefaults.Track(
                                                        modifier =
                                                            Modifier
                                                                .height(4.dp),
                                                        enabled = true,
                                                        sliderState = sliderState,
                                                        colors =
                                                            SliderDefaults.colors().copy(
                                                                thumbColor = textColor,
                                                                activeTrackColor = textColor,
                                                                inactiveTrackColor = textColor.copy(alpha = 0.3f),
                                                            ),
                                                        thumbTrackGapSize = 0.dp,
                                                        drawTick = { _, _ -> },
                                                        drawStopIndicator = null,
                                                    )
                                                },
                                                thumb = {
                                                    // No thumb: it never sat visually centred on the rotated track, and the
                                                    // slider drags the same without one — the active/inactive split marks the level.
                                                    Spacer(Modifier.size(0.dp))
                                                },
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector =
                                            if (controllerState.volume > 0f) {
                                                SimpIcons.VolumeUp
                                            } else {
                                                SimpIcons.VolumeOff
                                            },
                                        tint = textColor.copy(alpha = 0.7f),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = { onClose() }) {
                        Icon(SimpIcons.Close, "", tint = textColor)
                    }
                }
            }
        }
    }
}

private val VOLUME_POPUP_HEIGHT = 180.dp
private val VOLUME_SLIDER_LENGTH = 96.dp

/**
 * The capsule player's progress bar: a real [Slider] so it can be dragged, drawn over a
 * buffered-position indicator. [trackHeight] and [thumbSize] are what make it read as a
 * hairline at rest and as a scrubber on hover — pass `thumbSize = 0.dp` to hide the thumb
 * without losing the drag target, which stays the full 16dp row height either way.
 */
@Composable
private fun CapsuleProgress(
    sliderValue: Float,
    loading: Boolean,
    trackHeight: Dp,
    thumbSize: Dp,
    textColor: Color,
    progressColor: Color,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            // Buffering turns the track into an indeterminate sweep, the same language NowPlaying's
            // scrubber uses. It REPLACES the slider rather than sitting under it: a
            // LinearProgressIndicator underneath was tried here once and removed, because the Slider
            // reserves room for its thumb at both ends while the indicator runs edge to edge, and
            // the two tracks were visibly different lengths. Only one is ever on screen.
            //
            // Nothing here is gated on the platform — CapsuleProgress is only ever built by the
            // Desktop arm of MiniPlayer. The Android arm swaps its play/pause button for a spinner
            // instead, and reads the same `loading` flag to do it.
            Crossfade(targetState = loading, label = "capsuleProgress") { isLoading ->
                // Both branches are boxed to the full height and centred, so the Crossfade's own
                // Box never changes size. Its default alignment is TopStart and it measures to the
                // tallest child currently visible — with a 2dp indicator against a Slider that is
                // taller, the bar pins to the top of the box mid-transition and then drops to the
                // middle when the Slider leaves composition. It reads as the track falling in.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(trackHeight)
                                    .clip(RoundedCornerShape(8.dp)),
                            // The capsule's own colours, not NowPlaying's hardcoded greys: this track
                            // sits on liquid glass whose tone follows the artwork behind it.
                            color = progressColor,
                            trackColor = textColor.copy(alpha = 0.25f),
                            strokeCap = StrokeCap.Round,
                        )
                    } else {
                        Slider(
                            // Fraction, not 0..100 — see the note in NowPlayingScreen: material3 alpha25
                            // drops valueRange on its binary-compatibility overload.
                            value = sliderValue / 100f,
                            onValueChange = onValueChange,
                            onValueChangeFinished = onValueChangeFinished,
                            modifier = Modifier.fillMaxWidth(),
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    modifier = Modifier.height(trackHeight),
                                    enabled = true,
                                    sliderState = sliderState,
                                    colors =
                                        SliderDefaults.colors().copy(
                                            thumbColor = progressColor,
                                            activeTrackColor = progressColor,
                                            inactiveTrackColor = textColor.copy(alpha = 0.25f),
                                        ),
                                    thumbTrackGapSize = 0.dp,
                                    drawTick = { _, _ -> },
                                    drawStopIndicator = null,
                                )
                            },
                            thumb = {
                                if (thumbSize > 0.dp) {
                                    SliderDefaults.Thumb(
                                        modifier = Modifier.size(thumbSize),
                                        thumbSize = DpSize(thumbSize, thumbSize),
                                        interactionSource = remember { MutableInteractionSource() },
                                        colors =
                                            SliderDefaults.colors().copy(
                                                thumbColor = progressColor,
                                                activeTrackColor = progressColor,
                                                inactiveTrackColor = textColor.copy(alpha = 0.25f),
                                            ),
                                        enabled = true,
                                    )
                                } else {
                                    Spacer(Modifier.size(0.dp))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}