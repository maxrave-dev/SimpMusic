package com.maxrave.data.parser

import com.maxrave.domain.data.model.browse.artist.ResultPlaylist
import com.maxrave.domain.data.model.home.chart.Artists
import com.maxrave.domain.data.model.home.chart.Chart
import com.maxrave.domain.data.model.home.chart.ChartItemPlaylist
import com.maxrave.domain.data.model.home.chart.ItemArtist
import com.maxrave.kotlinytmusicscraper.models.MusicCarouselShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.SectionListRenderer

internal fun parseChart(data: SectionListRenderer?): Chart? {
    val content = data?.contents ?: return null
    val listArtistItem: ArrayList<ItemArtist> = arrayListOf()
    val listItemPlaylist: ArrayList<ChartItemPlaylist> = arrayListOf()
    content.forEach {
        val contents = it.musicCarouselShelfRenderer?.contents ?: return@forEach
        if (contents
                .firstOrNull()
                ?.musicResponsiveListItemRenderer
                ?.navigationEndpoint
                ?.browseEndpoint
                ?.browseEndpointContextSupportedConfigs
                ?.browseEndpointContextMusicConfig
                ?.pageType ==
            "MUSIC_PAGE_TYPE_ARTIST"
        ) {
            parseArtistChart(contents)?.let { listArtistItem.addAll(it) }
        } else {
            listItemPlaylist.add(
                ChartItemPlaylist(
                    title =
                        it.musicCarouselShelfRenderer
                            ?.header
                            ?.musicCarouselShelfBasicHeaderRenderer
                            ?.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: "",
                    playlists =
                        contents.map { contentItem ->
                            ResultPlaylist(
                                id =
                                    contentItem.musicTwoRowItemRenderer
                                        ?.navigationEndpoint
                                        ?.browseEndpoint
                                        ?.browseId ?: return null,
                                thumbnails =
                                    contentItem.musicTwoRowItemRenderer
                                        ?.thumbnailRenderer
                                        ?.musicThumbnailRenderer
                                        ?.thumbnail
                                        ?.thumbnails
                                        ?.toListThumbnail() ?: emptyList(),
                                title =
                                    contentItem.musicTwoRowItemRenderer
                                        ?.title
                                        ?.runs
                                        ?.firstOrNull()
                                        ?.text ?: "",
                                author =
                                    contentItem.musicTwoRowItemRenderer?.subtitle?.runs?.joinToString(
                                        separator = " ",
                                    ) { it.text } ?: "",
                            )
                        },
                ),
            )
        }
    }
    return Chart(
        artists = Artists(itemArtists = listArtistItem, playlist = ""),
        countries = null,
        listChartItem = listItemPlaylist,
    )
}

internal fun parseArtistChart(contents: List<MusicCarouselShelfRenderer.Content>?): ArrayList<ItemArtist>? =
    if (contents != null) {
        val artists: ArrayList<ItemArtist> = arrayListOf()
        for (i in contents.indices) {
            val content = contents[i]
            if (content.musicResponsiveListItemRenderer != null) {
                val title =
                    content.musicResponsiveListItemRenderer
                        ?.flexColumns
                        ?.get(0)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.get(
                            0,
                        )?.text
                val subscriber =
                    content.musicResponsiveListItemRenderer
                        ?.flexColumns
                        ?.get(1)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.get(
                            0,
                        )?.text
                val thumbnails =
                    content.musicResponsiveListItemRenderer
                        ?.thumbnail
                        ?.musicThumbnailRenderer
                        ?.thumbnail
                        ?.thumbnails
                val artistId =
                    content.musicResponsiveListItemRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseId
                artists.add(
                    ItemArtist(
                        browseId = artistId ?: "",
                        rank = "${i + 1}",
                        subscribers = subscriber ?: "",
                        thumbnails = thumbnails?.toListThumbnail() ?: listOf(),
                        title = title ?: "",
                        trend = "",
                    ),
                )
            }
        }
        artists
    } else {
        null
    }