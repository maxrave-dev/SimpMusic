package com.maxrave.simpmusic.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.metadata.Line
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@ProvidedTypeConverter
class Converters {
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        val listType: Type = object : TypeToken<ArrayList<String?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: List<String>?): String? {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromStringToListTrack(value: String?): List<Track>? {
        val listType: Type = object : TypeToken<List<Track?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromListTrackToString(list: List<Track>?): String? {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromListLineToString(list: List<Line>?): String? {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromStringToListLine(value: String?): List<Line>? {
        val listType: Type = object : TypeToken<ArrayList<Line?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringNull(value: String?): List<String?>? {
        val listType: Type = object : TypeToken<ArrayList<String?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayListNull(list: List<String?>?): String? {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromListLineNullToString(list: List<Line?>?): String? {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromStringNullToListLine(value: String?): List<Line?>? {
        val listType: Type = object : TypeToken<ArrayList<Line?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? =
        if (value != null) LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC)
        else null

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? =
        date?.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
}