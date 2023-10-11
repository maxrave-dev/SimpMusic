package com.maxrave.kotlinytmusicscraper.models.response

import kotlinx.serialization.Serializable

@Serializable
data class CreatePlaylistResponse(
    val playlistId: String,
)