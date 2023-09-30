package com.maxrave.kotlinytmusicscraper.models.spotify

import kotlinx.serialization.Serializable

@Serializable
data class AccessToken(
    val clientId: String? = null,
    val accessToken: String? = null,
    val accessTokenExpirationTimestampMs: Long? = null,
    val isAnonymous: Boolean? = null,
)