package com.maxrave.kotlinytmusicscraper.models.lyrics


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    @SerialName("lyrics")
    val lyrics: LyricsX? = null,
) {
    @Serializable
    data class LyricsX(
        @SerialName("lines")
        val lines: List<Line>?,
        @SerialName("syncType")
        val syncType: String?,
    )
}