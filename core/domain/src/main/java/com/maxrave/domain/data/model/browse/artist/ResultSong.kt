package com.maxrave.domain.data.model.browse.artist
import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail

data class ResultSong(
    val videoId: String,
    val title: String,
    val artists: List<Artist>?,
    val durationSeconds: Int = 0,
    val album: Album,
    val likeStatus: String,
    val thumbnails: List<Thumbnail>,
    val isAvailable: Boolean,
    val isExplicit: Boolean,
    val videoType: String,
)