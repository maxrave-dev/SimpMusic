package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun PlatformRemoteDeviceButton(
    modifier: Modifier,
    tint: Color,
) {
    // No-op: the HEOS integration is built on Media3 and only exists on Android for now.
}

actual fun isPlatformRemoteDeviceAvailable(): Boolean = false
