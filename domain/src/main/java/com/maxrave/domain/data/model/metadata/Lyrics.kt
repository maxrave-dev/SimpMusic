package com.maxrave.domain.data.model.metadata

import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    val error: Boolean,
    val lines: List<Line>?,
    val syncType: String?,
    val captchaRequired: Boolean = false,
    val simpMusicLyricsId: String? = null,
)