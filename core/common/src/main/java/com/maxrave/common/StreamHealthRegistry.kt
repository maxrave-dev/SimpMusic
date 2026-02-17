package com.maxrave.common

import java.util.concurrent.ConcurrentHashMap

/**
 * Shared in-memory health tracker for stream candidates.
 *
 * The same stream URL/itag can be seen by scraper preflight, resolver preflight and runtime
 * ExoPlayer fetches. This registry lets those layers share failure memory and avoid immediate
 * reuse of recently poisoned candidates.
 */
object StreamHealthRegistry {
    private const val URL_FAIL_TTL_MS = 2 * 60_000L
    private const val ITAG_FAIL_TTL_MS = 3 * 60_000L
    private const val MEDIA_COOLDOWN_TTL_MS = 20_000L
    private const val MEDIA_CLIENT_FAIL_TTL_MS = 10 * 60_000L
    private const val SESSION_CLIENT_FAIL_TTL_MS = 45 * 60_000L

    private val badUrlUntilMs = ConcurrentHashMap<String, Long>()
    private val badMediaItagUntilMs = ConcurrentHashMap<String, Long>()
    private val mediaCooldownUntilMs = ConcurrentHashMap<String, Long>()
    private val badMediaClientUntilMs = ConcurrentHashMap<String, Long>()
    private val badSessionClientUntilMs = ConcurrentHashMap<String, Long>()
    private val urlToMediaItagClient = ConcurrentHashMap<String, Triple<String, Int?, String?>>()

    private fun nowMs(): Long = System.currentTimeMillis()

