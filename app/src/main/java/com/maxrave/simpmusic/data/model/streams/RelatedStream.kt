package com.maxrave.simpmusic.data.model.streams


import com.google.gson.annotations.SerializedName

data class RelatedStream(
    @SerializedName("duration")
    val duration: Int?,
    @SerializedName("isShort")
    val isShort: Boolean?,
    @SerializedName("shortDescription")
    val shortDescription: Any,
    @SerializedName("thumbnail")
    val thumbnail: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("uploaded")
    val uploaded: Long?,
    @SerializedName("uploadedDate")
    val uploadedDate: String?,
    @SerializedName("uploaderAvatar")
    val uploaderAvatar: String?,
    @SerializedName("uploaderName")
    val uploaderName: String?,
    @SerializedName("uploaderUrl")
    val uploaderUrl: String?,
    @SerializedName("uploaderVerified")
    val uploaderVerified: Boolean?,
    @SerializedName("url")
    val url: String?,
    @SerializedName("views")
    val views: Any
)