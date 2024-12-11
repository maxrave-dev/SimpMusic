package com.maxrave.simpmusic.data.model.explore.mood.genre

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class ItemsPlaylist(
    @SerializedName("contents")
    val contents: List<Content>,
    @SerializedName("header")
    val header: String,
    @SerializedName("type")
    val type: String,
)