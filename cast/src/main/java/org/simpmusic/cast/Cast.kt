package org.simpmusic.cast

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.media3.cast.CastPlayer
import androidx.media3.common.Player
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouter.RouteInfo
import androidx.mediarouter.media.MediaRouteSelector
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.maxrave.logger.Logger

private const val TAG = "Cast"

@Volatile
private var castContext: CastContext? = null

@Volatile
private var appContext: Context? = null

@Volatile
private var castAvailable: Boolean = false

private var mediaRouterCallback: MediaRouter.Callback? = null

/** Emits route list changes so the Compose picker can observe available devices. */
typealias CastRoutesChangedListener = () -> Unit

/** Stored so [stopRouteDiscovery] can remove it without leaking. */
private var sessionManagerListener: SessionManagerListener<CastSession>? = null

/**
 * Safely initializes the Google Cast framework. Never throws — devices/emulators
 * without Google Play services (or a misconfigured Cast receiver) simply end up
 * with casting disabled instead of crashing the app.
 */
fun initCast(context: Context): Boolean {
    castContext?.let { return castAvailable }

    val ctx = context.applicationContext
    return try {
        val playServicesAvailable =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx) ==
                ConnectionResult.SUCCESS
        if (!playServicesAvailable) {
            Logger.d(TAG, "Google Play services unavailable, Cast disabled")
            castAvailable = false
            return false
        }

        castContext = CastContext.getSharedInstance(ctx)
        appContext = ctx
        castAvailable = true
        true
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to initialize CastContext: ${e.message}", e)
        castContext = null
        castAvailable = false
        false
    }
}

fun isCastAvailable(): Boolean = castAvailable

/**
 * Wraps [localPlayer] in a [CastPlayer] so playback can switch seamlessly between
 * local and remote (Cast) output. Falls back to returning [localPlayer] unchanged
 * when Cast isn't available or the CastPlayer fails to build.
 */
fun wrapWithCastPlayer(
    context: Context,
    localPlayer: Player,
): Player {
    if (!castAvailable) return localPlayer

    return try {
        CastPlayer.Builder(context.applicationContext)
            .setLocalPlayer(localPlayer)
            .build()
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to create CastPlayer, falling back to local player: ${e.message}", e)
        localPlayer
    }
}

/**
 * Ends the current Cast session, stopping remote playback and resuming locally.
 */
fun disconnectFromCast() {
    try {
        castContext?.sessionManager?.endCurrentSession(true)
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to disconnect cast: ${e.message}", e)
    }
}

/**
 * Returns the Cast device's volume as a 0f..1f fraction, or null if no session is active.
 * This reads the **device volume** (what Google Home shows), not the media stream volume.
 */
fun getCastDeviceVolume(): Float? =
    try {
        val session = castContext?.sessionManager?.currentCastSession
        if (session == null) {
            Logger.w(TAG, "getCastDeviceVolume: currentCastSession is null")
            return null
        }
        val vol = session.volume?.toFloat()
        Logger.d(TAG, "getCastDeviceVolume: $vol")
        vol
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to get cast device volume: ${e.message}", e)
        null
    }

/**
 * Sets the Cast device's volume as a 0f..1f fraction.
 * This controls the **device volume** (what Google Home shows), not the media stream volume.
 */
fun setCastDeviceVolume(volume: Float) {
    try {
        val session = castContext?.sessionManager?.currentCastSession
        if (session == null) {
            Logger.w(TAG, "setCastDeviceVolume: currentCastSession is null")
            return
        }
        Logger.d(TAG, "setCastDeviceVolume: $volume")
        session.setVolume(volume.toDouble())
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to set cast device volume: ${e.message}", e)
    }
}

// ========== Route Discovery ==========

/**
 * A discovered Cast route, surfaced to the Compose picker.
 */
data class CastDeviceRoute(
    val id: String,
    val name: String,
    val isConnected: Boolean,
    val isActive: Boolean,
    val route: RouteInfo?,
)

/**
 * Starts listening for Cast route changes. The [listener] fires whenever
 * the list of available devices changes so the Compose picker can refresh.
 * Registers both a SessionManagerListener (for connect/disconnect) and a
 * MediaRouter.Callback (for new devices appearing on the network).
 */
