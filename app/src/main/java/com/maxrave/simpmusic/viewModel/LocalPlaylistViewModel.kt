package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SetVideoIdEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.toListVideoId
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalPlaylistViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    private val application: Application
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
                val temp: ArrayList<SongEntity> = arrayListOf()
                var count = 0
                it.forEach { track ->
                    temp.add(track)
                    if (track.downloadState == DownloadState.STATE_DOWNLOADED) {
                        count++
                    }
                }
                _listTrack.postValue(temp)
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

    private var _listSetVideoId: MutableStateFlow<ArrayList<SetVideoIdEntity>?> = MutableStateFlow(null)
    val listSetVideoId: StateFlow<ArrayList<SetVideoIdEntity>?> = _listSetVideoId

    fun getSetVideoId(youtubePlaylistId: String) {
        viewModelScope.launch {
            mainRepository.getYouTubeSetVideoId(youtubePlaylistId).collect {
                _listSetVideoId.value = it
            }
        }
    }

    fun removeYouTubePlaylistItem(youtubePlaylistId: String, videoId: String) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylist.value?.id!!, LocalPlaylistEntity.YouTubeSyncState.Syncing)
            mainRepository.removeYouTubePlaylistItem(youtubePlaylistId, videoId).collect {
                if (it == 200) {
                    Toast.makeText(application, application.getString(R.string.removed_from_YouTube_playlist), Toast.LENGTH_SHORT).show()
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylist.value?.id!!, LocalPlaylistEntity.YouTubeSyncState.Synced)
                }
                else {
                    Toast.makeText(application, application.getString(R.string.error), Toast.LENGTH_SHORT).show()
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylist.value?.id!!, LocalPlaylistEntity.YouTubeSyncState.NotSynced)
                }
            }
        }
    }

    fun syncPlaylistWithYouTubePlaylist(playlist: LocalPlaylistEntity) {
        viewModelScope.launch {
            mainRepository.createYouTubePlaylist(playlist).collect {
                if (it != null) {
                    val ytId = "VL$it"
                    mainRepository.updateLocalPlaylistYouTubePlaylistId(playlist.id, ytId)
                    mainRepository.updateLocalPlaylistYouTubePlaylistSynced(playlist.id, 1)
                    mainRepository.getLocalPlaylistByYoutubePlaylistId(ytId).collect { yt ->
                        if (yt != null) {
                            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(yt.id, LocalPlaylistEntity.YouTubeSyncState.Synced)
                            mainRepository.getLocalPlaylist(playlist.id).collect {last ->
                                _listLocalPlaylist.postValue(last)
                                Toast.makeText(application, application.getString(R.string.synced), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                else {
                    Toast.makeText(application, application.getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun unsyncPlaylistWithYouTubePlaylist(playlist: LocalPlaylistEntity) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistId(playlist.id, null)
            mainRepository.updateLocalPlaylistYouTubePlaylistSynced(playlist.id, 0)
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(playlist.id, LocalPlaylistEntity.YouTubeSyncState.NotSynced)
            mainRepository.getLocalPlaylist(playlist.id).collect {last ->
                if (last.syncedWithYouTubePlaylist == 0) {
                    _listLocalPlaylist.postValue(last)
                    Toast.makeText(application, application.getString(R.string.unsynced), Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    fun updateYouTubePlaylistTitle(title: String, youtubePlaylistId: String) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylist.value?.id!!, LocalPlaylistEntity.YouTubeSyncState.Syncing)
            mainRepository.editYouTubePlaylist(title, youtubePlaylistId).collect { status ->
                if (status == 200) {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylist.value?.id!!, LocalPlaylistEntity.YouTubeSyncState.Synced)
                    Toast.makeText(application, application.getString(R.string.synced), Toast.LENGTH_SHORT).show()
                }
                else {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylist.value?.id!!, LocalPlaylistEntity.YouTubeSyncState.NotSynced)
                    Toast.makeText(application, application.getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateListTrackSynced(id: Long, list: List<String>, youtubeId: String) {
        viewModelScope.launch {
            mainRepository.getPlaylistData(youtubeId).collect {yt ->
                if (yt is Resource.Success) {
                    if (yt.data != null) {
                        val listTrack: ArrayList<String> = arrayListOf()
                        listTrack.addAll(list)
                        yt.data.tracks.forEach { track ->
                            if (!list.contains(track.videoId)) {
                                listTrack.add(track.videoId)
                            }
                        }
                        mainRepository.updateLocalPlaylistTracks(listTrack, id)
                        if (yt.data.tracks.size < list.size) {
                            list.forEach { track2 ->
                                if (!yt.data.tracks.toListVideoId().contains(track2)) {
                                    mainRepository.addYouTubePlaylistItem(youtubeId, track2).collect { status ->
                                        if (status == "STATUS_SUCCEEDED") {
                                            Toast.makeText(application, application.getString(R.string.added_to_youtube_playlist), Toast.LENGTH_SHORT).show()
                                        }
                                        else {
                                            Toast.makeText(application, application.getString(R.string.error), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                        Toast.makeText(application, application.getString(R.string.synced), Toast.LENGTH_SHORT).show()
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(id, LocalPlaylistEntity.YouTubeSyncState.Synced)
                        mainRepository.getLocalPlaylist(id).collect {last ->
                            _listLocalPlaylist.postValue(last)
                        }
                    }
                }
            }
        }
    }

}