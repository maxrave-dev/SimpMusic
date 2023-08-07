package com.maxrave.simpmusic.data.model.explore.mood.genre

import com.maxrave.simpmusic.data.model.searchResult.songs.Artist

data class ItemsSong(
    val title: String,
    val artist: List<Artist>?,
    val videoId: String,
)
