package com.maxrave.simpmusic.data.parser

import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.maxrave.kotlinytmusicscraper.models.Run
import com.maxrave.kotlinytmusicscraper.models.SectionListRenderer
import com.maxrave.kotlinytmusicscraper.models.Thumbnail
import com.maxrave.simpmusic.data.model.home.Content
import com.maxrave.simpmusic.data.model.home.HomeItem
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist

fun parseMixedContent(data: List<SectionListRenderer.Content>?): List<HomeItem> {
    val list = mutableListOf<HomeItem>()
    if (data != null) {
        for (row in data) {
            val results = row.musicDescriptionShelfRenderer
            if (results != null) {
                val title = results.header?.runs?.get(0)?.text
                val content = results.description.runs?.get(0)?.text
                list.add(HomeItem(contents = listOf(Content(
                    album = null,
                    artists = listOf(),
                    description = content,
                    isExplicit = null,
                    playlistId = null,
                    browseId = null,
                    thumbnails = listOf(),
                    title = content ?: " ",
                    videoId = null,
                    views = null
                )), title = title ?: ""))
            }
            else {
                val results1 = row.musicCarouselShelfRenderer
                val title = results1?.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.get(0)?.text
                val listContent = mutableListOf<Content?>()
                results1?.contents?.forEach { result1 ->
                    if (result1.musicTwoRowItemRenderer != null) {
                        val pageType = result1.musicTwoRowItemRenderer!!.title.runs?.get(0)?.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                        if (pageType == null) {
                            if (result1.musicTwoRowItemRenderer!!.navigationEndpoint.watchEndpoint?.playlistId != null){
                                val content = parseWatchPlaylist(result1.musicTwoRowItemRenderer!!)
                                listContent.add(content)
                            }
                            else {
                                val content = parseSong(result1.musicTwoRowItemRenderer!!)
                                listContent.add(content)
                            }
                        }
                        else if (pageType == "MUSIC_PAGE_TYPE_ALBUM"){
                            val content = parseAlbum(result1.musicTwoRowItemRenderer!!)
                            listContent.add(content)
                        }
                        else if (pageType == "MUSIC_PAGE_TYPE_ARTIST"){
                            val content = parseRelatedArtists(result1.musicTwoRowItemRenderer!!)
                            listContent.add(content)
                        }
                        else if (pageType == "MUSIC_PAGE_TYPE_PLAYLIST") {
                            val content = parsePlaylist(result1.musicTwoRowItemRenderer!!)
                            listContent.add(content)
                        }
                    }
                    else
                    {
                        val content = parseSongFlat(result1.musicResponsiveListItemRenderer)
                        listContent.add(content)
                    }
                }
                if (title != null){
                    list.add(HomeItem(contents = listContent, title = title))
                }
            }
        }
    }
    return list
}

fun parseSongFlat(data: MusicResponsiveListItemRenderer?): Content? {
    if (data?.flexColumns != null) {
        val column = mutableListOf<MusicResponsiveListItemRenderer.FlexColumn.MusicResponsiveListItemFlexColumnRenderer?>()
        for (i in 0..data.flexColumns.size){
            column.add(getFlexColumnItem(data, i))
        }
//        val song = mapOf(
//            "title" to column[0]?.text?.runs?.get(0)?.text,
//            "videoId" to column[0]?.text?.runs?.get(0)?.navigationEndpoint?.watchEndpoint?.videoId,
//            "thumbnails" to data.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails,
//            "isExplicit" to null,
//            "artists" to parseSongArtists(data, 1),
//        )
//        if (column.size > 2 && column[2] != null && column[2]?.text?.runs?.get(0)?.text != null) {
//            song.plus("album" to mapOf(
//                "name" to column[2]?.text?.runs?.get(0)?.text,
//                "id" to column[2]?.text?.runs?.get(0)?.navigationEndpoint?.browseEndpoint?.browseId
//            ))
//        }
//        else {
//            song.plus("views" to column[1]?.text?.runs?.last()?.text?.split(" ")?.get(0))
//        }
        return Content(
            album = if (column.size > 2 && column[2] != null && column[2]?.text?.runs?.get(0)?.text != null) {
                Album(
                    id = column[2]?.text?.runs?.get(0)?.navigationEndpoint?.browseEndpoint?.browseId!!,
                    name = column[2]?.text?.runs?.get(0)?.text!!
                )
            }
            else {
                null
            },
            artists = parseSongArtists(data, 1),
            description = null,
            isExplicit = null,
            playlistId = null,
            browseId = null,
            thumbnails = data.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail() ?: listOf(),
            title = column[0]?.text?.runs?.get(0)?.text ?: "",
            videoId = column[0]?.text?.runs?.get(0)?.navigationEndpoint?.watchEndpoint?.videoId,
            views = if (column.size <= 2 || column[2] == null || column[2]?.text?.runs?.get(0)?.text == null) {
                column[1]?.text?.runs?.last()?.text?.split(" ")?.get(0)
            }
            else {
                null
            }
        )
    }
    else {
        return null
    }
}

