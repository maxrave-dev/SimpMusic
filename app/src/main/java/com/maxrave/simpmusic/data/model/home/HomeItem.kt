package com.maxrave.simpmusic.data.model.home


import com.google.gson.annotations.SerializedName

data class HomeItem(
    @SerializedName("contents")
    val contents: List<Content?>,
    @SerializedName("title")
    val title: String
)