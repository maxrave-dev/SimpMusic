package com.maxrave.simpmusic.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "new_format")
data class NewFormatEntity(
    @PrimaryKey val videoId: String,
    val itag: Int,
    val mimeType: String?,
    val codecs: String?,
    val bitrate: Int?,
    val sampleRate: Int?,
    val contentLength: Long?,
    val loudnessDb: Float?,
    val lengthSeconds: Int?,
    val playbackTrackingVideostatsPlaybackUrl: String?,
    val playbackTrackingAtrUrl: String?,
    val playbackTrackingVideostatsWatchtimeUrl: String?,
    val cpn: String?,
)