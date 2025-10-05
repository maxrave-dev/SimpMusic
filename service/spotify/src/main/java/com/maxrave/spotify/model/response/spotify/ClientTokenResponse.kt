package com.maxrave.spotify.model.response.spotify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientTokenResponse(
    @SerialName("response_type")
    val responseType: String,
    @SerialName("granted_token")
    val grantedToken: GrantedToken
) {
    @Serializable
    data class GrantedToken(
        val token: String,
        @SerialName("expires_after_seconds")
        val expiresAfterSeconds: Int,
        @SerialName("refresh_after_seconds")
        val refreshAfterSeconds: Int,
        val domains: List<Domain>
    ) {
        @Serializable
        data class Domain(
            val domain: String
        )
    }
}