fun startRouteDiscovery(listener: CastRoutesChangedListener) {
    val ctx = appContext ?: return
    if (!castAvailable) return
    val castCtx = castContext ?: return

    // 1. Listen for session changes via CastSessionManagerListener
    val smListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) { listener() }
        override fun onSessionStarted(session: CastSession, sessionId: String) { listener() }
        override fun onSessionStartFailed(session: CastSession, error: Int) { listener() }
        override fun onSessionEnding(session: CastSession) { listener() }
        override fun onSessionEnded(session: CastSession, error: Int) { listener() }
        override fun onSessionResuming(session: CastSession, sessionId: String) { listener() }
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) { listener() }
        override fun onSessionResumeFailed(session: CastSession, error: Int) { listener() }
        override fun onSessionSuspended(session: CastSession, reason: Int) { listener() }
    }
    sessionManagerListener = smListener
    castCtx.sessionManager.addSessionManagerListener(smListener, CastSession::class.java)

    // 2. Listen for MediaRouter route discovery (new devices appearing on network)
    val selector = MediaRouteSelector.Builder()
        .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
        .build()

    val routerCallback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: RouteInfo) { listener() }
        override fun onRouteRemoved(router: MediaRouter, route: RouteInfo) { listener() }
        override fun onRouteChanged(router: MediaRouter, route: RouteInfo) { listener() }
        override fun onRouteSelected(router: MediaRouter, route: RouteInfo) { listener() }
        override fun onRouteUnselected(router: MediaRouter, route: RouteInfo) { listener() }
    }
    mediaRouterCallback = routerCallback
    MediaRouter.getInstance(ctx).addCallback(selector, routerCallback)
    Logger.d(TAG, "Route discovery started (session + MediaRouter listener)")
}

/**
 * Stops listening for Cast route changes.
 */
fun stopRouteDiscovery() {
    val ctx = appContext
    val callback = mediaRouterCallback
    val smListener = sessionManagerListener
    if (ctx != null && callback != null) {
        MediaRouter.getInstance(ctx).removeCallback(callback)
    }
    if (smListener != null) {
        castContext?.sessionManager?.removeSessionManagerListener(smListener, CastSession::class.java)
    }
    mediaRouterCallback = null
    sessionManagerListener = null
    Logger.d(TAG, "Route discovery stopped")
}

/**
 * Returns all Cast routes discovered on the local network, plus the connected
 * session device if one exists. Uses the same MediaRouter selector that
 * CastButtonFactory.setUpMediaRouteButton registers internally.
 */
fun getAvailableCastRoutes(): List<CastDeviceRoute> {
    if (!castAvailable) return emptyList()
    val ctx = appContext ?: return emptyList()

    val mediaRouter = MediaRouter.getInstance(ctx)
    val connectedId = try {
        castContext?.sessionManager?.currentCastSession?.castDevice?.deviceId
    } catch (_: Exception) { null }

    return mediaRouter.routes
        .filter { it.playbackType == RouteInfo.PLAYBACK_TYPE_REMOTE }
        .distinctBy { it.name }
        .map { route ->
            CastDeviceRoute(
                id = route.id,
                name = route.name ?: "Unknown Device",
                isConnected = route.id == connectedId,
                isActive = route.isSelected,
                route = route,
            )
        }
}

/**
 * Selects a Cast route to start a session.
 */
fun selectCastRoute(route: RouteInfo?) {
    val ctx = appContext ?: return
    if (route == null) {
        Logger.w(TAG, "Cannot select null route")
        return
    }
    try {
        MediaRouter.getInstance(ctx).selectRoute(route)
        Logger.d(TAG, "Selected route: ${route.name}")
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to select route: ${e.message}", e)
    }
}

/**
 * Returns the name of the currently connected Cast device, or null.
 */
fun getCurrentCastDeviceName(): String? =
    try {
        castContext?.sessionManager?.currentCastSession?.castDevice?.friendlyName
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to read current cast device name: ${e.message}", e)
        null
    }

/**
 * Whether there is an active Cast session.
 */
fun isCastSessionActive(): Boolean =
    try {
        castContext?.sessionManager?.currentCastSession?.isConnected == true
    } catch (e: Exception) {
        false
    }

/**
 * Selects a Cast route by its [routeId].
 * Finds the route in MediaRouter and selects it to start a Cast session.
 */
fun selectCastRouteById(routeId: String) {
    val ctx = appContext ?: return
    if (!castAvailable) return

    val route = MediaRouter.getInstance(ctx).routes
        .firstOrNull { it.id == routeId && it.playbackType == RouteInfo.PLAYBACK_TYPE_REMOTE }

    if (route != null) {
        selectCastRoute(route)
    } else {
        Logger.w(TAG, "Route not found for id: $routeId")
    }
}
