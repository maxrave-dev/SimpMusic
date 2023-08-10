package com.maxrave.simpmusic.data.model.explore.mood.genre


import com.google.gson.annotations.SerializedName

data class GenreObject(
    @SerializedName("header")
    val header: String,
    @SerializedName("itemsPlaylist")
    val itemsPlaylist: List<ItemsPlaylist>,
    @SerializedName("itemsSong")
    val itemsSong: List<ItemsSong>?
)