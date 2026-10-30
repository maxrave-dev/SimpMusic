package com.maxrave.simpmusic.artwork

/**
 * Pinned cover art for songs that have been downloaded.
 *
 * A download's cover is written to app storage under a name derived from its thumbnail URL, so
 * the player, lists, notification and MediaSession keep their artwork when the device is offline
 * and when Coil's shared 512 MiB cache has been evicted. Songs that are not downloaded never
 * reach this store: they keep the existing 24 hour image cache.
 */
interface ArtworkStore {
    /**
     * Writes the cover at [url] for [videoId] if it is not pinned yet. Returns whether a pinned
     * copy exists afterwards.
     */
    suspend fun save(
        videoId: String,
        url: String,
    ): Boolean

    /** Removes the pinned cover for [videoId], using [url] to find the exact file when known. */
    suspend fun delete(
        videoId: String,
        url: String? = null,
    )

    /** Removes every pinned cover whose thumbnail URL is not in [urls]. */
    suspend fun retainOnly(urls: Collection<String>)

    /** The local file for [url], or null when that image is not pinned. */
    fun localArtwork(url: String): String?
}

/** Desktop does not pin covers yet; its player keeps using the shared image cache. */
object NoOpArtworkStore : ArtworkStore {
    override suspend fun save(
        videoId: String,
        url: String,
    ): Boolean = false

    override suspend fun delete(
        videoId: String,
        url: String?,
    ) = Unit

    override suspend fun retainOnly(urls: Collection<String>) = Unit

    override fun localArtwork(url: String): String? = null
}
