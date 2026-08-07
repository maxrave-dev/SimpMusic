package org.simpmusic.lastfm

import com.maxrave.logger.Logger

// LAST.FM build: the real scrobbler. `lastfm-empty` mirrors every declaration in this file as a
// no-op, so callers never branch on the build flavour — see `cast` / `cast-empty` for the same
// arrangement.

/** A logged-in Last.fm account. The session key has no expiry; the user revokes it from last.fm. */
data class LastfmSession(
    val username: String,
    val sessionKey: String,
)

/** What SimpMusic knows about a track, in the shape Last.fm's parameters expect. */
data class LastfmTrack(
    val artist: String,
    val track: String,
    val album: String? = null,
    val albumArtist: String? = null,
    val durationSeconds: Int? = null,
)

/**
 * The outcome of a write call.
 *
 * [Ignored] is the case worth spelling out: Last.fm answers `status="ok"` even when it threw the
 * scrobble away, and only says so in `ignoredMessage`. Treating a 200 as success would hide bad
 * metadata forever — codes 1 and 2 mean the artist or track name failed Last.fm's filter, which is
 * exactly what a polluted artist like "13M plays" produces.
 */
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
        /** Code 9 means the stored session key is dead — the user has to log in again. */
        val needsReauth: Boolean get() = code == ERROR_INVALID_SESSION

        /** 11 offline, 16 temporary, 29 rate limited. Every other code is a malformed request. */
        val retryable: Boolean get() = code == 11 || code == 16 || code == 29
    }

    companion object {
        const val ERROR_INVALID_SESSION = 9
    }
}

private const val TAG = "Lastfm"

private var apiKey: String = ""
private var sharedSecret: String = ""

/**
 * Hands the module its credentials.
 *
 * They arrive from the caller rather than living here because BuildKonfig generates into
 * `composeApp`, which depends on this module and not the other way round — the same reason
 * `configCrashlytics` takes the Sentry DSN as a parameter.
 */
fun configLastfm(
    key: String,
    secret: String,
) {
    apiKey = key
    sharedSecret = secret
    Logger.d(TAG, "Last.fm configured: ${if (isLastfmAvailable()) "credentials present" else "no credentials"}")
}

/**
 * Whether this build can talk to Last.fm at all.
 *
 * False in a FOSS build, where the empty module answers for this function, and also false in a full
 * build whose `local.properties` carries no key — so the settings entry hides itself rather than
 * offering a login that could never succeed.
 */
fun isLastfmAvailable(): Boolean = apiKey.isNotEmpty() && sharedSecret.isNotEmpty()

/**
 * Step one: where to send the user so they can grant access.
 *
 * This is Last.fm's **web** flow, and the missing `token` parameter is the whole point of it.
 * Last.fm generates the token itself and redirects to the callback registered on the API account
 * with `?token=` appended. The desktop flow — call `auth.getToken` first, then open this URL with
 * `&token=` already on it — tells Last.fm the app is holding the token, so it renders a "return to
 * the application" page and never calls the callback at all.
 *
 * No `cb` parameter is passed either: the callback on the API account is the one to use.
 */
fun authorizeUrl(): String? {
    if (!isLastfmAvailable()) return null
    return "https://www.last.fm/api/auth/?api_key=$apiKey"
}

/**
 * Step two: trade the token that came back on the callback for a session key.
 *
 * A token is user- and account-specific, lives 60 minutes, and is consumed by this call — so a
 * failure here means sending the user through [authorizeUrl] again, not retrying the same token.
 */
suspend fun completeLogin(token: String): LastfmSession? {
    if (!isLastfmAvailable()) return null
    return LastfmClient.getSession(apiKey, sharedSecret, token)
}

/** Tells Last.fm what is playing right now. Does not affect charts, and is not worth retrying. */
suspend fun updateNowPlaying(
    sessionKey: String,
    track: LastfmTrack,
): LastfmOutcome {
    if (!isLastfmAvailable()) return LastfmOutcome.Error(0, "Last.fm is not configured")
    return LastfmClient.updateNowPlaying(apiKey, sharedSecret, sessionKey, track)
}

/**
 * Records a play.
 *
 * [startedAtEpochSeconds] is when playback STARTED. Last.fm's own docs disagree with themselves
 * here — the method page says the track started, the scrobbling guide says it finished — and every
 * scrobbler in the wild sends the start time, so that is what this takes.
 */
suspend fun scrobble(
    sessionKey: String,
    track: LastfmTrack,
    startedAtEpochSeconds: Long,
): LastfmOutcome {
    if (!isLastfmAvailable()) return LastfmOutcome.Error(0, "Last.fm is not configured")
    return LastfmClient.scrobble(apiKey, sharedSecret, sessionKey, track, startedAtEpochSeconds)
}

/** Mirrors a like onto Last.fm's loved tracks. */
suspend fun setLoved(
    sessionKey: String,
    artist: String,
    track: String,
    loved: Boolean,
): LastfmOutcome {
    if (!isLastfmAvailable()) return LastfmOutcome.Error(0, "Last.fm is not configured")
    return LastfmClient.setLoved(apiKey, sharedSecret, sessionKey, artist, track, loved)
}
