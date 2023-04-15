package com.maxrave.simpmusic.di

import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.YoutubeCallback
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo
import com.github.kiulian.downloader.downloader.response.Response
import com.github.kiulian.downloader.model.videos.VideoInfo


class YoutubeModule(var videoId: String){
    var downloader = YoutubeDownloader()
    fun Request(): VideoInfo {
        val request = RequestVideoInfo(videoId)
            .callback(object : YoutubeCallback<VideoInfo?> {
                override fun onFinished(videoInfo: VideoInfo?) {
                    println("Finished parsing")
                }

                override fun onError(throwable: Throwable) {
                    println("Error: " + throwable.message)
                }
            })
            .async()
        val response: Response<VideoInfo> = downloader.getVideoInfo(request)
        val video: VideoInfo = response.data()
        return video
    }

}