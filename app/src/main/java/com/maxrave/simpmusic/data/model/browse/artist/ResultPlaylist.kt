package com.maxrave.simpmusic.data.model.browse.artist

import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class ResultPlaylist(
    val id: String,
    val author: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
) {
}