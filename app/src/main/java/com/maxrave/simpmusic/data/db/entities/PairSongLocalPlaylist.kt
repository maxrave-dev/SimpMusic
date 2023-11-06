package com.maxrave.simpmusic.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "pair_song_local_playlist",
    foreignKeys = [
        ForeignKey(
            entity = LocalPlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["videoId"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE)
    ]
)
data class PairSongLocalPlaylist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(index = true) val playlistId: Long,
    @ColumnInfo(index = true) val songId: String,
    val position: Int = 0,
    val inPlaylist: LocalDateTime = LocalDateTime.now()
)