package com.maxrave.simpmusic.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.maxrave.simpmusic.common.DownloadState
import java.time.LocalDateTime

@Entity(tableName = "playlist")
data class PlaylistEntity (
    @PrimaryKey(autoGenerate = false)
    val id: String = "",
    val author: String? = "",
    val description: String = "",
    val duration: String = "",
    val durationSeconds: Int = 0,
    val privacy: String = "PRIVATE",
    val thumbnails: String = "",
    val title: String,
    val trackCount: Int = 0,
    val tracks: List<String>? = null,
    val year: String? = null,
    val liked: Boolean = false,
    val inLibrary: LocalDateTime = LocalDateTime.now(),
    val downloadState: Int = DownloadState.STATE_NOT_DOWNLOADED,
        )