package com.maxrave.simpmusic.ui.screen.home.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.AnalyticsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun AnalyticsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    analyticsViewModel: AnalyticsViewModel = koinViewModel()
) {
    val hazeState = rememberHazeState()
    val uiState by analyticsViewModel.analyticsUiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        Logger.d("AnalyticsScreen", "UI State updated: ${uiState.scrobblesCount.data}, ${uiState.artistCount.data}, ${uiState.totalListenTimeInSeconds.data}")
        Logger.d("AnalyticsScreen", "Top Tracks: ${uiState.topTracks.data?.joinToString { it.second.title }}")
        Logger.d("AnalyticsScreen", "Top Artists: ${uiState.topArtists.data?.joinToString { it.second.name }}")
        Logger.d("AnalyticsScreen", "Top Albums: ${uiState.topAlbums.data?.joinToString { it.second.title }}")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState),
        ) {
            item {
                Spacer(
                    Modifier.size(
                        innerPadding.calculateTopPadding() + 64.dp,
                    ),
                )
            }

            item {
                EndOfPage()
            }
        }

        // Top App Bar with haze effect
        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    },
            title = {
                Text(
                    text = "Analytics",
                    style = typo().titleMedium,
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
            actions = {
                IconButton(
                    onClick = {

                    }
                ) {
                    Box {
                        Icon(Icons.Rounded.AutoGraph, "Analytics", tint = Color.White)
                        Text("NEW", Modifier.align(Alignment.BottomEnd), style = typo().bodySmall.copy(
                            fontSize = 5.sp
                        ))
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