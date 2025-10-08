package com.maxrave.ktorext.crypto

actual class Hmac actual constructor(algorithm: String, secretKey: String) {
    actual fun getMacTimestampPair(uri: String): Pair<String, String> {
        TODO("Not yet implemented")
    }

    actual fun generateHmac(data: String): String {
        TODO("Not yet implemented")
    }

    actual fun validateHmac(data: String, hmac: String): Boolean {
        TODO("Not yet implemented")
    }

    actual fun isValidTimestamp(timestamp: String): Boolean {
        TODO("Not yet implemented")
    }
}