package com.maxrave.kotlinytmusicscraper.test

import com.google.gson.annotations.SerializedName
import com.maxrave.kotlinytmusicscraper.Ytmusic
import com.maxrave.kotlinytmusicscraper.models.GridRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.maxrave.kotlinytmusicscraper.models.Run
import com.maxrave.kotlinytmusicscraper.models.SectionListRenderer
import com.maxrave.kotlinytmusicscraper.models.Thumbnail
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient
import com.maxrave.kotlinytmusicscraper.models.YouTubeClient.Companion.WEB_REMIX
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import com.maxrave.kotlinytmusicscraper.parser.getPlaylistContinuation
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val yt = Ytmusic()
        yt
            .browse(WEB_REMIX, "VLPL_pRLf9CPBPEvMTXA4rb-RNECtiH9C2NP")
            .body<BrowseResponse>()
            .let { res ->
                val continuation =
                    res.getPlaylistContinuation()
                println("Continuation: $continuation")
                yt.browse(WEB_REMIX, continuation = continuation).bodyAsText().let {
                    println(it)
                }
            }
    }
}

fun testPlayer() {
    runBlocking {
        val yt = Ytmusic()
        val id = "VLPL_pRLf9CPBPEvMTXA4rb-RNECtiH9C2NP"
        yt
            .browse(WEB_REMIX, id, null, null, "VN", true)
            .bodyAsText()
    }
}

fun testNext() {
    runBlocking {
        Ytmusic()
            .apply {
                locale = YouTubeLocale("VN", "vi")
            }.next(YouTubeClient.WEB, "Fwsl_XS4sYQ", null, null, null, null)
            .apply {
//            val response = body<NextResponse>()
//            println(response)
//            if (response.contents.twoColumnWatchNextResults?.results?.results?.content?.find {
//                it?.itemSectionRenderer?.contents?.firstOrNull()?.continuationItemRenderer != null
//                } != null) {
//                println("\nHas continuation")
//                val continuation = response.contents.twoColumnWatchNextResults.results.results.content.find {
//                    it?.itemSectionRenderer?.contents?.firstOrNull()?.continuationItemRenderer != null
//                }?.itemSectionRenderer?.contents?.firstOrNull()?.continuationItemRenderer?.continuationEndpoint?.continuationCommand?.token
//                println("Continuation: $continuation")
//            }
                println(bodyAsText())
            }
    }
}

fun parseLibraryPlaylist(input: List<GridRenderer.Item>): ArrayList<PlaylistsResult> {
    val list: ArrayList<PlaylistsResult> = arrayListOf()
    if (input.isNotEmpty()) {
        if (input.size >= 2) {
            input[1].musicTwoRowItemRenderer?.let {
                list.add(
                    PlaylistsResult(
                        author =
                            it.subtitle
                                ?.runs
                                ?.get(0)
                                ?.text ?: "",
                        browseId = it.navigationEndpoint.browseEndpoint?.browseId ?: "",
                        category = "",
                        itemCount = "",
                        resultType = "",
                        thumbnails =
                            it.thumbnailRenderer.musicThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?: listOf(),
                        title =
                            it.title.runs
                                ?.get(0)
                                ?.text ?: "",
                    ),
                )
            }
            if (input.size >= 3) {
                for (i in 2 until input.size) {
                    input[i].musicTwoRowItemRenderer?.let {
                        list.add(
                            PlaylistsResult(
                                author =
                                    it.subtitle
                                        ?.runs
                                        ?.get(0)
                                        ?.text ?: "",
                                browseId = it.navigationEndpoint.browseEndpoint?.browseId ?: "",
                                category = "",
                                itemCount = "",
                                resultType = "",
                                thumbnails =
                                    it.thumbnailRenderer.musicThumbnailRenderer
                                        ?.thumbnail
                                        ?.thumbnails
                                        ?: listOf(),
                                title =
                                    it.title.runs
                                        ?.get(0)
                                        ?.text ?: "",
                            ),
                        )
                    }
                }
            }
        }
    }
    return list
}

