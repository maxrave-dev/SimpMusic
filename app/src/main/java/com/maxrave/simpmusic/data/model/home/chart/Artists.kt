package com.maxrave.simpmusic.data.model.home.chart

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class Artists(
    @SerializedName("items")
    val itemArtists: ArrayList<ItemArtist>,
    @SerializedName("playlist")
    val playlist: Any,
)