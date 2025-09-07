package com.maxrave.domain.data.model.browse.artist

import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail

data class ResultVideo(
    val artists: List<Artist>?,
    val category: String?,
    val duration: String?,
    val durationSeconds: Int?,
    val resultType: String?,
    val thumbnails: List<Thumbnail>?,
    val title: String,
    val videoId: String,
    val videoType: String?,
    val views: String?,
    val year: Any,
)