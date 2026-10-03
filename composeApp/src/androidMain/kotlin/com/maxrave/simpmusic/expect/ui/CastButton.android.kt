package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.simpmusic.cast.CastIconButton
import org.simpmusic.cast.isCastAvailable
import org.simpmusic.cast.rememberCastRoutes
import org.simpmusic.cast.selectCastRoute
import org.simpmusic.cast.stopCasting

@Composable
actual fun PlatformCastButton(
    modifier: Modifier,
    tint: Color,
) {
    CastIconButton(modifier = modifier, tint = tint)
}

actual fun isPlatformCastAvailable(): Boolean = isCastAvailable()

@Composable
actual fun rememberCastReceivers(discover: Boolean): CastReceivers {
    val context = LocalContext.current
    val routes = rememberCastRoutes(discover)
    return CastReceivers(
        receivers = routes.map { CastReceiver(id = it.id, name = it.name, isConnected = it.isSelected) },
        connect = { id -> selectCastRoute(context, id) },
        disconnect = { stopCasting(context) },
    )
}

