package com.maxrave.simpmusic.data.model.browse.playlist

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class AlbumPlaylist(
    @SerializedName("id")
    val id: Any,
    @SerializedName("name")
    val name: String,
)