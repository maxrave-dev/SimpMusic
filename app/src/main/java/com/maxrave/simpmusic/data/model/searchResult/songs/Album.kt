package com.maxrave.simpmusic.data.model.searchResult.songs


import com.google.gson.annotations.SerializedName

data class Album(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String
)