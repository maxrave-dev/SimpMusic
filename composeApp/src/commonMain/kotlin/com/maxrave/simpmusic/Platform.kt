package com.maxrave.simpmusic

sealed class Platform {
    object Android : Platform()
    object iOS : Platform()
    object Desktop : Platform()

    fun osName(): String = when (this) {
        Android -> "android"
        iOS -> "iOS"
        Desktop -> System.getProperty("os.name") ?: "jvm"
    }
}

expect fun getPlatform(): Platform

/**
 * True on Android TV, Google TV and Fire TV: a remote with a D-pad, no touch.
 *
 * Deliberately not a [Platform]: a TV is still [Platform.Android] for everything that answers
 * (Media3, Cast, permissions), so a `Platform.AndroidTv` would break every `== Platform.Android`.
 */
expect fun isTv(): Boolean
