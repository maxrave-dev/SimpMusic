package com.maxrave.simpmusic.data.model.browse.playlist

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class Author(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
)