package com.maxrave.kotlinytmusicscraper.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PipedResponse(
    val audioStreams: List<AudioStream>,
    val videoStreams: List<AudioStream>,
    @SerialName("category")
    val category: String?,
    @SerialName("description")
    val description: String?,
    @SerialName("dislikes")
    val dislikes: Int?,
    @SerialName("duration")
    val duration: Int?,
    @SerialName("proxyUrl")
    val proxyUrl: String?,
    @SerialName("tags")
    val tags: List<String>?,
    @SerialName("thumbnailUrl")
    val thumbnailUrl: String?,
    @SerialName("title")
    val title: String?,
    @SerialName("uploadDate")
    val uploadDate: String?,
    @SerialName("uploader")
    val uploader: String?,
    @SerialName("uploaderAvatar")
    val uploaderAvatar: String?,
    @SerialName("uploaderSubscriberCount")
    val uploaderSubscriberCount: Int?,
    @SerialName("uploaderUrl")
    val uploaderUrl: String?,
    @SerialName("uploaderVerified")
    val uploaderVerified: Boolean?,
    @SerialName("views")
    val views: Int?,
    @SerialName("visibility")
    val visibility: String?
) {
    @Serializable
    data class AudioStream(
        val itag: Int,
        val url: String,
        val bitrate: Int,
        val format: String,
        val quality: String,
        val mimeType: String?,
        val codec: String?,
        val audioTrackId: String?,
        val audioTrackName: String?,
        val audioTrackType: String?,
        val audioTrackLocale: String?,
        val videoOnly: Boolean,
        val initStart: Int,
        val initEnd: Int,
        val indexStart: Int,
        val indexEnd: Int,
        val width: Int,
        val height: Int,
        val fps: Int,
        val contentLength: Int
    )
}
