package com.maxrave.simpmusic.data.model.browse.artist

import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class ResultVideo(
    @SerializedName("artists")
    val artists: List<Artist>?,
    @SerializedName("category")
    val category: String?,
    @SerializedName("duration")
    val duration: String?,
    @SerializedName("duration_seconds")
    val durationSeconds: Int?,
    @SerializedName("resultType")
    val resultType: String?,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>?,
    @SerializedName("title")
    val title: String,
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("videoType")
    val videoType: String?,
    @SerializedName("views")
    val views: String?,
    @SerializedName("year")
    val year: Any
)