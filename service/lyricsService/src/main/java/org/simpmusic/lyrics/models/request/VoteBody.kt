package org.simpmusic.lyrics.models.request

import kotlinx.serialization.Serializable

@Serializable
data class VoteBody(
    val id: String,
    val vote: Int,
    // 1 for upvote, 0 for downvote
)