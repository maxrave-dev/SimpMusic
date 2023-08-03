package com.maxrave.simpmusic.data.model.streams


import com.google.gson.annotations.SerializedName

data class AudioStream(
    @SerializedName("audioTrackId")
    val audioTrackId: Any,
    @SerializedName("audioTrackLocale")
    val audioTrackLocale: Any,
    @SerializedName("audioTrackName")
    val audioTrackName: Any,
    @SerializedName("audioTrackType")
    val audioTrackType: Any,
    @SerializedName("bitrate")
    val bitrate: Int?,
    @SerializedName("codec")
    val codec: String?,
    @SerializedName("contentLength")
    val contentLength: Int?,
    @SerializedName("format")
    val format: String?,
    @SerializedName("fps")
    val fps: Int?,
    @SerializedName("height")
    val height: Int?,
    @SerializedName("indexEnd")
    val indexEnd: Int?,
    @SerializedName("indexStart")
    val indexStart: Int?,
    @SerializedName("initEnd")
    val initEnd: Int?,
    @SerializedName("initStart")
    val initStart: Int?,
    @SerializedName("itag")
    val itag: Int?,
    @SerializedName("mimeType")
    val mimeType: String?,
    @SerializedName("quality")
    val quality: String?,
    @SerializedName("url")
    val url: String?,
    @SerializedName("videoOnly")
    val videoOnly: Boolean?,
    @SerializedName("width")
    val width: Int?
)