package com.maxrave.domain.data.model.browse.artist

data class Singles(
    val browseId: String,
    val params: String,
    val results: List<ResultSingle>,
)