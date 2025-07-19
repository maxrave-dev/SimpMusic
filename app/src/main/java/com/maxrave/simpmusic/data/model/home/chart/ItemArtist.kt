package com.maxrave.simpmusic.data.model.home.chart

import androidx.compose.runtime.Immutable
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

@Immutable
data class ItemArtist(
    val browseId: String,
    val rank: String,
    val subscribers: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
    val trend: String,
)