package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Manager for the desktop fullscreen player window state.
 * Controls whether the separate fullscreen player window is open or closed.
 */
object FullscreenPlayerManager {
    var isOpen by mutableStateOf(false)
}
