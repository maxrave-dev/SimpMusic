package org.simpmusic.lyrics

import android.util.Log
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HmacService {
    // Secret key for HMAC generation and validation of "api-lyrics.simpmusic.org" endpoints
    private val secretKey: String = "simpmusic-lyrics"

    private var tokenTtl: Long = 300000 // 5 minutes in milliseconds

    private val algorithm = "HmacSHA256"

    // Lazy initialization of Mac to ensure secretKey has been injected
    private val mac: Mac by lazy {
        try {
            Mac.getInstance(algorithm).apply {
                init(SecretKeySpec(secretKey.toByteArray(), algorithm))
            }
        } catch (e: Exception) {
            Log.e("HmacService", "Failed to initialize HMAC: ${e.message}", e)
            throw RuntimeException("Failed to initialize HMAC", e)
        }
    }

    fun getMacTimestampPair(uri: String): Pair<String, String> {
        val timestamp = Instant.now().toEpochMilli().toString()
        val data = "$timestamp$uri"
        val hmac = this.generateHmac(data)
        Log.d("HmacService", "Generated HMAC: $hmac for URI: $uri at timestamp: $timestamp")
        return hmac to timestamp
    }

    /**
     * Generate HMAC token for given data
     *
     * @param data The data to generate HMAC for
     * @return Base64 encoded HMAC token
     */
    fun generateHmac(data: String): String = Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray()))

    /**
     * Validate HMAC token for given data
     *
     * @param data The data that was used to generate HMAC
     * @param hmac The HMAC token to validate
     * @return True if HMAC is valid, false otherwise
     */
    fun validateHmac(
        data: String,
        hmac: String,
    ): Boolean {
        val calculatedHmac = generateHmac(data)
        return calculatedHmac == hmac
    }

    /**
     * Validate timestamp to prevent replay attacks
     *
     * @param timestamp The timestamp to validate (in milliseconds)
     * @return True if timestamp is within allowed time window
     */
    fun isValidTimestamp(timestamp: String): Boolean {
        val requestTime = timestamp.toLongOrNull() ?: return false
        val currentTime = System.currentTimeMillis()
        return (currentTime - requestTime) < tokenTtl
    }

    companion object {
        const val BASE_HMAC_URI = "/v1"
        const val TRANSLATED_HMAC_URI = "/v1/translated"
        const val VOTE_HMAC_URI = "/v1/vote"
        const val VOTE_TRANSLATED_HMAC_URI = "/v1/translated/vote"
    }
}