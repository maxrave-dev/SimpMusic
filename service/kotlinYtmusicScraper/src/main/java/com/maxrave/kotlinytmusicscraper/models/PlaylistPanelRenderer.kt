package com.maxrave.kotlinytmusicscraper.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistPanelRenderer(
    val title: String?,
    val titleText: Runs?,
    val shortBylineText: Runs?,
    val contents: List<Content>,
    val currentIndex: Int?,
    val isInfinite: Boolean? = null,
    val numItemsToShow: Int?,
    val playlistId: String? = null,
    val continuations: List<Continuation>?,
) {
    @Serializable
    data class Content(
        val playlistPanelVideoWrapperRenderer: PlaylistPanelWrapperVideoRenderer?,
        val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer?,
        val automixPreviewVideoRenderer: AutomixPreviewVideoRenderer?,
    ) {
        @Serializable
        data class PlaylistPanelWrapperVideoRenderer(
            val primaryRenderer: PrimaryRenderer,
        ) {
            @Serializable
            data class PrimaryRenderer(
                val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer?,
            )
        }
    }
}