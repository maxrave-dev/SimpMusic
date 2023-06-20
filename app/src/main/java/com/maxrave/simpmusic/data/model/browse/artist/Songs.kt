package com.maxrave.simpmusic.data.model.browse.artist


import com.google.gson.annotations.SerializedName

data class Songs(
    @SerializedName("browseId")
    val browseId: String?,
    @SerializedName("results")
    val results: List<ResultSong>?
)