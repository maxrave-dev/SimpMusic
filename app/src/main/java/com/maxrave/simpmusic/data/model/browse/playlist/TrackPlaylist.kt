package com.maxrave.simpmusic.data.model.browse.playlist


import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class TrackPlaylist(
    @SerializedName("album")
    val albumPlaylist: AlbumPlaylist,
    @SerializedName("artists")
    val artistPlaylists: List<Artist>?,
    @SerializedName("duration")
    val duration: String,
    @SerializedName("duration_seconds")
    val durationSeconds: Int,
    @SerializedName("isAvailable")
    val isAvailable: Boolean,
    @SerializedName("isExplicit")
    val isExplicit: Boolean,
    @SerializedName("likeStatus")
    val likeStatus: String,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("title")
    val title: String,
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("videoType")
    val videoType: String
)