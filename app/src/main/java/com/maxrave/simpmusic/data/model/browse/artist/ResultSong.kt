package com.maxrave.simpmusic.data.model.browse.artist
import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class ResultSong (
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("artists")
    val artists: List<Artist>?,
    @SerializedName("album")
    val album: Album,
    @SerializedName("likeStatus")
    val likeStatus: String,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("isAvailable")
    val isAvailable: Boolean,
    @SerializedName("isExplicit")
    val isExplicit: Boolean,
    @SerializedName("videoType")
    val videoType: String,
)
fun ResultSong.toTrack(): Track {
    return Track(
        album = album,
        artists = artists,
        duration = "",
        durationSeconds = 0,
        isAvailable = isAvailable,
        isExplicit = isExplicit,
        likeStatus = likeStatus,
        thumbnails = thumbnails,
        title = title,
        videoId = videoId,
        videoType = videoType,
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = ""
    )
}