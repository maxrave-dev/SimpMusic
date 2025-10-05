package com.maxrave.domain.data.model.streams

data class AudioStream(
    val audioTrackId: Any,
    val audioTrackLocale: Any,
    val audioTrackName: Any,
    val audioTrackType: Any,
    val bitrate: Int?,
    val codec: String?,
    val contentLength: Int?,
    val format: String?,
    val fps: Int?,
    val height: Int?,
    val indexEnd: Int?,
    val indexStart: Int?,
    val initEnd: Int?,
    val initStart: Int?,
    val itag: Int?,
    val mimeType: String?,
    val quality: String?,
    val url: String?,
    val videoOnly: Boolean?,
    val width: Int?,
)