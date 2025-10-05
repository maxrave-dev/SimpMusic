package org.simpmusic.lyrics.models.response

import kotlinx.serialization.Serializable

@Serializable
data class LyricsResponse(
    val id: String,
    val videoId: String,
    val songTitle: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Int,
    val plainLyric: String,
    val syncedLyrics: String?,
    val richSyncLyrics: String?,
    val vote: Int,
    val contributor: String,
    val contributorEmail: String,
)