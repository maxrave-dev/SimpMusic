package com.maxrave.domain.data.model.browse.artist

import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.model.streams.YouTubeWatchEndpoint

data class ArtistBrowse(
    val albums: Albums?,
    val channelId: String?,
    val description: String?,
    val name: String,
    val radioId: YouTubeWatchEndpoint?,
    val related: Related?,
    val shuffleId: YouTubeWatchEndpoint?,
    val singles: Singles?,
    val songs: Songs?,
    val video: List<ResultVideo>?,
    val featuredOn: List<ResultPlaylist>?,
    val videoList: String?,
    val subscribed: Boolean?,
    val subscribers: String?,
    val thumbnails: List<Thumbnail>?,
    val views: String?,
) {
    data class Videos(
        val video: List<Track> = emptyList(),
        val videoListParam: String? = null,
    )
}