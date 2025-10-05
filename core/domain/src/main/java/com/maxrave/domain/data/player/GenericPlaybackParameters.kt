package com.maxrave.domain.data.player

/**
 * Generic playback parameters wrapper
 */
data class GenericPlaybackParameters(
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
) {
    companion object {
        val DEFAULT = GenericPlaybackParameters()
    }
}