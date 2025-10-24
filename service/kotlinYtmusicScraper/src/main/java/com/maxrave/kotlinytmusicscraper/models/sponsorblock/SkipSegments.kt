package com.maxrave.kotlinytmusicscraper.models.sponsorblock

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkipSegments(
    @SerialName("actionType")
    val actionType: String,
    @SerialName("category")
    val category: String,
    @SerialName("description")
    val description: String,
    @SerialName("locked")
    val locked: Int,
    @SerialName("segment")
    val segment: List<Double>,
    @SerialName("UUID")
    val uUID: String,
    @SerialName("videoDuration")
    val videoDuration: Double,
    @SerialName("votes")
    val votes: Int,
)