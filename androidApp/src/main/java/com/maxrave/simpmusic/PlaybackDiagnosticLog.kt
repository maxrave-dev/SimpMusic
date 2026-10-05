package com.maxrave.simpmusic

import android.os.Build
import android.util.Log
import java.time.Instant
import java.util.ArrayDeque

/**
 * Keeps a small, in-memory rolling window of SimpMusic playback logs for user-requested debugging.
 * This deliberately does not read the system logcat, which an ordinary app is not allowed to do.
 */
object PlaybackDiagnosticLog {
    private const val MAX_ENTRIES = 3_000
    private const val MAX_CHARACTERS = 1_000_000
    private const val MAX_ENTRY_CHARACTERS = 12_000

    private val lock = Any()
    private val entries = ArrayDeque<String>()
    private var totalCharacters = 0

    private val playbackTagFragments =
        listOf(
            "player",
            "stream",
            "youtube",
            "newpipe",
            "media",
            "queue",
            "crossfade",
            "servicehandler",
            "musicsource",
            "radio",
            "songrepository",
            "loadmore",
            "related",
        )

    private val urlPattern = Regex("https?://[^\\s\\\"'<>]+")
    private val sensitiveHeaderPattern = Regex("(?i)(\\b(?:cookie|authorization)\\s*:\\s*)[^\\r\\n]*")
    private val googleCookieValuePattern =
        Regex("(?i)\\b((?:__Secure-[A-Za-z0-9_-]+|SID|HSID|SSID|APISID|SAPISID|LSID|LOGIN_INFO|YSC|VISITOR_[A-Z0-9_]+|SAPISIDHASH)(?:CC)?\\s*[:=]\\s*)[^;,\\s'\"]+")
    private val visitorDataPattern = Regex("(?i)(\\\"(?:visitorData|visitor_data)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")")
    private val sapisidHashPattern = Regex("(?i)(SAPISIDHASH\\s+)[^\\s'\"]+")
    private val secretFieldPattern =
        Regex("(?i)\\b(authorization|cookie|access[_-]?token|refresh[_-]?token|signature|sig|sparams|token|cpn)\\b(\\s*[:=]\\s*)([^&\\s,;]+)")

    fun record(
        severity: String,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (!isPlaybackRelated(tag)) return

        val stackTrace = throwable?.let(Log::getStackTraceString).orEmpty()
        val details = if (stackTrace.isEmpty()) message else "$message\n$stackTrace"
        val safeDetails = redactSecrets(details).take(MAX_ENTRY_CHARACTERS)
        val line = "${Instant.now()} $severity/$tag: $safeDetails"

        synchronized(lock) {
            entries.addLast(line)
            totalCharacters += line.length + 1
            while (entries.size > MAX_ENTRIES || totalCharacters > MAX_CHARACTERS) {
                val removed = entries.removeFirst()
                totalCharacters -= removed.length + 1
            }
        }
    }

    fun snapshot(): String =
        synchronized(lock) {
            buildString {
                appendLine("SimpMusic playback diagnostics")
                appendLine("App version: ${BuildConfig.VERSION_NAME}")
                appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Scope: recent SimpMusic playback-related logs only; this is not full device logcat.")
                appendLine("URL queries, cookie/authorization headers, and common auth tokens are redacted.")
                appendLine("Track names and playback/queue identifiers may still appear.")
                appendLine()
                if (entries.isEmpty()) {
                    appendLine("No playback log entries were captured.")
                } else {
                    entries.forEach(::appendLine)
                }
            }
        }

    private fun isPlaybackRelated(tag: String): Boolean {
        val normalized = tag.lowercase()
        return playbackTagFragments.any(normalized::contains)
    }

    private fun redactSecrets(text: String): String {
        val urlsRedacted =
            urlPattern.replace(text) { match ->
                val fullUrl = match.value
                val trimmed = fullUrl.trimEnd(',', '.', ')', ']', '}')
                val trailingPunctuation = fullUrl.drop(trimmed.length)
                val safeUrl =
                    if ('?' in trimmed) {
                        "${trimmed.substringBefore('?')}?<query-redacted>"
                    } else {
                        trimmed
                    }
                safeUrl + trailingPunctuation
            }
        val headersRedacted =
            sensitiveHeaderPattern.replace(urlsRedacted) { match ->
                "${match.groupValues[1]}<redacted>"
            }
        val cookieValuesRedacted =
            googleCookieValuePattern.replace(headersRedacted) { match ->
                "${match.groupValues[1]}<redacted>"
            }
        val visitorDataRedacted =
            visitorDataPattern.replace(cookieValuesRedacted) { match ->
                "${match.groupValues[1]}<redacted>${match.groupValues[2]}"
            }
        val sapisidHashRedacted =
            sapisidHashPattern.replace(visitorDataRedacted) { match ->
                "${match.groupValues[1]}<redacted>"
            }
        return secretFieldPattern.replace(sapisidHashRedacted) { match ->
            "${match.groupValues[1]}${match.groupValues[2]}<redacted>"
        }
    }
}
