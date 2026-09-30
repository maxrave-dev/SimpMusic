package org.simpmusic.cast

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.mediarouter.media.MediaRouter

/** A Cast receiver on the network. [id] is its MediaRouter route id; [isSelected] while playback is on it. */
data class CastRouteInfo(
    val id: String,
    val name: String,
    val isSelected: Boolean,
)

/**
 * The Cast receivers this app can play to, for a picker of its own rather than the stock chooser
 * dialog. Scanning the network is only requested while [discover] is true — the list is on screen —
 * because discovery keeps the radio busy. Empty when Cast is unavailable.
 */
@Composable
fun rememberCastRoutes(discover: Boolean): List<CastRouteInfo> {
    val context = LocalContext.current
    val selector = remember { castRouteSelector() } ?: return emptyList()
    val router = remember(context) { MediaRouter.getInstance(context.applicationContext) }
    var routes by remember { mutableStateOf(emptyList<CastRouteInfo>()) }
    DisposableEffect(router, selector, discover) {
        fun read() {
            routes =
                router.routes
                    .filter { !it.isDefault && it.isEnabled && it.matchesSelector(selector) }
                    .map { CastRouteInfo(id = it.id, name = it.name, isSelected = it.isSelected) }
        }
        val callback =
            object : MediaRouter.Callback() {
                override fun onRouteAdded(
                    router: MediaRouter,
                    route: MediaRouter.RouteInfo,
                ) = read()

                override fun onRouteRemoved(
                    router: MediaRouter,
                    route: MediaRouter.RouteInfo,
                ) = read()

                override fun onRouteChanged(
                    router: MediaRouter,
                    route: MediaRouter.RouteInfo,
                ) = read()

                override fun onRouteSelected(
                    router: MediaRouter,
                    selectedRoute: MediaRouter.RouteInfo,
                    reason: Int,
                    requestedRoute: MediaRouter.RouteInfo,
                ) = read()

                override fun onRouteUnselected(
                    router: MediaRouter,
                    route: MediaRouter.RouteInfo,
                    reason: Int,
                ) = read()
            }
        router.addCallback(selector, callback, if (discover) MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY else 0)
        read()
        onDispose { router.removeCallback(callback) }
    }
    return routes
}

/**
 * Hand playback to the receiver [id]. Selecting the route is all a custom picker does: the Cast
 * SDK's SessionManager watches MediaRouter selection and starts the session itself — see
 * developers.google.com/cast/docs/android_sender/advanced, "Custom Cast Dialogs".
 */
fun selectCastRoute(
    context: Context,
    id: String,
) {
    val router = MediaRouter.getInstance(context.applicationContext)
    router.routes.firstOrNull { it.id == id }?.let(router::selectRoute)
}

/** Stop casting and bring playback back here; SessionManager ends the session when the route is unselected. */
fun stopCasting(context: Context) {
    MediaRouter.getInstance(context.applicationContext).unselect(MediaRouter.UNSELECT_REASON_STOPPED)
}
