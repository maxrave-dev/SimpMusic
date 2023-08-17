package com.maxrave.simpmusic.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "format")
data class FormatEntity(
    @PrimaryKey val videoId: String,
    val itag: Int? = null,
    val mimeType: String? = null,
    val bitrate: Long? = null,
    val contentLength: Long? = null,
    val lastModified: Long? = null,
    val loudnessDb: Float? = null
)