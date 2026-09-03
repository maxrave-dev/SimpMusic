package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Cast button. Renders nothing on platforms without Cast, when the device has no Google Play
 * services, or when no Cast receiver is reachable on the network.
 *
 * [tint] colours the icon — callers flip it to signal an active Cast session.
 * [onShowPicker] if provided, clicking the button calls this instead of opening Google's dialog.
 */
@Composable
expect fun PlatformCastButton(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    onShowPicker: (() -> Unit)? = null,
)

/**
 * Whether [PlatformCastButton] would actually render anything. Layouts that give the button its
 * own container (e.g. a slot in a connected button group) must hide the container too when this
 * is false — the button hides itself, but it cannot hide a wrapper it doesn't own.
 */
expect fun isPlatformCastAvailable(): Boolean

/**
 * Disconnects from the current Cast session. No-op on platforms without Cast support.
 */
expect fun disconnectFromCast()

/**
 * A discovered Cast device, platform-agnostic representation.
 */
data class CastDevice(
    val id: String,
    val name: String,
    val isConnected: Boolean,
    val isActive: Boolean,
)

/**
 * Starts observing Cast route changes. [listener] fires when available devices change.
 * No-op on platforms without Cast support.
 */
@Composable
expect fun rememberCastRouteDiscovery(
    listener: (() -> Unit)? = null,
)

/**
 * Returns the list of available Cast devices.
 * Empty on platforms without Cast support.
 */
expect fun getAvailableCastDevices(): List<CastDevice>

/**
 * Connects to a Cast route by its [deviceId].
 * No-op on platforms without Cast support.
 */
expect fun selectCastDevice(deviceId: String)

/**
 * Whether a Cast session is currently active.
 */
expect fun isCastSessionActive(): Boolean

/**
 * Returns the name of the currently connected Cast device, or null.
 */
expect fun getCurrentCastDeviceName(): String?
