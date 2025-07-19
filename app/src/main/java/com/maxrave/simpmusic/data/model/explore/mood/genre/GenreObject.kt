package com.maxrave.simpmusic.data.model.explore.mood.genre

import androidx.compose.runtime.Immutable

@Immutable
data class GenreObject(
    val header: String,
    val itemsPlaylist: List<ItemsPlaylist>,
    val itemsSong: List<ItemsSong>?,
)