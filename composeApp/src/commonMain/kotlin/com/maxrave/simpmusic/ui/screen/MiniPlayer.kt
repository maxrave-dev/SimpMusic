package com.maxrave.simpmusic.ui.screen

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.holder
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

private const val TAG = "MiniPlayer"

val itunesMiniPlayerClient = HttpClient(CIO)

suspend fun fetchITunesCoverForMiniPlayer(title: String, artist: String): String? = withContext(Dispatchers.IO) {
    try {
        var clean = title
        val bracketsRegex = Regex("(?i)[\\(\\[].*?(official|video|audio|lyric|live|remastered|version|edit|mix|cover)[\\)\\]]")
        clean = clean.replace(bracketsRegex, "")
        val ftRegex = Regex("(?i)(ft\\.|feat\\.|featuring).*$")
        clean = clean.replace(ftRegex, "").trim()
        val query = "$clean $artist".replace(" ", "+").replace("&", "%26")
        val url = "https://itunes.apple.com/search?term=$query&entity=song&limit=1"
        val response = itunesMiniPlayerClient.get(url).bodyAsText()
        val regex = Regex(""""artworkUrl100":"([^"]+)"""")
        val match = regex.find(response)
        if (match != null) {
            return@withContext match.groupValues[1].replace("100x100bb", "500x500bb")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext null
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    modifier: Modifier,
    backdrop: PlatformBackdrop,
    sharedViewModel: SharedViewModel = koinInject(),
    onClose: () -> Unit,
    onClick: () -> Unit,
) {
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()

    val defaultBgColor = MaterialTheme.colorScheme.surfaceVariant
    val background = remember {
        Animatable(defaultBgColor)
    }

    val isLightBg = background.value.luminance() > 0.5f
    val targetTextColor = if (isLightBg) Color.Black else Color.White
    val textShadow = if (!isLightBg) Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 4f) else null

    val textColor by animateColorAsState(
        targetValue = targetTextColor,
        label = "MiniPlayerTextColor",
        animationSpec = tween(500),
    )

    val (songEntity, setSongEntity) = remember {
        mutableStateOf<SongEntity?>(null)
    }
    val (liked, setLiked) = remember {
        mutableStateOf(false)
    }
    val (isPlaying, setIsPlaying) = remember {
        mutableStateOf(false)
    }
    val (progress, setProgress) = remember {
        mutableFloatStateOf(0f)
    }
    val (isCrossfading, setIsCrossfading) = remember {
        mutableStateOf(false)
    }

    var dynamicThumbnailUrl by remember { mutableStateOf<String?>(null) }
    var currentSongId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(songEntity?.videoId) {
        if (songEntity?.videoId != null && songEntity.videoId != currentSongId) {
            currentSongId = songEntity.videoId
            val title = songEntity.title
            val artist = songEntity.artistName?.connectArtists() ?: ""
            val itunesCover = fetchITunesCoverForMiniPlayer(title, artist)
            dynamicThumbnailUrl = itunesCover ?: songEntity.thumbnails
        } else if (songEntity == null) {
            dynamicThumbnailUrl = null
            currentSongId = null
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "",
    )

    val paletteState = rememberPaletteState()

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
                val color = it.getColorFromPalette()
                if (color != Color.Transparent) {
                    background.animateTo(color)
                } else {
                    background.animateTo(defaultBgColor)
                }
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
        Card(
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = background.value,
                    disabledContainerColor = background.value,
                ),
            modifier =
                modifier
                    .clipToBounds()
                    .offset { IntOffset(0, offsetY.value.roundToInt()) },
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                },
                                onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                                    if (offsetY.value + dragAmount > 0) {
                                        coroutineScope.launch {
                                            change.consume()
                                            offsetY.animateTo(offsetY.value + 2 * dragAmount)
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        offsetY.animateTo(0f)
                                    }
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (offsetY.value > 70) {
                                            onClose()
                                        }
                                        offsetY.animateTo(0f)
                                    }
                                },
                            )
                        },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxSize(),
                ) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Box(
                        modifier =
                            Modifier
                                .weight(1F)
                                .fillMaxHeight()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = onClick,
                                ),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
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
                                                }
                                            },
                                            onDragCancel = {
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
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model =
                                    ImageRequest
                                        .Builder(LocalPlatformContext.current)
                                        .data(dynamicThumbnailUrl)
                                        .crossfade(550)
                                        .build(),
                                placeholder = painterResource(Res.drawable.holder),
                                error = painterResource(Res.drawable.holder),
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
                                    if (targetState != initialState) {
                                        (
                                            slideInHorizontally { width ->
                                                width
                                            } + fadeIn()
                                            ).togetherWith(
                                                slideOutHorizontally { width -> +width } + fadeOut(),
                                            )
                                    } else {
                                        (
                                            slideInHorizontally { width ->
                                                +width
                                            } + fadeIn()
                                            ).togetherWith(
                                                slideOutHorizontally { width -> width } + fadeOut(),
                                            )
                                    }.using(
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
                                            style = typo().labelSmall.copy(shadow = textShadow),
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
                                        val lyricLine by sharedViewModel.currentLyricLine.collectAsStateWithLifecycle()
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
                                                text = lyricLine ?: (songEntity?.artistName?.connectArtists() ?: ""),
                                                style = typo().bodySmall.copy(shadow = textShadow),
                                                maxLines = 1,
                                                color = textColor.copy(alpha = 0.8f),
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
                    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides textColor) {
                        HeartCheckBox(checked = liked, size = 30) {
                            sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                        }
                    }
                    Spacer(modifier = Modifier.width(15.dp))
                    Crossfade(targetState = loading, label = "") {
                        if (it) {
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = textColor.copy(alpha = 0.7f),
                                    strokeWidth = 3.dp,
                                )
                            }
                        } else {
                            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides textColor) {
                                PlayPauseButton(isPlaying = isPlaying, modifier = Modifier.size(48.dp)) {
                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                }
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
                                .height(2.dp)
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
        Box(
            modifier.then(
                Modifier.clickable {
                    onClick()
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f).padding(vertical = 16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(dynamicThumbnailUrl)
                                    .crossfade(550)
                                    .build(),
                            placeholder = painterResource(Res.drawable.holder),
                            error = painterResource(Res.drawable.holder),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            onSuccess = {
                                bitmap =
                                    it.result.image.toImageBitmap()
                            },
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .align(Alignment.CenterVertically)
                                    .clip(
                                        RoundedCornerShape(4.dp),
                                    ),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = (songEntity?.title ?: "").toString(),
                                style = typo().labelSmall.copy(shadow = textShadow),
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
                                    style = typo().bodySmall.copy(shadow = textShadow),
                                    color = textColor.copy(alpha = 0.8f),
                                    maxLines = 1,
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
                Box(modifier = Modifier.weight(1f)) {
                    Column(Modifier.width(600.dp).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides textColor) {
                                PlayerControlLayout(
                                    controllerState,
                                    isSmallSize = true,
                                ) {
                                    sharedViewModel.onUIEvent(it)
                                }
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatDuration((timelineState.total * (sliderValue / 100f)).roundToLong()),
                                style = typo().bodyMedium.copy(shadow = textShadow),
                                color = textColor,
                                textAlign = TextAlign.Left,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(50.dp),
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
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
                                                    color = textColor.copy(alpha = 0.5f),
                                                    trackColor = textColor.copy(alpha = 0.2f),
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
                                                    color = textColor.copy(alpha = 0.5f),
                                                    trackColor = textColor.copy(alpha = 0.2f),
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
                                                        thumbColor = textColor,
                                                        activeTrackColor = textColor,
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
                                                        thumbColor = textColor,
                                                        activeTrackColor = textColor,
                                                        inactiveTrackColor = Color.Transparent,
                                                    ),
                                                enabled = true,
                                            )
                                        },
                                    )
                                }
                            }
                            Text(
                                text = formatDuration(timelineState.total),
                                style = typo().bodyMedium.copy(shadow = textShadow),
                                color = textColor,
                                textAlign = TextAlign.Right,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(50.dp),
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxHeight().align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
                        CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides textColor) {
                            HeartCheckBox(checked = controllerState.isLiked, size = 30) {
                                sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        if (getPlatform() == Platform.Desktop) {
                            IconButton(onClick = { toggleMiniPlayer() }) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = "Mini Player",
                                    tint = textColor
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val newVolume = if (controllerState.volume > 0f) 0f else 1f
                                sharedViewModel.onUIEvent(UIEvent.UpdateVolume(newVolume))
                            },
                        ) {
                            Icon(
                                imageVector =
                                    if (controllerState.volume > 0f) {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    },
                                contentDescription = if (controllerState.volume > 0f) "Mute" else "Unmute",
                                tint = textColor
                            )
                        }
                        Spacer(Modifier.width(2.dp))
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
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
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
                                        .padding(top = 3.dp)
                                        .width(64.dp),
                                track = { sliderState ->
                                    SliderDefaults.Track(
                                        modifier =
                                            Modifier
                                                .height(5.dp),
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
                                                thumbColor = textColor,
                                                activeTrackColor = textColor,
                                                inactiveTrackColor = textColor.copy(alpha = 0.3f),
                                            ),
                                        enabled = true,
                                    )
                                },
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { onClose() }) {
                            Icon(Icons.Rounded.Close, "", tint = textColor)
                        }
                    }
                }
            }
        }
    }
}