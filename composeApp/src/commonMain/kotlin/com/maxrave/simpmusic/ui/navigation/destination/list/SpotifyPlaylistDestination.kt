package com.maxrave.simpmusic.ui.navigation.destination.list

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyPlaylistDestination(
    val id: String,
    val name: String,
)
