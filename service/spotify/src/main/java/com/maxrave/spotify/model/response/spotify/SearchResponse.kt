package com.maxrave.spotify.model.response.spotify

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val tracks: Tracks,
) {
    @Serializable
    data class Tracks(
        val items: List<Item>,
    ) {
        @Serializable
        data class Item(
            val artists: List<Artist>,
            val external_urls: ExternalUrls,
            val id: String,
            val name: String,
            val duration_ms: Int,
        ) {
            @Serializable
            data class Artist(
                val name: String,
            )

            @Serializable
            data class ExternalUrls(
                val spotify: String,
            )
        }
    }
}