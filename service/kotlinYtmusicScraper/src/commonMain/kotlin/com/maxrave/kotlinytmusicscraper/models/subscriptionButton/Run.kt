package com.maxrave.kotlinytmusicscraper.models.subscriptionButton

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Run(
    @SerialName("text")
    val text: String,
)