package com.maxrave.lyricsproviders.utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun fromString(
    value: String?,
    json: Json,
): List<String>? {
    return try {
        json.decodeFromString<List<String>>(value ?: return null)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun fromArrayListNull(
    list: List<String?>?,
    json: Json,
): String? {
    return try {
        json.encodeToString(list?.filterNotNull() ?: return null)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}