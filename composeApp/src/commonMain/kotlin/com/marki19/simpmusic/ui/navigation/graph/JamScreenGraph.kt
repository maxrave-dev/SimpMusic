package com.marki19.simpmusic.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.marki19.simpmusic.ui.navigation.destination.jam.JamGuestDestination
import com.marki19.simpmusic.ui.navigation.destination.jam.JamHostDestination
import com.marki19.simpmusic.ui.navigation.destination.jam.JamMenuDestination
import com.marki19.simpmusic.ui.navigation.destination.jam.JamSessionDestination
import com.marki19.simpmusic.ui.screen.jam.JamGuestScreen
import com.marki19.simpmusic.ui.screen.jam.JamHostScreen
import com.marki19.simpmusic.ui.screen.jam.JamMenuScreen
import com.marki19.simpmusic.ui.screen.jam.JamSessionScreen
import com.marki19.simpmusic.viewModel.jam.JamViewModel
import org.koin.compose.koinInject

fun NavGraphBuilder.jamScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
) {
    composable<JamMenuDestination> {
        JamMenuScreen(
            navController = navController
        )
    }
    
    composable<JamHostDestination> {
        val jamViewModel: JamViewModel = koinInject()
        JamHostScreen(
            viewModel = jamViewModel,
            onNavigateToSession = { 
                navController.navigate(JamSessionDestination(roomCode = jamViewModel.sessionState.value?.roomId ?: "")) {
                    popUpTo(JamHostDestination::class) { inclusive = true }
                }
            },
            onBack = { navController.navigateUp() }
        )
    }
    
    composable<JamGuestDestination> {
        val jamViewModel: JamViewModel = koinInject()
        JamGuestScreen(
            viewModel = jamViewModel,
            onNavigateToSession = { 
                navController.navigate(JamSessionDestination(roomCode = jamViewModel.sessionState.value?.roomId ?: "")) {
                    popUpTo(JamGuestDestination::class) { inclusive = true }
                }
            },
            onBack = { navController.navigateUp() }
        )
    }
    
    composable<JamSessionDestination> { entry ->
        val jamViewModel: JamViewModel = koinInject()
        val params = entry.toRoute<JamSessionDestination>()
        JamSessionScreen(
            viewModel = jamViewModel,
            onBack = { navController.navigateUp() }
        )
    }
}
