package com.maxrave.simpmusic.data.model.searchResult.artists


import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class ArtistsResult(
    @SerializedName("artist")
    val artist: String,
    @SerializedName("browseId")
    val browseId: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("radioId")
    val radioId: String,
    @SerializedName("resultType")
    val resultType: String,
    @SerializedName("shuffleId")
    val shuffleId: String,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>
)