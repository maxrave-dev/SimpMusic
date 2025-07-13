package org.simpmusic.lyrics.models.response

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: Boolean,
    val code: Int,
    val reason: String,
)