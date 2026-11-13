package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Navigation uses the visible back buttons and Compose's iOS back gesture.
}
