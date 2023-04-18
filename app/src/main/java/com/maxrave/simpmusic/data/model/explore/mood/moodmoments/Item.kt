package com.maxrave.simpmusic.data.model.explore.mood.moodmoments


import com.google.gson.annotations.SerializedName

data class Item(
    @SerializedName("contents")
    val contents: List<Content>,
    @SerializedName("header")
    val header: String
)