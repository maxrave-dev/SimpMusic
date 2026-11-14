package com.maxrave.simpmusic.canvas

/**
 * A canvas pinned for a downloaded song.
 *
 * [url] is a local `file://` url: a rewritten HLS playlist for Apple Music's animated artwork,
 * or the downloaded MP4/image for a Spotify canvas. [thumbUrl] is the preview still Apple Music
 * sends with its animated artwork, pinned so the frame has something to show before the first
 * video frame while offline.
 */
data class PinnedCanvas(
    val url: String,
    val thumbUrl: String?,
    val isVideo: Boolean,
    val source: CanvasSource,
) {
    /** Whether the setting that produced this canvas is still on. */
    fun isEnabled(
        amEnabled: Boolean,
        spotifyEnabled: Boolean,
    ): Boolean = source.isEnabled(amEnabled, spotifyEnabled)
}

/** Which setting produced a pinned canvas; the matching toggle is checked before it is shown. */
enum class CanvasSource {
    AM,
    SPOTIFY,
    ;

    fun isEnabled(
        amEnabled: Boolean,
        spotifyEnabled: Boolean,
    ): Boolean =
        when (this) {
            AM -> amEnabled
            SPOTIFY -> spotifyEnabled
        }
}

/**
 * Canvas files for downloaded songs.
 *
 * Like [com.maxrave.simpmusic.artwork.ArtworkStore], this is only written for songs that are
 * downloaded: everything else keeps using the online canvas and the shared media cache.
 */
interface CanvasStore {
    /**
     * Downloads and pins the canvas at [url] for [videoId]. Returns the pinned canvas, or null
     * when it could not be downloaded. Calling this again for an already pinned song is cheap.
     */
    suspend fun save(
        videoId: String,
        url: String,
        thumbUrl: String? = null,
        isVideo: Boolean = true,
    ): PinnedCanvas?

    /** The canvas pinned for [videoId], or null when there is none. */
    suspend fun pinned(videoId: String): PinnedCanvas?

    /** Removes the pinned canvas for [videoId]. */
    suspend fun delete(videoId: String)

    /** Removes every pinned canvas whose song id is not in [videoIds]. */
    suspend fun retainOnly(videoIds: Collection<String>)

    /** Removes every pinned canvas. */
    suspend fun clear()
}

/** Desktop does not pin canvases yet; its player keeps using the online canvas. */
object NoOpCanvasStore : CanvasStore {
    override suspend fun save(
        videoId: String,
        url: String,
        thumbUrl: String?,
        isVideo: Boolean,
    ): PinnedCanvas? = null

    override suspend fun pinned(videoId: String): PinnedCanvas? = null

    override suspend fun delete(videoId: String) = Unit

    override suspend fun retainOnly(videoIds: Collection<String>) = Unit

    override suspend fun clear() = Unit
}
