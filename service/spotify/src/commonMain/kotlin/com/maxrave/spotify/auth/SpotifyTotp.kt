package com.maxrave.spotify.auth

import io.ktor.utils.io.core.*
import kotlin.io.encoding.Base64

/**
 * Implementation of Time-based One-Time Password for Spotify authentication
 * Logic from https://github.com/misiektoja/spotify_monitor/blob/main/debug/spotify_monitor_totp_test.py
 */
object SpotifyTotp {
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    val TOTP_SECRET_V22 = 22 to listOf(99,101,119,123,69,120,91,123,97,74,53,48,76,102,55,69,110,54)

    /**
     * Generate a TOTP value for the given timestamp
     */
    fun at(timestamp: Long, totpSecret: Pair<Int, List<Int>>?): String = generate(timestamp, totpSecret)

    private fun generate(timestamp: Long, totpSecret: Pair<Int, List<Int>>?): String {
        val secret = totpSecret?.let { generateSecret(it) } ?: generateSecret(TOTP_SECRET_V22)
        return generateTotp(secret, timestamp)
    }

    private fun generateSecret(totpSecret: Pair<Int, List<Int>>): String {

        val secretCipherBytes = totpSecret.second
        println("TOTP cipher: $secretCipherBytes")

        // Transform bytes: e ^ ((t % 33) + 9) for each byte
        val transformed = secretCipherBytes.mapIndexed { index, byte ->
            byte xor ((index % 33) + 9)
        }

        // Join numbers as string and convert to hex
        val joined = transformed.joinToString("")
        val hexStr = joined.toByteArray().toHexString()

        // Convert to base32 secret (without padding)
        val secret = base64ToBase32(Base64.encode(hexStr.hexToByteArray()))
            .trimEnd('=')

        println("Computed secret: $secret")
        return secret
    }

    private fun base64ToBase32(base64: String): String {
        val bytes = Base64.decode(base64)
        return base32Encode(bytes)
    }

    private fun base32Encode(data: ByteArray): String {
        if (data.isEmpty()) return ""

        val result = StringBuilder()
        var bits = 0
        var value = 0

        for (byte in data) {
            value = (value shl 8) or (byte.toInt() and 0xFF)
            bits += 8

            while (bits >= 5) {
                result.append(BASE32_ALPHABET[(value shr (bits - 5)) and 0x1F])
                bits -= 5
            }
        }

        if (bits > 0) {
            result.append(BASE32_ALPHABET[(value shl (5 - bits)) and 0x1F])
        }

        // Add padding
        while (result.length % 8 != 0) {
            result.append('=')
        }

        return result.toString()
    }
}

expect fun generateTotp(secret: String, timestamp: Long): String