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

actual fun isPlatformCastAvailable(): Boolean = false

@Composable
actual fun rememberCastReceivers(discover: Boolean): CastReceivers = NoCastReceivers

// Desktop has no Google Cast sender, so the list is always empty and the actions do nothing.
private val NoCastReceivers = CastReceivers(receivers = emptyList(), connect = {}, disconnect = {})

