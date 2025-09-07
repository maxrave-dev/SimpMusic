package com.maxrave.domain.data.model.browse.playlist

import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail

data class TrackPlaylist(
    val albumPlaylist: AlbumPlaylist,
    val artistPlaylists: List<Artist>?,
    val duration: String,
    val durationSeconds: Int,
    val isAvailable: Boolean,
    val isExplicit: Boolean,
    val likeStatus: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
    val videoId: String,
    val videoType: String,
)