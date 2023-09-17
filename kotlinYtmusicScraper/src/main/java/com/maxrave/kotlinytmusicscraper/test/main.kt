package com.maxrave.kotlinytmusicscraper.test

import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.maxrave.kotlinytmusicscraper.models.Run
import com.maxrave.kotlinytmusicscraper.models.SectionListRenderer
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        YouTube.cookie = "VISITOR_INFO1_LIVE=zhhx1-pXFL0; __utmz=27069237.1673094830.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); VISITOR_PRIVACY_METADATA=CgJWThICGgA%3D; __utma=27069237.597876058.1673094830.1691393097.1693037834.24; SID=awiHuIzML_IVPtnIi79j6ROA2WhRT-W4-c52-MLFqgroCU_Un_pe-kcRiM64XCZGu1KkoQ.; __Secure-1PSID=awiHuIzML_IVPtnIi79j6ROA2WhRT-W4-c52-MLFqgroCU_UFqjPiN5bEcivwgZg5kdyXw.; __Secure-3PSID=awiHuIzML_IVPtnIi79j6ROA2WhRT-W4-c52-MLFqgroCU_URAVONF3mY51fW5e7zQ7uBA.; HSID=A6fCLyhfmgiR8cgzQ; SSID=Ar-puWJ0jxD84eCD6; APISID=7se7s_CznxJlRn2t/A2KFwppY54Ybm3Abw; SAPISID=MZx1n771jRq7sE4c/AyEQHi5NsLCkvKcGt; __Secure-1PAPISID=MZx1n771jRq7sE4c/AyEQHi5NsLCkvKcGt; __Secure-3PAPISID=MZx1n771jRq7sE4c/AyEQHi5NsLCkvKcGt; LOGIN_INFO=AFmmF2swRQIhALlK-GIKSX-_-wAq3MR4LiZxDEyZau-CdERSaDCSToolAiBHixfkKWkvAOsMjXax7ia-Nsld97BRcVkrl0mP4wkxNA:QUQ3MjNmeVBBcWJNVnp1cWNLVnFnNVhCdTQ5d3JzaTNsaUVLZWwzNUFFM21Gd3BaWTZHdTh6VzJDZDhaR09UTDVHYkpORWdudXFwNVNybE1wWVVKM3p5ZFpoODgyQWc2OUlISnc1TUJpU09DXzVJR3FUVWdhVGxZT3RLcTBXcThZdGhjVENjMmI4ZnJZWGtrQ29UejhRMVl0N1NTVUxRYnl3; PREF=f6=40000000&tz=Asia.Saigon&f7=100&autoplay=true&volume=56&gl=VN&hl=vi&guide_collapsed=false; YSC=0ukOXfdxEmA; __Secure-1PSIDTS=sidts-CjEB3e41hXWXBiV8H7ZyfdAHIzSKGskCWZbwARRHAW3uCaJSInbKtluacecZbxO_DLtVEAA; __Secure-3PSIDTS=sidts-CjEB3e41hXWXBiV8H7ZyfdAHIzSKGskCWZbwARRHAW3uCaJSInbKtluacecZbxO_DLtVEAA; SIDCC=APoG2W_d8Hwm_kU1nc-fFziapVRismDsb5B06nunLQUovlVnI7xsYBZEmbyRxEwAjqhto-ZZcAs; __Secure-1PSIDCC=APoG2W8W9LS5IoFFDiEuUE-89IgRe0haSCQodm3DUV06fXpD-Jk8kMeET0L5OiGmEw2CTgzoQIg; __Secure-3PSIDCC=APoG2W_Ii-jswtLqsV-v6vWLJnVW-5bw6arSWwLyZ_e3gZGAMwsy-xSRyFWH5B-TsGihP7ZIyGA"

        YouTube.player("cPcGnUJmcFg", "").onSuccess { player ->
//            player.playbackTracking?.videostatsPlaybackUrl?.baseUrl?.let { url ->
//                YouTube.initPlayback(url).onSuccess { playback ->
//                    println(playback)
//                    delay(5000)
//                    player.playbackTracking.atrUrl?.baseUrl?.let {
//                        YouTube.initPlayback(it).onSuccess { atr ->
//                            println(atr)
//                        }
//                    }
//                }
//            }
            if (player.playbackTracking?.videostatsPlaybackUrl?.baseUrl != null && player.playbackTracking.atrUrl?.baseUrl != null && player.playbackTracking.videostatsWatchtimeUrl?.baseUrl != null) {
                YouTube.initPlayback(player.playbackTracking.videostatsPlaybackUrl.baseUrl,
                    player.playbackTracking.atrUrl.baseUrl, player.playbackTracking.videostatsWatchtimeUrl.baseUrl
                ).onSuccess { playback ->

                }
            }
        }
            .onFailure { error ->
                error.printStackTrace()
            }
