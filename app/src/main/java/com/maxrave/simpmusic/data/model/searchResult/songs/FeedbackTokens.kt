package com.maxrave.simpmusic.data.model.searchResult.songs

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackTokens(
    val add: String?,
    val remove: String?,
)