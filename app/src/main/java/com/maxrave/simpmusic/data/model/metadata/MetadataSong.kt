package com.maxrave.simpmusic.data.model.metadata


import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class MetadataSong(
    @SerializedName("album")
    val album: Album,
    @SerializedName("artists")
    val artists: List<Artist>,
    @SerializedName("duration")
    val duration: String,
    @SerializedName("duration_seconds")
    val durationSeconds: Int,
    @SerializedName("isExplicit")
    val isExplicit: Boolean,
    @SerializedName("lyrics")
    val lyrics: Lyrics,
    @SerializedName("resultType")
    val resultType: String,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("title")
    val title: String,
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("videoType")
    val videoType: String,
    @SerializedName("year")
    val year: Any
)