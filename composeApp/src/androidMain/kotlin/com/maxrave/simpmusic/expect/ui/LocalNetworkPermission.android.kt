package com.maxrave.simpmusic.expect.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

@Composable
actual fun rememberLocalNetworkPermission(): LocalNetworkPermissionRequester {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Nothing to do with the answer: the setting is saved either way.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    return remember(context, launcher) {
        object : LocalNetworkPermissionRequester {
            override fun requestIfNeeded(url: String) {
                if (Build.VERSION.SDK_INT < 37 ||
                    ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                scope.launch {
                    // Resolving a host name is network I/O, which Android refuses on the main thread.
                    if (withContext(Dispatchers.IO) { isLocalNetworkUrl(url) }) {
                        launcher.launch(ACCESS_LOCAL_NETWORK)
                    }
                }
            }
        }
    }
}

/**
 * Whether Android would count [url]'s host as the local network: a `.local` name (those cannot even
 * be resolved without the permission), or an address in AOSP's `IPV4_LOCAL_PREFIXES` — link-local,
 * CGNAT (Tailscale), RFC 1918 — or IPv6 link-local / unique-local. Not covered: a global IPv6
 * address inside the phone's own on-link prefix, which Android also counts — telling it apart needs
 * the phone's own addresses. A host that does not resolve asks nothing.
 */
private fun isLocalNetworkUrl(url: String): Boolean {
    val host = runCatching { URI(url).host }.getOrNull() ?: return false
    if (host.endsWith(".local", ignoreCase = true)) return true
    val addresses = runCatching { InetAddress.getAllByName(host) }.getOrNull() ?: return false
    return addresses.any { address ->
        when (address) {
            is Inet4Address -> {
                val (first, second) = address.address.map { it.toInt() and 0xFF }
                address.isSiteLocalAddress || address.isLinkLocalAddress || (first == 100 && second in 64..127)
            }
            is Inet6Address -> address.isLinkLocalAddress || (address.address[0].toInt() and 0xFE) == 0xFC
            else -> false
        }
    }
}
