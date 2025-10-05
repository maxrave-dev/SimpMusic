package com.maxrave.domain.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.maxrave.domain.data.model.metadata.Line

@Entity(tableName = "translated_lyrics")
data class TranslatedLyricsEntity(
    @PrimaryKey(autoGenerate = false) val videoId: String,
    val language: String = "en",
    val error: Boolean,
    val lines: List<Line>?,
    val syncType: String?,
)