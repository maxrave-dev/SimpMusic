package com.maxrave.kotlinytmusicscraper.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Continuation(
    @JsonNames("nextContinuationData", "nextRadioContinuationData")
    val nextContinuationData: NextContinuationData?,
    val reloadContinuationData: ReloadContinuationData?,
) {
    @Serializable
    data class NextContinuationData(
        val continuation: String,
    )

    @Serializable
    data class ReloadContinuationData(
        val continuation: String?,
        val clickTrackingParams: String?,
    )
}