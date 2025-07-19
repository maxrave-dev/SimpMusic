package com.maxrave.simpmusic.data.model.browse.artist

import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class ResultRelated(
    val browseId: String,
    val subscribers: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
)