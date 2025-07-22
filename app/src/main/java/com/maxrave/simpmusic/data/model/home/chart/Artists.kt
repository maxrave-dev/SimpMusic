package com.maxrave.simpmusic.data.model.home.chart

import androidx.compose.runtime.Immutable

@Immutable
data class Artists(
    val itemArtists: ArrayList<ItemArtist>,
    val playlist: Any,
)