//        YouTube.customQuery("FEmusic_history", setLogin = true).onSuccess {
//            println(Json.encodeToString(it))
//        }
//            .onFailure { error ->
//                error.printStackTrace()
//            }
//        YouTube.scrapeYouTube("XLbiC-ly7v8").onSuccess { scrape ->
//            var response = ""
//            var data = ""
//            val ksoupHtmlParser = KsoupHtmlParser(
//                object : KsoupHtmlHandler {
//                    override fun onText(text: String) {
//                        super.onText(text)
//                        if (text.contains("var ytInitialPlayerResponse")) {
//                            val temp = text.replace("var ytInitialPlayerResponse = ", "").dropLast(1)
//                            response = temp.trimIndent()
//                        }
//                        else if (text.contains("var ytInitialData")) {
//                            val temp = text.replace("var ytInitialData = ", "").dropLast(1)
//                            data = temp.trimIndent()
//                        }
//                    }
//                }
//            )
//            ksoupHtmlParser.write(scrape)
//            ksoupHtmlParser.end()
//            val json = Json {ignoreUnknownKeys = true}
//            val ytScrapeData = json.decodeFromString<YouTubeDataPage>(data)
//            val ytScrapeInitial = json.decodeFromString<YouTubeInitialPage>(response)
//            println(ytScrapeInitial.playbackTracking.toString())
//
//        }
//            .onFailure { error ->
//                error.printStackTrace()
//            }
    }
}

