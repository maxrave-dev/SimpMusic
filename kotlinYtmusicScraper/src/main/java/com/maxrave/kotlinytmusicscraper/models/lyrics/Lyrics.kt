package com.maxrave.kotlinytmusicscraper.models.lyrics


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lyrics(
    @SerialName("error")
    val error: Boolean,
    @SerialName("lines")
    val lines: List<Line>?,
    @SerialName("syncType")
    val syncType: String
)