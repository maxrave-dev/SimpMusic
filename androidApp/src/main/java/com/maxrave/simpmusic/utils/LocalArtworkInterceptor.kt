package com.maxrave.simpmusic.utils

import android.net.Uri
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.ImageResult
import com.maxrave.simpmusic.artwork.ArtworkStore
import java.io.File

/**
 * Serves the cover pinned by a download instead of its network URL.
 *
 * Registered before [NetworkFirstInterceptor], so a downloaded song's cover is read from app
 * storage before the 24 hour cache rule can send the request to the network. URLs without a
 * pinned file fall through unchanged.
 */
class LocalArtworkInterceptor(
    private val artworkStore: ArtworkStore,
) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val url =
            when (val data = request.data) {
                is String -> data
                is Uri -> data.toString()
                else -> null
            }
        if (url == null || (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true))) {
            return chain.proceed()
        }
        val localPath = artworkStore.localArtwork(url) ?: return chain.proceed()
        return chain
            .withRequest(request.newBuilder().data(File(localPath)).build())
            .proceed()
    }
}
