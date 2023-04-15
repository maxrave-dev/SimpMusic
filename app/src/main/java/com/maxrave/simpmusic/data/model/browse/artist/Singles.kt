package com.maxrave.simpmusic.data.model.browse.artist


import com.google.gson.annotations.SerializedName

data class Singles(
    @SerializedName("browseId")
    val browseId: String,
    @SerializedName("params")
    val params: String,
    @SerializedName("results")
    val results: List<ResultSingle>
)