package com.maxrave.simpmusic.ui.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.extension.copy
import com.maxrave.simpmusic.ui.component.GridLibraryPlaylist
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LibraryViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
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

    var topAppBarHeight by remember { mutableStateOf(0.dp) }

    LaunchedEffect(Unit) {
        if (mixForYou.data.isNullOrEmpty()) {
            viewModel.getYouTubeMixedForYou()
        }
    }

    Box(Modifier.hazeSource(hazeState)) {
        GridLibraryPlaylist(
            navController,
            innerPadding.copy(top = topAppBarHeight),
            mixForYou,
            emptyText = Res.string.no_mixes_found,
            onScrolling = onScrolling,
        ) {
            viewModel.getYouTubeMixedForYou()
        }
    }
    Column(
        Modifier
            .background(Color.Transparent)
            .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                blurEnabled = true
            }.onGloballyPositioned { coordinates ->
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
