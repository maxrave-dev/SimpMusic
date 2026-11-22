package com.maxrave.simpmusic.expect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

/**
 * Full screen for the main window (#2864, #2794, #2865): F11 toggles it on every OS and Esc leaves
 * it; on macOS the system title bar's green button does too. DesktopApp attaches its window state.
 *
 * All three use Compose's [WindowPlacement.Fullscreen]: AWT `setFullScreenWindow` on Windows/Linux,
 * `-[NSWindow toggleFullScreen:]` on macOS. Compose reads a full screen started from the green button
 * back into the state on `componentResized`.
 */
object DesktopFullscreen {
    private var windowState by mutableStateOf<WindowState?>(null)

    // Compose ignores Fullscreen -> Maximized (CMP-4006), so leaving goes to Floating and maximizes
    // again from [restoreMaximized] once the window is out.
    private var reMaximize = false

    val isFullscreen: Boolean
        get() = windowState?.placement == WindowPlacement.Fullscreen

    fun attach(state: WindowState) {
        windowState = state
    }

    fun toggle() {
        val state = windowState ?: return
        if (state.placement == WindowPlacement.Fullscreen) {
            state.placement = WindowPlacement.Floating
        } else {
            reMaximize = state.placement == WindowPlacement.Maximized
            state.placement = WindowPlacement.Fullscreen
        }
    }

    /** Called when the window's placement becomes Floating; maximizes it again after full screen. */
    fun restoreMaximized() {
        if (!reMaximize) return
        reMaximize = false
        windowState?.placement = WindowPlacement.Maximized
    }
}
