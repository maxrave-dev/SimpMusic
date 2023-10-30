package com.maxrave.simpmusic.data.model.browse.playlist


import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class PlaylistBrowse(
    @SerializedName("author")
    val author: Author,
    @SerializedName("description")
    val description: String?,
    @SerializedName("duration")
    val duration: String,
    @SerializedName("duration_seconds")
    val durationSeconds: Int,
    @SerializedName("id")
    val id: String,
    @SerializedName("privacy")
    val privacy: String,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("title")
    val title: String,
    @SerializedName("trackCount")
    val trackCount: Int,
    @SerializedName("tracks")
    val tracks: List<Track>,
    @SerializedName("year")
    val year: String,
)