package com.maxrave.domain.data.model.browse.playlist

import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint

data class PlaylistBrowse(
    val author: Author,
    val description: String?,
    val duration: String,
    val durationSeconds: Int,
    val id: String,
    val privacy: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
    val trackCount: Int,
    val tracks: List<Track>,
    val year: String,
    val shuffleEndpoint: YouTubeWatchEndpoint? = null,
    val radioEndpoint: YouTubeWatchEndpoint? = null,
)