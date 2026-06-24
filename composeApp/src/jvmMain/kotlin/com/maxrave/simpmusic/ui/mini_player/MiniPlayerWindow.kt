package com.maxrave.simpmusic.ui.mini_player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import org.jetbrains.compose.resources.painterResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.circle_app_icon
import java.awt.Dimension
import java.util.prefs.Preferences

/**
 * Mini player window - a separate always-on-top window for music controls.
 * Spotify-style frameless design with custom close button.
 *
 * Features:
 * - Always on top of other windows
 * - Frameless (no title bar)
 * - Resizable (default 400x110 dp)
 * - Shares player state with main window
 * - Close-safe (doesn't close main app)
 * - Remembers window position
 * - Keyboard shortcuts (Space: play/pause, Arrow keys: prev/next)
 */
@Composable
fun MiniPlayerWindow(
    sharedViewModel: SharedViewModel,
    onCloseRequest: () -> Unit,
) {
    val prefs = remember { Preferences.userRoot().node("SimpMusic/MiniPlayer") }

    // Minimum size constraints
    val minWidth = 200f
    val minHeight = 56f

    // Load saved position or use default (with minimum constraints)
    val savedX = prefs.getFloat("windowX", Float.NaN)
    val savedY = prefs.getFloat("windowY", Float.NaN)
    val savedWidth = prefs.getFloat("windowWidth", 400f).coerceAtLeast(minWidth)
    val savedHeight = prefs.getFloat("windowHeight", 56f).coerceAtLeast(minHeight)

    var windowState by remember {
        mutableStateOf(
            WindowState(
                placement = WindowPlacement.Floating,
                position =
                    if (savedX.isNaN() || savedY.isNaN()) {
                        WindowPosition(Alignment.BottomEnd)
                    } else {
                        WindowPosition(savedX.coerceAtLeast(0f).dp, savedY.coerceAtLeast(0f).dp)
                    },
                size = DpSize(savedWidth.coerceAtLeast(0f).dp, savedHeight.coerceAtLeast(0f).dp),
            ),
        )
    }

    // Save position on change
    LaunchedEffect(windowState.position, windowState.size) {
        val pos = windowState.position
        Logger.w("MiniPlayerWindow", "Saving position: $pos")
        if (pos is WindowPosition.Absolute) {
            prefs.putFloat("windowX", pos.x.value)
            prefs.putFloat("windowY", pos.y.value)
        }
        prefs.putFloat("windowWidth", windowState.size.width.value)
        prefs.putFloat("windowHeight", windowState.size.height.value)
    }

    Window(
        onCloseRequest = onCloseRequest,
        title = "SimpMusic - Mini Player",
        icon = painterResource(Res.drawable.circle_app_icon),
        alwaysOnTop = true,
        undecorated = true,
        transparent = true,
        resizable = true,
        state = windowState,
        onKeyEvent = { keyEvent ->
            when {
                keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Spacebar -> {
                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                    true
                }

                keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionRight -> {
                    sharedViewModel.onUIEvent(UIEvent.Next)
                    true
                }

                keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionLeft -> {
                    sharedViewModel.onUIEvent(UIEvent.Previous)
                    true
                }

                else -> {
                    false
                }
            }
        },
    ) {
        // Set minimum size at AWT level to prevent flickering
        LaunchedEffect(Unit) {
            (window as? java.awt.Window)?.minimumSize =
                Dimension(
                    (minWidth * window.graphicsConfiguration.defaultTransform.scaleX).toInt(),
                    (minHeight * window.graphicsConfiguration.defaultTransform.scaleY).toInt(),
                )
        }

        MiniPlayerRoot(
            sharedViewModel = sharedViewModel,
            onClose = onCloseRequest,
            windowState = windowState,
        )
    }
}