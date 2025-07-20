package com.maxrave.spotify.auth

import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import java.nio.ByteBuffer
import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor
import kotlin.math.pow

/**
 * Implementation of Time-based One-Time Password for Spotify authentication
 * Logic from https://github.com/misiektoja/spotify_monitor/blob/main/debug/spotify_monitor_totp_test.py
 */
object SpotifyTotp {
    /**
     * Version 5 secret
     */
    private const val SECRET_STRING = "GU2TANZRGQ2TQNJTGQ4DONBZHE2TSMRSGQ4DMMZQGMZDSMZUG4"

    /**
     * Version 10 secret
     */
    private const val SECRET_STRING_V10 = "GUZDCMBQGQ4TCMJQGQ3DMNJRGIZDQNJRGE4TSMBXHEYTCNBYGA3TKNRSGEZDKNJRHAYQ"

    private const val SECRET_STRING_V17 = "GQ3DONRVGUYTANBXGUZTGMJSGY4DCNJRHA2DMNZWGMYTCMJRGEZDQNZVGI2DCNZYGEYTQNRRHA3DQNZRGI3TCMJRHE3DSMQ"

    /**
     * Generate a TOTP value for the given timestamp
     */
    fun at(timestamp: Long): String = generate(timestamp)

    private fun generate(timestamp: Long): String {
        val googleAuthenticator = GoogleAuthenticator(SECRET_STRING_V17.toByteArray())
        return googleAuthenticator.generate(timestamp = Date(timestamp))
    }

    /**
     * Version 8
     */
    private fun generateTotp(serverTimeSeconds: Long): String {
        val secret = "449443649084886328893534571041315"
        val period = 30
        val digits = 6

        // Calculate counter (number of time steps)
        val counter = floor(serverTimeSeconds.toDouble() / period).toLong()

        // Convert counter to an 8-byte array (big-endian)
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()

        // Compute HMAC-SHA1
        val secretBytes = secret.toByteArray(Charsets.UTF_8)
        val keySpec = SecretKeySpec(secretBytes, "HmacSHA1")
        val hmac = Mac.getInstance("HmacSHA1")
        hmac.init(keySpec)
        val hmacResult = hmac.doFinal(counterBytes)

        // Dynamic truncation
        val offset = hmacResult[hmacResult.size - 1].toInt() and 0x0F
        val binary =
            ((hmacResult[offset].toInt() and 0x7F) shl 24) or
                ((hmacResult[offset + 1].toInt() and 0xFF) shl 16) or
                ((hmacResult[offset + 2].toInt() and 0xFF) shl 8) or
                (hmacResult[offset + 3].toInt() and 0xFF)

        // Generate code with the specified number of digits
        val code = binary % 10.0.pow(digits).toInt()
        return code.toString().padStart(digits, '0')
    }
}