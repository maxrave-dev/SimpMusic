package com.maxrave.domain.data.model.mediaService

data class SponsorSkipSegments(
    val actionType: String,
    val category: String,
    val description: String,
    val locked: Int,
    val segment: List<Double>,
    val uUID: String,
    val videoDuration: Double,
    val votes: Int,
)