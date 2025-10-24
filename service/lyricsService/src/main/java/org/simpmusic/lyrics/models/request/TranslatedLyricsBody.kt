package org.simpmusic.lyrics.models.request

import kotlinx.serialization.Serializable

@Serializable
data class TranslatedLyricsBody(
    val videoId: String,
    val translatedLyric: String, // LRC format
    val language: String, // 2-letter code
    val contributor: String,
    val contributorEmail: String,
)