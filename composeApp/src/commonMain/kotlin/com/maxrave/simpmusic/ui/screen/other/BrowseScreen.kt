package com.maxrave.simpmusic.ui.screen.other

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.utils.toSongEntity
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AmbientGlowHeight
import com.maxrave.simpmusic.ui.component.CenterLoadingBox
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.HomeContentCard
import com.maxrave.simpmusic.ui.component.MoodMomentAndGenreHomeItem
import com.maxrave.simpmusic.ui.component.NowPlayingBottomSheet
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.component.homeCardAspectRatio
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.home.MoodDestination
import com.maxrave.simpmusic.ui.theme.desktopPanelDark
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.BrowseUIState
import com.maxrave.simpmusic.viewModel.BrowseViewModel
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import org.koin.compose.viewmodel.koinViewModel

private val RowGap = 10.dp

// The thumbnail height a justified row aims for; rows land a little above or below it.
private val TargetThumbnailHeight = 180.dp

// The home cards' own 10dp start + 10dp end padding, outside the thumbnail.
private val CardHorizontalPadding = 20.dp

// The width a mood button asks for, as the old grid's cells did.
private val MoodCellWidth = 170.dp

/**
 * The page a home section's "More" endpoint opens when YouTube names no album, playlist, artist or
 * podcast behind it. Every shelf is flattened into justified rows — each card keeps its own shape
 * (16:9 video, square otherwise), every thumbnail in a row is the same height, and the number of
 * cards per row follows the shapes in it — drawn with Home's own cards, so an item opens and plays
 * exactly as it does on Home. The glow and the bar are Mix for you's.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun BrowseScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    browseId: String,
    params: String?,
    title: String?,
    viewModel: BrowseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState(blurEnabled = true)
    val listState = rememberLazyListState()
    // Mix for you's bar rule: transparent only while pixel 0 is on screen.
    val isAtTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
    }
    var track by remember { mutableStateOf<Track?>(null) }
    var bottomSheetShow by remember { mutableStateOf(false) }

    // Mix for you's ambient top glow, tinted from this page's first item instead of its first mix.
    // The client is remembered, as WrappedScreen does, rather than built on every recomposition.
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f
    val pageBackground =
        if (getPlatform() == Platform.Desktop) {
            if (isLightTheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        } else {
            backgroundColor
        }
    var topHeaderColor by remember { mutableStateOf(backgroundColor) }
    val animatedColor by animateColorAsState(topHeaderColor, tween(500))
    val httpClient = remember { HttpClient(CIO) }
    val networkLoader = rememberNetworkLoader(httpClient)
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = backgroundColor,
            defaultOnColor = backgroundColor,
            loader = networkLoader,
        )
    val firstThumbnail =
        (uiState as? BrowseUIState.Success)
            ?.page
            ?.contents
            ?.firstOrNull()
            ?.thumbnails
            ?.lastOrNull()
            ?.url
    LaunchedEffect(firstThumbnail) {
        firstThumbnail?.let { dominantColorState.updateFrom(Url(it)) }
    }
    LaunchedEffect(dominantColorState, isLightTheme) {
        snapshotFlow { dominantColorState.color }.collect {
            topHeaderColor = if (isLightTheme) lerp(it, Color.White, 0.85f) else it.rgbFactor(0.3f)
        }
    }

    LaunchedEffect(browseId, params) {
        viewModel.getBrowsePage(browseId, params)
    }

    if (bottomSheetShow) {
        NowPlayingBottomSheet(
            onDismiss = { bottomSheetShow = false },
            song = track?.toSongEntity(),
            navController = navController,
        )
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
        ) {
            // Drawn before the list and scrolled away with it, as on Mix for you: the draw-phase
            // translation tracks the first row and parks once it has passed.
            Box(
                Modifier
                    .graphicsLayer {
                        translationY =
                            if (listState.firstVisibleItemIndex == 0) {
                                -listState.firstVisibleItemScrollOffset.toFloat()
                            } else {
                                -size.height
                            }
                    }.fillMaxWidth()
                    .height(AmbientGlowHeight)
                    .angledGradientBackground(listOf(animatedColor, pageBackground), 25f),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(artworkScrimBrush(pageBackground)),
                )
            }
            Crossfade(targetState = uiState) { state ->
                when (state) {
                    is BrowseUIState.Success -> {
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            val cardRows =
                                remember(state.page.contents, maxWidth) {
                                    justifyRows(state.page.contents, maxWidth)
                                }
                            val moodColumns = maxOf(1, ((maxWidth + RowGap) / (MoodCellWidth + RowGap)).toInt())
                            val moodRows =
                                remember(state.page.moods, moodColumns) {
                                    state.page.moods.chunked(moodColumns)
                                }
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                // Padding rather than a spacer item, so the first item is a real row and
                                // the glow hands off once a row has scrolled by, as on Mix for you.
                                contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 64.dp),
                                verticalArrangement = Arrangement.spacedBy(RowGap),
                            ) {
                                items(moodRows) { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(RowGap),
                                    ) {
                                        row.forEach { mood ->
                                            Box(Modifier.weight(1f)) {
                                                MoodMomentAndGenreHomeItem(
                                                    title = mood.title,
                                                    stripeColor = mood.stripeColor,
                                                    fillMaxWidth = true,
                                                ) {
                                                    navController.navigate(MoodDestination(mood.params))
                                                }
                                            }
                                        }
                                        repeat(moodColumns - row.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                                items(cardRows) { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(RowGap)) {
                                        row.forEach { card ->
                                            Box(Modifier.width(card.width)) {
                                                HomeContentCard(
                                                    temp = card.content,
                                                    navController = navController,
                                                    viewModel = viewModel,
                                                    onLongClick = {
                                                        track = it.toTrack()
                                                        bottomSheetShow = true
                                                    },
                                                    fillMaxWidth = true,
                                                )
                                            }
                                        }
                                    }
                                }
                                item {
                                    EndOfPage()
                                }
                            }
                        }
                    }

                    is BrowseUIState.Error -> {
                        LaunchedEffect(state) {
                            viewModel.makeToast(state.message)
                        }
                    }

                    BrowseUIState.Loading -> {
                        CenterLoadingBox(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(15.dp),
                        )
                    }
                }
            }
        }
        // Transparent while the list sits at pixel zero, frosted the moment it scrolls — Mix for
        // you's crossfade and frost recipe.
        AnimatedContent(
            targetState = isAtTop,
            transitionSpec = {
                fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
            },
        ) { atTop ->
            Column(
                Modifier.then(
                    if (atTop) {
                        Modifier.background(Color.Transparent)
                    } else {
                        Modifier.hazeEffect(hazeState) {
                            blurEnabled = true
                            blurRadius = 24.dp
                            // `this.` is load-bearing: the local `val backgroundColor` above would
                            // otherwise win over the haze scope's member.
                            this.backgroundColor = pageBackground
                            tints = listOf(HazeTint(pageBackground.copy(alpha = 0.3f)))
                        }
                    },
                ),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = (uiState as? BrowseUIState.Success)?.page?.title ?: title.orEmpty(),
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
                )
            }
        }
    }
}

private data class JustifiedCard(
    val content: Content,
    val width: Dp,
)

/**
 * Packs [contents] into rows that fill [rowWidth] exactly, each card keeping its own thumbnail shape,
 * so every thumbnail in a row is the same height and the number of cards per row follows the shapes
 * in it. A row closes when the next card would take its height below [TargetThumbnailHeight] —
 * with or without that card, whichever lands closer. The last row keeps the target height instead
 * of stretching to fill.
 */
