package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Cast button. Renders nothing on platforms without Cast, when the device has no Google Play
 * services, or when no Cast receiver is reachable on the network.
 *
 * [tint] colours the icon — callers flip it to signal an active Cast session.
 */
@Composable
expect fun PlatformCastButton(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
)

/**
 * Whether [PlatformCastButton] would actually render anything. Layouts that give the button its
 * own container (e.g. a slot in a connected button group) must hide the container too when this
 * is false — the button hides itself, but it cannot hide a wrapper it doesn't own.
 */
expect fun isPlatformCastAvailable(): Boolean

/** A Cast receiver playback can be handed to. [isConnected] while playback is on it. */
@Immutable
data class CastReceiver(
    val id: String,
    val name: String,
    val isConnected: Boolean,
)

/** The receivers on the network, plus the two things a picker does with them. */
@Stable
class CastReceivers(
    val receivers: List<CastReceiver>,
    val connect: (id: String) -> Unit,
    val disconnect: () -> Unit,
)

/**
 * Cast receivers for a picker of the app's own, scanned only while [discover] is true — i.e. while
 * the list is on screen. Always empty where Cast is not built in (desktop, the FOSS build).
 */
@Composable
expect fun rememberCastReceivers(discover: Boolean): CastReceivers

