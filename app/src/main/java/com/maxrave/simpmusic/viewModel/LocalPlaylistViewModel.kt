package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalPlaylistViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    application: Application
) : AndroidViewModel(application) {
    @Inject
    lateinit var downloadUtils: DownloadUtils

    val id: MutableLiveData<Long> = MutableLiveData()

    private var _listLocalPlaylist: MutableLiveData<LocalPlaylistEntity?> = MutableLiveData()
    val localPlaylist: LiveData<LocalPlaylistEntity?> = _listLocalPlaylist

    private var _listTrack: MutableLiveData<List<SongEntity>> = MutableLiveData()
    val listTrack: LiveData<List<SongEntity>> = _listTrack

    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()

    fun getLocalPlaylist(id: Long) {
        viewModelScope.launch {
            mainRepository.getLocalPlaylist(id).collect {
                _listLocalPlaylist.postValue(it)
            }
        }
    }

    fun getListTrack(list: List<String>) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(list).collect {
                _listTrack.postValue(it)
                var count = 0
                it.forEach { track ->
                    if (track.downloadState == DownloadState.STATE_DOWNLOADED) {
                        count++
                    }
                }
                if (count == it.size && localPlaylist.value?.downloadState != DownloadState.STATE_DOWNLOADED) {
                    updatePlaylistDownloadState(id.value!!, DownloadState.STATE_DOWNLOADED)
                    getLocalPlaylist(id.value!!)
                }
                else if (count != it.size && localPlaylist.value?.downloadState != DownloadState.STATE_NOT_DOWNLOADED && localPlaylist.value?.downloadState != DownloadState.STATE_DOWNLOADING) {
                    updatePlaylistDownloadState(id.value!!, DownloadState.STATE_NOT_DOWNLOADED)
                    getLocalPlaylist(id.value!!)
                }
            }
        }
    }

    val playlistDownloadState: MutableStateFlow<Int> = MutableStateFlow(DownloadState.STATE_NOT_DOWNLOADED)


    fun updatePlaylistDownloadState(id: Long, state: Int) {
        viewModelScope.launch {
            mainRepository.getLocalPlaylist(id).collect { playlist ->
                _listLocalPlaylist.value = playlist
                mainRepository.updateLocalPlaylistDownloadState(state, id)
                playlistDownloadState.value = state
            }
        }
    }

    val listJob: MutableStateFlow<ArrayList<SongEntity>> = MutableStateFlow(arrayListOf())

