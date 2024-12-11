package com.maxrave.simpmusic.data.model.explore.mood.genre

import androidx.compose.runtime.Immutable
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist

@Immutable
data class ItemsSong(
    val title: String,
    val artist: List<Artist>?,
    val videoId: String,
)