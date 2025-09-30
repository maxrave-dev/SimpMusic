package com.maxrave.domain.data.model.account

import com.maxrave.domain.data.model.searchResult.songs.Thumbnail

data class AccountInfo(
    val name: String,
    val email: String,
    val thumbnails: List<Thumbnail>,
)