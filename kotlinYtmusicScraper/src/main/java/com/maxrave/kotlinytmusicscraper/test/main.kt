package com.maxrave.kotlinytmusicscraper.test

import com.google.gson.annotations.SerializedName
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.GridRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.maxrave.kotlinytmusicscraper.models.Run
import com.maxrave.kotlinytmusicscraper.models.SectionListRenderer
import com.maxrave.kotlinytmusicscraper.models.Thumbnail
import kotlinx.coroutines.runBlocking


fun main() {
    runBlocking {
        YouTube.player("JzpxJcJwDZ0").onSuccess { player ->
            println(player)
        }
    }
//        Ytmusic().player(YouTubeClient.ANDROID_MUSIC, )
//        YouTube.spotifyCookie =
//            "sp_t=a910acb941e990e349d79e8170f2dafe; _scid=e96b060c-30fc-4c59-9e44-25f376482edd; _ga=GA1.2.2112854199.1671017821; sp_adid=95e621d6-b86c-4ce5-bed4-6b03ab65155b; _ga_ZWG1NSHWD8=GS1.1.1671017821.1.0.1671017823.0.0.0; sp_dc=AQAYI79gjZIRlNjJUDXJLIq1yw5Nb_E85T55B8jgHOVWOseGrnhlVuEKRziSRV1XA8Ca2d7bx7RAQ-6hjGLWNlsxHrZ6DP6Jmn_Joz-6djb05evRb31kKFGrTXhRzFBRMr5MePVo2pujxkVtOjCZp_qNP1J3ryQ; sp_key=595700a0-212b-4ce5-a497-0f7eea005f35; OptanonAlertBoxClosed=2022-12-27T15:05:03.114Z; sp_m=vn-vi; sp_landing=https%3A%2F%2Fopen.spotify.com%2Ftrack%2F1GPRHgVXzRfzoc44HEZZQI%3Fsp_cid%3Da910acb941e990e349d79e8170f2dafe%26device%3Ddesktop; OptanonConsent=isIABGlobal=false&datestamp=Sat+Sep+30+2023+22%3A30%3A16+GMT%2B0700+(Indochina+Time)&version=6.26.0&hosts=&landingPath=NotLandingPage&groups=s00%3A1%2Cf00%3A1%2Cm00%3A1%2Ct00%3A1%2Ci00%3A1%2Cf11%3A0&AwaitingReconsent=false&geolocation=VN%3BSG"
//        YouTube.cookie =
//            "VISITOR_INFO1_LIVE=zhhx1-pXFL0; __utmz=27069237.1673094830.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); VISITOR_PRIVACY_METADATA=CgJWThICGgA%3D; __utma=27069237.597876058.1673094830.1691393097.1693037834.24; LOGIN_INFO=AFmmF2swRQIhALlK-GIKSX-_-wAq3MR4LiZxDEyZau-CdERSaDCSToolAiBHixfkKWkvAOsMjXax7ia-Nsld97BRcVkrl0mP4wkxNA:QUQ3MjNmeVBBcWJNVnp1cWNLVnFnNVhCdTQ5d3JzaTNsaUVLZWwzNUFFM21Gd3BaWTZHdTh6VzJDZDhaR09UTDVHYkpORWdudXFwNVNybE1wWVVKM3p5ZFpoODgyQWc2OUlISnc1TUJpU09DXzVJR3FUVWdhVGxZT3RLcTBXcThZdGhjVENjMmI4ZnJZWGtrQ29UejhRMVl0N1NTVUxRYnl3; SID=bAiHuOSAVkOcHMA4cuCGz3Esa2qGLgDbdsMbk5UHs0UUkVJkLIWqC4og39fPQOPkDMu2xg.; __Secure-1PSID=bAiHuOSAVkOcHMA4cuCGz3Esa2qGLgDbdsMbk5UHs0UUkVJkk5EjmxcjfH2pbmn4iPwjuw.; __Secure-3PSID=bAiHuOSAVkOcHMA4cuCGz3Esa2qGLgDbdsMbk5UHs0UUkVJkYXoKeyvKnENZHS6ubzdUJg.; HSID=Anikjx73sQ4s47Hjp; SSID=AKVpi9FZspt0u7zj8; APISID=GF09QQoyirI05dF7/ALTs2_GFCeABgB3sQ; SAPISID=T_NjR_vKV6wufMQH/A1MpNmKnmYUpD9ALL; __Secure-1PAPISID=T_NjR_vKV6wufMQH/A1MpNmKnmYUpD9ALL; __Secure-3PAPISID=T_NjR_vKV6wufMQH/A1MpNmKnmYUpD9ALL; PREF=f6=40000000&tz=Asia.Saigon&f7=100&autoplay=true&volume=56&gl=VN&hl=vi&guide_collapsed=false&f5=20000; YSC=zlOmTzYFmfA; __Secure-1PSIDTS=sidts-CjEB3e41hVMGhnny0RrJ_9Xo7vlv9Snp5RI0Os4UJf3o0eTVQpu6XIgu1WiB4h-85ieZEAA; __Secure-3PSIDTS=sidts-CjEB3e41hVMGhnny0RrJ_9Xo7vlv9Snp5RI0Os4UJf3o0eTVQpu6XIgu1WiB4h-85ieZEAA; SIDCC=ACA-OxNYWmgFXxZbBBCm28QjtcfRwJ-5KptqbNeVrmmzSTfn17hF36FVWfNBS-xsXZZQ2MtFaAWJ; __Secure-1PSIDCC=ACA-OxMd3YwVX5FAZmRVz9dag48RWH8hv-WvzziDAcc4aYGNDEfK7OXwf3N_uMe7Z7KeBEBpvbn3; __Secure-3PSIDCC=ACA-OxP0D1K8y4lhUZR35NgHhmj3PZ_1o51V9gUcB8X-SChuTiQF7gyOCq9cTx5Orr483Owq2_U"
//        YouTube.next(endpoint = WatchEndpoint(playlistId = "RDEMgtw3oC9Zt7NLrWZyopXq4Q"))
//            .onSuccess {
//                val songs = ArrayList<SongItem>()
//                songs.addAll(it.items)
//                println(songs.size)
//                if (it.continuation != null) {
//                    YouTube.next(endpoint = WatchEndpoint(playlistId = "RDEMgtw3oC9Zt7NLrWZyopXq4Q"), continuation = it.continuation).onSuccess { value ->
//                        songs.addAll(value.items)
//                        println(songs.size)
//                        println(songs)
//                    }
//                }
//            }
//        YouTube.getMusixmatchUserToken().onSuccess {
//            println(it.message.body.user_token)
//            YouTube.postMusixmatchCredentials("ndtminh2608@gmail.com", "minh123456", it.message.body.user_token).onSuccess { value ->
//                println(value)
//                YouTube.musixmatchUserToken = it.message.body.user_token
//                delay(2000)
//                YouTube.getMusixmatchTranslateLyrics("144114431", it.message.body.user_token, "vi").onSuccess { lyrics ->
//                    println(lyrics)
//                }.onFailure {
//                    it.printStackTrace()
//                }
//
//            }
//                .onFailure {
//                    it.printStackTrace()
//                }
//        }
//            .onFailure {
//                it.printStackTrace()
//            }
//            YouTube.browse(browseId = "VLOLAK5uy_nX6nwnLobDcXKOKCk2gH-xql7B6aU1Ta4", params = "ggMCCAI%3D").onSuccess { result ->
//                println(result.items)
//            }.onFailure { error ->
//                error.printStackTrace()
//            }
    }