data class PlaylistsResult(
    @SerializedName("author")
    val author: String,
    @SerializedName("browseId")
    val browseId: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("itemCount")
    val itemCount: String,
    @SerializedName("resultType")
    val resultType: String,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("title")
    val title: String,
)

fun levenshtein(
    lhs: CharSequence,
    rhs: CharSequence,
): Int {
    val lhsLength = lhs.length
    val rhsLength = rhs.length

    var cost = IntArray(lhsLength + 1) { it }
    var newCost = IntArray(lhsLength + 1) { 0 }

    for (i in 1..rhsLength) {
        newCost[0] = i

        for (j in 1..lhsLength) {
            val editCost = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

            val costReplace = cost[j - 1] + editCost
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1

            newCost[j] = minOf(costInsert, costDelete, costReplace)
        }

        val swap = cost
        cost = newCost
        newCost = swap
    }

    return cost[lhsLength]
}

fun bestMatchingIndex(
    s: String,
    list: List<String>,
): Int {
    val listCost = ArrayList<Int>()
    for (i in list.indices) {
        listCost.add(levenshtein(s, list[i]))
    }
    return listCost.indexOf(listCost.minOrNull())
}

fun parseMixedContent(data: List<SectionListRenderer.Content>?): List<Map<String, Any?>> {
    var list = mutableListOf<Map<String, Any?>>()
    if (data != null) {
        for (row in data) {
            if (row.musicDescriptionShelfRenderer != null) {
                val results = row.musicDescriptionShelfRenderer
                var title =
                    results.header
                        ?.runs
                        ?.get(0)
                        ?.text
                var content =
                    results.description.runs
                        ?.get(0)
                        ?.text
                val map = mapOf("title" to title, "content" to content)
                list.add(map)
            } else {
                val results1 = row.musicCarouselShelfRenderer
                val title =
                    results1
                        ?.header
                        ?.musicCarouselShelfBasicHeaderRenderer
                        ?.title
                        ?.runs
                        ?.get(0)
                        ?.text
                val listContent = mutableListOf<Any?>()
                results1?.contents?.forEach { result1 ->
                    if (result1.musicTwoRowItemRenderer != null) {
                        val pageType =
                            result1.musicTwoRowItemRenderer.title.runs
                                ?.get(
                                    0,
                                )?.navigationEndpoint
                                ?.browseEndpoint
                                ?.browseEndpointContextSupportedConfigs
                                ?.browseEndpointContextMusicConfig
                                ?.pageType
                        if (pageType == null) {
                            if (result1.musicTwoRowItemRenderer.navigationEndpoint.watchEndpoint
                                    ?.playlistId != null
                            ) {
                                val content = parseWatchPlaylist(result1.musicTwoRowItemRenderer)
                                listContent.add(content)
                            } else {
                                val content = parseSong(result1.musicTwoRowItemRenderer)
                                listContent.add(content)
                            }
                        } else if (pageType == "MUSIC_PAGE_TYPE_ALBUM") {
                            val content = parseAlbum(result1.musicTwoRowItemRenderer)
                            listContent.add(content)
                        } else if (pageType == "MUSIC_PAGE_TYPE_ARTIST") {
                            val content = parseRelatedArtists(result1.musicTwoRowItemRenderer)
                            listContent.add(content)
                        } else if (pageType == "MUSIC_PAGE_TYPE_PLAYLIST") {
                            val content = parsePlaylist(result1.musicTwoRowItemRenderer)
                            listContent.add(content)
                        }
                    } else {
                        val content = parseSongFlat(result1.musicResponsiveListItemRenderer)
                        listContent.add(content)
                    }
                }
                if (title != null) {
                    list.add(mapOf("title" to title, "content" to listContent))
                }
            }
        }
    }
    return list
}

