package com.maxrave.simpmusic.expect

import androidx.compose.runtime.Composable

// No-op on Android - fullscreen player window is desktop only
actual fun toggleFullscreenPlayer() {
    // Do nothing on Android
}

@Composable
actual fun isFullscreenPlayerOpen(): Boolean = false

