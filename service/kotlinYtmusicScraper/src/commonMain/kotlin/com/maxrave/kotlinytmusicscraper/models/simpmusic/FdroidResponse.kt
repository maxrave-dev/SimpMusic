package com.maxrave.kotlinytmusicscraper.models.simpmusic

import kotlinx.serialization.Serializable

@Serializable
data class FdroidResponse(
    val packageName: String,
    val suggestedVersionCode: Int,
    val packages: List<FdroidPackage>,
) {
    @Serializable
    data class FdroidPackage(
        val versionName: String,
        val versionCode: Int,
    )
}