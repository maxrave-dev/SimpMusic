package org.simpmusic.lastfm

import com.maxrave.logger.Logger

// NON-LASTFM build: direct scrobbling is not available in this build flavour, because a FOSS build
// ships no API secret. Every declaration mirrors the real module so callers never need to branch on
// build flavour — `isLastfmAvailable()` returning false is what hides the feature in the UI.

data class LastfmSession(
    val username: String,
    val sessionKey: String,
)

data class LastfmTrack(
    val artist: String,
    val track: String,
    val album: String? = null,
    val albumArtist: String? = null,
    val durationSeconds: Int? = null,
)

sealed interface LastfmOutcome {
    data object Ok : LastfmOutcome

    data class Ignored(
        val code: Int,
        val message: String,
    ) : LastfmOutcome

    data class Error(
        val code: Int,
        val message: String,
    ) : LastfmOutcome {
        val needsReauth: Boolean get() = code == ERROR_INVALID_SESSION

        val retryable: Boolean get() = code == 11 || code == 16 || code == 29
    }

    companion object {
        const val ERROR_INVALID_SESSION = 9
    }
}

private const val TAG = "Lastfm"

private const val UNAVAILABLE = "NON-LASTFM build: Last.fm is not available"

fun configLastfm(
    key: String,
    secret: String,
) {
    Logger.d(TAG, UNAVAILABLE)
}

fun isLastfmAvailable(): Boolean = false

fun authorizeUrl(): String? = null

suspend fun completeLogin(token: String): LastfmSession? {
    Logger.d(TAG, UNAVAILABLE)
    return null
}

suspend fun updateNowPlaying(
    sessionKey: String,
    track: LastfmTrack,
): LastfmOutcome = LastfmOutcome.Error(0, UNAVAILABLE)

suspend fun scrobble(
    sessionKey: String,
    track: LastfmTrack,
    startedAtEpochSeconds: Long,
): LastfmOutcome = LastfmOutcome.Error(0, UNAVAILABLE)

suspend fun setLoved(
    sessionKey: String,
    artist: String,
    track: String,
    loved: Boolean,
): LastfmOutcome = LastfmOutcome.Error(0, UNAVAILABLE)
