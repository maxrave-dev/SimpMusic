package com.maxrave.simpmusic.data.model.searchResult.playlists

import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail
import com.maxrave.simpmusic.data.type.PlaylistType

data class PlaylistsResult(
    val author: String,
    val browseId: String,
    val category: String,
    val itemCount: String,
    val resultType: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
) : PlaylistType {
    override fun playlistType(): PlaylistType.Type =
        if (resultType == "Podcast") {
            PlaylistType.Type.PODCAST
        } else if (browseId.startsWith("RDEM") || browseId.startsWith("RDAMVM") || browseId.startsWith("RDAT")) {
            PlaylistType.Type.RADIO
        } else {
            PlaylistType.Type.YOUTUBE_PLAYLIST
        }
}