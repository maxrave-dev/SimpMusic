package com.maxrave.simpmusic.ui.mini_player

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import com.maxrave.simpmusic.viewModel.SharedViewModel

/**
 * Mini player window - a separate always-on-top window for music controls.
 * This window is independent of the main application window.
 */
@Composable
fun MiniPlayerWindow(
    sharedViewModel: SharedViewModel,
    onCloseRequest: () -> Unit
) {
    Window(
        onCloseRequest = onCloseRequest,
        title = "Mini Player",
        alwaysOnTop = true,
        resizable = true,
        state = WindowState(
            size = DpSize(360.dp, 120.dp)
        )
    ) {
        MiniPlayerRoot(sharedViewModel)
    }
}
