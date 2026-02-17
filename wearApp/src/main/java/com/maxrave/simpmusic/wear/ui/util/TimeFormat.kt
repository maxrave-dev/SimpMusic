package com.maxrave.simpmusic.wear.ui.util

import kotlin.math.max

fun formatDurationMs(durationMs: Long): String {
    val totalSeconds = max(0L, durationMs) / 1000L
    val seconds = (totalSeconds % 60).toInt()
    val minutes = ((totalSeconds / 60) % 60).toInt()
    val hours = (totalSeconds / 3600).toInt()

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

