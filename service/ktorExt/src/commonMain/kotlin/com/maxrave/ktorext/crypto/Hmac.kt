package com.maxrave.ktorext.crypto

expect class Hmac(
    algorithm: String,
    secretKey: String,
) {
    fun getMacTimestampPair(uri: String): Pair<String, String>
    fun generateHmac(data: String): String
    fun validateHmac(
        data: String,
        hmac: String,
    ): Boolean
    fun isValidTimestamp(timestamp: String): Boolean
}

object HmacUri {
    const val BASE_HMAC_URI = "/v1"
    const val TRANSLATED_HMAC_URI = "/v1/translated"
    const val VOTE_HMAC_URI = "/v1/vote"
    const val VOTE_TRANSLATED_HMAC_URI = "/v1/translated/vote"
}