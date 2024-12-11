package com.maxrave.kotlinytmusicscraper.models

data class AccountInfo(
    val name: String,
    val email: String,
    val thumbnails: List<Thumbnail>,
)