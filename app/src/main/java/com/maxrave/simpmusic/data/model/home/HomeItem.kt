package com.maxrave.simpmusic.data.model.home

import androidx.compose.runtime.Immutable
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail

@Immutable
data class HomeItem(
    val contents: List<Content?>,
    val title: String,
    val subtitle: String? = null,
    val thumbnail: List<Thumbnail>? = null,
    val channelId: String? = null,
)