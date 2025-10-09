package com.maxrave.simpmusic.ui.navigation.destination.library

import kotlinx.serialization.Serializable

/**
 * @param type Using LibraryDynamicPlaylistType.toStringParams
 */
@Serializable
data class LibraryDynamicPlaylistDestination(
    val type: String,
)