fun parseSongFlat(data: MusicResponsiveListItemRenderer?): Map<String, Any?>? {
    if (data?.flexColumns != null) {
        val column =
            mutableListOf<MusicResponsiveListItemRenderer.FlexColumn.MusicResponsiveListItemFlexColumnRenderer?>()
        for (i in 0..data.flexColumns.size) {
            column.add(getFlexColumnItem(data, i))
        }
        val song =
            mapOf(
                "title" to
                    column[0]
                        ?.text
                        ?.runs
                        ?.get(0)
                        ?.text,
                "videoId" to
                    column[0]
                        ?.text
                        ?.runs
                        ?.get(0)
                        ?.navigationEndpoint
                        ?.watchEndpoint
                        ?.videoId,
                "thumbnails" to
                    data.thumbnail
                        ?.musicThumbnailRenderer
                        ?.thumbnail
                        ?.thumbnails,
                "isExplicit" to null,
                "artists" to parseSongArtists(data, 1),
            )
        if (column.size > 2 &&
            column[2] != null &&
            column[2]
                ?.text
                ?.runs
                ?.get(0)
                ?.text != null
        ) {
            song.plus(
                "album" to
                    mapOf(
                        "name" to
                            column[2]
                                ?.text
                                ?.runs
                                ?.get(0)
                                ?.text,
                        "id" to
                            column[2]
                                ?.text
                                ?.runs
                                ?.get(0)
                                ?.navigationEndpoint
                                ?.browseEndpoint
                                ?.browseId,
                    ),
            )
        } else {
            song.plus(
                "views" to
                    column[1]
                        ?.text
                        ?.runs
                        ?.last()
                        ?.text
                        ?.split(" ")
                        ?.get(0),
            )
        }
        return song
    } else {
        return null
    }
}

fun parseSongArtists(
    data: MusicResponsiveListItemRenderer,
    index: Int,
): List<Map<String, Any?>>? {
    val flexItem = getFlexColumnItem(data, index)
    if (flexItem == null) {
        return null
    } else {
        val runs = flexItem.text?.runs
        return runs?.let { parseSongArtistsRuns(it) }
    }
}

fun getFlexColumnItem(
    data: MusicResponsiveListItemRenderer,
    index: Int,
): MusicResponsiveListItemRenderer.FlexColumn.MusicResponsiveListItemFlexColumnRenderer? {
    if (data.flexColumns.size <= index ||
        data.flexColumns[index].musicResponsiveListItemFlexColumnRenderer.text == null ||
        data.flexColumns[index]
            .musicResponsiveListItemFlexColumnRenderer.text
            ?.runs == null
    ) {
        return null
    }
    return data.flexColumns[index].musicResponsiveListItemFlexColumnRenderer
}

fun parsePlaylist(data: MusicTwoRowItemRenderer): Map<String, Any?> {
    val subtitle = data.subtitle
    val playlist =
        mapOf(
            "title" to
                data.title.runs
                    ?.get(0)
                    ?.text,
            "playlistId" to
                data.title.runs
                    ?.get(0)
                    ?.navigationEndpoint
                    ?.browseEndpoint
                    ?.browseId,
            "thumbnails" to
                data.thumbnailRenderer.musicThumbnailRenderer
                    ?.thumbnail
                    ?.thumbnails,
        )
    if (subtitle?.runs != null) {
        val descripton = ""
        for (run in subtitle.runs) {
            descripton.plus(run.text)
        }
        playlist.plus("description" to descripton)
        if (subtitle.runs.size == 3) {
            val count =
                data.subtitle.runs
                    ?.get(2)
                    ?.text
                    ?.split(" ")
                    ?.get(0)
            playlist.plus("count" to count)
            val author = parseSongArtistsRuns(subtitle.runs.take(1))
            playlist.plus("author" to author)
        }
    }
    return playlist
}

