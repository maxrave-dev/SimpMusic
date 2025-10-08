package com.maxrave.kotlinytmusicscraper.extractor

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.liskovsoft.sharedutils.prefs.GlobalPreferences
import com.liskovsoft.youtubeapi.app.AppService
import com.liskovsoft.youtubeapi.videoinfo.V2.VideoInfoService
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.response.DownloadProgress
import com.maxrave.logger.Logger
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatform.getKoin
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo

private const val TAG = "Extractor"
actual class Extractor {
    private val mAppService = AppService.instance()
    private val mVideoInfoService = VideoInfoService.instance()

    actual fun init() {
        try {
            YoutubeDL.getInstance().init(getKoin().get())
            YoutubeDL.getInstance()
        } catch (e: YoutubeDLException) {
            e.printStackTrace()
        }
    }

    actual fun update() {
        YoutubeDL.getInstance().updateYoutubeDL(getKoin().get())
    }

    actual fun ytdlpGetStreamUrl(
        videoId: String,
        poToken: String?,
        clientName: String,
        cookiePath: String?
    ): String? {
        val ytDlp = YoutubeDL.getInstance()
        val ytRequest = YoutubeDLRequest("https://music.youtube.com/watch?v=$videoId")
        if (!cookiePath.isNullOrEmpty()) {
            ytRequest.addOption("--cookies", cookiePath ?: "")
        }
        ytRequest.addOption(
            "--extractor-args",
            "youtube:player_client=$clientName;youtube:webpage_skip" +
                if (clientName.contains("web") && poToken != null) ";youtube:po_token=$clientName.gvs+$poToken;" else "",
        )
        ytRequest.addOption("--dump-json")
        val result = ytDlp?.execute(ytRequest)
        return result?.out
    }

    actual fun smartTubePlayer(videoId: String): List<Pair<Int, String>> {
        try {
            if (GlobalPreferences.sInstance == null) {
                GlobalPreferences.instance(getKoin().get())
            }
            mAppService.resetClientPlaybackNonce()
            mAppService.refreshCacheIfNeeded()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val videoInfo = mVideoInfoService.getVideoInfo(videoId, "")
        val streamsList =
            (videoInfo?.adaptiveFormats ?: emptyList()) +
                (videoInfo?.regularFormats ?: emptyList()) +
                (videoInfo?.restrictedFormats ?: emptyList())
        return streamsList.mapNotNull {
            it.iTag to it.url
        }
    }

    actual fun newPipePlayer(videoId: String): List<Pair<Int, String>> {
        val streamInfo = StreamInfo.getInfo(NewPipe.getService(0), "https://www.youtube.com/watch?v=$videoId")
        val streamsList = streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams
        return streamsList.mapNotNull {
            (it.itagItem?.id ?: return@mapNotNull null) to it.content
        }
    }

    actual fun mergeAudioVideoDownload(filePath: String): DownloadProgress {
        val command =
            listOf(
                "-i",
                ("$filePath.mp4"),
                "-i",
                ("$filePath.webm"),
                "-c:v",
                "copy",
                "-c:a",
                "aac",
                "-map",
                "0:v:0",
                "-map",
                "1:a:0",
                "-shortest",
                "$filePath-SimpMusic.mp4",
            ).joinToString(" ")

        if (FileSystem.SYSTEM.exists("$filePath-SimpMusic.mp4".toPath())) {
            FileSystem.SYSTEM.delete("$filePath-SimpMusic.mp4".toPath())
        }

        val session =
            FFmpegKit.execute(
                command,
            )
        if (ReturnCode.isSuccess(session.returnCode)) {
            // SUCCESS
            Logger.d(TAG, "Command succeeded ${session.state}, ${session.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                FileSystem.SYSTEM.delete("$filePath.mp4".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.VIDEO_DONE)
        } else if (ReturnCode.isCancel(session.returnCode)) {
            // CANCEL
            Logger.d(TAG, "Command cancelled ${session.state}, ${session.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                FileSystem.SYSTEM.delete("$filePath.mp4".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed(session.failStackTrace))
        } else {
            // FAILURE
            Logger.d(TAG, "Command failed ${session.state}, ${session.returnCode}, ${session.failStackTrace}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                FileSystem.SYSTEM.delete("$filePath.mp4".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed(session.failStackTrace))
        }
    }

    actual fun saveAudioWithThumbnail(
        filePath: String,
        track: SongItem
    ): DownloadProgress {
        val command =
            listOf(
                "-i",
                ("$filePath.webm"),
                "-q:a 0",
                "$filePath.mp3",
            ).joinToString(" ")

        try {
            if (FileSystem.SYSTEM.exists("$filePath.mp3".toPath())) {
                FileSystem.SYSTEM.delete("$filePath.mp3".toPath())
            }
            if (FileSystem.SYSTEM.exists("$filePath-simpmusic.mp3".toPath())) {
                FileSystem.SYSTEM.delete("$filePath-simpmusic.mp3".toPath())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val session =
            FFmpegKit.execute(
                command,
            )
        if (ReturnCode.isSuccess(session.returnCode)) {
            // SUCCESS
            Logger.d(TAG, "Command succeeded ${session.state}, ${session.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else if (ReturnCode.isCancel(session.returnCode)) {
            // CANCEL
            Logger.d(TAG, "Command cancelled ${session.state}, ${session.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed("Error"))
        } else {
            // FAILURE
            Logger.d(TAG, "Command failed ${session.state}, ${session.returnCode}, ${session.failStackTrace}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed("Error"))
        }

        val commandInject =
            listOf(
                "-i",
                "$filePath.mp3",
                "-i $filePath.jpg",
                "-map 0:a",
                "-map 1:v",
                "-c copy",
                "-id3v2_version 3",
                "-metadata",
                "title=\"${track.title}\"",
                "-metadata",
                "artist=\"${track.artists.joinToString(", ") { it.name }}\"",
                "-metadata",
                "album=\"${track.album?.name ?: track.title}\"",
                "-disposition:v:0 attached_pic",
                "$filePath-simpmusic.mp3",
            ).joinToString(" ")
        val sessionInject =
            FFmpegKit.execute(
                commandInject,
            )
        if (ReturnCode.isSuccess(sessionInject.returnCode)) {
            // SUCCESS
            Logger.d(TAG, "Command succeeded ${sessionInject.state}, ${sessionInject.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.mp3".toPath())
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.AUDIO_DONE)
        } else if (ReturnCode.isCancel(sessionInject.returnCode)) {
            // CANCEL
            Logger.d(TAG, "Command cancelled ${sessionInject.state}, ${sessionInject.returnCode}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                FileSystem.SYSTEM.delete("$filePath-simpmusic.mp3".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed("Error"))
        } else {
            // FAILURE
            Logger.d(TAG, "Command failed ${sessionInject.state}, ${sessionInject.returnCode}, ${sessionInject.failStackTrace}")
            try {
                FileSystem.SYSTEM.delete("$filePath.jpg".toPath())
                FileSystem.SYSTEM.delete("$filePath.webm".toPath())
                FileSystem.SYSTEM.delete("$filePath-simpmusic.mp3".toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return (DownloadProgress.failed("Error"))
        }
    }
}