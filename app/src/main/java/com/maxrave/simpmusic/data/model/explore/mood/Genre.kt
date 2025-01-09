package com.maxrave.simpmusic.data.model.explore.mood

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class Genre(
    @SerializedName("params")
    val params: String,
    @SerializedName("title")
    val title: String,
)