package com.maxrave.simpmusic.data.model.searchResult.songs

data class SongsResult(
    val album: Album?,
    val artists: List<Artist>?,
    val category: String?,
    val duration: String?,
    val durationSeconds: Int?,
    val feedbackTokens: FeedbackTokens?,
    val isExplicit: Boolean?,
    val resultType: String?,
    val thumbnails: List<Thumbnail>?,
    val title: String?,
    val videoId: String,
    val videoType: String?,
    val year: Any,
)