fun parseMixedContent(data: List<SectionListRenderer.Content>?): List<Map<String, Any?>> {
    var list = mutableListOf<Map<String, Any?>>()
    if (data != null) {
        for (row in data) {

            if (row.musicDescriptionShelfRenderer != null) {
                val results = row.musicDescriptionShelfRenderer
                var title = results.header?.runs?.get(0)?.text
                var content = results.description.runs?.get(0)?.text
                val map = mapOf("title" to title, "content" to content)
                list.add(map)
            }
            else {
                val results1 = row.musicCarouselShelfRenderer
                val title = results1?.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.get(0)?.text
                val listContent = mutableListOf<Any?>()
                results1?.contents?.forEach { result1 ->
                    if (result1.musicTwoRowItemRenderer != null) {
                        val pageType = result1.musicTwoRowItemRenderer.title.runs?.get(0)?.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                        if (pageType == null) {
                            if (result1.musicTwoRowItemRenderer.navigationEndpoint.watchEndpoint?.playlistId != null){
                                val content = parseWatchPlaylist(result1.musicTwoRowItemRenderer)
                                listContent.add(content)
                            }
                            else {
                                val content = parseSong(result1.musicTwoRowItemRenderer)
                                listContent.add(content)
                            }
                        }
                        else if (pageType == "MUSIC_PAGE_TYPE_ALBUM"){
                            val content = parseAlbum(result1.musicTwoRowItemRenderer)
                            listContent.add(content)
                        }
                        else if (pageType == "MUSIC_PAGE_TYPE_ARTIST"){
                            val content = parseRelatedArtists(result1.musicTwoRowItemRenderer)
                            listContent.add(content)
                        }
                        else if (pageType == "MUSIC_PAGE_TYPE_PLAYLIST") {
                            val content = parsePlaylist(result1.musicTwoRowItemRenderer)
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
                    list.add(mapOf("title" to title, "content" to listContent))
                }
            }
        }
    }
    return list
}

fun parseSongFlat(data: MusicResponsiveListItemRenderer?): Map<String, Any?>? {
    if (data?.flexColumns != null) {
        val column = mutableListOf<MusicResponsiveListItemRenderer.FlexColumn.MusicResponsiveListItemFlexColumnRenderer?>()
        for (i in 0..data.flexColumns.size){
            column.add(getFlexColumnItem(data, i))
        }
        val song = mapOf(
            "title" to column[0]?.text?.runs?.get(0)?.text,
            "videoId" to column[0]?.text?.runs?.get(0)?.navigationEndpoint?.watchEndpoint?.videoId,
            "thumbnails" to data.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails,
            "isExplicit" to null,
            "artists" to parseSongArtists(data, 1),
        )
        if (column.size > 2 && column[2] != null && column[2]?.text?.runs?.get(0)?.text != null) {
            song.plus("album" to mapOf(
                "name" to column[2]?.text?.runs?.get(0)?.text,
                "id" to column[2]?.text?.runs?.get(0)?.navigationEndpoint?.browseEndpoint?.browseId
            ))
        }
        else {
            song.plus("views" to column[1]?.text?.runs?.last()?.text?.split(" ")?.get(0))
        }
        return song
    }
    else {
        return null
    }
}

fun parseSongArtists(data: MusicResponsiveListItemRenderer, index: Int): List<Map<String, Any?>>? {
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

fun parsePlaylist(data: MusicTwoRowItemRenderer): Map<String, Any?> {
    val subtitle = data.subtitle
    val playlist = mapOf(
        "title" to data.title.runs?.get(0)?.text,
        "playlistId" to data.title.runs?.get(0)?.navigationEndpoint?.browseEndpoint?.browseId,
        "thumbnails" to data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails,
    )
    if (subtitle?.runs != null) {
        val descripton = ""
        for (run in subtitle.runs) {
            descripton.plus(run.text)
        }
        playlist.plus("description" to descripton)
        if (subtitle.runs.size == 3) {
            val count = data.subtitle.runs?.get(2)?.text?.split(" ")?.get(0)
            playlist.plus("count" to count)
            val author = parseSongArtistsRuns(subtitle.runs.take(1))
            playlist.plus("author" to author)
        }
    }
    return playlist
}

fun parseSongArtistsRuns(runs: List<Run>): List<Map<String, Any?>> {
    val artists = mutableListOf<Map<String, Any?>>()
    for (i in 0..(runs.size/2).toInt()) {
        artists.add(mapOf(
            "name" to runs[i * 2].text,
            "id" to runs[i * 2].navigationEndpoint?.browseEndpoint?.browseId
        ))
    }
    return artists
}

fun parseRelatedArtists(data: MusicTwoRowItemRenderer): Map<String, Any?> {
    return mapOf(
        "title" to data.title.runs?.get(0)?.text,
        "browseId" to data.title.runs?.get(0)?.navigationEndpoint?.browseEndpoint?.browseId,
        "thumbnails" to data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails.toString(),
        "subscriber" to data.subtitle?.runs?.get(0)?.text?.split(" ")?.get(0)
    )

}

fun parseAlbum(data: MusicTwoRowItemRenderer): Map<String, Any?> {
    val title = data.title.runs?.get(0)?.text
    val year = data.subtitle?.runs?.get(2)?.text
    val browseId = data.title.runs?.get(0)?.navigationEndpoint?.browseEndpoint?.browseId
    val thumbnails = data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail
    val isExplicit = ""
    return mapOf (
        "title" to title,
        "year" to year,
        "browseId" to browseId,
        "thumbnails" to thumbnails,
        "isExplicit" to isExplicit
    )
}

fun parseSong(data: MusicTwoRowItemRenderer): Map<String, Any?> {
    val title = data.title.runs?.get(0)?.text
    val videoId = data.navigationEndpoint.watchEndpoint?.videoId
    val thumbnails = data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail
    val runs = data.subtitle?.runs
    val artist = parseSongRuns(runs)
    return mapOf (
        "title" to title,
        "videoId" to videoId,
        "thumbnails" to thumbnails
    ).plus(artist)
}

fun parseSongRuns(runs: List<Run>?): Map<String, Any?> {
    val list: Map<String, Any?> = mutableMapOf()
    if ((runs?.size?.rem(2) ?: 1) == 0) {
        runs?.forEach { run ->
            val text = run.text
            if (run.navigationEndpoint != null) {
                val item = mapOf("name" to text, "id" to run.navigationEndpoint.browseEndpoint?.browseId)
                if (item["id"] != null){
                    if (item["id"]!!.startsWith("MPRE") || item["id"]!!.contains("release_detail")){
                        list.plus(mapOf("album" to item))
                    }
                    else {
                        list.plus(mapOf("artist" to item))
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
                    list.plus(mapOf("artist" to mapOf("name" to text, "id" to null)))
                }
            }
        }
    }

    return list
}

fun parseWatchPlaylist(data: MusicTwoRowItemRenderer): Map<String, String?> {
    val title = data.title.runs?.get(0)?.text
    val playlistId = data.navigationEndpoint.watchPlaylistEndpoint?.playlistId
    val thumbnails = data.thumbnailRenderer.musicThumbnailRenderer?.thumbnail
    return mapOf("title" to title, "playlistId" to playlistId, "thumbnails" to thumbnails.toString())
}

