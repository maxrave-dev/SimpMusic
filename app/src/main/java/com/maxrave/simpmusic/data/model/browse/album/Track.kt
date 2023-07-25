package com.maxrave.simpmusic.data.model.browse.album


import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.FeedbackTokens
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class Track(
    @SerializedName("album")
    val album: Album?,
    @SerializedName("artists")
    val artists: List<Artist>?,
    @SerializedName("duration")
    val duration: String?,
    @SerializedName("duration_seconds")
    val durationSeconds: Int?,
    @SerializedName("isAvailable")
    val isAvailable: Boolean,
    @SerializedName("isExplicit")
    val isExplicit: Boolean,
    @SerializedName("likeStatus")
    val likeStatus: String?,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>?,
    @SerializedName("title")
    val title: String,
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("videoType")
    val videoType: String?,
    @SerializedName("category")
    val category: String?,
    @SerializedName("feedbackTokens")
    val feedbackTokens: FeedbackTokens?,
    @SerializedName("resultType")
    val resultType: String?,
    @SerializedName("year")
    val year: Any?
)