package com.maxrave.spotify.auth

import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import java.util.Date

/**
 * Implementation of Time-based One-Time Password for Spotify authentication
 */
object SpotifyTotp {
    private const val SECRET_STRING = "GU2TANZRGQ2TQNJTGQ4DONBZHE2TSMRSGQ4DMMZQGMZDSMZUG4"

    /**
     * Generate a TOTP value for the given timestamp
     */
    fun at(timestamp: Long): String = generate(timestamp)

    private fun generate(timestamp: Long): String {
        val googleAuthenticator = GoogleAuthenticator(SECRET_STRING.toByteArray())
        return googleAuthenticator.generate(timestamp = Date(timestamp))
    }
}