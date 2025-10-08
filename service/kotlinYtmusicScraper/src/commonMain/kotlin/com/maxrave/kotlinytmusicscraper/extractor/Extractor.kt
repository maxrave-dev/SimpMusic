package com.maxrave.kotlinytmusicscraper.extractor

import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.response.DownloadProgress

expect class Extractor() {
    fun init()
    fun update()
    fun mergeAudioVideoDownload(filePath: String): DownloadProgress
    fun saveAudioWithThumbnail(
        filePath: String,
        track: SongItem
    ): DownloadProgress
    fun ytdlpGetStreamUrl(
        videoId: String,
        poToken: String?,
        clientName: String,
        cookiePath: String?
    ): String? // text response
    fun smartTubePlayer(
        videoId: String
    ): List<Pair<Int, String>>
    fun newPipePlayer(
        videoId: String
    ): List<Pair<Int, String>>
}