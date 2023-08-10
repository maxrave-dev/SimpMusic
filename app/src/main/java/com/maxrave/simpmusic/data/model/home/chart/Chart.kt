package com.maxrave.simpmusic.data.model.home.chart


import com.google.gson.annotations.SerializedName

data class Chart(
    @SerializedName("artists")
    val artists: Artists,
    @SerializedName("countries")
    val countries: Countries?,
    @SerializedName("videos")
    val videos: Videos
)