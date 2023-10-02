package com.maxrave.kotlinytmusicscraper.models.body

import kotlinx.serialization.Serializable

@Serializable
data class CreatePlaylistBody(
    val title: String,
    val description: String? = "Created by SimpMusic",
    val privacyStatus: String = PrivacyStatus.PRIVATE,
    val videoIds: List<String>? = null
) {
    object PrivacyStatus {
        const val PRIVATE = "PRIVATE"
        const val PUBLIC = "PUBLIC"
        const val UNLISTED = "UNLISTED"
    }
}