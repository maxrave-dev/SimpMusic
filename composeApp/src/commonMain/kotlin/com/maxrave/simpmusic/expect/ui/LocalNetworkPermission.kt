@file:Suppress("ktlint:standard:filename")

package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

interface LocalNetworkPermissionRequester {
    /**
     * Asks for local network access when [url] points into the user's own network and the app does
     * not hold it yet. Fire and forget: the caller saves the setting whatever the answer is, and a
     * refusal only leaves that address unreachable — which is what the system dialog said.
     */
    fun requestIfNeeded(url: String)
}

/**
 * Gate in front of a URL the user typed for the app to connect to later — today the custom AI base
 * URL, which often points at a model server on the user's own network.
 *
 * Only Android 17+ needs anything: it blocks the local network per app, and because the manifest
 * declares ACCESS_LOCAL_NETWORK the implicit grant for targetSdk < 37 never applies (see
 * `loginSyncPermissions`). Without it the connection does not fail — it times out.
 */
@Composable
expect fun rememberLocalNetworkPermission(): LocalNetworkPermissionRequester
