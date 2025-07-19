package com.maxrave.simpmusic.data.model.metadata

import kotlinx.serialization.Serializable

@Serializable
data class Line(
    val endTimeMs: String,
    val startTimeMs: String,
    val syllables: List<String>? = null,
    val words: String,
)