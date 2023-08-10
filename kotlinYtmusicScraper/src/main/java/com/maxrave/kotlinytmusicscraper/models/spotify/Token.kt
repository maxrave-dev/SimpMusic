package com.maxrave.kotlinytmusicscraper.models.spotify


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Token(
    @SerialName("access_token")
    val accessToken: String?,
    @SerialName("expires_in")
    val expiresIn: Int?,
    @SerialName("token_type")
    val tokenType: String?
)