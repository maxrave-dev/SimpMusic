package com.maxrave.kotlinytmusicscraper.models.musixmatch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusixmatchLyricsResponseByQ(
    @SerialName("message")
    val message: Message
) {
    @Serializable
    data class Message(
        @SerialName("body")
        val body: Body
    ) {
        @Serializable
        data class Body(
            @SerialName("subtitle_list")
            val subtitle_list: List<SubtitleList>?
        ) {
            @Serializable
            data class SubtitleList(
                @SerialName("subtitle")
                val subtitle: Subtitle
            ) {
                @Serializable
                data class Subtitle(
                    @SerialName("subtitle_id")
                    val subtitle_id: Int,
                    @SerialName("subtitle_body")
                    val subtitle_body: String
                )
            }
        }
    }
}