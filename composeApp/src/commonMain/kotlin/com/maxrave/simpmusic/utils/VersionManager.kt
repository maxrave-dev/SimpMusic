package com.maxrave.simpmusic.utils

import com.maxrave.simpmusic.BuildKonfig

object VersionManager {
    private var versionName: String? = null

    fun initialize() {
        if (versionName == null) {
            versionName =
                try {
                    BuildKonfig.versionName
                } catch (_: Exception) {
                    String()
                }
        }
    }

    fun getVersionName(): String = removeDevSuffix(versionName ?: String())

    /** True when [tag] names a release newer than this build. */
    fun isBehind(tag: String): Boolean = isNewerRelease(current = getVersionName(), tag = tag)

    private fun removeDevSuffix(versionName: String): String {
        return if (versionName.endsWith("-dev")) {
            versionName.replace("-dev", "")
        } else {
            versionName
        }
    }
}

/**
 * True when release [tag] should be offered to a build running [current]. GitHub tags carry a leading
 * "v" ("v2.3.0") and F-Droid's do not ("2.3.0"). It is offered unless it is this very version or this
 * build is already ahead of it (dev and beta builds run ahead of the latest release). A hotfix
 * ("2.3.0-hf") has the same numbers as the release it fixes, so only its name tells it apart.
 */
internal fun isNewerRelease(
    current: String,
    tag: String,
): Boolean {
    val release = tag.trim().removePrefix("v")
    return release != current && compareVersions(current, release) <= 0
}

/** Dotted versions compared number by number, so "2.10.0" is newer than "2.9.0". A suffix such as "-hf" is ignored. */
internal fun compareVersions(
    a: String,
    b: String,
): Int {
    val x = versionNumbers(a)
    val y = versionNumbers(b)
    for (i in 0 until maxOf(x.size, y.size)) {
        val diff = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return 0
}

private fun versionNumbers(version: String): List<Int> = version.substringBefore('-').split('.').map { it.trim().toIntOrNull() ?: 0 }
