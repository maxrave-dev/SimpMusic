package org.simpmusic.cast

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.common.Player
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.maxrave.logger.Logger

private const val TAG = "Cast"

@Volatile
private var castContext: CastContext? = null

@Volatile
private var castAvailable: Boolean = false

/**
 * Safely initializes the Google Cast framework. Never throws — devices/emulators
 * without Google Play services (or a misconfigured Cast receiver) simply end up
 * with casting disabled instead of crashing the app.
 */
fun initCast(context: Context): Boolean {
    castContext?.let { return castAvailable }

    val appContext = context.applicationContext
    return try {
        val playServicesAvailable =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext) ==
                ConnectionResult.SUCCESS
        if (!playServicesAvailable) {
            Logger.d(TAG, "Google Play services unavailable, Cast disabled")
            castAvailable = false
            return false
        }

        castContext = CastContext.getSharedInstance(appContext)
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

fun currentCastDeviceName(): String? =
    try {
        castContext
            ?.sessionManager
            ?.currentCastSession
            ?.castDevice
            ?.friendlyName
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to read current cast device name: ${e.message}", e)
        null
    }
