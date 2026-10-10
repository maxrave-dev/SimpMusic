package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // Desktop uses the on-screen back button.
}
