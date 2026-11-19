package com.maxrave.simpmusic.utils

/**
 * Version helpers for the vivo edition (`:vivoAndroidApp`), which ships under `com.spotify.music`
 * so vivo's Origin Island and built-in music widget follow SimpMusic's `MediaSession`.
 *
 * Vivo's built-in music widget inspects `PackageInfo` of `com.spotify.music` and blocks old `2.x`
 * version numbers with "Update Spotify to the latest version" (#2751). The manifest therefore
 * advertises a modern `9.1.28.<code >` Spotify-compatible version while adding SimpMusic's own
 * catalog `versionCode` to [SPOTIFY_COMPAT_VERSION_CODE_BASE] so every release still upgrades
 * monotonically in place, and [appIdentityVersionName] keeps SimpMusic's real version string for
 * internal telemetry and headers.
 */
object VivoSpotifyVersionCompat {
    const val SPOTIFY_COMPAT_VERSION_CODE_BASE = 120_000_000
    const val SPOTIFY_COMPAT_VERSION_PREFIX = "9.1.28"

    fun compatVersionCode(simpMusicVersionCode: Int): Int = SPOTIFY_COMPAT_VERSION_CODE_BASE + simpMusicVersionCode.coerceAtLeast(1)

    fun compatVersionName(simpMusicVersionCode: Int): String = "$SPOTIFY_COMPAT_VERSION_PREFIX.${simpMusicVersionCode.coerceAtLeast(1)}"

    fun appIdentityVersionName(
        buildKonfigVersionName: String,
        isDebug: Boolean,
    ): String =
        if (isDebug && !buildKonfigVersionName.endsWith("-dev")) {
            "$buildKonfigVersionName-dev"
        } else {
            buildKonfigVersionName
        }
}
