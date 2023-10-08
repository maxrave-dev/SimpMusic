package com.maxrave.simpmusic.data.db.entities

import androidx.room.ColumnInfo
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

    @ColumnInfo(name = "synced_with_youtube_playlist", defaultValue = "0")
    val syncedWithYouTubePlaylist: Int = 0,
    val youtubePlaylistId: String? = null,
    @ColumnInfo(name = "youtube_sync_state", defaultValue = "0")
    val syncState : Int = YouTubeSyncState.NotSynced,

    val tracks: List<String>? = null,

    //Only synced with YouTube playlist
    //val listSetVideoId: List<String>? = null,
) {
    object YouTubeSyncState {
        const val NotSynced = 0
        const val Syncing = 1
        const val Synced = 2
    }
}