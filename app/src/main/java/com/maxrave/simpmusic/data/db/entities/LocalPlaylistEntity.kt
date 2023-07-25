package com.maxrave.simpmusic.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.maxrave.simpmusic.common.DownloadState
import java.time.LocalDateTime


@Entity(tableName = "local_playlist")
data class LocalPlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val thumbnail: String? = null,
    val inLibrary: LocalDateTime = LocalDateTime.now(),
    val downloadState: Int = DownloadState.STATE_NOT_DOWNLOADED,

    val tracks: List<String>? = null,
)