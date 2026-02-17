package com.maxrave.simpmusic.wear.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.maxrave.logger.Logger

/**
 * WearOS commonly has both Wi-Fi and Bluetooth (phone proxy) networks active.
 * YouTube `googlevideo.com/videoplayback` URLs are often bound to the IP/network path
 * used when generating them (InnerTube player endpoint). If playback uses a different
 * network than the one used to generate the URL, the request can 403.
 *
 * Binding the process to validated Wi-Fi (when available) keeps URL generation + media
 * download on the same network, improving playback reliability on WearOS.
 */
class ProcessNetworkBinder(
    private val appContext: Context,
) {
    private val cm: ConnectivityManager =
        appContext.getSystemService(ConnectivityManager::class.java)

    @Volatile
    private var boundNetwork: Network? = null

    private val wifiCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateBinding()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                updateBinding()
            }

            override fun onLost(network: Network) {
                updateBinding()
            }
        }

    private val defaultCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateBinding()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                updateBinding()
            }

            override fun onLost(network: Network) {
                updateBinding()
            }
        }

    fun start() {
        runCatching {
            val req =
                NetworkRequest
                    .Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
            cm.registerNetworkCallback(req, wifiCallback)
        }.onFailure {
            Logger.w("NetBind", "registerNetworkCallback(wifi) failed: ${it.message}")
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(defaultCallback)
            }
        }.onFailure {
            Logger.w("NetBind", "registerDefaultNetworkCallback failed: ${it.message}")
        }

        updateBinding()
    }

    fun stop() {
        runCatching { cm.unregisterNetworkCallback(wifiCallback) }
        runCatching { cm.unregisterNetworkCallback(defaultCallback) }
        unbind()
    }

    private fun updateBinding() {
        val wifi = findValidatedWifiNetwork()
        if (wifi != null) {
            bind(wifi)
        } else {
            unbind()
        }
    }

    private fun findValidatedWifiNetwork(): Network? {
        val networks =
            runCatching { cm.allNetworks.toList() }
                .getOrDefault(emptyList())

        for (n in networks) {
            val caps = cm.getNetworkCapabilities(n) ?: continue
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
            // Prefer validated Wi-Fi to avoid binding to a captive portal / no-internet AP.
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) continue
            return n
        }
        return null
    }

    private fun bind(network: Network) {
        val current = boundNetwork
        if (current == network) return

        val ok =
            runCatching {
                cm.bindProcessToNetwork(network)
                true
            }.getOrDefault(false)

        if (ok) {
            boundNetwork = network
            Logger.w("NetBind", "Bound process to validated Wi-Fi network")
        } else {
            Logger.w("NetBind", "bindProcessToNetwork(wifi) failed")
        }
    }

    private fun unbind() {
        if (boundNetwork == null) return

        val ok =
            runCatching {
                cm.bindProcessToNetwork(null)
                true
            }.getOrDefault(false)

        if (ok) {
            boundNetwork = null
            Logger.w("NetBind", "Unbound process network (back to default)")
        } else {
            Logger.w("NetBind", "bindProcessToNetwork(null) failed")
        }
    }
}

