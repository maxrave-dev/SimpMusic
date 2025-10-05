package com.maxrave.domain.data.model.streams

data class PreviewFrame(
    val durationPerFrame: Int?,
    val frameHeight: Int?,
    val frameWidth: Int?,
    val framesPerPageX: Int?,
    val framesPerPageY: Int?,
    val totalCount: Int?,
    val urls: List<String?>?,
)