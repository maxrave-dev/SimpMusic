package com.maxrave.simpmusic.data.model.explore.mood.genre

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class Title(
    @SerializedName("subtitle")
    val subtitle: String,
    @SerializedName("title")
    val title: String,
)