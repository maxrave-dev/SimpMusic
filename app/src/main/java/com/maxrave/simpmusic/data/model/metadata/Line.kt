package com.maxrave.simpmusic.data.model.metadata


import com.google.gson.annotations.SerializedName

data class Line(
    @SerializedName("endTimeMs")
    val endTimeMs: String,
    @SerializedName("startTimeMs")
    val startTimeMs: String,
    @SerializedName("syllables")
    val syllables: List<Any>,
    @SerializedName("words")
    val words: String
)