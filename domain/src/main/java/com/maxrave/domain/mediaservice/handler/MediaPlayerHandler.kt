package com.maxrave.domain.mediaservice.handler

import android.app.Activity
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.maxrave.domain.data.entities.NewFormatEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.mediaService.SponsorSkipSegments
import com.maxrave.domain.data.player.GenericMediaItem
import com.maxrave.domain.mediaservice.player.MediaPlayerInterface
import kotlinx.coroutines.flow.StateFlow

/**
 * Handler interface for managing media playback service operations
 */
interface MediaPlayerHandler {
    // Core player interface
    val player: MediaPlayerInterface

    // State flows
    val simpleMediaState: StateFlow<SimpleMediaState>
    val nowPlaying: StateFlow<GenericMediaItem?>
    val queueData: StateFlow<QueueData?>
    val controlState: StateFlow<ControlState>
    val nowPlayingState: StateFlow<NowPlayingTrackState>
    val sleepTimerState: StateFlow<SleepTimerState>
    val skipSegments: StateFlow<List<SponsorSkipSegments>?>
    val format: StateFlow<NewFormatEntity?>
    val currentSongIndex: StateFlow<Int>

    // Playback control
    suspend fun onPlayerEvent(playerEvent: PlayerEvent)

    fun toggleRadio()

    fun toggleLike()

    fun like(liked: Boolean)

    fun resetSongAndQueue()

    // Sleep timer
    fun sleepStart(minutes: Int)

    fun sleepStop()

    // Media management
    fun removeMediaItem(position: Int)

    fun addMediaItem(
        mediaItem: GenericMediaItem,
        playWhenReady: Boolean = true,
    )

    fun clearMediaItems()

    fun addMediaItemList(mediaItemList: List<GenericMediaItem>)

    fun playMediaItemInMediaSource(index: Int)

    fun currentSongIndex(): Int

    suspend fun swap(
        from: Int,
        to: Int,
    )

    fun resetCrossfade()

    // Queue management
    fun shufflePlaylist(randomTrackIndex: Int = 0)

    fun loadMore()

    fun getRelated(videoId: String)

    fun setQueueData(queueData: QueueData.Data)

    fun getCurrentMediaItem(): GenericMediaItem?

    // Track operations
    suspend fun moveItemUp(position: Int)

    suspend fun moveItemDown(position: Int)

    fun addFirstMediaItemToIndex(
        mediaItem: GenericMediaItem?,
        index: Int,
    )

    fun reset()

    suspend fun load(
        downloaded: Int = 0,
        index: Int? = null,
    )

    suspend fun loadMoreCatalog(
        listTrack: ArrayList<Track>,
        isAddToQueue: Boolean = false,
    )

    suspend fun updateCatalog(
        downloaded: Int = 0,
        index: Int? = null,
    ): Boolean

    fun addQueueToPlayer()

    fun loadPlaylistOrAlbum(index: Int? = null)

    fun setCurrentSongIndex(index: Int)

    suspend fun playNext(track: Track)

    suspend fun <T> loadMediaItem(
        anyTrack: T,
        type: String,
        index: Int? = null,
    )

    // Playback information
    fun getPlayerDuration(): Long

    fun getProgress(): Long

    fun startProgressUpdate()

    fun stopProgressUpdate()

    fun startBufferedUpdate()

    fun stopBufferedUpdate()

    // Settings
    fun mayBeNormalizeVolume()

    fun mayBeSaveRecentSong(runBlocking: Boolean = false)

    fun mayBeSavePlaybackState()

    fun mayBeRestoreQueue()

    // Lifecycle
    fun shouldReleaseOnTaskRemoved(): Boolean

    fun release()

    /**
     * Service
     */
    fun startMediaService(
        context: Context,
        serviceConnection: ServiceConnection,
    )

    fun stopMediaService(context: Context)

    fun setActivitySession(
        context: Context,
        cls: Class<out Activity>,
        service: IBinder?,
    )
}

// State classes and enums - these would need to be defined in domain layer
sealed class RepeatState {
    data object None : RepeatState()

