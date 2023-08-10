package com.maxrave.kotlinytmusicscraper.models.spotify


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tracks(
    @SerialName("href")
    val href: String?,
    @SerialName("items")
    val items: List<Item?>?,
    @SerialName("limit")
    val limit: Int?,
    @SerialName("next")
    val next: String?,
    @SerialName("offset")
    val offset: Int?,
    @SerialName("previous")
    val previous: String? = null,
    @SerialName("total")
    val total: Int?
)