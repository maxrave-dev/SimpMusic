package com.maxrave.simpmusic

import com.eygraber.uri.Uri
import com.maxrave.data.io.getHomeFolderPath
import com.maxrave.domain.data.model.intent.GenericIntent
import com.maxrave.logger.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Singleton to handle deep link URIs on Desktop.
 * Caches URI if app UI is not ready yet, delivers immediately if listener is set.
 *
 * Also provides file-based IPC for single-instance deep link forwarding:
 * when a second instance launches with a URI, it writes the URI to a temp file,
 * and the first instance reads it on restore.
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

    private val pendingUriFile: File by lazy {
        File(getHomeFolderPath(listOf(".simpmusic")), "simpmusic_pending_deeplink.txt").also {
            it.parentFile?.mkdirs()
        }
    }

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
     * Write URI to a pending file so the running (first) instance can pick it up.
     * Called by the second instance before waking the primary instance.
     */
    fun writePendingUri(
        uri: String,
        targetFile: File = pendingUriFile,
    ) {
        try {
            targetFile.parentFile?.mkdirs()
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.${System.nanoTime()}.tmp")
            try {
                tempFile.writeText(uri)
                try {
                    Files.move(
                        tempFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                } catch (_: Exception) {
                    Files.move(
                        tempFile.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                }
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
            Logger.d(TAG, "Wrote pending URI to file: $uri")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to write pending URI: ${e.message}")
        }
    }

    /**
     * Read and consume the pending URI file written by a second instance.
     * Called by the first instance when it receives a restore request.
     */
    fun consumePendingUri(sourceFile: File = pendingUriFile): Boolean {
        return try {
            if (!sourceFile.exists()) return false
            val uri = sourceFile.readText().trim()
            if (uri.isNotEmpty()) {
                sourceFile.delete()
                Logger.d(TAG, "Consumed pending URI from file: $uri")
                onNewUri(uri)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to read pending URI: ${e.message}")
            false
        }
    }

    /**
     * Remove a staged pending URI file. When [expectedUri] is supplied, only deletes the file if
     * its content still matches [expectedUri], preserving any newer URI written by a second
     * instance during cold start.
     */
    fun clearPendingUri(
        expectedUri: String? = null,
        targetFile: File = pendingUriFile,
    ) {
        try {
            if (!targetFile.exists()) return
            if (expectedUri == null || targetFile.readText().trim() == expectedUri) {
                targetFile.delete()
            }
        } catch (_: Exception) {
        }
    }

    internal fun resetForTest() {
        cached = null
        listener = null
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
