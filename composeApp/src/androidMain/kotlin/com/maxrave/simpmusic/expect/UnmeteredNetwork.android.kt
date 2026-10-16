package com.maxrave.simpmusic.expect

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.koin.mp.KoinPlatform.getKoin

actual fun isOnUnmeteredNetwork(): Boolean {
    val context: Context = getKoin().get()
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}
