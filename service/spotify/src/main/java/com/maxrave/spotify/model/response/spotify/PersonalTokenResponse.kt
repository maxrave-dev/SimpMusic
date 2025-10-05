package com.maxrave.spotify.model.response.spotify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonalTokenResponse(
    @SerialName("accessToken")
    val accessToken: String = "",
    @SerialName("accessTokenExpirationTimestampMs")
    val accessTokenExpirationTimestampMs: Long = 0,
    @SerialName("isAnonymous")
    val isAnonymous: Boolean = false,
    @SerialName("clientId")
    val clientId: String? = null
)