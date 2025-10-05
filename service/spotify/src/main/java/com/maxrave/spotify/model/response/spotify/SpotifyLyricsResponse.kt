package com.maxrave.spotify.model.response.spotify

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyLyricsResponse(
    val lyrics: Lyrics,
) {
    @Serializable
    data class Lyrics(
        val syncType: String,
        val lines: List<Line>,
    ) {
        @Serializable
        data class Line(
            val startTimeMs: String,
            val endTimeMs: String,
            val words: String,
        )
    }
}