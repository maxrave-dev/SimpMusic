package com.maxrave.simpmusic.extension

import kotlin.math.roundToInt

internal fun formatPlaybackSpeed(speed: Float): String {
    val tenths = (speed * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

internal fun formatGroupedNumber(value: Int?): String = value?.toString()?.reversed()?.chunked(3)?.joinToString(",")?.reversed() ?: "null"
