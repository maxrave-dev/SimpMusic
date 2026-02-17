package com.maxrave.kotlinytmusicscraper.models.youtube

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlValue

@Serializable
@SerialName("transcript")
data class Transcript(
    val text: List<Text>,
) {
    @Serializable
    @SerialName("text")
    data class Text(
        val start: String,
        val dur: String,
        @XmlValue(true) val content: String,
    )
}