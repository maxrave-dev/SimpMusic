package com.maxrave.simpmusic.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "format")
data class FormatEntity(
    @PrimaryKey val videoId: String,
    val itag: Int? = null,
    val mimeType: String? = null,
    val bitrate: Long? = null,
    val contentLength: Long? = null,
    val lastModified: Long? = null,
    val loudnessDb: Float? = null,
    val uploader: String? = null,
    val uploaderId: String? = null,
    val uploaderSubCount: String? = null,
    val uploaderThumbnail: String? = null,
    val lengthSeconds: Int? = null,
    val description: String? = null,
    val youtubeCaptionsUrl: String? = null,
    val playbackTrackingVideostatsPlaybackUrl: String? = null,
    val playbackTrackingAtrUrl: String? = null,
    val playbackTrackingVideostatsWatchtimeUrl: String? = null,
    val cpn: String? = null,
)