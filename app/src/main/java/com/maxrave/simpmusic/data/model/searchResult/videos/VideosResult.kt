package com.maxrave.simpmusic.data.model.searchResult.videos
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist

import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class VideosResult(
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

fun VideosResult.toTrack(): Track {
    return Track(
        album = null,
        artists = artists,
        duration = duration?: "",
        durationSeconds = durationSeconds?: 0,
        isAvailable = true,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails = thumbnails,
        title = title,
        videoId = videoId,
        videoType = videoType?: "",
        category = category,
        feedbackTokens = null,
        resultType = resultType,
        year = "")
}
fun ArrayList<VideosResult>.toListTrack(): ArrayList<Track> {
    val listTrack = arrayListOf<Track>()
    for (video in this) {
        listTrack.add(video.toTrack())
    }
    return listTrack
}