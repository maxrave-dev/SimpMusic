package com.maxrave.simpmusic.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.metadata.Line
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@ProvidedTypeConverter
class Converters {
    // Json serialization for Room
    val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

    @TypeConverter
    fun fromString(value: String?): List<String>? =
        try {
            value?.let { json.decodeFromString<List<String>>(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    @TypeConverter
    fun fromArrayList(list: List<String>?): String? = list?.let { json.encodeToString(it) }

    // No use in database
    fun fromListIntToString(list: List<Int>?): String? = list?.let { json.encodeToString(list) }

    fun fromStringToListInt(value: String?): List<Int>? =
        try {
            value?.let { json.decodeFromString<List<Int>>(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    @TypeConverter
    fun fromStringToListTrack(value: String?): List<Track>? =
        try {
            value?.let {
                json.decodeFromString<List<Track>>(value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    @TypeConverter
    fun fromListTrackToString(list: List<Track>?): String? =
        list?.let {
            json.encodeToString(it)
        }

    @TypeConverter
    fun fromListLineToString(list: List<Line>?): String? =
        list?.let {
            json.encodeToString(it)
        }

    @TypeConverter
    fun fromStringToListLine(value: String?): List<Line>? =
        try {
            value?.let {
                json.decodeFromString<List<Line>>(value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    @TypeConverter
    fun fromStringNull(value: String?): List<String?>? =
        try {
            value?.let {
                json.decodeFromString<List<String?>>(value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    @TypeConverter
    fun fromArrayListNull(list: List<String?>?): String? =
        list?.let {
            json.encodeToString(it)
        }

    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? =
        if (value != null) {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC)
        } else {
            null
        }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? = date?.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun fromListMapToString(list: List<Map<String, String>>): String = json.encodeToString(list)

    @TypeConverter
    fun fromStringToListMap(value: String): List<Map<String, String>> = json.decodeFromString(value)
}