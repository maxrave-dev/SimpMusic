package com.maxrave.kotlinytmusicscraper.models.body

import com.maxrave.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
)
