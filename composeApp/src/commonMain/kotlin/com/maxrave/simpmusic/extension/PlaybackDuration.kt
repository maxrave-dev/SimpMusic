package com.maxrave.simpmusic.extension

/** Locale-independent mm:ss, keeping total minutes for tracks longer than an hour. */
internal fun formatPlaybackDuration(milliseconds: Long): String {
    require(milliseconds >= 0) { "Playback duration must be non-negative" }
    val totalSeconds = milliseconds / 1_000
    val minutes = (totalSeconds / 60).toString().padStart(2, '0')
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
