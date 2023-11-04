package com.maxrave.simpmusic.data.model.browse.artist


import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class ArtistBrowse(
    @SerializedName("albums")
    val albums: Albums?,
    @SerializedName("channelId")
    val channelId: String?,
    @SerializedName("description")
    val description: Any?,
    @SerializedName("name")
    val name: String,
    @SerializedName("radioId")
    val radioId: String?,
    @SerializedName("related")
    val related: Related?,
    @SerializedName("shuffleId")
    val shuffleId: String?,
    @SerializedName("singles")
    val singles: Singles?,
    @SerializedName("songs")
    val songs: Songs?,
    @SerializedName("video")
    val video: List<ResultVideo>?,
    val featuredOn: List<ResultPlaylist>?,
    val videoList: String?,
    @SerializedName("subscribed")
    val subscribed: Boolean?,
    @SerializedName("subscribers")
    val subscribers: String?,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>?,
    @SerializedName("views")
    val views: Any?
)