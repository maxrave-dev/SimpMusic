package com.maxrave.kotlinytmusicscraper.models.musixmatch

import kotlinx.serialization.Serializable

@Serializable
data class MusixmatchCredential(
    val message: Message
) {
    @Serializable
    data class Message(
        val body: List<Credential>,
        val header: Header
    ) {
        @Serializable
        data class Credential(
            val credential : CredentialData
        ) {
            @Serializable
            data class CredentialData(
                val type: String,
                val action: String,
                val email: String,
                val id: String,
                val error: Error?,
                val account: Account?
            ) {
                @Serializable
                data class Error(
                    val status_code: Int,
                    val description: String,
                )
                @Serializable
                data class Account(
                    val first_name: String,
                    val last_name: String,
                    val email: String,
                    val id: String,
                )
            }
        }

        @Serializable
        data class Header(
            val status_code: Int
        )
    }
}