    data object All : RepeatState()

    data object One : RepeatState()
}

sealed class PlayerEvent {
    data object PlayPause : PlayerEvent()

    data object Backward : PlayerEvent()

    data object Forward : PlayerEvent()

    data object Stop : PlayerEvent()

    data object Next : PlayerEvent()

    data object Previous : PlayerEvent()

    data object Shuffle : PlayerEvent()

    data object Repeat : PlayerEvent()

    data class UpdateProgress(
        val newProgress: Float,
    ) : PlayerEvent()

    data object ToggleLike : PlayerEvent()
}

sealed class SimpleMediaState {
    data object Initial : SimpleMediaState()

    data object Ended : SimpleMediaState()

    data class Ready(
        val duration: Long,
    ) : SimpleMediaState()

    data class Loading(
        val bufferedPercentage: Int,
        val duration: Long,
    ) : SimpleMediaState()

    data class Progress(
        val progress: Long,
    ) : SimpleMediaState()

    data class Buffering(
        val position: Long,
    ) : SimpleMediaState()
}

data class ControlState(
    val isPlaying: Boolean,
    val isShuffle: Boolean,
    val repeatState: RepeatState,
    val isLiked: Boolean,
    val isNextAvailable: Boolean,
    val isPreviousAvailable: Boolean,
    val isCrossfading: Boolean,
)

data class NowPlayingTrackState(
    val mediaItem: GenericMediaItem,
    val track: Track?,
    val songEntity: SongEntity?,
) {
    fun isNotEmpty(): Boolean = this != initial()

    companion object {
        fun initial(): NowPlayingTrackState =
            NowPlayingTrackState(
                mediaItem = GenericMediaItem.EMPTY,
                track = null,
                songEntity = null,
            )
    }
}

data class SleepTimerState(
    val isDone: Boolean,
    val timeRemaining: Int,
)

data class QueueData(
    val queueState: StateSource = StateSource.STATE_CREATED,
    val data: Data = Data(),
) {
    data class Data(
        val listTracks: List<Track> = arrayListOf(),
        val firstPlayedTrack: Track? = null,
        val playlistId: String? = null,
        val playlistName: String? = null,
        val playlistType: PlaylistType? = null,
        val continuation: String? = null,
    )

    enum class StateSource {
        STATE_CREATED,
        STATE_INITIALIZING,
        STATE_INITIALIZED,
        STATE_ERROR,
    }

    fun addTrackList(tracks: Collection<Track>): QueueData {
        val temp = this.data.listTracks.toMutableList()
        temp.addAll(tracks)
        return this.copy(
            data =
                this.data.copy(
                    listTracks = temp,
                ),
        )
    }

    fun addToIndex(
        track: Track,
        index: Int,
    ): QueueData {
        val temp = this.data.listTracks.toMutableList()
        temp.add(index, track)
        return this.copy(
            data =
                this.data.copy(
                    listTracks = temp,
                ),
        )
    }

    fun removeFirstTrackForPlaylistAndAlbum(): QueueData {
        val temp = this.data.listTracks.toMutableList()
        temp.removeAt(0)
        return this.copy(
            data =
                this.data.copy(
                    listTracks = temp,
                    firstPlayedTrack = null,
                ),
        )
    }

    fun removeTrackWithIndex(index: Int): QueueData {
        val temp = this.data.listTracks.toMutableList()
        temp.removeAt(index)
        return this.copy(
            data =
                this.data.copy(
                    listTracks = temp,
                ),
        )
    }

    fun setContinuation(continuation: String): QueueData =
        this.copy(
            data =
                this.data.copy(
                    continuation = continuation,
                ),
        )

    fun isLocalPlaylist(): Boolean = this.data.playlistType == PlaylistType.LOCAL_PLAYLIST

    fun isRadio(): Boolean = this.data.playlistType == PlaylistType.RADIO

    fun isPlaylist(): Boolean = this.data.playlistType == PlaylistType.PLAYLIST
}

enum class PlaylistType {
    PLAYLIST,
    LOCAL_PLAYLIST,
    RADIO,
}