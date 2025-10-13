package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
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
import com.kmpalette.rememberPaletteState
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.getColorFromPalette
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.DescriptionView
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HeartCheckBox
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.PodcastEpisodeFullWidthItem
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.md_theme_dark_background
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.PodcastUIEvent
import com.maxrave.simpmusic.viewModel.PodcastUIState
import com.maxrave.simpmusic.viewModel.PodcastViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.album_length
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24
import simpmusic.composeapp.generated.resources.baseline_play_circle_24
import simpmusic.composeapp.generated.resources.baseline_share_24
import simpmusic.composeapp.generated.resources.baseline_shuffle_24
import simpmusic.composeapp.generated.resources.holder
import simpmusic.composeapp.generated.resources.no_description
import simpmusic.composeapp.generated.resources.podcasts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    viewModel: PodcastViewModel = koinViewModel(),
    podcastId: String,
    navController: NavController,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }

    var currentTrack by rememberSaveable {
        mutableStateOf<Track?>(null)
    }
    var shouldShowMoreBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }

    // Theo dõi gradient cho background
    var gradientColors by remember { mutableStateOf(listOf(md_theme_dark_background, md_theme_dark_background)) }

    val paletteState = rememberPaletteState()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

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
                gradientColors = listOf(it.getColorFromPalette(), md_theme_dark_background)
            }
    }

    LaunchedEffect(key1 = podcastId) {
        if ((uiState as? PodcastUIState.Success)?.id == podcastId) {
            return@LaunchedEffect
        }
        viewModel.getPodcastBrowse(podcastId)
    }

    Crossfade(targetState = uiState) { state ->
        when (state) {
            is PodcastUIState.Success -> {
                val data = state.data
                val id = state.id
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(Color.Black),
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
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(260.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .angledGradientBackground(gradientColors, 25f),
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
                                Modifier.background(Color.Transparent),
                            ) {
                                Row(
                                    modifier =
                                        Modifier
                                            .wrapContentWidth()
                                            .padding(16.dp)
                                            .windowInsetsPadding(WindowInsets.statusBars),
                                ) {
                                    RippleIconButton(
                                        resId = Res.drawable.baseline_arrow_back_ios_new_24,
                                    ) {
                                        navController.navigateUp()
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(LocalPlatformContext.current)
                                                .data(data.thumbnail.lastOrNull()?.url)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(data.thumbnail.lastOrNull()?.url)
                                                .crossfade(true)
                                                .build(),
                                        placeholder = painterResource(Res.drawable.holder),
                                        error = painterResource(Res.drawable.holder),
                                        contentDescription = null,
                                        contentScale = ContentScale.FillHeight,
                                        onSuccess = {
                                            bitmap =
                                                it.result.image
                                                    .toBitmap()
                                                    .asImageBitmap()
                                        },
                                        modifier =
                                            Modifier
                                                .height(250.dp)
                                                .wrapContentWidth()
                                                .align(Alignment.CenterHorizontally)
                                                .clip(RoundedCornerShape(8.dp)),
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
                                                text = data.title,
                                                style = typo().titleLarge,
                                                color = Color.White,
                                                maxLines = 2,
                                            )
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    AsyncImage(
                                                        model =
                                                            ImageRequest
                                                                .Builder(LocalPlatformContext.current)
                                                                .data(data.authorThumbnail)
                                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                                .diskCacheKey(data.authorThumbnail)
                                                                .crossfade(true)
                                                                .build(),
                                                        placeholder = painterResource(Res.drawable.holder),
                                                        error = painterResource(Res.drawable.holder),
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier
                                                                .size(25.dp)
                                                                .clip(
                                                                    CircleShape,
                                                                ),
                                                    )
                                                    Spacer(modifier = Modifier.size(8.dp))
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
                                                                val authorId = data.author.id
                                                                if (authorId.isNullOrEmpty().not()) {
                                                                    navController.navigate(
                                                                        ArtistDestination(
                                                                            authorId,
                                                                        ),
                                                                    )
                                                                }
                                                            },
                                                        ) {
                                                            Text(
                                                                text = data.author.name,
                                                                style = typo().labelSmall,
                                                                color = Color.White,
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.size(8.dp))
                                                Text(
                                                    text = stringResource(Res.string.podcasts),
                                                    style = typo().bodyMedium,
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                // Play button
                                                RippleIconButton(
                                                    resId = Res.drawable.baseline_play_circle_24,
                                                    fillMaxSize = true,
                                                    modifier = Modifier.size(36.dp),
                                                ) {
                                                    viewModel.onUIEvent(PodcastUIEvent.PlayAll(id))
                                                }

                                                // Favorite
                                                HeartCheckBox(
                                                    size = 32,
                                                    checked = isFavorite,
                                                    onStateChange = {
                                                        viewModel.onUIEvent(
                                                            PodcastUIEvent.ToggleFavorite(
                                                                id,
                                                                !isFavorite,
                                                            ),
                                                        )
                                                    },
                                                )

                                                Spacer(Modifier.weight(1f))

                                                // Shuffle
                                                RippleIconButton(
                                                    modifier = Modifier.size(36.dp),
                                                    resId = Res.drawable.baseline_shuffle_24,
                                                    fillMaxSize = true,
                                                ) {
                                                    viewModel.onUIEvent(PodcastUIEvent.Shuffle(id))
                                                }

                                                Spacer(Modifier.size(5.dp))

                                                // More options
                                                RippleIconButton(
                                                    modifier = Modifier.size(36.dp),
                                                    resId = Res.drawable.baseline_share_24,
                                                    fillMaxSize = true,
                                                ) {
                                                    viewModel.onUIEvent(PodcastUIEvent.Share(id))
                                                }
                                            }

                                            // Description
                                            val uriHandler = LocalUriHandler.current
                                            DescriptionView(
                                                modifier = Modifier.padding(top = 8.dp),
                                                text = data.description ?: stringResource(Res.string.no_description),
                                                limitLine = 3,
                                                onTimeClicked = {},
                                                onURLClicked = { url ->
                                                    uriHandler.openUri(url)
                                                },
                                            )

                                            Text(
                                                text =
                                                    stringResource(
                                                        Res.string.album_length,
                                                        data.listEpisode.size.toString(),
                                                        "",
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

                    // Podcast episodes
                    items(count = data.listEpisode.size, key = { index ->
                        val item = data.listEpisode.getOrNull(index)
                        (item?.videoId ?: "") + "item_$index"
                    }) { index ->
                        val episode = data.listEpisode.getOrNull(index)
                        if (episode != null) {
                            PodcastEpisodeFullWidthItem(
                                episode = episode,
                                onClick = {
                                    viewModel.onUIEvent(
                                        PodcastUIEvent.EpisodeClick(
                                            episode.videoId,
                                            id,
                                        ),
                                    )
                                },
                                onMoreClickListener = {
                                    currentTrack = episode.toTrack()
                                    shouldShowMoreBottomSheet = true
                                },
                            )
                        }
                    }

                    item {
                        EndOfPage()
                    }
                }
                // Animated TopAppBar when scrolling
                AnimatedVisibility(
                    visible = shouldHideTopBar,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = data.title,
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
                                    Modifier.size(32.dp),
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
                        modifier = Modifier.angledGradientBackground(gradientColors, 90f),
                    )
                }

                if (shouldShowMoreBottomSheet) {
                    val song = currentTrack?.toSongEntity() ?: return@Crossfade
                    NowPlayingBottomSheet(
                        onDismiss = { shouldShowMoreBottomSheet = false },
                        navController = navController,
                        song = song,
                    )
                }
            }

            is PodcastUIState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CenterLoadingBox(
                        modifier =
                            Modifier
                                .fillMaxSize(),
                    )
                }
            }

            is PodcastUIState.Error -> {
                viewModel.makeToast("Error: ${state.message}")
                navController.navigateUp()
            }
        }
    }
}