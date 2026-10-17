package com.maxrave.simpmusic.expect

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.koin.mp.KoinPlatform.getKoin

actual fun isOnUnmeteredNetwork(): Boolean {
    val context: Context = getKoin().get()
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    // Some devices throw SecurityException from these calls (b/163342798; WorkManager's
    // NetworkStateTracker guards the same ones). Read that as "not on Wi-Fi" instead of crashing.
    val capabilities =
        try {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        } catch (e: SecurityException) {
            null
        } ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}
