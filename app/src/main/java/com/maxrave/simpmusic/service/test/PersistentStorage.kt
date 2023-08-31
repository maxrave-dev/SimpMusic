package com.maxrave.simpmusic.service.test

import android.content.Context
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.di.DownloadCache
import com.maxrave.simpmusic.di.PlayerCache
import com.maxrave.simpmusic.extension.connectArtists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class PersistentStorage private constructor(val context: Context) {

    /**
     * Store any data which must persist between restarts, such as the most recently played song.
     */
    @Inject
    lateinit var dataStoreManager: DataStoreManager

    @Inject
    lateinit var mainRepository: MainRepository

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    companion object {

        @Volatile
        private var instance: PersistentStorage? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: PersistentStorage(context).also { instance = it }
            }
    }

    suspend fun saveRecentSong(mediaItem: MediaItem, position: Long) {

        withContext(Dispatchers.IO) {
            /**
             * After booting, Android will attempt to build static media controls for the most
             * recently played song. Artwork for these media controls should not be loaded
             * from the network as it may be too slow or unavailable immediately after boot. Instead
             * we convert the iconUri to point to the Glide on-disk cache.
             */
            dataStoreManager.saveRecentSong(mediaItem.mediaId , position)
        }
    }

    @UnstableApi
    suspend fun loadRecentSong(): MediaItem? {
        val mediaId = runBlocking { dataStoreManager.recentMediaId.first() }
        if (mediaId == null) {
            return null
        } else {
            val extras = Bundle().also {
                val position = runBlocking { dataStoreManager.recentPosition.first() }.toLong()
                it.putLong(MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS, position)
            }
            val mediaItem = runBlocking(Dispatchers.IO) {
                val data = runBlocking { mainRepository.getSongById(mediaId).first() }
                val metadata = MediaMetadata.Builder()
                    .setTitle(data?.title)
                    .setArtist(data?.artistName?.connectArtists())
                    .setArtworkUri(data?.thumbnails?.toUri())
                    .setAlbumTitle(data?.albumName)
                    .setExtras(extras)
                    .build()

                if (playerCache.keys.contains(mediaId)){
                    MediaItem.Builder()
                        .setMediaId(mediaId)
                        .setMediaMetadata(metadata)
                        .build()
                }
                else if (downloadCache.keys.contains(mediaId)) {
                    MediaItem.Builder()
                        .setMediaId(mediaId)
                        .setMediaMetadata(metadata)
                        .build()
                }
                else {
                    val uri = mainRepository.getStream(mediaId, 251).first()
                    MediaItem.Builder()
                        .setUri(uri)
                        .setMediaId(mediaId)
                        .setMediaMetadata(metadata)
                        .build()
                }
            }
            return mediaItem
        }
    }
}

const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px

private const val PREFERENCES_NAME = "simpmusic"
const val MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS = "playback_start_position_ms"