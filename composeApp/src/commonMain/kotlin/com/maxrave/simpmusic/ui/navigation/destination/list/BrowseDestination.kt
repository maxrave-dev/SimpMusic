package com.maxrave.simpmusic.ui.navigation.destination.list

import kotlinx.serialization.Serializable

@Serializable
data class BrowseDestination(
    val browseId: String,
    val params: String? = null,
    /** The home section's own title, shown when the page names none. */
    val title: String? = null,
)
