package com.maxrave.simpmusic.utils

import com.eygraber.uri.Uri
import com.maxrave.common.LibraryChipType

// Every path the deep-link router in App.kt opens; anything else gets its "link not supported" toast.
// A path added there must be added here too, or no launch banner can point at it.
private val APP_LINK_PATHS = setOf("notification", "watch", "playlist", "channel", "c", "album", "library", "analytics", "taste")

/**
 * Whether the screen a `simpmusic://` link opens exists for this user right now. Analytics, the
 * Wrapped chip and the AI taste card (`taste`) exist only while local tracking is on, and the YouTube
 * playlists chip only while signed in. The router turns such a link away with a toast, and a launch
 * banner pointing at one is not shown, so the two always agree.
 */
fun isAppLinkAvailable(
    appPath: String?,
    libraryTab: LibraryChipType?,
    localTrackingOn: Boolean,
    youTubeLoggedIn: Boolean,
): Boolean =
    when {
        appPath !in APP_LINK_PATHS -> false
        appPath == "analytics" || appPath == "taste" -> localTrackingOn
        appPath == "library" && libraryTab == LibraryChipType.WRAPPED -> localTrackingOn
        appPath == "library" && libraryTab == LibraryChipType.YOUTUBE_MUSIC_PLAYLIST -> youTubeLoggedIn
        else -> true
    }

/**
 * [isAppLinkAvailable] for a link written out in full, as a banner carries it: an `https://` page
 * always is, and a link that is neither that nor `simpmusic://` never is.
 */
fun isAppLinkAvailable(
    link: String,
    localTrackingOn: Boolean,
    youTubeLoggedIn: Boolean,
): Boolean {
    if (link.startsWith("https://", ignoreCase = true)) return true
    if (!link.startsWith("simpmusic://")) return false
    val uri = runCatching { Uri.parse(link) }.getOrNull() ?: return false
    return isAppLinkAvailable(uri.host, libraryTabOf(uri), localTrackingOn, youTubeLoggedIn)
}

/** The Library chip a link asks for with `?tab=`. A `?type=` opens one of the lists instead and wins. */
fun libraryTabOf(link: Uri): LibraryChipType? =
    if (link.getQueryParameter("type").isNullOrBlank()) {
        link.getQueryParameter("tab")?.let { LibraryChipType.fromStringValue(it) }
    } else {
        null
    }
