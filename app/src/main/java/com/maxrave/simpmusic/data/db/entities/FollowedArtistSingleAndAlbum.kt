package com.maxrave.simpmusic.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "followed_artist_single_and_album")
data class FollowedArtistSingleAndAlbum(
    @PrimaryKey(autoGenerate = false) val channelId: String,
    val name: String,
    val single: List<Map<String, String>> = listOf(),
    val album: List<Map<String, String>> = listOf(),
)