package com.maxrave.simpmusic.expect

import androidx.compose.runtime.Composable
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.ui.screen.player.FullscreenPlayerManager

actual fun toggleFullscreenPlayer() {
    Logger.d("FullscreenPlayer", "Toggle called, current state: ${FullscreenPlayerManager.isOpen}")
    FullscreenPlayerManager.isOpen = !FullscreenPlayerManager.isOpen
    Logger.d("FullscreenPlayer", "New state: ${FullscreenPlayerManager.isOpen}")
}

@Composable
actual fun isFullscreenPlayerOpen(): Boolean = FullscreenPlayerManager.isOpen