fun parseSongArtists(data: MusicResponsiveListItemRenderer, index: Int): List<Artist>? {
    val flexItem = getFlexColumnItem(data, index)
    if (flexItem == null){
        return null
    }
    else {
        val runs = flexItem.text?.runs
        return runs?.let { parseSongArtistsRuns(it) }
    }
}

fun getFlexColumnItem(data: MusicResponsiveListItemRenderer, index: Int): MusicResponsiveListItemRenderer.FlexColumn.MusicResponsiveListItemFlexColumnRenderer? {
    if (data.flexColumns.size <= index || data.flexColumns[index].musicResponsiveListItemFlexColumnRenderer.text == null || data.flexColumns[index].musicResponsiveListItemFlexColumnRenderer.text?.runs == null){
        return null
    }
    return data.flexColumns[index].musicResponsiveListItemFlexColumnRenderer
}

fun parsePlaylist(data: MusicTwoRowItemRenderer): Content {
    val subtitle = data.subtitle
    val playlist = mapOf(
        "title" to data.title.runs?.get(0)?.text,
        "playlistId" to data.title.runs?.get(0)?.navigationEndpoint?.browseEndpoint?.browseId
    )
    val thumbnails = data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails
    if (subtitle?.runs != null) {
        val descripton = ""
        for (run in subtitle.runs!!) {
            descripton.plus(run.text)
        }
        playlist.plus("description" to descripton)
        if (subtitle.runs!!.size == 3) {
            val count = data.subtitle!!.runs?.get(2)?.text?.split(" ")?.get(0)
            playlist.plus("count" to count)
            val author = parseSongArtistsRuns(subtitle.runs!!.take(1))
            playlist.plus("author" to author)
        }
    }
    return Content(
        album = null,
        artists = listOf(),
        description = playlist["description"].toString(),
        isExplicit = false,
        playlistId = playlist["playlistId"].toString(),
        browseId = null,
        thumbnails = thumbnails?.toListThumbnail() ?: listOf(),
        title = playlist["title"].toString(),
        videoId = null,
        views = null
    )
}

fun parseSongArtistsRuns(runs: List<Run>): List<Artist> {
    val artists = mutableListOf<Artist>()
    for (i in 0..(runs.size/2)) {
        artists.add(Artist(name = runs[i * 2].text, id = runs[i * 2].navigationEndpoint?.browseEndpoint?.browseId))
    }
    return artists
}

fun parseRelatedArtists(data: MusicTwoRowItemRenderer): Content {
    return Content(
        album = null,
        artists = listOf(),
        description = data.subtitle?.runs?.get(0)?.text?.split(" ")?.get(0),
        isExplicit = false,
        playlistId = null,
        browseId = data.title.runs?.get(0)?.navigationEndpoint?.browseEndpoint?.browseId,
        thumbnails = data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails?.toListThumbnail() ?: listOf(),
        title = data.title.runs?.get(0)?.text ?: "",
        videoId = null,
        views = null
    )
}

