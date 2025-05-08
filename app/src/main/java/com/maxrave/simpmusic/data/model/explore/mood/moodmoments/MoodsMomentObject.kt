package com.maxrave.simpmusic.data.model.explore.mood.moodmoments

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class MoodsMomentObject(
    @SerializedName("endpoint")
    val endpoint: String,
    @SerializedName("header")
    val header: String,
    @SerializedName("items")
    val items: List<Item>,
    @SerializedName("params")
    val params: String,
)