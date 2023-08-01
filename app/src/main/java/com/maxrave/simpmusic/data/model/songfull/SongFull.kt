package com.maxrave.simpmusic.data.model.songfull


import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail
import com.maxrave.simpmusic.data.model.streams.Streams

data class SongFull(
    @SerializedName("artist")
    val artist: List<Artist>,
    @SerializedName("audioStreams")
    val audioStreams: List<Streams>,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("title")
    val title: String
)