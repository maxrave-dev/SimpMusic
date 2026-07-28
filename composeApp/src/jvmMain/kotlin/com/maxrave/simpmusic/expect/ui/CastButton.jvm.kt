package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun PlatformCastButton(
    modifier: Modifier,
    tint: Color,
) {
    // No-op: desktop has no Google Cast sender.
}
