package com.maxrave.simpmusic.service.test.source

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@UnstableApi
class MergingMediaSourceFactory(
    private val defaultMediaSourceFactory: DefaultMediaSourceFactory,
    private val dataStoreManager: DataStoreManager,
) : MediaSource.Factory {
    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
        defaultMediaSourceFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
        return this
    }

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
        defaultMediaSourceFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
        return this
    }

    override fun getSupportedTypes(): IntArray = defaultMediaSourceFactory.supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        Log.w("Merging Media Source", mediaItem.mediaMetadata.description.toString())
        val getVideo = runBlocking(Dispatchers.IO) { dataStoreManager.watchVideoInsteadOfPlayingAudio.first() } == DataStoreManager.TRUE
        Log.w("Merging Media Source", getVideo.toString())
        if (mediaItem.mediaMetadata.description == isVideo && getVideo) {
            val videoItem =
                mediaItem
                    .buildUpon()
                    .setMediaId("$isVideo${mediaItem.mediaId}")
                    .setCustomCacheKey("$isVideo${mediaItem.mediaId}")
                    .build()
            Log.w("Stream", "Video Item " + videoItem.mediaId)
            val videoSource =
                defaultMediaSourceFactory.createMediaSource(
                    videoItem,
                )
            Log.w("Stream", "VideoSource " + videoSource.mediaItem.mediaId)
            val audioSource = defaultMediaSourceFactory.createMediaSource(mediaItem)
            Log.w("Stream", "AudioSource " + audioSource.mediaItem.mediaId)
            return MergingMediaSource(videoSource, audioSource)
        } else {
            return defaultMediaSourceFactory.createMediaSource(mediaItem)
        }

//        val default = defaultMediaSourceFactory.createMediaSource(mediaItem.buildUpon().setMediaId("AUDIO-${mediaItem.mediaId}").build())
    }

    companion object {
        const val isVideo = "Video"
        const val isSong = "Song"
    }
}