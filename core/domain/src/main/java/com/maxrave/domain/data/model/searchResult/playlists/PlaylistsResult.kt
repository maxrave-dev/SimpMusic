package com.maxrave.domain.data.model.searchResult.playlists

import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.type.PlaylistType
import com.maxrave.domain.data.type.SearchResultType

data class PlaylistsResult(
    val author: String,
    val browseId: String,
    val category: String,
    val itemCount: String,
    val resultType: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
) : PlaylistType,
    SearchResultType {
    override fun playlistType(): PlaylistType.Type =
        if (resultType == "Podcast") {
            PlaylistType.Type.PODCAST
        } else if (browseId.startsWith("RDEM") || browseId.startsWith("RDAMVM") || browseId.startsWith("RDAT")) {
            PlaylistType.Type.RADIO
        } else {
            PlaylistType.Type.YOUTUBE_PLAYLIST
        }

    override fun objectType(): SearchResultType.Type =
        if (resultType == "Podcast") {
            SearchResultType.Type.PODCAST
        } else {
            SearchResultType.Type.PLAYLIST
        }
}