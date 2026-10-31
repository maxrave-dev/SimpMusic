package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.promo.Promo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

private val promoJson = Json { ignoreUnknownKeys = true }

/**
 * The cached `promos` block, entry by entry: one entry written with a wrong type (a number where a
 * string belongs, say) is dropped on its own instead of taking every other banner down with it.
 * Nothing at all when the block is missing or is not an array.
 */
fun decodePromos(json: String?): List<Promo> {
    val entries = json?.let { runCatching { promoJson.parseToJsonElement(it).jsonArray }.getOrNull() } ?: return emptyList()
    return entries.mapNotNull { runCatching { promoJson.decodeFromJsonElement(Promo.serializer(), it) }.getOrNull() }
}

/** The operating systems a banner can be limited to through `requires`. */
private val OS_REQUIREMENTS = setOf("android", "macos", "windows", "linux")

/**
 * Every banner this user can be shown, in the config's order, each paired with the image for its
 * language. An entry is skipped when this build cannot fully honour it (no id, no image, a link it
 * cannot open, too new, an unknown requirement) or when the screen its link opens does not exist for
 * this user right now ([isAppLinkAvailable]), so a banner is never shown that a tap could not act on.
 *
 * The operating systems an entry lists in `requires` are alternatives: it shows on any of them, and an
 * entry that lists none shows on every one. Every other requirement must hold. [os] is this device's,
 * from [osRequirementOf]. Builds up to 2.4.0 understand only `local_tracking`, so they hide an entry
 * limited by OS on every device, while still showing the entries around it.
 */
fun eligiblePromos(
    promos: List<Promo>,
    appVersion: String,
    language: String,
    localTrackingEnabled: Boolean,
    youTubeLoggedIn: Boolean,
    os: String?,
): List<Pair<Promo, String>> =
    promos.mapNotNull { promo ->
        // A language left blank falls back to "en" like a missing one.
        val image = promo.image[language]?.takeUnless { it.isBlank() } ?: promo.image["en"]
        val link = promo.link
        if (promo.id.isNullOrBlank() || image.isNullOrBlank() || link == null ||
            !isAppLinkAvailable(link, localTrackingEnabled, youTubeLoggedIn) ||
            !requirementsMet(promo.requires, os, localTrackingEnabled) || !isAtLeast(appVersion, promo.minVersion)
        ) {
            null
        } else {
            promo to image
        }
    }

/** The operating systems in [requires] are alternatives; every other requirement must hold. */
private fun requirementsMet(
    requires: List<String>,
    os: String?,
    localTrackingEnabled: Boolean,
): Boolean {
    val (osRequirements, otherRequirements) = requires.partition { it in OS_REQUIREMENTS }
    return (osRequirements.isEmpty() || os in osRequirements) &&
        otherRequirements.all { it == "local_tracking" && localTrackingEnabled }
}

/**
 * An entry of the `promos` block placed under the lyrics (`"placement": "lyrics"`): a line of [text]
 * (language code -> words, falling back to `en`) instead of a picture. It has no `image`, so every
 * build skips it as a launch banner, and it takes `minVersion` and `requires` like a banner. Its
 * `link` must be an `https://` page (see [lyricsPromo]).
 */
@Serializable
data class LyricsPromo(
    val id: String? = null,
    val placement: String? = null,
    val text: Map<String, String> = emptyMap(),
    val link: String? = null,
    val minVersion: String? = null,
    val requires: List<String> = emptyList(),
)

/** The entries of the cached `promos` block placed under the lyrics, decoded one by one. */
fun decodeLyricsPromos(json: String?): List<LyricsPromo> {
    val entries = json?.let { runCatching { promoJson.parseToJsonElement(it).jsonArray }.getOrNull() } ?: return emptyList()
    return entries
        .mapNotNull { runCatching { promoJson.decodeFromJsonElement(LyricsPromo.serializer(), it) }.getOrNull() }
        .filter { it.placement == LYRICS_PLACEMENT }
}

/**
 * The line to show under the lyrics, paired with its words for [language]: the first entry this
 * build can fully honour, by a banner's rules, that the user has not closed or tapped in this app
 * version ([shown] holds those ids). Null when there is none.
 *
 * Its link must be an `https://` page. A `simpmusic://` link would open its screen behind the player
 * and the fullscreen lyrics page, which the deep-link router does not close.
 */
fun lyricsPromo(
    promos: List<LyricsPromo>,
    shown: Set<String>,
    appVersion: String,
    language: String,
    localTrackingEnabled: Boolean,
    os: String?,
): Pair<LyricsPromo, String>? =
    promos.firstNotNullOfOrNull { promo ->
        val text = promo.text[language]?.takeUnless { it.isBlank() } ?: promo.text["en"]
        if (promo.placement != LYRICS_PLACEMENT || promo.id.isNullOrBlank() || promo.id in shown ||
            text.isNullOrBlank() || promo.link?.startsWith("https://", ignoreCase = true) != true ||
            !requirementsMet(promo.requires, os, localTrackingEnabled) || !isAtLeast(appVersion, promo.minVersion)
        ) {
            null
        } else {
            promo to text
        }
    }

private const val LYRICS_PLACEMENT = "lyrics"

/**
 * The `requires` value for the operating system [osName] names, or null for one a banner cannot be
 * limited to. Pass `Platform.osName()`, not the JVM's `os.name`: on Android `os.name` is "Linux",
 * and only `osName()` says "android" there. On Desktop it is `os.name` ("Mac OS X", "Windows 11", "Linux").
 */
fun osRequirementOf(osName: String): String? {
    val name = osName.lowercase()
    return when {
        name == "android" -> "android"
        name.startsWith("mac") -> "macos"
        name.startsWith("windows") -> "windows"
        name.startsWith("linux") -> "linux"
        else -> null
    }
}

/**
 * The banner ids [stored] lists as shown in [appVersion]. [stored] holds the version on its first
 * line and one id per line after it, so a new version starts with nothing shown.
 */
fun shownPromoIds(
    stored: String?,
    appVersion: String,
): Set<String> {
    val lines = stored?.lines().orEmpty()
    return if (lines.firstOrNull() == appVersion) lines.drop(1).filter { it.isNotBlank() }.toSet() else emptySet()
}

/** [stored] with banner [id] marked as shown in [appVersion], ready to save. */
fun withShownPromo(
    stored: String?,
    appVersion: String,
    id: String,
): String = (listOf(appVersion) + (shownPromoIds(stored, appVersion) + id)).joinToString("\n")

/** [version] is [minimum] or newer, compared number by number. No minimum means any version. */
internal fun isAtLeast(
    version: String,
    minimum: String?,
): Boolean = minimum.isNullOrBlank() || compareVersions(version, minimum) >= 0
