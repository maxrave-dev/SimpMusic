package com.maxrave.simpmusic.ui.navigation.destination.list

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDestination(
    val playlistId: String,
    val isYourYouTubePlaylist: Boolean = false,
)