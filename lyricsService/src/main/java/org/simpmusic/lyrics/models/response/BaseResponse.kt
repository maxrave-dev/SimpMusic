package org.simpmusic.lyrics.models.response

import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    val success: Boolean,
    val error: ErrorResponse? = null,
    val data: T? = null,
)