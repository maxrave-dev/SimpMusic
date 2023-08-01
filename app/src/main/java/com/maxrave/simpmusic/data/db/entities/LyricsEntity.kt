package com.maxrave.simpmusic.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.maxrave.simpmusic.data.model.metadata.Line

@Entity(tableName = "lyrics")
data class LyricsEntity (
    @PrimaryKey(autoGenerate = false) val videoId: String,
    @SerializedName("error")
    val error: Boolean,
    @SerializedName("lines")
    val lines: List<Line>?,
    @SerializedName("syncType")
    val syncType: String?
)