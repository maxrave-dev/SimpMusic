package com.maxrave.simpmusic.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

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
    @ColumnInfo(name = "expired_time", defaultValue = "0")
    val expiredTime: LocalDateTime = LocalDateTime.now(),
    val cpn: String?,
    val audioUrl : String? = null,
    val videoUrl : String? = null,
)