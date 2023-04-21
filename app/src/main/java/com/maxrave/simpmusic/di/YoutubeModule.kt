package com.maxrave.simpmusic.di

import com.github.kiulian.downloader.Config
import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.YoutubeCallback
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo
import com.github.kiulian.downloader.downloader.response.Response
import com.github.kiulian.downloader.model.videos.VideoInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object YoutubeModule{
    @Singleton
    @Provides
    fun provideConfig(): Config = Config.Builder()
        .maxRetries(5)
        .build()

    @Singleton
    @Provides
    fun provideYoutubeDownloader(config: Config): YoutubeDownloader = YoutubeDownloader(config)
}