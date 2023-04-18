package com.maxrave.simpmusic.data.model.explore.mood.moodmoments


import com.google.gson.annotations.SerializedName

data class MoodsMomentObject(
    @SerializedName("endpoint")
    val endpoint: String,
    @SerializedName("header")
    val header: String,
    @SerializedName("items")
    val items: List<Item>,
    @SerializedName("params")
    val params: String
)