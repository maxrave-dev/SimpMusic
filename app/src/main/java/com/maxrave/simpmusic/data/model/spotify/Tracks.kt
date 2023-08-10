package com.maxrave.simpmusic.data.model.spotify


import com.google.gson.annotations.SerializedName

data class Tracks(
    @SerializedName("href")
    val href: String?,
    @SerializedName("items")
    val items: List<Item?>?,
    @SerializedName("limit")
    val limit: Int?,
    @SerializedName("next")
    val next: String?,
    @SerializedName("offset")
    val offset: Int?,
    @SerializedName("previous")
    val previous: Any?,
    @SerializedName("total")
    val total: Int?
)