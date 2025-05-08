package com.maxrave.simpmusic.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class GoogleAccountEntity(
    @PrimaryKey(autoGenerate = false)
    val email: String = "",
    val name: String = "",
    val thumbnailUrl: String = "",
    val cache: String? = null,
    val isUsed: Boolean = false,
)