package com.maxrave.simpmusic.expect

import androidx.compose.runtime.Composable

// No-op on iOS - fullscreen player window is desktop only
actual fun toggleFullscreenPlayer() {
    // Do nothing on iOS
}

@Composable
actual fun isFullscreenPlayerOpen(): Boolean = false

