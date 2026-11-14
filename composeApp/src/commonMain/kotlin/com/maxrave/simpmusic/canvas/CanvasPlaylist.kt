package com.maxrave.simpmusic.canvas

import java.net.URI

/** One remote resource a canvas playlist points at, and the local file it is saved as. */
internal data class CanvasPlaylistEntry(
    val sourceUrl: String,
    val fileName: String,
)

/** A playlist rewritten to relative local file names, plus the resources to download. */
internal data class RewrittenCanvasPlaylist(
    val text: String,
    val entries: List<CanvasPlaylistEntry>,
)

private val URI_ATTRIBUTE = Regex("""URI="([^"]*)"""")

/**
 * Rewrites a media playlist so every URI points at a file next to it.
 *
 * Apple Music's animated artwork is a VOD HLS stream: one media playlist, an optional
 * `EXT-X-MAP` initialisation segment, optional `EXT-X-KEY` entries, and media segments. The
 * player has to open the same playlist from disk when the device is offline, so every remote
 * URI is downloaded and replaced with the local file name. Relative names are resolved by the
 * HLS parser against the playlist's own `file://` url, so no absolute paths are written into it.
 *
 * A master playlist (`EXT-X-STREAM-INF`) is rejected: the repository already resolves the ladder
 * to one rendition, and choosing between codecs here would duplicate that policy.
 */
internal fun rewriteCanvasPlaylist(
    playlist: String,
    playlistUrl: String,
): RewrittenCanvasPlaylist {
    require(!playlist.contains("#EXT-X-STREAM-INF")) {
        "Master playlist must be resolved before it is pinned"
    }

    // Insertion order keeps the generated names stable and readable in logs.
    val entries = LinkedHashMap<String, String>()
    val rewritten = StringBuilder(playlist.length)

    playlist.lineSequence().forEach { rawLine ->
        val line = rawLine.removeSuffix("\r")
        val rewrittenLine =
            when {
                line.startsWith("#EXT-X-MAP:") ||
                    line.startsWith("#EXT-X-KEY:") ||
                    line.startsWith("#EXT-X-SESSION-KEY:") ||
                    line.startsWith("#EXT-X-MEDIA:") -> rewriteUriAttribute(line, playlistUrl, entries)
                line.isBlank() || line.startsWith("#") -> line
                else -> rewriteSegment(line, playlistUrl, entries)
            }
        rewritten.append(rewrittenLine).append('\n')
    }

    return RewrittenCanvasPlaylist(
        text = rewritten.toString(),
        entries = entries.map { (sourceUrl, fileName) -> CanvasPlaylistEntry(sourceUrl, fileName) },
    )
}

private fun rewriteUriAttribute(
    line: String,
    playlistUrl: String,
    entries: MutableMap<String, String>,
): String {
    val match = URI_ATTRIBUTE.find(line) ?: return line
    val reference = match.groupValues[1]
    if (reference.isEmpty()) return line
    val sourceUrl = resolveCanvasUrl(playlistUrl, reference)
    val fileName = entries.getOrPut(sourceUrl) { canvasPartFileName(sourceUrl, entries.size) }
    return line.replaceRange(match.range, "URI=\"$fileName\"")
}

private fun rewriteSegment(
    line: String,
    playlistUrl: String,
    entries: MutableMap<String, String>,
): String {
    val reference = line.trim()
    if (reference.isEmpty()) return line
    val sourceUrl = resolveCanvasUrl(playlistUrl, reference)
    return entries.getOrPut(sourceUrl) { canvasPartFileName(sourceUrl, entries.size) }
}

private fun resolveCanvasUrl(
    baseUrl: String,
    reference: String,
): String =
    runCatching { URI(baseUrl).resolve(reference).toString() }
        .getOrElse { reference }
