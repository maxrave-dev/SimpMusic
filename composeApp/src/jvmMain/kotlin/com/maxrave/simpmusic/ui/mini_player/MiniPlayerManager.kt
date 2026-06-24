package com.maxrave.simpmusic.ui.mini_player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Manager for the mini player window state.
 * Controls whether the mini player window is open or closed.
 */
object MiniPlayerManager {
    var isOpen by mutableStateOf(false)
}
