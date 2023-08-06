package com.maxrave.kotlinytmusicscraper.models

import kotlinx.serialization.Serializable

@Serializable
data class Badges(
    val musicInlineBadgeRenderer: MusicInlineBadgeRenderer,
) {
    @Serializable
    data class MusicInlineBadgeRenderer(
        val icon: Icon,
    )
}
