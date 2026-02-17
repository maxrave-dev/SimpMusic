package com.maxrave.simpmusic.wear.ui.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.util.Locale

fun Context.isOnline(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    // On some devices/transport combinations `VALIDATED` can be flaky; for UX we mainly care
    // whether the system reports general internet capability.
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun Context.friendlyNetworkError(rawMessage: String?): String {
    if (!isOnline()) {
        return "No internet connection. Enable Wi-Fi on your watch (or keep your phone nearby if using Bluetooth)."
    }

    val msg = rawMessage?.trim().orEmpty()
    if (msg.isBlank()) return "Network error. Please try again."

    val lower = msg.lowercase(Locale.US)
    return when {
        "timeout" in lower || "timed out" in lower ->
            "Network timeout. Your watch connection may be slow or unstable. Try Wi-Fi, then retry."
        "unable to resolve host" in lower || "no address associated with hostname" in lower ->
            "Can't reach the server. Check Wi-Fi and DNS, then retry."
        "connection reset" in lower || "broken pipe" in lower ->
            "Connection dropped. Try again (Wi-Fi tends to be more reliable than Bluetooth)."
        else -> msg
    }
}

