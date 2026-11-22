package com.maxrave.simpmusic.extension

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal fun formatLocalTimestamp(epochMillis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone)
    fun Int.padded(width: Int = 2) = toString().padStart(width, '0')
    return "${local.year.padded(4)}-${local.month.ordinal.plus(1).padded()}-${local.day.padded()} " +
        "${local.hour.padded()}:${local.minute.padded()}:${local.second.padded()}"
}
