package com.maxrave.lyricsproviders.models.body

import kotlinx.serialization.Serializable

@Serializable
data class MusixmatchCredentialsBody(
    val credential_list: List<Credential>,
) {
    @Serializable
    data class Credential(
        val credential: CredentialData,
    ) {
        @Serializable
        data class CredentialData(
            val type: String = "mxm",
            val action: String = "login",
            val email: String,
            val password: String,
        )
    }
}