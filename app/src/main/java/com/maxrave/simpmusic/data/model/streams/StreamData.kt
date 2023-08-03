package com.maxrave.simpmusic.data.model.streams


import com.google.gson.annotations.SerializedName

data class StreamData(
    @SerializedName("audioStreams")
    val audioStreams: List<AudioStream>?,
    @SerializedName("category")
    val category: String?,
    @SerializedName("chapters")
    val chapters: List<Any>?,
    @SerializedName("dash")
    val dash: Any,
    @SerializedName("description")
    val description: String?,
    @SerializedName("dislikes")
    val dislikes: Int?,
    @SerializedName("duration")
    val duration: Int?,
    @SerializedName("hls")
    val hls: String?,
    @SerializedName("lbryId")
    val lbryId: Any?,
    @SerializedName("license")
    val license: String?,
    @SerializedName("likes")
    val likes: Int?,
    @SerializedName("livestream")
    val livestream: Boolean?,
    @SerializedName("previewFrames")
    val previewFrames: List<PreviewFrame?>?,
    @SerializedName("proxyUrl")
    val proxyUrl: String?,
    @SerializedName("relatedStreams")
    val relatedStreams: List<RelatedStream?>?,
    @SerializedName("subtitles")
    val subtitles: List<Any>?,
    @SerializedName("tags")
    val tags: List<String>?,
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("uploadDate")
    val uploadDate: String?,
    @SerializedName("uploader")
    val uploader: String?,
    @SerializedName("uploaderAvatar")
    val uploaderAvatar: String?,
    @SerializedName("uploaderSubscriberCount")
    val uploaderSubscriberCount: Int?,
    @SerializedName("uploaderUrl")
    val uploaderUrl: String?,
    @SerializedName("uploaderVerified")
    val uploaderVerified: Boolean?,
    @SerializedName("videoStreams")
    val videoStreams: List<VideoStream?>?,
    @SerializedName("views")
    val views: Int?,
    @SerializedName("visibility")
    val visibility: String?
)