    private fun normalizeMediaId(mediaId: String): String =
        mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)

    private fun mediaItagKey(
        mediaId: String,
        itag: Int?,
    ): String? {
        val normalized = normalizeMediaId(mediaId).trim()
        if (normalized.isBlank() || itag == null) return null
        return "$normalized|$itag"
    }

    private fun normalizeClientName(clientName: String?): String? =
        clientName
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }

    private fun mediaClientKey(
        mediaId: String,
        clientName: String?,
    ): String? {
        val normalizedMediaId = normalizeMediaId(mediaId).trim()
        val normalizedClient = normalizeClientName(clientName)
        if (normalizedMediaId.isBlank() || normalizedClient.isNullOrBlank()) return null
        return "$normalizedMediaId|$normalizedClient"
    }

    private fun extractClientFromUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val match = Regex("""[?&]c=([^&]+)""", RegexOption.IGNORE_CASE).find(url) ?: return null
        return normalizeClientName(match.groupValues.getOrNull(1))
    }

    fun rememberCandidate(
        mediaId: String,
        itag: Int?,
        url: String?,
    ) {
        if (url.isNullOrBlank()) return
        val normalized = normalizeMediaId(mediaId).trim()
        if (normalized.isBlank()) return
        urlToMediaItagClient[url] = Triple(normalized, itag, extractClientFromUrl(url))
    }

    fun markUrlFailure(
        url: String?,
        ttlMs: Long = URL_FAIL_TTL_MS,
    ) {
        if (url.isNullOrBlank()) return
        badUrlUntilMs[url] = nowMs() + ttlMs
        urlToMediaItagClient[url]?.let { (mediaId, itag, clientName) ->
            markMediaItagFailure(mediaId, itag)
            markMediaFailure(mediaId)
            markClientFailure(mediaId, clientName)
        }
    }

    fun markMediaItagFailure(
        mediaId: String,
        itag: Int?,
        ttlMs: Long = ITAG_FAIL_TTL_MS,
    ) {
        val key = mediaItagKey(mediaId, itag) ?: return
        badMediaItagUntilMs[key] = nowMs() + ttlMs
    }

    fun markMediaFailure(
        mediaId: String,
        ttlMs: Long = MEDIA_COOLDOWN_TTL_MS,
    ) {
        val normalized = normalizeMediaId(mediaId).trim()
        if (normalized.isBlank()) return
        mediaCooldownUntilMs[normalized] = nowMs() + ttlMs
    }

    fun markClientFailure(
        mediaId: String,
        clientName: String?,
        ttlMs: Long = MEDIA_CLIENT_FAIL_TTL_MS,
    ) {
        val normalizedClient = normalizeClientName(clientName)
        val effectiveTtlMs =
            when (normalizedClient) {
                // VR streams can fail transiently on watches; keep penalty short so retries are not starved.
                "ANDROID_VR" -> minOf(ttlMs, 8_000L)
                else -> ttlMs
            }
        val mediaClientKey = mediaClientKey(mediaId, clientName)
        if (!mediaClientKey.isNullOrBlank()) {
            badMediaClientUntilMs[mediaClientKey] = nowMs() + effectiveTtlMs
        }
        if (normalizedClient == "IOS") {
            markSessionClientFailure(clientName)
        }
    }

    fun markSessionClientFailure(
        clientName: String?,
        ttlMs: Long = SESSION_CLIENT_FAIL_TTL_MS,
    ) {
        val normalized = normalizeClientName(clientName) ?: return
        badSessionClientUntilMs[normalized] = nowMs() + ttlMs
    }

    fun markClientFailureFromUrl(
        mediaId: String,
        url: String?,
    ) {
        markClientFailure(mediaId, extractClientFromUrl(url))
    }

    fun markUrlHealthy(url: String?) {
        if (url.isNullOrBlank()) return
        badUrlUntilMs.remove(url)
    }

    fun isUrlBlocked(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val until = badUrlUntilMs[url] ?: return false
        val now = nowMs()
        if (until <= now) {
            badUrlUntilMs.remove(url)
            return false
        }
        return true
    }

    fun isMediaItagBlocked(
        mediaId: String,
        itag: Int?,
    ): Boolean {
        val key = mediaItagKey(mediaId, itag) ?: return false
        val until = badMediaItagUntilMs[key] ?: return false
        val now = nowMs()
        if (until <= now) {
            badMediaItagUntilMs.remove(key)
            return false
        }
        return true
    }

    fun isMediaCoolingDown(mediaId: String): Boolean {
        val normalized = normalizeMediaId(mediaId).trim()
        if (normalized.isBlank()) return false
        val until = mediaCooldownUntilMs[normalized] ?: return false
        val now = nowMs()
        if (until <= now) {
            mediaCooldownUntilMs.remove(normalized)
            return false
        }
        return true
    }

    fun isClientBlocked(
        mediaId: String,
        clientName: String?,
    ): Boolean {
        val normalizedClient = normalizeClientName(clientName) ?: return false
        val now = nowMs()

        val sessionUntil = badSessionClientUntilMs[normalizedClient]
        if (sessionUntil != null) {
            if (sessionUntil <= now) {
                badSessionClientUntilMs.remove(normalizedClient)
            } else {
                return true
            }
        }

        val key = mediaClientKey(mediaId, normalizedClient) ?: return false
        val mediaUntil = badMediaClientUntilMs[key] ?: return false
        if (mediaUntil <= now) {
            badMediaClientUntilMs.remove(key)
            return false
        }
        return true
    }

    fun isClientBlockedByUrl(
        mediaId: String,
        url: String?,
    ): Boolean = isClientBlocked(mediaId, extractClientFromUrl(url))

    fun urlClient(url: String?): String? = extractClientFromUrl(url)

    fun candidatePenalty(
        mediaId: String,
        itag: Int?,
        url: String?,
    ): Int {
        var penalty = 0
        if (isUrlBlocked(url)) penalty += 1000
        if (isMediaItagBlocked(mediaId, itag)) penalty += 400
        if (isMediaCoolingDown(mediaId)) penalty += 60
        if (isClientBlockedByUrl(mediaId, url)) penalty += 2000
        return penalty
    }
}
