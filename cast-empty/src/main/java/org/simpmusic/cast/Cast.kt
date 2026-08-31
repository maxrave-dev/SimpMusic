package org.simpmusic.cast

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.Player
import androidx.mediarouter.media.MediaRouter.RouteInfo
import com.maxrave.logger.Logger

// NON-CAST build: Google Cast is not available in this build flavor. All
// functions are safe no-ops so callers never need to branch on build flavor.

/** Emits route list changes so the Compose picker can observe available devices. */
typealias CastRoutesChangedListener = () -> Unit

fun initCast(context: Context): Boolean {
    Logger.d("Cast", "NON-CAST build: Google Cast is not available")
    return false
}

fun isCastAvailable(): Boolean = false

fun wrapWithCastPlayer(
    context: Context,
    localPlayer: Player,
): Player = localPlayer

fun disconnectFromCast() {}

fun getCastDeviceVolume(): Float? = null

fun setCastDeviceVolume(volume: Float) {}

fun startRouteDiscovery(listener: CastRoutesChangedListener) {}

fun stopRouteDiscovery() {}

fun getAvailableCastRoutes(): List<CastDeviceRoute> = emptyList()

fun selectCastRoute(route: RouteInfo?) {}

fun getCurrentCastDeviceName(): String? = null

fun isCastSessionActive(): Boolean = false

fun selectCastRouteById(routeId: String) {}

@Composable
fun CastIconButton(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    // No-op: Google Cast is not available in this build flavor.
}
