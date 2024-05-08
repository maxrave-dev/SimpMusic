package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.wear.compose.material3.ripple
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.PlaylistItems
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.SuggestItems
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.palette.PalettePlugin
import com.skydoves.landscapist.palette.rememberPaletteState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import java.time.format.DateTimeFormatter

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class,
)
@Composable
fun PlaylistScreen(
    id: Long,
    sharedViewModel: SharedViewModel,
    viewModel: LocalPlaylistViewModel,
    navController: NavController,
) {
    val context = LocalContext.current

    val offset by viewModel.offset.collectAsState()
    val isLoadingMore by viewModel.loadingMore.collectAsState(initial = false)
    val filterState by viewModel.filter.collectAsState()

    val aiPainter = painterResource(id = R.drawable.baseline_tips_and_updates_24)
    val limit = 1.5f
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progressAnimated by transition.animateFloat(
        initialValue = -limit,
        targetValue = limit,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "shimmer",
    )
    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    val lastItemVisible by remember {
        derivedStateOf {
            lazyState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == lazyState.layoutInfo.totalItemsCount - 1
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestions by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestButton by rememberSaveable { mutableStateOf(false) }
    val hazeState = remember { HazeState() }
    var palette by rememberPaletteState(null)
    val bg by viewModel.brush.collectAsState()
    val localPlaylist by viewModel.localPlaylist.collectAsState()
    val listTrack by viewModel.listTrack.collectAsState()
    val playingTrack by sharedViewModel.nowPlayingMediaItem.collectAsState(initial = null)
    val suggestedTracks by viewModel.listSuggestions.collectAsState()
    LaunchedEffect(key1 = shouldShowSuggestions) {
        localPlaylist?.youtubePlaylistId?.let { viewModel.getSuggestions(it) }
    }
    LaunchedEffect(key1 = id) {
        viewModel.id.postValue(id)
        viewModel.setOffset(0)
        viewModel.clearLocalPlaylist()
        viewModel.clearListPair()
        viewModel.id.postValue(id)
        viewModel.getLocalPlaylist(id)
    }
    LaunchedEffect(key1 = offset) {
        if (offset >= 1) {
            localPlaylist?.id?.let { viewModel.getListTrack(it, offset, filterState) }
        }
    }
    LaunchedEffect(key1 = localPlaylist) {
        if (localPlaylist != null) {
            localPlaylist?.id?.let { viewModel.getListTrack(it, offset, filterState) }
            shouldShowSuggestButton =
                localPlaylist?.youtubePlaylistId != null &&
                localPlaylist?.youtubePlaylistId != ""
        }
    }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    LaunchedEffect(key1 = lastItemVisible) {
        if (lastItemVisible && offset > 0 && !isLoadingMore) {
            localPlaylist?.id?.let { viewModel.getListTrack(it, offset, filterState) }
        }
    }
    LaunchedEffect(key1 = palette) {
        val p = palette
        if (p != null) {
            val defaultColor = 0x000000
            var startColor = p.getDarkVibrantColor(defaultColor)
            if (startColor == defaultColor) {
                startColor = p.getDarkMutedColor(defaultColor)
                if (startColor == defaultColor) {
                    startColor = p.getVibrantColor(defaultColor)
                    if (startColor == defaultColor) {
                        startColor =
                            p.getMutedColor(defaultColor)
                        if (startColor == defaultColor) {
                            startColor =
                                p.getLightVibrantColor(
                                    defaultColor,
                                )
                            if (startColor == defaultColor) {
                                startColor =
                                    p.getLightMutedColor(
                                        defaultColor,
                                    )
                            }
                        }
                    }
                }
            }
            val endColor =
                context.resources.getColor(R.color.md_theme_dark_background, null)
            val colorAndroid = ColorUtils.setAlphaComponent(startColor, 255)

            val brush =
                Brush.verticalGradient(
                    listOf(
                        Color(colorAndroid),
                        Color(endColor),
                    ),
                )
            viewModel.setBrush(brush)
        }
    }
    Box {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
            state = lazyState,
        ) {
            item {
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
                                .fillMaxWidth()
                                .haze(
                                    hazeState,
                                    style = HazeMaterials.thin(),
                                ),
                    ) {
                        CoilImage(
                            imageModel = {
                                localPlaylist?.thumbnail
                            },
                            imageOptions =
                                ImageOptions(
                                    contentScale = ContentScale.FillWidth,
                                    alignment = Alignment.Center,
                                ),
                            previewPlaceholder = painterResource(id = R.drawable.holder),
                            component =
                                rememberImageComponent {
                                    CrossfadePlugin(
                                        duration = 550,
                                    )
                                },
                            modifier =
                                Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth()
                                    .clip(
                                        RoundedCornerShape(8.dp),
                                    ),
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
                            .background(Color.Transparent)
                            .hazeChild(hazeState, style = HazeMaterials.thin()),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .wrapContentWidth()
                                    .padding(16.dp)
                                    .windowInsetsPadding(WindowInsets.statusBars),
                        ) {
                            RippleIconButton(
                                resId = R.drawable.baseline_arrow_back_ios_new_24,
                            ) {
                                navController.popBackStack()
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.Start,
                        ) {
                            CoilImage(
                                imageModel = {
                                    localPlaylist?.thumbnail
                                },
                                imageOptions =
                                    ImageOptions(
                                        contentScale = ContentScale.FillHeight,
                                        alignment = Alignment.Center,
                                    ),
                                previewPlaceholder = painterResource(id = R.drawable.holder),
                                component =
                                    rememberImageComponent {
                                        add(
                                            CrossfadePlugin(
                                                duration = 550,
                                            ),
                                        )
                                        add(
                                            PalettePlugin(
                                                paletteLoadedListener = {
                                                    palette = it
                                                },
                                            ),
                                        )
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
                                        text = localPlaylist?.title ?: "",
                                        style = typo.titleLarge,
                                        color = Color.White,
                                    )
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.your_playlist),
                                            style = typo.bodyLarge,
                                            color = Color.White,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text =
                                                stringResource(
                                                    id = R.string.created_at,
                                                    localPlaylist?.inLibrary?.format(
                                                        DateTimeFormatter.ofPattern(
                                                            "kk:mm - dd MMM uuuu",
                                                        ),
                                                    ) ?: "",
                                                ),
                                            style = typo.bodyLarge,
                                            color = Color(0xC4FFFFFF),
                                        )
                                    }
                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        RippleIconButton(
                                            resId = R.drawable.baseline_play_circle_24,
                                            fillMaxSize = true,
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                        }
                                        Spacer(modifier = Modifier.size(5.dp))
                                        RippleIconButton(
                                            fillMaxSize = true,
                                            resId = R.drawable.download_button,
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Spacer(Modifier.size(5.dp))
                                        AnimatedVisibility(visible = shouldShowSuggestButton) {
                                            Box(
                                                modifier =
                                                    Modifier.size(36.dp)
                                                        .clip(CircleShape)
                                                        .graphicsLayer {
                                                            compositingStrategy =
                                                                CompositingStrategy.Offscreen
                                                        }
                                                        .clickable(
                                                            onClick = {
                                                                shouldShowSuggestions = !shouldShowSuggestions
                                                            },
                                                            interactionSource =
                                                                remember {
                                                                    MutableInteractionSource()
                                                                },
                                                            indication = ripple(),
                                                        )
                                                        .drawWithCache {
                                                            val width = size.width - 10
                                                            val height = size.height - 10

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
                                                                    draw(
                                                                        size = Size(width, width),
                                                                    )
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
                                            resId = R.drawable.baseline_shuffle_24,
                                            fillMaxSize = true,
                                        ) {
                                        }
                                        Spacer(Modifier.size(5.dp))
                                        RippleIconButton(
                                            modifier =
                                                Modifier.size(36.dp),
                                            resId = R.drawable.baseline_more_vert_24,
                                            fillMaxSize = true,
                                        ) {
                                        }
                                    }
                                    // Hide in local playlist
                                    //                                ExpandableText(
                                    //                                    modifier = Modifier.padding(vertical = 8.dp),
                                    //                                    text = stringResource(id = R.string.demo_description),
                                    //                                    fontSize = typo.bodyLarge.fontSize,
                                    //                                    showMoreStyle = SpanStyle(Color.Gray),
                                    //                                    showLessStyle = SpanStyle(Color.Gray),
                                    //                                    style = TextStyle(
                                    //                                        color = Color(0xC4FFFFFF)
                                    //                                    )
                                    //                                )
                                    Text(
                                        text =
                                            stringResource(
                                                id = R.string.album_length,
                                                (localPlaylist?.tracks?.size ?: 0).toString(),
                                                "",
                                            ),
                                        color = Color.White,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                    AnimatedVisibility(visible = shouldShowSuggestions) {
                                        Column {
                                            Spacer(modifier = Modifier.size(8.dp))
                                            Text(
                                                text =
                                                    stringResource(
                                                        id = R.string.suggest,
                                                    ),
                                                color = Color.White,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                            )
                                            Spacer(modifier = Modifier.size(8.dp))
                                            suggestedTracks?.forEach { track ->
                                                SuggestItems(
                                                    track = track,
                                                    isPlaying = playingTrack?.mediaId == track.videoId,
                                                )
                                            }
                                            HorizontalDivider(
                                                color = Color.Gray,
                                                thickness = 0.5.dp,
                                            )
                                            Spacer(modifier = Modifier.size(8.dp))
                                        }
                                    }
                                    //
                                }
                            }
                        }
                    }
                }
            }
            items(listTrack ?: listOf()) { item ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                }
                if (playingTrack?.mediaId == item.videoId) {
                    PlaylistItems(
                        isPlaying = true,
                        songEntity = item,
                    )
                } else {
                    PlaylistItems(
                        isPlaying = false,
                        songEntity = item,
                    )
                }
            }
            item {
                AnimatedVisibility(visible = isLoadingMore) {
                    Spacer(modifier = Modifier.height(15.dp))
                    CenterLoadingBox(modifier = Modifier.size(32.dp).align(Alignment.Center))
                    Spacer(modifier = Modifier.height(15.dp))
                }
            }
            item {
                EndOfPage()
            }
        }
        AnimatedVisibility(
            visible = shouldHideTopBar,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = localPlaylist?.title ?: stringResource(id = R.string.playlist),
                        style = typo.titleMedium,
                    )
                },
                navigationIcon = {
                    RippleIconButton(
                        R.drawable.baseline_arrow_back_ios_new_24,
                        Modifier
                            .size(32.dp),
                        true,
                    ) {
                        navController.popBackStack()
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                modifier = Modifier.background(bg),
            )
        }
    }
}