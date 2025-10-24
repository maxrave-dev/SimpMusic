package com.maxrave.kotlinytmusicscraper.pages

import com.maxrave.kotlinytmusicscraper.models.PlaylistItem
import com.maxrave.kotlinytmusicscraper.models.VideoItem

data class ExplorePage(
    val released: List<PlaylistItem>,
    val musicVideo: List<VideoItem>,
)