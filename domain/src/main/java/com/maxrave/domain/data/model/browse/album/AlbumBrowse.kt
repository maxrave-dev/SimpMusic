package com.maxrave.domain.data.model.browse.album

import com.maxrave.domain.data.model.browse.artist.ResultAlbum
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail

data class AlbumBrowse(
    val artists: List<Artist>,
    val audioPlaylistId: String,
    val description: String?,
    val duration: String?,
    val durationSeconds: Int,
    val thumbnails: List<Thumbnail>?,
    val title: String,
    val trackCount: Int,
    val tracks: List<Track>,
    val type: String,
    val year: String?,
    val otherVersion: List<ResultAlbum> = emptyList(),
)