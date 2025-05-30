package com.maxrave.simpmusic.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.maxrave.simpmusic.data.type.ArtistType
import com.maxrave.simpmusic.data.type.RecentlyType
import java.time.LocalDateTime

@Entity(tableName = "artist")
data class ArtistEntity(
    @PrimaryKey(autoGenerate = false)
    val channelId: String,
    val name: String,
    val thumbnails: String?,
    val followed: Boolean = false,
    val inLibrary: LocalDateTime = LocalDateTime.now(),
) : RecentlyType, ArtistType {
    override fun objectType() = RecentlyType.Type.ARTIST
}