fun parseSongArtistsRuns(runs: List<Run>): List<Map<String, Any?>> {
    val artists = mutableListOf<Map<String, Any?>>()
    for (i in 0..(runs.size / 2).toInt()) {
        artists.add(
            mapOf(
                "name" to runs[i * 2].text,
                "id" to runs[i * 2].navigationEndpoint?.browseEndpoint?.browseId,
            ),
        )
    }
    return artists
}

fun parseRelatedArtists(data: MusicTwoRowItemRenderer): Map<String, Any?> =
    mapOf(
        "title" to
            data.title.runs
                ?.get(0)
                ?.text,
        "browseId" to
            data.title.runs
                ?.get(0)
                ?.navigationEndpoint
                ?.browseEndpoint
                ?.browseId,
        "thumbnails" to
            data.thumbnailRenderer.musicThumbnailRenderer
                ?.thumbnail
                ?.thumbnails
                .toString(),
        "subscriber" to
            data.subtitle
                ?.runs
                ?.get(0)
                ?.text
                ?.split(" ")
                ?.get(0),
    )

fun parseAlbum(data: MusicTwoRowItemRenderer): Map<String, Any?> {
    val title =
        data.title.runs
            ?.get(0)
            ?.text
    val year =
        data.subtitle
            ?.runs
            ?.get(2)
            ?.text
    val browseId =
        data.title.runs
            ?.get(0)
            ?.navigationEndpoint
            ?.browseEndpoint
            ?.browseId
    val thumbnails = data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail
    val isExplicit = ""
    return mapOf(
        "title" to title,
        "year" to year,
        "browseId" to browseId,
        "thumbnails" to thumbnails,
        "isExplicit" to isExplicit,
    )
}

fun parseSong(data: MusicTwoRowItemRenderer): Map<String, Any?> {
    val title =
        data.title.runs
            ?.get(0)
            ?.text
    val videoId = data.navigationEndpoint.watchEndpoint?.videoId
    val thumbnails = data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail
    val runs = data.subtitle?.runs
    val artist = parseSongRuns(runs)
    return mapOf(
        "title" to title,
        "videoId" to videoId,
        "thumbnails" to thumbnails,
    ).plus(artist)
}

fun parseSongRuns(runs: List<Run>?): Map<String, Any?> {
    val list: Map<String, Any?> = mutableMapOf()
    if ((runs?.size?.rem(2) ?: 1) == 0) {
        runs?.forEach { run ->
            val text = run.text
            if (run.navigationEndpoint != null) {
                val item =
                    mapOf("name" to text, "id" to run.navigationEndpoint.browseEndpoint?.browseId)
                if (item["id"] != null) {
                    if (item["id"]!!.startsWith("MPRE") || item["id"]!!.contains("release_detail")) {
                        list.plus(mapOf("album" to item))
                    } else {
                        list.plus(mapOf("artist" to item))
                    }
                }
            } else {
                if (Regex("^\\d([^ ])* [^ ]*\$").matches(text)) {
                    list.plus(mapOf("views" to text.split(" ")[0]))
                } else if (Regex("^(\\d+:)*\\d+:\\d+\$").matches(text)) {
                    list.plus(mapOf("duration" to text))
                    list.plus(
                        mapOf(
                            "duration_seconds" to
                                text
                                    .split(":")
                                    .let { it[0].toInt() * 60 + it[1].toInt() },
                        ),
                    )
                } else if (Regex("^\\d{4}\$").matches(text)) {
                    list.plus(mapOf("year" to text))
                } else {
                    list.plus(mapOf("artist" to mapOf("name" to text, "id" to null)))
                }
            }
        }
    }

    return list
}

fun parseWatchPlaylist(data: MusicTwoRowItemRenderer): Map<String, String?> {
    val title =
        data.title.runs
            ?.get(0)
            ?.text
    val playlistId = data.navigationEndpoint.watchPlaylistEndpoint?.playlistId
    val thumbnails = data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail
    return mapOf(
        "title" to title,
        "playlistId" to playlistId,
        "thumbnails" to thumbnails.toString(),
    )
}