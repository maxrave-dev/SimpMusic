package com.maxrave.kotlinytmusicscraper.models.youtube

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouTubeInitialPage(
    @SerialName("videoDetails")
    val videoDetails: VideoDetails?,
)
