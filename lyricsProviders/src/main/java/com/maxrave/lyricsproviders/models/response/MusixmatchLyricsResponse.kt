package com.maxrave.lyricsproviders.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusixmatchLyricsResponse(
    val message: Message,
) {
    @Serializable
    data class Message(
        val body: Body,
        val header: Header,
    ) {
        @Serializable
        data class Body(
            val subtitle: Subtitle?,
            val lyrics: Lyrics?,
            val macro_calls: MacroCalls?,
            val subtitle_list: List<Subtitle>? = null,
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

            @Serializable
            data class MacroCalls(
                @SerialName("track.subtitles.get")
                val trackSubtitlesGet: MusixmatchLyricsResponse? = null,
                @SerialName("track.lyrics.get")
                val trackLyricsGet: MusixmatchLyricsResponse? = null,
                @SerialName("matcher.track.get")
                val trackGet: SearchMusixmatchResponse? = null,
            )
        }

        @Serializable
        data class Header(
            val status_code: Int,
            val hint: String? = null,
        )
    }
}