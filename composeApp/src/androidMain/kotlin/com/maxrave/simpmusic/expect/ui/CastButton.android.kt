package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.awaitCancellation
import org.simpmusic.cast.CastIconButton
import org.simpmusic.cast.getAvailableCastRoutes
import org.simpmusic.cast.isCastAvailable
import org.simpmusic.cast.selectCastRouteById
import org.simpmusic.cast.startRouteDiscovery
import org.simpmusic.cast.stopRouteDiscovery
import com.maxrave.simpmusic.ui.icon.MusicCast
import com.maxrave.simpmusic.ui.icon.SimpIcons

@Composable
actual fun PlatformCastButton(
    modifier: Modifier,
    tint: Color,
    onShowPicker: (() -> Unit)?,
) {
    if (onShowPicker != null) {
        // Custom picker mode: regular Compose button that opens our bottom sheet
        IconButton(
            onClick = onShowPicker,
            modifier = modifier.size(48.dp),
        ) {
            Icon(
                imageVector = SimpIcons.MusicCast,
                contentDescription = "Cast",
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
    } else {
        // Legacy mode: delegate to MediaRouteButton which opens Google's dialog
        CastIconButton(modifier = modifier, tint = tint)
    }
}

actual fun isPlatformCastAvailable(): Boolean = org.simpmusic.cast.isCastAvailable()

actual fun disconnectFromCast() {
    org.simpmusic.cast.disconnectFromCast()
}

@Composable
actual fun rememberCastRouteDiscovery(
    listener: (() -> Unit)?,
) {
    var generation by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        startRouteDiscovery { generation++ }
        try {
            awaitCancellation()
        } finally {
            stopRouteDiscovery()
        }
    }

    LaunchedEffect(generation) {
        listener?.invoke()
    }
}

actual fun getAvailableCastDevices(): List<CastDevice> {
    val routes = getAvailableCastRoutes()
    return routes.map {
        CastDevice(
            id = it.id,
            name = it.name,
            isConnected = it.isConnected,
            isActive = it.isActive,
        )
    }
}

actual fun selectCastDevice(deviceId: String) {
    selectCastRouteById(deviceId)
}

actual fun isCastSessionActive(): Boolean = org.simpmusic.cast.isCastSessionActive()

actual fun getCurrentCastDeviceName(): String? = org.simpmusic.cast.getCurrentCastDeviceName()
