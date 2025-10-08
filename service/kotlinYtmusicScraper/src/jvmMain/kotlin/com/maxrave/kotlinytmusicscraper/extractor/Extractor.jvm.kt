package com.maxrave.kotlinytmusicscraper.extractor

import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.response.DownloadProgress
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo

actual class Extractor {
    actual fun init() {
    }

    actual fun update() {
    }

    actual fun ytdlpGetStreamUrl(
        videoId: String,
        poToken: String?,
        clientName: String,
        cookiePath: String?
    ): String? = null

    actual fun smartTubePlayer(videoId: String): List<Pair<Int, String>> = emptyList()

    actual fun newPipePlayer(videoId: String): List<Pair<Int, String>> {
        val streamInfo = StreamInfo.getInfo(NewPipe.getService(0), "https://www.youtube.com/watch?v=$videoId")
        val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
        return streamsList.mapNotNull {
            (it.itagItem?.id ?: return@mapNotNull null) to it.content
        }
    }

    actual fun mergeAudioVideoDownload(filePath: String): DownloadProgress {
        return DownloadProgress.failed("Not supported on JVM")
    }

    actual fun saveAudioWithThumbnail(filePath: String, track: SongItem): DownloadProgress {
        return DownloadProgress.failed("Not supported on JVM")
    }
}