package com.maxrave.simpmusic.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_info")
data class SongInfoEntity(
    @PrimaryKey(autoGenerate = false) val videoId: String,
    val author: String? = null,
    val authorId: String? = null,
    val authorThumbnail: String? = null,
    val description: String? = null,
    val subscribers: String? = null,
    val viewCount: Int? = null,
    val uploadDate: String? = null,
    val like: Int? = null,
    val dislike: Int? = null,
)