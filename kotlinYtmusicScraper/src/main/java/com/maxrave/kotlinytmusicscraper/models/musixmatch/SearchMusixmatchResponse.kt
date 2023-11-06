package com.maxrave.kotlinytmusicscraper.models.musixmatch

import kotlinx.serialization.Serializable

@Serializable
data class SearchMusixmatchResponse(
    val message: Message
) {
    @Serializable
    data class Message(
        val body: Body,
        val header: Header
    ) {
        @Serializable
        data class Body(
            val track_list: List<Track>
        ) {
            @Serializable
            data class Track(
                val track: TrackX
            ) {
                @Serializable
                data class TrackX(
                    val track_id: Int,
                    val track_name: String,
                    val artist_name: String,
                    val track_length: Int,
                )
            }
        }
        @Serializable
        data class Header(
            val status_code: Int
        )
    }
}