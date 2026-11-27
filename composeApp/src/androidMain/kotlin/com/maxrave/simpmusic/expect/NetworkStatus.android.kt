package com.maxrave.simpmusic.expect

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.koin.mp.KoinPlatform.getKoin

actual fun isNetworkAvailable(): Boolean {
    val context: Context = getKoin().get()
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java) ?: return true
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities =
        try {
            connectivityManager.getNetworkCapabilities(network)
        } catch (e: SecurityException) {
            null
        } ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}