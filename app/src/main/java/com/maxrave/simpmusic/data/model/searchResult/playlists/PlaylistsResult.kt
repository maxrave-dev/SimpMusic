package com.maxrave.simpmusic.data.model.searchResult.playlists

import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail
import com.maxrave.simpmusic.data.type.PlaylistType

data class PlaylistsResult(
    @SerializedName("author")
    val author: String,
    @SerializedName("browseId")
    val browseId: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("itemCount")
    val itemCount: String,
    @SerializedName("resultType")
    val resultType: String,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("title")
    val title: String,
) : PlaylistType {
    override fun playlistType(): PlaylistType.Type =
        if (browseId.startsWith("RDEM") || browseId.startsWith("RDAMVM") || browseId.startsWith("RDAT")) {
            PlaylistType.Type.RADIO
        } else {
            PlaylistType.Type.YOUTUBE_PLAYLIST
        }
}