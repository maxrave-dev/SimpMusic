package com.maxrave.domain.data.model.mood.moodmoments

import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.type.HomeContentType

data class Content(
    val playlistBrowseId: String,
    val subtitle: String,
    val thumbnails: List<Thumbnail>?,
    val title: String,
) : HomeContentType