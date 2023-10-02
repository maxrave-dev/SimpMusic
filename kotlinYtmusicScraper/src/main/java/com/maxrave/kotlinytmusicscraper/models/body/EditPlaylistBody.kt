package com.maxrave.kotlinytmusicscraper.models.body

import kotlinx.serialization.Serializable

@Serializable
data class EditPlaylistBody(
    val playlistId: String,
    val actions: Action
) {
    @Serializable
    data class Action(
        val action: String = "ACTION_SET_PLAYLIST_NAME",
        val playlistName: String?,
        val addedVideoId: String? = null,
        val removedVideoId: String? = null,
        val setVideoId: String? = null,
    )
}