//      YouTube.authentication().onSuccess { token ->
//            if (token.accessToken != null) {
//
//                YouTube.getSongId(token.accessToken, "Feel Good Gryffin").onSuccess { spotifyResult ->
//                    if (!spotifyResult.tracks?.items.isNullOrEmpty()) {
//                        val list = arrayListOf<String>()
//                        for (index in spotifyResult.tracks?.items!!.indices) {
//                            list.add(spotifyResult.tracks?.items?.get(index)?.name + spotifyResult.tracks?.items?.get(index)?.artists?.get(0)?.name)
//                        }
//                        bestMatchingIndex("Feel Good Gryffin", list).let {
//                            spotifyResult.tracks?.items?.get(it)?.let {item ->
//                                item.id?.let { it1 ->
//                                    YouTube.getAccessToken().onSuccess { value: AccessToken ->
//                                        println(value)
//                                        YouTube.getLyrics(it1, value.accessToken).onSuccess { lyrics ->
//                                            println(lyrics)
//                                        }.onFailure {
//                                            it.printStackTrace()
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }.onFailure {
//                    it.printStackTrace()
//                }
//            }
//
//        }
//    }.onFailure {
//        it.printStackTrace()
//    }
    //        YouTube.player("cPcGnUJmcFg", "").onSuccess { player ->
////            player.playbackTracking?.videostatsPlaybackUrl?.baseUrl?.let { url ->
////                YouTube.initPlayback(url).onSuccess { playback ->
////                    println(playback)
////                    delay(5000)
////                    player.playbackTracking.atrUrl?.baseUrl?.let {
////                        YouTube.initPlayback(it).onSuccess { atr ->
////                            println(atr)
////                        }
////                    }
////                }
////            }
//            if (player.playbackTracking?.videostatsPlaybackUrl?.baseUrl != null && player.playbackTracking.atrUrl?.baseUrl != null && player.playbackTracking.videostatsWatchtimeUrl?.baseUrl != null) {
//                YouTube.initPlayback(player.playbackTracking.videostatsPlaybackUrl.baseUrl,
//                    player.playbackTracking.atrUrl.baseUrl, player.playbackTracking.videostatsWatchtimeUrl.baseUrl
//                ).onSuccess { playback ->
//
//                }
//            }
//        }
//            .onFailure { error ->
//                error.printStackTrace()
//            }
//        YouTube.getLibraryPlaylists().onSuccess {data ->
//            val input = data.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(0)?.gridRenderer?.items ?: null
//            if (input != null) {
//                val list = parseLibraryPlaylist(input)
//                println(list)
//            }
//            else {
//                println("null")
//            }
//        }
//            .onFailure { error ->
//                error.printStackTrace()
//            }
//        }
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

fun parseLibraryPlaylist(input: List<GridRenderer.Item>): ArrayList<PlaylistsResult> {
    val list : ArrayList<PlaylistsResult> = arrayListOf()
    if (input.isNotEmpty()) {
        if (input.size >= 2) {
            input[1].musicTwoRowItemRenderer?.let {
                list.add(PlaylistsResult(
                    author = it.subtitle?.runs?.get(0)?.text ?: "",
                    browseId = it.navigationEndpoint.browseEndpoint?.browseId ?: "",
                    category = "",
                    itemCount = "",
                    resultType = "",
                    thumbnails = it.thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails ?: listOf(),
                    title = it.title.runs?.get(0)?.text ?: ""
                ))
            }
            if (input.size >= 3) {
                for (i in 2 until input.size) {
                    input[i].musicTwoRowItemRenderer?.let {
                        list.add(PlaylistsResult(
                            author = it.subtitle?.runs?.get(0)?.text ?: "",
                            browseId = it.navigationEndpoint.browseEndpoint?.browseId ?: "",
                            category = "",
                            itemCount = "",
                            resultType = "",
                            thumbnails = it.thumbnailRenderer.musicThumbnailRenderer?.thumbnail?.thumbnails ?: listOf(),
                            title = it.title.runs?.get(0)?.text ?: ""
                        ))
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
    val title: String
)

fun levenshtein(lhs : CharSequence, rhs : CharSequence) : Int {
    val lhsLength = lhs.length
    val rhsLength = rhs.length

    var cost = IntArray(lhsLength + 1) { it }
    var newCost = IntArray(lhsLength + 1) { 0 }

    for (i in 1..rhsLength) {
        newCost[0] = i

        for (j in 1..lhsLength) {
            val editCost= if(lhs[j - 1] == rhs[i - 1]) 0 else 1

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

fun bestMatchingIndex(s: String, list: List<String>): Int {
    val listCost = ArrayList<Int>()
    for (i in list.indices){
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

