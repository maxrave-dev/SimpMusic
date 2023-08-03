package com.maxrave.simpmusic.data.model.streams


import com.google.gson.annotations.SerializedName

data class PreviewFrame(
    @SerializedName("durationPerFrame")
    val durationPerFrame: Int?,
    @SerializedName("frameHeight")
    val frameHeight: Int?,
    @SerializedName("frameWidth")
    val frameWidth: Int?,
    @SerializedName("framesPerPageX")
    val framesPerPageX: Int?,
    @SerializedName("framesPerPageY")
    val framesPerPageY: Int?,
    @SerializedName("totalCount")
    val totalCount: Int?,
    @SerializedName("urls")
    val urls: List<String?>?
)