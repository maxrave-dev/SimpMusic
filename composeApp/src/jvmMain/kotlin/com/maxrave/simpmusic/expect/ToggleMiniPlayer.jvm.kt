package com.maxrave.simpmusic.expect

import com.maxrave.simpmusic.ui.mini_player.MiniPlayerManager

actual fun toggleMiniPlayer() {
    MiniPlayerManager.isOpen = !MiniPlayerManager.isOpen
}
