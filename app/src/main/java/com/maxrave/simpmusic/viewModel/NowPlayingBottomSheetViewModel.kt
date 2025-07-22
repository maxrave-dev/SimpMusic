package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.LRCLIB
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.MUSIXMATCH
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.SIMPMUSIC
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.YOUTUBE
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.manager.LocalPlaylistManager
import com.maxrave.simpmusic.data.model.searchResult.songs.Album
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.PlaylistType
import com.maxrave.simpmusic.service.QueueData
import com.maxrave.simpmusic.service.SleepTimerState
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.utils.collectLatestResource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.inject

@UnstableApi
class NowPlayingBottomSheetViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    private val downloadUtils: DownloadUtils by inject()
    private val localPlaylistManager: LocalPlaylistManager by inject()

    private val _uiState: MutableStateFlow<NowPlayingBottomSheetUIState> =
        MutableStateFlow(
            NowPlayingBottomSheetUIState(
                listLocalPlaylist = emptyList(),
                mainLyricsProvider = MUSIXMATCH,
                sleepTimer =
                    SleepTimerState(
                        false,
                        0,
                    ),
            ),
        )
    val uiState: StateFlow<NowPlayingBottomSheetUIState> get() = _uiState.asStateFlow()

    private var getSongAsFlow: Job? = null

    init {
        viewModelScope.launch {
            val sleepTimerJob =
                launch {
                    simpleMediaServiceHandler.sleepTimerState.collectLatest { sl ->
                        _uiState.update { it.copy(sleepTimer = sl) }
                    }
                }
            val listLocalPlaylistJob =
                launch {
                    mainRepository.getAllLocalPlaylists().collectLatest { list ->
                        _uiState.update { it.copy(listLocalPlaylist = list) }
                    }
                }
            val mainLyricsProviderJob =
                launch {
                    dataStoreManager.lyricsProvider.collectLatest { lyricsProvider ->
                        when (lyricsProvider) {
                            SIMPMUSIC -> {
                                _uiState.update { it.copy(mainLyricsProvider = SIMPMUSIC) }
                            }
                            MUSIXMATCH -> {
                                _uiState.update { it.copy(mainLyricsProvider = MUSIXMATCH) }
                            }
                            YOUTUBE -> {
                                _uiState.update { it.copy(mainLyricsProvider = YOUTUBE) }
                            }
                            LRCLIB -> {
                                _uiState.update { it.copy(mainLyricsProvider = LRCLIB) }
                            }
                            else -> {
                                log("Unknown lyrics provider", Log.ERROR)
                            }
                        }
                    }
                }
            sleepTimerJob.join()
            listLocalPlaylistJob.join()
            mainLyricsProviderJob.join()
        }
    }

    fun setSongEntity(songEntity: SongEntity?) {
        val songOrNowPlaying = songEntity ?: (simpleMediaServiceHandler.nowPlayingState.value.songEntity ?: return)
        viewModelScope.launch {
            songOrNowPlaying.videoId.let {
                mainRepository.getSongById(it).singleOrNull().let { song ->
                    if (song != null) {
                        getSongEntityFlow(videoId = song.videoId)
                    } else {
                        mainRepository.insertSong(songOrNowPlaying).singleOrNull()?.let {
                            getSongEntityFlow(videoId = songOrNowPlaying.videoId)
                        }
                    }
                }
            }
        }
    }

    private fun getSongEntityFlow(videoId: String) {
        getSongAsFlow?.cancel()
        if (videoId.isEmpty()) return
        getSongAsFlow =
            viewModelScope.launch {
                mainRepository.getSongAsFlow(videoId).collectLatest { song ->
                    log("getSongEntityFlow: $song", Log.WARN)
                    if (song != null) {
                        _uiState.update {
                            it.copy(
                                songUIState =
                                    NowPlayingBottomSheetUIState.SongUIState(
                                        videoId = song.videoId,
                                        title = song.title,
                                        listArtists =
                                            song.artistName?.mapIndexed { i, name ->
                                                Artist(name = name, id = song.artistId?.getOrNull(i) ?: "")
                                            } ?: emptyList(),
                                        thumbnails = song.thumbnails,
                                        liked = song.liked,
                                        downloadState = song.downloadState,
                                        album =
                                            song.albumName?.let { name ->
                                                Album(name = name, id = song.albumId ?: "")
                                            },
                                    ),
                            )
                        }
                    }
                }
            }
    }

    fun onUIEvent(ev: NowPlayingBottomSheetUIEvent) {
        val songUIState = uiState.value.songUIState
        if (songUIState.videoId.isEmpty()) return
        viewModelScope.launch {
            when (ev) {
                is NowPlayingBottomSheetUIEvent.DeleteFromPlaylist -> {
                }
                is NowPlayingBottomSheetUIEvent.ToggleLike -> {
                    mainRepository.updateLikeStatus(
                        songUIState.videoId,
                        if (songUIState.liked) 0 else 1,
                    )
                }
                is NowPlayingBottomSheetUIEvent.Download -> {
                    when (songUIState.downloadState) {
                        DownloadState.STATE_NOT_DOWNLOADED -> {
                            mainRepository.updateDownloadState(
                                videoId = songUIState.videoId,
                                downloadState = DownloadState.STATE_PREPARING,
                            )
                            downloadUtils.downloadTrack(
                                videoId = songUIState.videoId,
                                title = songUIState.title,
                                thumbnail = songUIState.thumbnails ?: "",
                            )
                            makeToast(getString(R.string.downloading))
                        }
                        DownloadState.STATE_PREPARING, DownloadState.STATE_DOWNLOADING -> {
                            downloadUtils.removeDownload(songUIState.videoId)
                            mainRepository.updateDownloadState(
                                songUIState.videoId,
                                DownloadState.STATE_NOT_DOWNLOADED,
                            )
                            makeToast(getString(R.string.removed_download))
                        }
                        DownloadState.STATE_DOWNLOADED -> {
                            downloadUtils.removeDownload(songUIState.videoId)
                            mainRepository.updateDownloadState(
                                songUIState.videoId,
                                DownloadState.STATE_NOT_DOWNLOADED,
                            )
                            makeToast(getString(R.string.removed_download))
                        }
                    }
                }
                is NowPlayingBottomSheetUIEvent.AddToPlaylist -> {
                    val targetPlaylist = uiState.value.listLocalPlaylist.find { it.id == ev.playlistId } ?: return@launch
                    val newList = (targetPlaylist.tracks ?: emptyList<String>()).toMutableList()
                    if (newList.contains(songUIState.videoId)) {
                        return@launch
                    } else {
                        val songEntity = mainRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                        localPlaylistManager.addTrackToLocalPlaylist(id = ev.playlistId, song = songEntity).collectLatestResource(
                            onSuccess = {
                                makeToast(getString(R.string.added_to_playlist))
                            },
                            onError = {
                                makeToast(getString(R.string.error))
                            },
                        )
                    }
                }
                is NowPlayingBottomSheetUIEvent.PlayNext -> {
                    val songEntity = mainRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                    simpleMediaServiceHandler.playNext(songEntity.toTrack())
                    makeToast(getString(R.string.play_next))
                }
                is NowPlayingBottomSheetUIEvent.AddToQueue -> {
                    val songEntity = mainRepository.getSongById(songUIState.videoId).singleOrNull() ?: return@launch
                    simpleMediaServiceHandler.loadMoreCatalog(arrayListOf(songEntity.toTrack()), isAddToQueue = true)
                    makeToast(getString(R.string.added_to_queue))
                }
                is NowPlayingBottomSheetUIEvent.ChangeLyricsProvider -> {
                    if (listOf(SIMPMUSIC, MUSIXMATCH, YOUTUBE, LRCLIB).contains(ev.lyricsProvider)) {
                        dataStoreManager.setLyricsProvider(ev.lyricsProvider)
                    } else {
                        return@launch
                    }
                }
                is NowPlayingBottomSheetUIEvent.SetSleepTimer -> {
                    if (ev.cancel) {
                        simpleMediaServiceHandler.sleepStop()
                        makeToast(getString(R.string.sleep_timer_off_done))
                    } else if (ev.minutes > 0) {
                        simpleMediaServiceHandler.sleepStart(ev.minutes)
                    }
                }
                is NowPlayingBottomSheetUIEvent.ChangePlaybackSpeedPitch -> {
                    dataStoreManager.setPlaybackSpeed(ev.speed)
                    dataStoreManager.setPitch(ev.pitch)
                }
                is NowPlayingBottomSheetUIEvent.Share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    val url = "https://music.youtube.com/watch?v=${songUIState.videoId}"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                    val chooserIntent =
                        Intent.createChooser(shareIntent, getString(R.string.share_url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    application.startActivity(chooserIntent)
                }

                is NowPlayingBottomSheetUIEvent.StartRadio -> {
                    mainRepository
                        .getRadioArtist(
                            WatchEndpoint(
                                videoId = ev.videoId,
                                playlistId = "RDAMVM${ev.videoId}",
                            ),
                        ).collectLatest { res ->
                            val data = res.data
                            when (res) {
                                is Resource.Success if (data != null && data.first.isNotEmpty()) -> {
                                    setQueueData(
                                        QueueData(
                                            listTracks = data.first,
                                            firstPlayedTrack = data.first.first(),
                                            playlistId = "RDAMVM${ev.videoId}",
                                            playlistName = ev.name,
                                            playlistType = PlaylistType.RADIO,
                                            continuation = data.second,
                                        ),
                                    )
                                    loadMediaItem(
                                        data.first.first(),
                                        Config.PLAYLIST_CLICK,
                                        0,
                                    )
                                }
                                else -> {
                                    makeToast(res.message ?: getString(R.string.error))
                                }
                            }
                        }
                }
            }
        }
    }
}

