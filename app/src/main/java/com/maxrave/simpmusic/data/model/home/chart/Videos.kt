package com.maxrave.simpmusic.data.model.home.chart


import com.google.gson.annotations.SerializedName

data class Videos(
    @SerializedName("items")
    val items: List<ItemVideo>,
    @SerializedName("playlist")
    val playlist: String
)