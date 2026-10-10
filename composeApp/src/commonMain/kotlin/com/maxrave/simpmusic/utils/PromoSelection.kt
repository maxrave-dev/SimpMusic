package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.promo.Promo
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

/**
 * Every banner this user can be shown, in the config's order, each paired with the image for its
 * language. An entry is skipped when this build cannot fully honour it (no id, no image, a link it
 * cannot open, too new, an unknown requirement) or when the screen its link opens does not exist for
 * this user right now ([isAppLinkAvailable]), so a banner is never shown that a tap could not act on.
 */
fun eligiblePromos(
    promos: List<Promo>,
    appVersion: String,
    language: String,
    localTrackingEnabled: Boolean,
    youTubeLoggedIn: Boolean,
): List<Pair<Promo, String>> =
    promos.mapNotNull { promo ->
        // A language left blank falls back to "en" like a missing one.
        val image = promo.image[language]?.takeUnless { it.isBlank() } ?: promo.image["en"]
        val requirementsMet = promo.requires.all { it == "local_tracking" && localTrackingEnabled }
        val link = promo.link
        if (promo.id.isNullOrBlank() || image.isNullOrBlank() || link == null ||
            !isAppLinkAvailable(link, localTrackingEnabled, youTubeLoggedIn) ||
            !requirementsMet || !isAtLeast(appVersion, promo.minVersion)
        ) {
            null
        } else {
            promo to image
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