data class NowPlayingBottomSheetUIState(
    val songUIState: SongUIState = SongUIState(),
    val listLocalPlaylist: List<LocalPlaylistEntity>,
    val mainLyricsProvider: String, // MUSIXMATCH OR YOUTUBE ONLY
    val sleepTimer: SleepTimerState,
) {
    data class SongUIState(
        val videoId: String = "",
        val title: String = "",
        val listArtists: List<Artist> = emptyList(),
        val thumbnails: String? = null,
        val liked: Boolean = false,
        val downloadState: Int = DownloadState.STATE_NOT_DOWNLOADED,
        val album: Album? = null,
    )
}

sealed class NowPlayingBottomSheetUIEvent {
    data class DeleteFromPlaylist(
        val videoId: String,
        val playlistId: Long,
    ) : NowPlayingBottomSheetUIEvent()

    data object ToggleLike : NowPlayingBottomSheetUIEvent()

    data object Download : NowPlayingBottomSheetUIEvent()

    data class AddToPlaylist(
        val playlistId: Long,
    ) : NowPlayingBottomSheetUIEvent()

    data object PlayNext : NowPlayingBottomSheetUIEvent()

    data object AddToQueue : NowPlayingBottomSheetUIEvent()

    data class ChangeLyricsProvider(
        val lyricsProvider: String,
    ) : NowPlayingBottomSheetUIEvent()

    data class SetSleepTimer(
        val cancel: Boolean = false,
        val minutes: Int = 0,
    ) : NowPlayingBottomSheetUIEvent()

    data class ChangePlaybackSpeedPitch(
        val speed: Float,
        val pitch: Int,
    ) : NowPlayingBottomSheetUIEvent()

    data class StartRadio(
        val videoId: String,
        val name: String,
    ) : NowPlayingBottomSheetUIEvent()

    data object Share : NowPlayingBottomSheetUIEvent()
}