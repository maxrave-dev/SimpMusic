package com.maxrave.simpmusic.data.model.browse.artist

import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

data class ArtistBrowse(
    val albums: Albums?,
    val channelId: String?,
    val description: String?,
    val name: String,
    val radioId: WatchEndpoint?,
    val related: Related?,
    val shuffleId: WatchEndpoint?,
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