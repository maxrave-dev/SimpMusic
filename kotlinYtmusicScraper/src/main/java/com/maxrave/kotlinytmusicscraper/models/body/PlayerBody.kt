package com.maxrave.kotlinytmusicscraper.models.body

import com.maxrave.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context,
    val videoId: String,
    val playlistId: String?,
    val cpn: String?,
    val contentCheckOk: Boolean = true,
    val racyCheckOk: Boolean = true,
    val playbackContext: PlaybackContext? = null,
    val serviceIntegrityDimensions: ServiceIntegrityDimensions? = ServiceIntegrityDimensions(),
) {
    @Serializable
    data class ServiceIntegrityDimensions(
        val poToken: String =
            "Mlt6vqPMnRAc93qGSJr4d9wyzWNClpcDwVQGZ7ooTJoc6IjxwPaMoyTMXRkU5OHQQvLdQqF4v9W_U6JRCUmCPatLIOlbBqjasxsmO3PnigwoLSQ81o0MpFeX8nJA",
    )
    @Serializable
    data class PlaybackContext(
        val contentPlaybackContext: ContentPlaybackContext = ContentPlaybackContext(),
    ) {
        @Serializable
        data class ContentPlaybackContext(
            val html5Preference: String = "HTML5_PREF_WANTS",
            val signatureTimestamp: Int = 20073,
        )
    }
}