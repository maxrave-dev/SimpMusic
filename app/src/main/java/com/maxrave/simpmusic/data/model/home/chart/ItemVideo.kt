package com.maxrave.simpmusic.data.model.home.chart

import androidx.compose.runtime.Immutable
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

@Immutable
data class ItemVideo(
    val artists: List<Artist>?,
    val playlistId: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
    val videoId: String,
    val views: String,
)

fun ItemVideo.toTrack(): Track =
    Track(
        album = null,
        artists = artists,
        duration = "",
        durationSeconds = 0,
        isAvailable = false,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails = thumbnails,
        title = title,
        videoId = videoId,
        videoType = "",
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = "",
    )