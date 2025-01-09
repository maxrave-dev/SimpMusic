package com.maxrave.simpmusic.data.model.searchResult.songs

import com.google.gson.annotations.SerializedName

data class SongsResult(
    @SerializedName("album")
    val album: Album?,
    @SerializedName("artists")
    val artists: List<Artist>?,
    @SerializedName("category")
    val category: String?,
    @SerializedName("duration")
    val duration: String?,
    @SerializedName("duration_seconds")
    val durationSeconds: Int?,
    @SerializedName("feedbackTokens")
    val feedbackTokens: FeedbackTokens?,
    @SerializedName("isExplicit")
    val isExplicit: Boolean?,
    @SerializedName("resultType")
    val resultType: String?,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("videoType")
    val videoType: String?,
    @SerializedName("year")
    val year: Any,
)