package com.maxrave.domain.data.model.metadata

import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail

data class MetadataSong(
    val album: Album,
    val artists: List<Artist>,
    val duration: String,
    val durationSeconds: Int,
    val isExplicit: Boolean,
    val lyrics: Lyrics,
    val resultType: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
    val videoId: String,
    val videoType: String,
    val year: Any,
)