package com.maxrave.simpmusic.artwork

import java.security.MessageDigest

/** Directory under `filesDir`; app-private and untouched by cache clearing. */
internal const val ARTWORK_DIRECTORY = "artwork"

/** `i.ytimg.com/vi/<id>/<anything>.jpg` — the id alone identifies the image. */
private val ARTWORK_VIDEO_ID = Regex("/vi/(?<videoId>[^/]+)/")

/** Everything after `=w120-h120` is a rendering instruction, not part of the image's identity. */
private val ARTWORK_SIZE_SEGMENT = Regex("=w\\d+-h\\d+[^/]*$")

/**
 * The file name a cover is pinned under.
 *
 * YouTube's own thumbnails are named after the video id, so `maxresdefault.jpg` and
 * `hqdefault.jpg` of the same song resolve to one file. YouTube Music's
 * `lh3.googleusercontent.com` URLs carry the requested size in the query, so their size segment
 * is stripped and the rest is hashed to stay inside file-name limits.
 */
internal fun artworkFileName(url: String): String? {
    val value = url.trim()
    if (value.isEmpty()) return null
    ARTWORK_VIDEO_ID.find(value)?.groups?.get("videoId")?.value?.let { return "$it.jpg" }
    val canonical = ARTWORK_SIZE_SEGMENT.replace(value, "")
    if (canonical.isEmpty()) return null
    return "${sha256(canonical)}.jpg"
}

private fun sha256(value: String): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(value.encodeToByteArray())
        .joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
