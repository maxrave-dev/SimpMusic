package com.maxrave.simpmusic.data.model.browse.artist

data class Albums(
    val browseId: Any,
    val results: List<ResultAlbum>,
    val params: String,
)