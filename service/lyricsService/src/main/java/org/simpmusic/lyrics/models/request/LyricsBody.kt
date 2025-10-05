package org.simpmusic.lyrics.models.request

import kotlinx.serialization.Serializable

@Serializable
data class LyricsBody(
    val videoId: String,
    val songTitle: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Int,
    val plainLyric: String,
    val syncedLyrics: String? = null,
    val richSyncLyrics: String? = null,
    val contributor: String,
    val contributorEmail: String,
)