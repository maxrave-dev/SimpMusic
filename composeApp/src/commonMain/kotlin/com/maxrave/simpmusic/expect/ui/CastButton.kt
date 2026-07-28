package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
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
