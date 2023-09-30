package com.maxrave.simpmusic.data.parser

import com.maxrave.kotlinytmusicscraper.models.GridRenderer
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult

fun parseLibraryPlaylist(input: List<GridRenderer.Item>): ArrayList<PlaylistsResult> {
    val list : ArrayList<PlaylistsResult> = arrayListOf()
    if (input.isNotEmpty()) {
        for (i in input.indices) {
            input[i].musicTwoRowItemRenderer?.let {
                if (it.navigationEndpoint.browseEndpoint?.browseId != "VLSE" && it.navigationEndpoint.browseEndpoint?.browseId != null) {
                    list.add(PlaylistsResult(
                        author = it.subtitle?.runs?.get(0)?.text ?: "",
                        browseId = it.navigationEndpoint.browseEndpoint?.browseId ?: "",
                        category = "",
                        itemCount = "",
                        resultType = "",
                        thumbnails = it.thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail() ?: listOf(),
                        title = it.title.runs?.get(0)?.text ?: ""
                    ))
                }
            }
        }
    }
    return list
}