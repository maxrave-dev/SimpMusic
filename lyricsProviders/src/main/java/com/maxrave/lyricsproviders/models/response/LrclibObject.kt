package com.maxrave.lyricsproviders.models.response

import kotlinx.serialization.Serializable

@Serializable
data class LrclibObject(
    val id: Int,
    val name: String,
    val trackName: String?,
    val artistName: String?,
    val albumName: String?,
    val duration: Float,
    val instrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?,
)