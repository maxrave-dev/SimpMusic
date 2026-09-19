package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * "Play on HEOS device" button (Denon / Marantz receivers, HEOS speakers). Tapping it opens a
 * picker that discovers HEOS players on the local network and hands the queue to the chosen
 * one; while a device is active the same sheet offers to bring playback back to this device.
 *
 * Renders nothing on platforms without the HEOS integration (Desktop, for now).
 *
 * [tint] colours the icon — callers flip it to signal an active remote session.
 */
@Composable
expect fun PlatformRemoteDeviceButton(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
)

/**
 * Whether [PlatformRemoteDeviceButton] would actually render anything. Layouts that give the
 * button its own container must hide the container too when this is false.
 */
expect fun isPlatformRemoteDeviceAvailable(): Boolean
