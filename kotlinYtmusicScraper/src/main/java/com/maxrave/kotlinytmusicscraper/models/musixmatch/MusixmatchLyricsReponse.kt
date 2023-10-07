package com.maxrave.kotlinytmusicscraper.models.musixmatch

import kotlinx.serialization.Serializable

@Serializable
data class MusixmatchLyricsReponse(
    val message: Message
) {
    @Serializable
    data class Message(
        val body: Body,
        val header: Header
    ) {
        @Serializable
        data class Body(
            val subtitle: Subtitle?,
            val lyrics: Lyrics?
        ) {
            @Serializable
            data class Subtitle(
                val subtitle_body: String,
                val subtitle_id: Int,
                val lyrics_copyright: String,
            )
            @Serializable
            data class Lyrics(
                val lyrics_body: String,
                val lyrics_id: Int,
                val lyrics_copyright: String,
            )
        }

        @Serializable
        data class Header(
            val status_code: Int
        )
    }
}