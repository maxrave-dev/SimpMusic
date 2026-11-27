package com.maxrave.simpmusic

import com.eygraber.uri.Uri
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.logger.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

/**
 * Singleton to handle deep link URIs on Desktop.
 * Caches URI if app UI is not ready yet, delivers immediately if listener is set.
 *
 * Also forwards a second instance's URI to the running one: the URI travels inside the
 * single-instance restore request, so it is there as soon as that request is seen.
 *
 * Supported URI patterns:
 * - simpmusic://open-app?url=<encoded_url>  (redirected from website)
 * - simpmusic://watch?v=VIDEO_ID            (direct scheme)
 * - simpmusic://playlist?list=PLAYLIST_ID   (direct scheme)
 * - simpmusic://channel/CHANNEL_ID          (direct scheme)
 * - simpmusic://album?id=ALBUM_ID           (direct scheme)
 * - https://simpmusic.org/app/...            (web URL passed via args)
 */
object DesktopDeepLinkHandler {
    private const val TAG = "DesktopDeepLinkHandler"

    // The link a second instance forwarded, read off its restore request on the watcher thread and
    // taken by the UI when it handles that restore.
    private val pendingUri = AtomicReference<String?>(null)

    private var cached: String? = null

    var listener: ((GenericIntent) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                cached?.let { uri ->
                    Logger.d(TAG, "Delivering cached URI: $uri")
                    value.invoke(parseToIntent(uri))
                    cached = null
                }
            }
        }

    fun onNewUri(uri: String) {
        Logger.d(TAG, "Received URI: $uri")
        val intent = parseToIntent(uri)
        val currentListener = listener
        if (currentListener != null) {
            currentListener.invoke(intent)
            cached = null
        } else {
            Logger.d(TAG, "Listener not ready, caching URI: $uri")
            cached = uri
        }
    }

    /**
     * Second instance: writes [uri] into its restore request [file] before the request is moved into
     * place. The link used to go into a file of its own after the request had been sent, and the
     * running instance, woken by the request, read that file before it existed: Discord and Last.fm
     * logins never finished on Windows and Linux (#2894).
     */
    fun writePendingUri(
        file: Path,
        uri: String,
    ) {
        try {
            Files.writeString(file, uri)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to write pending URI: ${e.message}")
        }
    }

    /** Running instance, on the watcher thread: reads the link before the library deletes [file]. */
    fun readPendingUri(file: Path) {
        try {
            Files.readString(file).trim().takeIf { it.isNotEmpty() }?.let { pendingUri.set(it) }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to read pending URI: ${e.message}")
        }
    }

    /** Delivers the link a second instance forwarded, if any. Called when the window is restored. */
    fun consumePendingUri() {
        pendingUri.getAndSet(null)?.let { onNewUri(it) }
    }

    internal fun resetForTest() {
        cached = null
        listener = null
        pendingUri.set(null)
    }

    /**
     * Converts a raw URI string into a [GenericIntent] that App.kt can process.
     *
     * Conversion rules:
     * 1. simpmusic://open-app?url=<encoded_url>
     *    → Extract the `url` param and use it as intent data
     *
     * 2. simpmusic://watch?v=xxx, simpmusic://playlist?list=xxx, etc.
     *    → Convert to https://simpmusic.org/app/watch?v=xxx format
     *      so App.kt handles it uniformly via the simpmusic.org branch
     *
     * 3. https://simpmusic.org/app/... or YouTube URLs
     *    → Pass through as-is
     */
    private fun parseToIntent(uri: String): GenericIntent {
        val parsed = Uri.parse(uri)

        val actualUri = when {
            // simpmusic://open-app?url=<encoded_url>
            parsed.scheme == "simpmusic" && parsed.host == "open-app" -> {
                val urlParam = parsed.getQueryParameter("url")
                if (urlParam != null) {
                    Logger.d(TAG, "Extracted URL from open-app: $urlParam")
                    Uri.parse(urlParam)
                } else {
                    // simpmusic://open-app without params → just open the app, no navigation
                    Logger.d(TAG, "open-app without URL param, just opening app")
                    null
                }
            }

            // simpmusic://discord-auth?code=…&state=… → pass through untouched: it is Discord's OAuth
            // redirect, which App.kt matches on this exact scheme and host, not a page to open.
            parsed.scheme == "simpmusic" && parsed.host == "discord-auth" -> parsed

            // simpmusic://watch?v=xxx → https://simpmusic.org/app/watch?v=xxx
            // simpmusic://playlist?list=xxx → https://simpmusic.org/app/playlist?list=xxx
            // simpmusic://channel/UCxxx → https://simpmusic.org/app/channel/UCxxx
            // simpmusic://album?id=xxx → https://simpmusic.org/app/album?id=xxx
            parsed.scheme == "simpmusic" && parsed.host != null -> {
                val host = parsed.host!!
                val query = parsed.query?.let { "?$it" } ?: ""
                val pathSuffix = parsed.pathSegments.joinToString("/").let {
                    if (it.isNotEmpty()) "/$it" else ""
                }
                val convertedUrl = "https://simpmusic.org/app/$host$pathSuffix$query"
                Logger.d(TAG, "Converted simpmusic:// to: $convertedUrl")
                Uri.parse(convertedUrl)
            }

            // wordbyword://lastfm-auth?token=xxx → pass through untouched. It must NOT be rewritten
            // to simpmusic.org like the branch above does: App.kt matches on this exact scheme to
            // read the Last.fm request token.
            parsed.scheme == "wordbyword" -> parsed

            // https://simpmusic.org/app/... or YouTube URLs → pass through
            else -> parsed
        }

        return if (actualUri != null) {
            GenericIntent(
                action = "android.intent.action.VIEW",
                data = actualUri,
            )
        } else {
            // No data → just triggers app restore, no navigation
            GenericIntent(action = "android.intent.action.VIEW")
        }
    }
}
