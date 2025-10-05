package com.maxrave.kotlinytmusicscraper.pages

import com.maxrave.kotlinytmusicscraper.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)