fun parseAlbum(data: MusicTwoRowItemRenderer): Content {
    val title = data.title.runs?.get(0)?.text
    val year = data.subtitle?.runs?.get(2)?.text
    val browseId = data.title.runs?.get(0)?.navigationEndpoint?.browseEndpoint?.browseId
    val thumbnails = data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails
    return Content(
        album = Album( id = browseId ?: "", name = title ?: ""),
        artists = listOf(),
        description = null,
        isExplicit = false,
        playlistId = "",
        browseId = browseId,
        thumbnails = thumbnails?.toListThumbnail() ?:listOf(),
        title = title ?: "",
        videoId = "",
        views = ""
    )
}

fun parseSong(data: MusicTwoRowItemRenderer): Content {
    val title = data.title.runs?.get(0)?.text
    val videoId = data.navigationEndpoint.watchEndpoint?.videoId
    val thumbnails = data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails
    val runs = data.subtitle?.runs
    val parsed = parseSongRuns(runs)
    return Content(
        album = parsed.first,
        artists = parsed.second,
        isExplicit = false,
        thumbnails = thumbnails?.toListThumbnail() ?: listOf(),
        title = title ?: "",
        videoId = videoId!!,
        views = parsed.third["views"] ?: "",
        browseId = "",
        playlistId = "",
        description = ""
    )
}

fun parseSongRuns(runs: List<Run>?): Triple<Album?, List<Artist>, Map<String, String?>> {
    val list: Map<String, String?> = mutableMapOf()
    val listArtist: MutableList<Artist> = mutableListOf()
    val listAlbum: MutableList<Album> = mutableListOf()
    if ((runs?.size?.rem(2) ?: 1) == 0) {
        runs?.forEach { run ->
            val text = run.text
            if (run.navigationEndpoint != null) {
                val item = mapOf("name" to text, "id" to run.navigationEndpoint!!.browseEndpoint?.browseId)
                if (item["id"] != null){
                    if (item["id"]!!.startsWith("MPRE") || item["id"]!!.contains("release_detail")){
                        listAlbum.add(Album(name = item["name"] as String, id = item["id"] as String))
                    }
                    else {
                        listArtist.add(Artist(name = item["name"] as String, id = item["id"] as String))
                    }
                }
            }
            else {
                if (Regex("^\\d([^ ])* [^ ]*\$").matches(text)){
                    list.plus(mapOf("views" to text.split(" ")[0]))

                }
                else if (Regex("^(\\d+:)*\\d+:\\d+\$").matches(text)){
                    list.plus(mapOf("duration" to text))
                    list.plus(mapOf("duration_seconds" to text.split(":").let { it[0].toInt() * 60 + it[1].toInt()}))
                }
                else if (Regex("^\\d{4}\$").matches(text)){
                    list.plus(mapOf("year" to text))
                }
                else {
                    listArtist.add(Artist(name = text, id = null))
                }
            }
        }
    }

    return Triple(listAlbum.firstOrNull(), listArtist, list)
}

fun parseWatchPlaylist(data: MusicTwoRowItemRenderer): Content {
    val title = data.title.runs?.get(0)?.text
    val playlistId = data.navigationEndpoint.watchPlaylistEndpoint?.playlistId
    val thumbnails = data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails
    return Content(
        album = null,
        artists = listOf(),
        description = null,
        isExplicit = null,
        playlistId = playlistId,
        browseId = null,
        thumbnails = thumbnails?.toListThumbnail() ?: listOf(),
        title = title ?: "",
        videoId = null,
        views = null
    )
}

fun Thumbnail.toThumbnail(): com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail {
    return com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail(
        height = this.height ?: 0,
        url = this.url,
        width = this.width ?: 0
    )
}

fun List<Thumbnail>.toListThumbnail(): List<com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail> {
    val list = mutableListOf<com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail>()
    this.forEach {
        list.add(it.toThumbnail())
    }
    return list
}