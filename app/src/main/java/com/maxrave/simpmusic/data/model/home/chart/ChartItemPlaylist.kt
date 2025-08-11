package com.maxrave.simpmusic.data.model.home.chart

import androidx.compose.runtime.Immutable
import com.maxrave.simpmusic.data.model.browse.artist.ResultPlaylist

@Immutable
data class ChartItemPlaylist(
    val title: String,
    val playlists: List<ResultPlaylist>,
)