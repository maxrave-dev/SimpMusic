package com.maxrave.simpmusic.data.model.spotify

data class Item(
    val album: Album?,
    val artists: List<ArtistX?>?,
    val availableMarkets: List<String?>?,
    val discNumber: Int?,
    val durationMs: Int?,
    val explicit: Boolean?,
    val externalIds: ExternalIds?,
    val externalUrls: ExternalUrlsXXX?,
    val href: String?,
    val id: String?,
    val isLocal: Boolean?,
    val name: String?,
    val popularity: Int?,
    val previewUrl: String?,
    val trackNumber: Int?,
    val type: String?,
    val uri: String?,
)