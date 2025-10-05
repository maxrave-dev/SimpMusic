package com.maxrave.domain.data.model.spotify

data class Album(
    val albumType: String,
    val artists: List<ArtistX>,
    val availableMarkets: List<String>,
    val externalUrls: ExternalUrlsXXX,
    val href: String,
    val id: String,
    val images: List<Image>,
    val name: String,
    val releaseDate: String,
    val releaseDatePrecision: String,
    val totalTracks: Int,
    val type: String,
    val uri: String,
)