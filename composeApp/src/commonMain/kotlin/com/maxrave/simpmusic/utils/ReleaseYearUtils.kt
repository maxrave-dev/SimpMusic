package com.maxrave.simpmusic.utils

/**
 * Normalizes a release year string coming from YouTube Music parsers or cached entities.
 *
 * Upstream album parsing converts nullable `Int?` release years with `.toString()`, which turns a
 * missing year into the literal `"null"` string (#2498). Treating blank, `"null"`, or `"0"` values
 * as absent lets UI screens hide the missing year cleanly instead of rendering `"null • Album"`.
 */
fun normalizeReleaseYear(rawYear: String?): String? {
    val trimmed = rawYear?.trim() ?: return null
    if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true) || trimmed == "0") {
        return null
    }
    return trimmed
}

inline fun formatYearAndCategory(
    rawYear: String?,
    category: String,
    formatWithYear: (year: String, category: String) -> String,
): String {
    val validYear = normalizeReleaseYear(rawYear)
    return if (validYear != null) {
        formatWithYear(validYear, category)
    } else {
        category
    }
}
