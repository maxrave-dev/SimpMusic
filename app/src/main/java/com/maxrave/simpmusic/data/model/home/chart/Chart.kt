package com.maxrave.simpmusic.data.model.home.chart

import androidx.compose.runtime.Immutable
import com.maxrave.simpmusic.data.model.browse.album.Track

@Immutable
data class Chart(
    val artists: Artists,
    val countries: Countries?,
    val videos: Videos,
    val songs: ArrayList<Track>? = null,
    val trending: ArrayList<Track>? = null,
)