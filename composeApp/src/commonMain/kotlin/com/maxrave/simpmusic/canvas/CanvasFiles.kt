package com.maxrave.simpmusic.canvas

/** Directory under `filesDir`; app-private and untouched by cache clearing. */
internal const val CANVAS_DIRECTORY = "canvas"

internal const val CANVAS_PLAYLIST_FILE = "index.m3u8"
internal const val CANVAS_VIDEO_FILE = "canvas.mp4"
internal const val CANVAS_IMAGE_FILE = "canvas.jpg"
internal const val CANVAS_THUMB_FILE = "thumb.jpg"
internal const val CANVAS_SOURCE_FILE = "source.txt"

/** YouTube ids are `[A-Za-z0-9_-]`; anything else is not a directory name this store will use. */
private val CANVAS_ID = Regex("[A-Za-z0-9_-]+")

internal fun isCanvasId(videoId: String): Boolean = CANVAS_ID.matches(videoId)

/** `file://` url ExoPlayer and Coil can open. */
internal fun canvasFileUrl(path: String): String = "file://$path"

/** The name a downloaded HLS part is stored under, keeping its extension for the extractors. */
internal fun canvasPartFileName(
    url: String,
    index: Int,
): String {
    val path = url.substringBefore('?').substringBefore('#')
    val extension = path.substringAfterLast('.', "").lowercase()
    val safe = extension.takeIf { it.length in 1..5 && it.all(Char::isLetterOrDigit) }
    return "part-$index.${safe ?: "bin"}"
}
