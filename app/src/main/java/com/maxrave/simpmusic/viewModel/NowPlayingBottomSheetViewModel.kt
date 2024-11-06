package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.MUSIXMATCH
import com.maxrave.simpmusic.data.dataStore.DataStoreManager.Settings.YOUTUBE
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.SleepTimerState
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@UnstableApi
@KoinViewModel
class NowPlayingBottomSheetViewModel(
    private val application: Application,
): BaseViewModel(application) {
    override val tag: String
        get() = "NowPlayingBottomSheetViewModel"

    private val _uiState: MutableStateFlow<NowPlayingBottomSheetUIState> = MutableStateFlow(
        NowPlayingBottomSheetUIState(
            songEntity = null,
            listLocalPlaylist = emptyList(),
            mainLyricsProvider = MUSIXMATCH,
            sleepTimer = SleepTimerState(
                false,
                0
            )
        )
    )
    val uiState: StateFlow<NowPlayingBottomSheetUIState> get() = _uiState.asStateFlow()

    private var getSongAsFlow: Job? = null

    init {
        viewModelScope.launch {
            val sleepTimerJob = launch {
                simpleMediaServiceHandler.sleepTimerState.collectLatest {
                    _uiState.value = _uiState.value.copy(sleepTimer = it)
                }
            }
            val listLocalPlaylistJob = launch {
                mainRepository.getAllLocalPlaylists().collectLatest {
                    _uiState.value = _uiState.value.copy(listLocalPlaylist = it)
                }
            }
            val mainLyricsProviderJob = launch {
                dataStoreManager.lyricsProvider.collectLatest {
                    when (it) {
                        MUSIXMATCH -> {
                            _uiState.value = _uiState.value.copy(mainLyricsProvider = MUSIXMATCH)
                        }
                        YOUTUBE -> {
                            _uiState.value = _uiState.value.copy(mainLyricsProvider = YOUTUBE)
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
        getSongAsFlow = viewModelScope.launch {
            mainRepository.getSongAsFlow(videoId).collectLatest {
                _uiState.value = _uiState.value.copy(songEntity = it)
            }
        }
    }

    fun onUIEvent(ev: NowPlayingBottomSheetUIEvent) {
        val songEntity = uiState.value.songEntity ?: return
        viewModelScope.launch {
            when (ev) {
                is NowPlayingBottomSheetUIEvent.DeleteFromPlaylist -> {

                }
                is NowPlayingBottomSheetUIEvent.ToggleLike -> {
                    mainRepository.updateLikeStatus(
                        songEntity.videoId, if (songEntity.liked) 0 else 1
                    )
                }
                is NowPlayingBottomSheetUIEvent.Download -> {
                    when (songEntity.downloadState) {
                        DownloadState.STATE_NOT_DOWNLOADED -> {
                            mainRepository.updateDownloadState(
                                    videoId = songEntity.videoId,
                                    downloadState = DownloadState.STATE_PREPARING
                                )
                            val downloadRequest = DownloadRequest.Builder(
                                songEntity.videoId,
                                songEntity.videoId.toUri()
                            ).setData(songEntity.title.toByteArray())
                                .setCustomCacheKey(songEntity.videoId)
                                .build()
                            DownloadService.sendAddDownload(
                                application,
                                MusicDownloadService::class.java,
                                downloadRequest,
                                false
                            )
                            makeToast(getString(R.string.downloading))
                        }
                        DownloadState.STATE_PREPARING, DownloadState.STATE_DOWNLOADING -> {
                            DownloadService.sendRemoveDownload(
                                application,
                                MusicDownloadService::class.java,
                                songEntity.videoId,
                                false
                            )
                            mainRepository.updateDownloadState(
                                songEntity.videoId,
                                DownloadState.STATE_NOT_DOWNLOADED
                            )
                            makeToast(getString(R.string.removed_download))
                        }
                        DownloadState.STATE_DOWNLOADED -> {
                            DownloadService.sendRemoveDownload(
                                application,
                                MusicDownloadService::class.java,
                                songEntity.videoId,
                                false
                            )
                            mainRepository.updateDownloadState(
                                songEntity.videoId,
                                DownloadState.STATE_NOT_DOWNLOADED
                            )
                            makeToast(getString(R.string.removed_download))
                        }
                    }
                }
                is NowPlayingBottomSheetUIEvent.AddToPlaylist -> {
                    val targetPlaylist = uiState.value.listLocalPlaylist.find { it.id == ev.playlistId } ?: return@launch
                    val newList = (targetPlaylist.tracks ?: emptyList<String>()).toMutableList()
                    if (newList.contains(songEntity.videoId)) {
                        return@launch
                    }
                    else {
                        mainRepository.updateLocalPlaylistTracks(
                            tracks = newList.apply {
                                add(songEntity.videoId)
                            },
                            id = ev.playlistId
                        )
                        makeToast(getString(R.string.added_to_playlist))
                    }

                }
                is NowPlayingBottomSheetUIEvent.PlayNext -> {
                    simpleMediaServiceHandler.playNext(songEntity.toTrack())
                    makeToast(getString(R.string.play_next))
                }
                is NowPlayingBottomSheetUIEvent.AddToQueue -> {
                    simpleMediaServiceHandler.loadMoreCatalog(arrayListOf(songEntity.toTrack()), isAddToQueue = true)
                    makeToast(getString(R.string.added_to_queue))
                }
                is NowPlayingBottomSheetUIEvent.ChangeLyricsProvider -> {
                    if (listOf(MUSIXMATCH, YOUTUBE).contains(ev.lyricsProvider)) {
                        dataStoreManager.setLyricsProvider(ev.lyricsProvider)
                    } else return@launch
                }
                is NowPlayingBottomSheetUIEvent.SetSleepTimer -> {
                    if (ev.cancel) {
                        simpleMediaServiceHandler.sleepStop()
                        makeToast(getString(R.string.sleep_timer_off_done))
                    } else if (ev.minutes > 0) {
                        simpleMediaServiceHandler.sleepStart(ev.minutes)
                    }
                }
                is NowPlayingBottomSheetUIEvent.Share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    val url = "https://music.youtube.com/watch?v=${songEntity.videoId}"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, url)
                    val chooserIntent = Intent.createChooser(shareIntent, getString(R.string.share_url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    application.startActivity(chooserIntent)
                }
            }
        }
    }

}

data class NowPlayingBottomSheetUIState(
    val songEntity: SongEntity?,
    val listLocalPlaylist: List<LocalPlaylistEntity>,
    val mainLyricsProvider: String, // MUSIXMATCH OR YOUTUBE ONLY
    val sleepTimer: SleepTimerState
)

sealed class NowPlayingBottomSheetUIEvent {
    data class DeleteFromPlaylist(val videoId: String, val playlistId: Long): NowPlayingBottomSheetUIEvent()
    data object ToggleLike: NowPlayingBottomSheetUIEvent()
    data object Download: NowPlayingBottomSheetUIEvent()
    data class AddToPlaylist(val playlistId: Long): NowPlayingBottomSheetUIEvent()
    data object PlayNext: NowPlayingBottomSheetUIEvent()
    data object AddToQueue: NowPlayingBottomSheetUIEvent()
    data class ChangeLyricsProvider(val lyricsProvider: String): NowPlayingBottomSheetUIEvent()
    data class SetSleepTimer(val cancel: Boolean = false, val minutes: Int = 0): NowPlayingBottomSheetUIEvent()
    data object Share: NowPlayingBottomSheetUIEvent()
}