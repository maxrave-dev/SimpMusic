package com.maxrave.kotlinytmusicscraper.models.musixmatch

import kotlinx.serialization.Serializable

@Serializable
data class UserTokenResponse(
    val message: Message
) {
    @Serializable
    data class Message(
        val body: Body,
        val header: Header
    ) {
        @Serializable
        data class Body(
            val user_token: String
        )

        @Serializable
        data class Header(
            val status_code: Int
        )
    }
}