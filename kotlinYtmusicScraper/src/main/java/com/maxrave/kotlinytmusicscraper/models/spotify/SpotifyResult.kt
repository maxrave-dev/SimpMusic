package com.maxrave.kotlinytmusicscraper.models.spotify


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyResult(
    @SerialName("tracks")
    val tracks: Tracks?
)