//        var downloadState: StateFlow<List<Download?>>
//        viewModelScope.launch {
//            downloadState = downloadUtils.getAllDownloads().stateIn(viewModelScope)
//            downloadState.collectLatest { down ->
//                if (down.isNotEmpty()){
//                    var count = 0
//                    down.forEach { downloadItem ->
//                        if (downloadItem?.state == Download.STATE_COMPLETED) {
//                            count++
//                        }
//                        else if (downloadItem?.state == Download.STATE_FAILED) {
//                            updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADING)
//                        }
//                    }
//                    if (count == down.size) {
//                        mainRepository.getLocalPlaylist(id).collect{ playlist ->
//                            mainRepository.getSongsByListVideoId(playlist.tracks!!).collect{ tracks ->
//                                tracks.forEach { track ->
//                                    if (track.downloadState != DownloadState.STATE_DOWNLOADED) {
//                                        mainRepository.updateDownloadState(track.videoId, DownloadState.STATE_NOT_DOWNLOADED)
//                                        Toast.makeText(getApplication(), "Download Failed", Toast.LENGTH_SHORT).show()
//                                    }
//                                }
//                            }
//                        }
//                        Log.d("Check Downloaded", "Downloaded")
//                        updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADED)
//                        Toast.makeText(getApplication(), "Download Completed", Toast.LENGTH_SHORT).show()
//                    }
//                    else {
//                        updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADING)
//                    }
//                }
//                else {
//                    updatePlaylistDownloadState(id, DownloadState.STATE_NOT_DOWNLOADED)
//                }
//            }
//        }
//    }

    @UnstableApi
    fun getDownloadStateFromService(videoId: String) {
        viewModelScope.launch {
            val downloadState = downloadUtils.getDownload(videoId).stateIn(viewModelScope)
            downloadState.collect { down ->
                if (down != null) {
                    when (down.state) {
                        Download.STATE_COMPLETED -> {
                            mainRepository.getSongById(videoId).collect{ song ->
                                if (song?.downloadState != DownloadState.STATE_DOWNLOADED) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_DOWNLOADED)
                                }
                            }
                            Log.d("Check Downloaded", "Downloaded")
                        }
                        Download.STATE_FAILED -> {
                            mainRepository.getSongById(videoId).collect{ song ->
                                if (song?.downloadState != DownloadState.STATE_NOT_DOWNLOADED) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_NOT_DOWNLOADED)
                                }
                            }
                            Log.d("Check Downloaded", "Failed")
                        }
                        Download.STATE_DOWNLOADING -> {
                            mainRepository.getSongById(videoId).collect{ song ->
                                if (song?.downloadState != DownloadState.STATE_DOWNLOADING) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_DOWNLOADING)
                                }
                            }
                            Log.d("Check Downloaded", "Downloading ${down.percentDownloaded}")
                        }
                        Download.STATE_QUEUED -> {
                            mainRepository.getSongById(videoId).collect{ song ->
                                if (song?.downloadState != DownloadState.STATE_PREPARING) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_PREPARING)
                                }
                            }
                            Log.d("Check Downloaded", "Queued")
                        }
                        else -> {
                            Log.d("Check Downloaded", "Not Downloaded")
                        }
                    }
                }
            }
        }
    }

    fun updatePlaylistTitle(title: String, id: Long) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistTitle(title, id)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            mainRepository.deleteLocalPlaylist(id)
        }
    }

    fun updatePlaylistThumbnail(uri: String, id: Long) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistThumbnail(uri, id)
        }
    }

    fun clearLocalPlaylist() {
        _listLocalPlaylist.value = null
    }

    fun updateDownloadState(videoId: String, state: Int) {
        viewModelScope.launch {
            mainRepository.updateDownloadState(videoId, state)
        }
    }

    fun deleteItem(song: SongEntity?, id: Long) {
        viewModelScope.launch {
            val tempList: ArrayList<SongEntity> = arrayListOf()
            tempList.addAll(listTrack.value!!)
            tempList.remove(song)
            val listTrack: ArrayList<String> = arrayListOf()
            tempList.forEach {
                listTrack.add(it.videoId)
            }
            mainRepository.updateLocalPlaylistTracks(
                listTrack, id
            )
            mainRepository.getLocalPlaylist(id).collect { playlist ->
                _listLocalPlaylist.value = playlist
                if (!playlist.tracks.isNullOrEmpty()) {
                    mainRepository.getSongsByListVideoId(playlist.tracks).collect { tracks ->
                        _listTrack.value = tracks
                    }
                }
                else {
                    _listTrack.postValue(arrayListOf())
                }
            }
        }
    }

    @UnstableApi
    fun downloadFullPlaylistState(id: Long) {
            viewModelScope.launch {
                downloadUtils.downloads.collect { download ->
                    playlistDownloadState.value =
                        if (listJob.value.all { download[it.videoId]?.state == Download.STATE_COMPLETED }) {
                            mainRepository.updateLocalPlaylistDownloadState(
                                DownloadState.STATE_DOWNLOADED,
                                id
                            )
                            DownloadState.STATE_DOWNLOADED
                        } else if (listJob.value.all {
                                download[it.videoId]?.state == Download.STATE_QUEUED
                                        || download[it.videoId]?.state == Download.STATE_DOWNLOADING
                                        || download[it.videoId]?.state == Download.STATE_COMPLETED
                            }) {
                            mainRepository.updateLocalPlaylistDownloadState(
                                DownloadState.STATE_DOWNLOADING,
                                id
                            )
                            DownloadState.STATE_DOWNLOADING
                        } else {
                            mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_NOT_DOWNLOADED, id)
                            DownloadState.STATE_NOT_DOWNLOADED
                        }
                }
            }
    }

}