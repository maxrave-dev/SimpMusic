package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.AmbientGlowHeight
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.theme.desktopPanelDark
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.mix_for_you
import simpmusic.composeapp.generated.resources.no_mixes_found

/**
 * The YouTube "Mix for you" playlists, promoted out of the Library chip row into a tab of its own.
 *
 * It shares [LibraryViewModel] with the Library screen rather than owning a view model, so the
 * mixes stay fetched once and the grid, its pull-to-refresh and its empty state behave exactly as
 * they did while this was a chip.
 *
 * The tab is hidden while signed out (see `App.kt`), which is what the chip did too — YouTube has
 * no mixes to give an anonymous session.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun MixForYouScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val density = LocalDensity.current
    val mixForYou by viewModel.youTubeMixForYou.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState(blurEnabled = true)
    val gridState = rememberLazyGridState()
    // Home's rule, verbatim: transparent only while pixel-0 is on screen. onScrolling is too
    // coarse for this — it stays "on top" through the whole first row. The frost itself is kept
    // light (below), so frosting over the glow reads as a veil rather than a lid.
    val isAtTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }
    }

    var topAppBarHeight by remember { mutableStateOf(0.dp) }

    // Home's ambient top glow, tinted from the FIRST mix's artwork — the same DominantColorState +
    // network-loader machinery HomeScreen runs on its main thumbnail, including the light-theme
    // pastel lift and the desktop-panel tail (the desktop shell paints its rounded panel, not
    // colorScheme.background, so a tail aimed at background would draw a seam where the glow ends).
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
    val networkLoader = rememberNetworkLoader(HttpClient(CIO))
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = backgroundColor,
            defaultOnColor = backgroundColor,
            loader = networkLoader,
        )
    val firstMixThumbnail =
        mixForYou.data
            ?.firstOrNull()
            ?.thumbnails
            ?.lastOrNull()
            ?.url
    LaunchedEffect(firstMixThumbnail) {
        firstMixThumbnail?.let { dominantColorState.updateFrom(Url(it)) }
    }
    LaunchedEffect(dominantColorState, isLightTheme) {
        snapshotFlow { dominantColorState.color }.collect {
            topHeaderColor = if (isLightTheme) lerp(it, Color.White, 0.85f) else it.rgbFactor(0.3f)
        }
    }

    LaunchedEffect(Unit) {
        if (mixForYou.data.isNullOrEmpty()) {
            viewModel.getYouTubeMixedForYou()
        }
    }

    Box(Modifier.hazeSource(hazeState)) {
        // Drawn before the grid, and it SCROLLS AWAY with it like Home's — the draw-phase
        // translation tracks the first row exactly and parks once it has passed; whatever remains
        // at the hand-off sits deep in the scrim tail, so the switch does not pop. Inside the haze
        // source on purpose: the blurred top app bar frosts the glow exactly as it frosts artwork.
        Box(
            Modifier
                .graphicsLayer {
                    translationY =
                        if (gridState.firstVisibleItemIndex == 0) {
                            -gridState.firstVisibleItemScrollOffset.toFloat()
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
        GridLibraryPlaylist(
            navController,
            innerPadding.copy(top = topAppBarHeight),
            mixForYou,
            emptyText = Res.string.no_mixes_found,
            state = gridState,
            onScrolling = onScrolling,
        ) {
            viewModel.getYouTubeMixedForYou()
        }
    }
    // Transparent while the grid sits at pixel zero, frosted the moment it scrolls — the
    // same AnimatedContent crossfade Home and Search run on their bars.
    AnimatedContent(
        targetState = isAtTop,
        transitionSpec = {
            fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
        },
    ) { atTop ->
        Column(
            Modifier
                .then(
                    if (atTop) {
                        Modifier.background(Color.Transparent)
                    } else {
                        // AlbumScreen's bar recipe, thinned to 0.3 — see SettingScreen.
                        Modifier.hazeEffect(hazeState) {
                            blurEnabled = true
                            blurRadius = 24.dp
                            // `this.` is load-bearing: this function has a local
                            // `val backgroundColor` for the glow machinery, and Kotlin resolves
                            // locals BEFORE implicit-receiver members — the bare name assigns to
                            // the val and does not compile.
                            this.backgroundColor = pageBackground
                            tints = listOf(HazeTint(pageBackground.copy(alpha = 0.3f)))
                        }
                    },
                ).onGloballyPositioned { coordinates ->
                    topAppBarHeight = with(density) { coordinates.size.height.toDp() }
                },
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.mix_for_you),
                        style = typo().titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
            )
        }
    }
}