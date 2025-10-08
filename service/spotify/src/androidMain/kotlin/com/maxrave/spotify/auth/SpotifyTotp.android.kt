package com.maxrave.spotify.auth

import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import io.ktor.utils.io.core.*
import java.util.*

actual fun generateTotp(secret: String, timestamp: Long): String {
    val googleAuthenticator = GoogleAuthenticator(secret.toByteArray())
    return googleAuthenticator.generate(timestamp = Date(timestamp))
}