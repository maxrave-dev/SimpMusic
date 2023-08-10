package com.maxrave.kotlinytmusicscraper.models.lyrics


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Line(
    @SerialName("endTimeMs")
    val endTimeMs: String,
    @SerialName("startTimeMs")
    val startTimeMs: String,
    @SerialName("syllables")
    val syllables: List<String>?,
    @SerialName("words")
    val words: String
)