package com.maxrave.simpmusic.ui.navigation.destination.list

import kotlinx.serialization.Serializable

@Serializable
data class MoreAlbumsDestination(
    val id: String,
    val type: String = ALBUM_TYPE,
) {
    companion object {
        const val ALBUM_TYPE = "album"
        const val SINGLE_TYPE = "single"
    }
}