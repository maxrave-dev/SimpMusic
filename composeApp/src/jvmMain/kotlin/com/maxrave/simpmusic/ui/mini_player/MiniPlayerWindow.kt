package com.maxrave.simpmusic.ui.mini_player

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.circle_app_icon

/**
 * Mini player window - a separate always-on-top window for music controls.
 * This window is independent of the main application window.
 * 
 * Features:
 * - Always on top of other windows
 * - Resizable (default 400x110 dp)
 * - Shares player state with main window
 * - Close-safe (doesn't close main app)
 */
@Composable
fun MiniPlayerWindow(
    sharedViewModel: SharedViewModel,
    onCloseRequest: () -> Unit
) {
    Window(
        onCloseRequest = onCloseRequest,
        title = "SimpMusic - Mini Player",
        icon = painterResource(Res.drawable.circle_app_icon),
        alwaysOnTop = true,
        resizable = true,
        state = WindowState(
            size = DpSize(400.dp, 110.dp)
        )
    ) {
        MiniPlayerRoot(sharedViewModel)
    }
}
