package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun PlatformCastButton(
    modifier: Modifier,
    tint: Color,
    onShowPicker: (() -> Unit)?,
) {
    // No-op: desktop has no Google Cast sender.
}

actual fun isPlatformCastAvailable(): Boolean = false

actual fun disconnectFromCast() {
    // No-op: desktop has no Google Cast sender.
}

@Composable
actual fun rememberCastRouteDiscovery(
    listener: (() -> Unit)?,
) {
    // No-op: desktop has no Google Cast sender.
}

actual fun getAvailableCastDevices(): List<CastDevice> = emptyList()

actual fun selectCastDevice(deviceId: String) {
    // No-op: desktop has no Google Cast sender.
}

actual fun isCastSessionActive(): Boolean = false

actual fun getCurrentCastDeviceName(): String? = null
