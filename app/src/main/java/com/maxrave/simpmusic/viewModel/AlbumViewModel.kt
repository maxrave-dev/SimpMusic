package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Artist
import com.maxrave.simpmusic.extension.toAlbumEntity
import com.maxrave.simpmusic.extension.toArrayListTrack
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.service.PlaylistType
import com.maxrave.simpmusic.service.QueueData
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import com.maxrave.simpmusic.viewModel.uiState.AlbumUIState
import com.maxrave.simpmusic.viewModel.uiState.LocalPlaylistState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import org.koin.core.component.inject
import java.time.LocalDateTime


@UnstableApi
class AlbumViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    private val downloadUtils: DownloadUtils by inject()

    override val tag: String = "AlbumViewModel"

    private val _uiState: MutableStateFlow<AlbumUIState> = MutableStateFlow(AlbumUIState.initial())
    val uiState: StateFlow<AlbumUIState> = _uiState

    private var job: Job? = null
    private var collectDownloadStateJob: Job? = null

    fun updateBrowseId(browseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(browseId = browseId) }
            mainRepository.getAlbumData(browseId).collectLatest { res ->
                when (res) {
                    is Resource.Success -> {
                        val data = res.data
                        if (data != null) {
                            _uiState.update {
                                it.copy(
                                    browseId = browseId,
                                    title = data.title,
                                    thumbnail = data.thumbnails?.lastOrNull()?.url,
                                    artist =
                                        data.artists.firstOrNull() ?: Artist(
                                            id = null,
                                            name = "",
                                        ),
                                    year = data.year ?: LocalDateTime.now().year.toString(),
                                    trackCount = data.trackCount,
                                    description = data.description,
                                    length = data.duration ?: "",
                                    listTrack = data.tracks,
                                    loadState = LocalPlaylistState.PlaylistLoadState.Success,
                                )
                            }
                            mainRepository.getAlbum(browseId).singleOrNull().let { album ->
                                if (album != null) {
                                    _uiState.update {
                                        it.copy(
                                            downloadState = album.downloadState,
                                            liked = album.liked,
                                        )
                                    }
                                    mainRepository.updateAlbumInLibrary(LocalDateTime.now(), browseId)
                                } else {
                                    mainRepository.insertAlbum(data.toAlbumEntity(browseId)).singleOrNull().let {
                                        log("Insert Album $it", Log.DEBUG)
                                        data.tracks.forEach { track ->
                                            mainRepository.insertSong(track.toSongEntity()).singleOrNull()?.let {
                                                log("Insert Song $it", Log.DEBUG)
                                            }
                                        }
                                    }
                                }
                            }
                            getAlbumFlow(browseId)
                        } else {
                            makeToast(getString(R.string.error) + ": Null data")
                            _uiState.update {
                                it.copy(
                                    loadState = LocalPlaylistState.PlaylistLoadState.Error,
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        mainRepository.getAlbum(browseId).singleOrNull().let { albumEntity ->
                            if (albumEntity != null) {
                                _uiState.update {
                                    it.copy(
                                        browseId = browseId,
                                        title = albumEntity.title,
                                        thumbnail = albumEntity.thumbnails,
                                        artist =
                                            Artist(
                                                id = albumEntity.artistId?.firstOrNull(),
                                                name = albumEntity.artistName?.firstOrNull() ?: "",
                                            ),
                                        year = albumEntity.year ?: LocalDateTime.now().year.toString(),
                                        trackCount = albumEntity.trackCount,
                                        description = albumEntity.description,
                                        length = albumEntity.duration ?: "",
                                        listTrack =
                                            (
                                                mainRepository
                                                    .getSongsByListVideoId(albumEntity.tracks ?: emptyList())
                                                    .singleOrNull() ?: emptyList()
                                            ).toArrayListTrack(),
                                        loadState = LocalPlaylistState.PlaylistLoadState.Success,
                                    )
                                }
                            } else {
                                log("Error: ${res.message}", Log.ERROR)
                                makeToast(getString(R.string.error) + ": ${res.message}")
                                _uiState.update {
                                    it.copy(
                                        loadState = LocalPlaylistState.PlaylistLoadState.Error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun setBrush(brush: List<Color>) {
        _uiState.update {
            it.copy(
                colors = brush,
            )
        }
    }

    private fun getAlbumFlow(browseId: String) {
        job?.cancel()
        collectDownloadStateJob?.cancel()
        job =
            viewModelScope.launch {
                mainRepository.getAlbumAsFlow(browseId).collectLatest { album ->
                    if (album != null) {
                        _uiState.update {
                            it.copy(
                                downloadState = album.downloadState,
                                liked = album.liked,
                            )
                        }
                    }
                }
            }
        collectDownloadStateJob =
            viewModelScope.launch {
                downloadUtils.downloadTask.collectLatest { downloadTask ->
                    var count = 0
                    uiState.value.listTrack.forEach { track ->
                        if (downloadTask.get(track.videoId) == DownloadState.STATE_DOWNLOADED) {
                            count++
                        }
                    }
                    if (count == uiState.value.listTrack.size) {
                        mainRepository.updateAlbumDownloadState(uiState.value.browseId, DownloadState.STATE_DOWNLOADED)
                        _uiState.update {
                            it.copy(
                                downloadState = DownloadState.STATE_DOWNLOADED,
                            )
                        }
                    }
                }
            }
    }

    fun playTrack(track: Track) {
        setQueueData(
            QueueData(
                listTracks = uiState.value.listTrack.toCollection(ArrayList()),
                firstPlayedTrack = track,
                playlistId = uiState.value.browseId.replaceFirst("VL", ""),
                playlistName = "${getString(R.string.album)} \"${uiState.value.title}\"",
                playlistType = PlaylistType.PLAYLIST,
                continuation = null,
            ),
        )
        val index = uiState.value.listTrack.indexOf(track)
        loadMediaItem(track, Config.ALBUM_CLICK, if (index == -1) 0 else index)
    }

    fun shuffle() {
        if (uiState.value.listTrack.isEmpty()) {
            makeToast(getString(R.string.playlist_is_empty))
            return
        }
        val shuffleList = uiState.value.listTrack.shuffled()
        val randomIndex = shuffleList.indices.random()
        setQueueData(
            QueueData(
                listTracks = shuffleList.toCollection(ArrayList()),
                firstPlayedTrack = shuffleList[randomIndex],
                playlistId = uiState.value.browseId.replaceFirst("VL", ""),
                playlistName = "${getString(R.string.album)} \"${uiState.value.title}\"",
                playlistType = PlaylistType.PLAYLIST,
                continuation = null,
            ),
        )
        loadMediaItem(shuffleList[randomIndex], Config.ALBUM_CLICK, randomIndex)
    }

    fun downloadFullAlbum() {
        viewModelScope.launch {
            // Insert all song to database
            uiState.value.listTrack.forEach { track ->
                mainRepository.insertSong(track.toSongEntity()).singleOrNull()?.let {
                    log("Insert Song $it", Log.DEBUG)
                }
            }
            val fullListSong =
                mainRepository
                    .getSongsByListVideoId(uiState.value.listTrack.map { it.videoId })
                    .singleOrNull() ?: emptyList()
            log("Full list song: $fullListSong", Log.DEBUG)
            if (fullListSong.isEmpty()) {
                makeToast(getString(R.string.playlist_is_empty))
                return@launch
            }
            val listJob = fullListSong.filter { it.downloadState != DownloadState.STATE_DOWNLOADED }
            log("List job: $listJob", Log.DEBUG)
            if (listJob.isEmpty()) {
                makeToast(getString(R.string.downloaded))
                return@launch
            }
            mainRepository.updateAlbumDownloadState(uiState.value.browseId, DownloadState.STATE_DOWNLOADING)
            listJob.forEach {
                log("Download: ${it.videoId} ${it.thumbnails}", Log.DEBUG)
                downloadUtils.downloadTrack(
                    it.videoId,
                    it.title,
                    it.thumbnails ?: "",
                )
            }
        }
    }
}