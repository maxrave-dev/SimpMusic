package com.maxrave.domain.data.model.searchResult.songs

import com.maxrave.domain.data.type.SearchResultType

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
) : SearchResultType {
    override fun objectType(): SearchResultType.Type = SearchResultType.Type.SONG
}