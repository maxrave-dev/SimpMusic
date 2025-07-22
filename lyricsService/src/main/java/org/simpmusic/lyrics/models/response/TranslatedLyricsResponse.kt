package org.simpmusic.lyrics.models.response

import kotlinx.serialization.Serializable

@Serializable
data class TranslatedLyricsResponse(
    val id: String,
    val videoId: String,
    val translatedLyric: String, // LRC format
    val language: String, // 2-letter code
    val vote: Int,
    val contributor: String,
    val contributorEmail: String,
)