// ponytail: greedy one-pass line breaking; a globally balanced break (Knuth–Plass style) only if
// uneven row heights ever show.
private fun justifyRows(
    contents: List<Content>,
    rowWidth: Dp,
): List<List<JustifiedCard>> {
    val target = TargetThumbnailHeight.value
    val rows = mutableListOf<List<JustifiedCard>>()
    var current = emptyList<Pair<Content, Float>>()

    // The thumbnail height at which these cards exactly fill the row.
    fun heightOf(row: List<Pair<Content, Float>>): Float {
        val fixed = CardHorizontalPadding.value * row.size + RowGap.value * (row.size - 1)
        return ((rowWidth.value - fixed) / row.sumOf { it.second.toDouble() }.toFloat()).coerceAtLeast(1f)
    }

    fun close(
        row: List<Pair<Content, Float>>,
        height: Float,
    ) {
        rows.add(row.map { (content, ratio) -> JustifiedCard(content, (CardHorizontalPadding.value + ratio * height).dp) })
    }

    for (content in contents) {
        val candidate = current + (content to content.homeCardAspectRatio())
        val height = heightOf(candidate)
        when {
            height > target -> {
                current = candidate
            }

            current.isNotEmpty() && heightOf(current) - target < target - height -> {
                close(current, heightOf(current))
                current = listOf(candidate.last())
            }

            else -> {
                close(candidate, height)
                current = emptyList()
            }
        }
    }
    if (current.isNotEmpty()) close(current, minOf(target, heightOf(current)))
    return rows
}
