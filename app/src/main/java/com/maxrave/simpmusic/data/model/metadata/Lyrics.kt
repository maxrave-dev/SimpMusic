package com.maxrave.simpmusic.data.model.metadata

data class Lyrics(
    val error: Boolean,
    val lines: List<Line>?,
    val syncType: String?,
    val captchaRequired: Boolean = false,
    val simpMusicLyricsId: String? = null,
)