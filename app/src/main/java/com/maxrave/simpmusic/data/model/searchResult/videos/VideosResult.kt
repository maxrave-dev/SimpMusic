package com.maxrave.simpmusic.data.model.searchResult.videos

import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class VideosResult(
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