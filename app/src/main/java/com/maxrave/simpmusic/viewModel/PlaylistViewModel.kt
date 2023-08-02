package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.addThumbnails
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(private val mainRepository: MainRepository, application: Application, private var dataStoreManager: DataStoreManager): AndroidViewModel(application) {
    @Inject
    lateinit var downloadUtils: DownloadUtils

    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    var loading = MutableLiveData<Boolean>()

    private val _playlistBrowse: MutableLiveData<Resource<PlaylistBrowse>?> = MutableLiveData()
    var playlistBrowse: LiveData<Resource<PlaylistBrowse>?> = _playlistBrowse

    private val _id: MutableLiveData<String> = MutableLiveData()
    var id: LiveData<String> = _id

    private var _playlistEntity: MutableLiveData<PlaylistEntity> = MutableLiveData()
    var playlistEntity: LiveData<PlaylistEntity> = _playlistEntity

    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var liked: MutableStateFlow<Boolean> = _liked

    private var regionCode: String? = null
    private var language: String? = null
    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    fun updateId(id: String){
        _id.value = id
    }

    fun browsePlaylist(id: String) {
        loading.value = true
        viewModelScope.launch {
            mainRepository.browsePlaylist(id, regionCode!!, SUPPORTED_LANGUAGE.serverCodes[SUPPORTED_LANGUAGE.codes.indexOf(language!!)]).collect{ values ->
                _playlistBrowse.value = values
            }
            withContext(Dispatchers.Main){
                loading.value = false
            }
        }
    }

    fun insertPlaylist(playlistEntity: PlaylistEntity){
        viewModelScope.launch {
            mainRepository.insertPlaylist(playlistEntity)
            mainRepository.getPlaylist(playlistEntity.id).collect{ values ->
                _playlistEntity.value = values
                if (values != null) {
                    _liked.value = values.liked
                }
            }
        }
    }

    fun getPlaylist(id: String){
        viewModelScope.launch {
            mainRepository.getPlaylist(id).collect{ values ->
                if (values != null) {
                    _liked.value = values.liked
                }
                val list = values?.tracks
                var count = 0
                list?.forEach { track ->
                    mainRepository.getSongById(track).collect { song ->
                        if (song != null) {
                            if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                                count++
                            }
                        }
                    }
                }
                if (count == list?.size) {
                    updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADED)
                }
                else {
                    updatePlaylistDownloadState(id, DownloadState.STATE_NOT_DOWNLOADED)
                }
                mainRepository.getPlaylist(id).collect { playlist ->
                    _playlistEntity.value = playlist
                }
            }
        }
    }
    private var _listTrack: MutableLiveData<List<SongEntity>> = MutableLiveData()
    var listTrack: LiveData<List<SongEntity>> = _listTrack

    fun getListTrack(tracks: List<String>?) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(tracks!!).collect { values ->
                _listTrack.value = values
            }
        }
    }

    fun checkAllSongDownloaded(list: ArrayList<Track>) {
        viewModelScope.launch {
            var count = 0
            list.forEach { track ->
                mainRepository.getSongById(track.videoId).collect { song ->
                    if (song != null) {
                        if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                            count++
                        }
                    }
                }
            }
            if (count == list.size) {
                updatePlaylistDownloadState(id.value!!, DownloadState.STATE_DOWNLOADED)
            }
            mainRepository.getPlaylist(id.value!!).collect { album ->
                if (album != null) {
                    if (playlistEntity.value?.downloadState != album.downloadState) {
                        _playlistEntity.value = album
                    }
                }
            }
        }
    }


    fun updatePlaylistLiked(liked: Boolean, id: String){
        viewModelScope.launch {
            val tempLiked = if(liked) 1 else 0
            mainRepository.updatePlaylistLiked(id, tempLiked)
            mainRepository.getPlaylist(id).collect{ values ->
                _playlistEntity.value = values
                if (values != null) {
                    _liked.value = values.liked
                }
            }
        }
    }
    val playlistDownloadState: MutableStateFlow<Int> = MutableStateFlow(DownloadState.STATE_NOT_DOWNLOADED)


    private fun updateSongDownloadState(song: SongEntity, state: Int) {
        viewModelScope.launch {
            mainRepository.insertSong(song)
            mainRepository.getSongById(song.videoId).collect {
                mainRepository.updateDownloadState(song.videoId, state)
            }
        }
    }


    private fun updatePlaylistDownloadState(id: String, state: Int) {
        viewModelScope.launch {
            mainRepository.getPlaylist(id).collect { playlist ->
                _playlistEntity.value = playlist
                mainRepository.updatePlaylistDownloadState(id, state)
                playlistDownloadState.value = state
            }
        }
    }
    @UnstableApi
    fun getAllDownloadStateFromService(browseId: String) {
        var downloadState: StateFlow<List<Download?>>
        viewModelScope.launch {
            downloadState = downloadUtils.getAllDownloads().stateIn(viewModelScope)
            downloadState.collect { down ->
                if (down.isNotEmpty()){
                    var count = 0
                    down.forEach { downloadItem ->
                        if (downloadItem?.state == Download.STATE_COMPLETED) {
                            count++
                        }
                        else if (downloadItem?.state == Download.STATE_FAILED) {
                            updatePlaylistDownloadState(browseId, DownloadState.STATE_NOT_DOWNLOADED)
                        }
                    }
                    if (count == down.size) {
                        mainRepository.getPlaylist(browseId).collect{ playlist ->
                            Log.d("Check Playlist", playlist.toString())
                            mainRepository.getSongsByListVideoId(playlist?.tracks!!).collect{ tracks ->
                                tracks.forEach { track ->
                                    if (track.downloadState != DownloadState.STATE_DOWNLOADED) {
                                        mainRepository.updateDownloadState(track.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                                        Toast.makeText(getApplication(), "Download Failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        Log.d("Check Downloaded", "Downloaded")
                        updatePlaylistDownloadState(browseId, DownloadState.STATE_DOWNLOADED)
                        Toast.makeText(getApplication(), "Download Completed", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        updatePlaylistDownloadState(browseId, DownloadState.STATE_DOWNLOADING)
                    }
                }
                else {
                    updatePlaylistDownloadState(browseId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
        }
    }

    @UnstableApi
    fun getDownloadStateFromService(videoId: String) {
        viewModelScope.launch {
            val downloadState = downloadUtils.getDownload(videoId).stateIn(viewModelScope)
            downloadState.collect { down ->
                Log.d("Check Downloaded", "$videoId ${down?.state}")
                if (down != null) {
                    when (down.state) {
                        Download.STATE_COMPLETED -> {
                            mainRepository.getSongById(videoId).collect{ song ->
                                if (song?.downloadState != DownloadState.STATE_DOWNLOADED) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_DOWNLOADED)
                                }
                            }
                        }
                        Download.STATE_FAILED -> {
                            mainRepository.getSongById(videoId).collect{ song ->
                                if (song?.downloadState != DownloadState.STATE_NOT_DOWNLOADED) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_NOT_DOWNLOADED)
                                }
                            }
                        }
                        Download.STATE_DOWNLOADING -> {
                            mainRepository.getSongById(videoId).collect{ song ->
                                if (song != null) {
                                    if (song.downloadState != DownloadState.STATE_DOWNLOADING) {
                                        mainRepository.updateDownloadState(videoId, DownloadState.STATE_DOWNLOADING)
                                    }
                                }
                            }
                        }
                        Download.STATE_QUEUED -> {
                            mainRepository.getSongById(videoId).collect{ song ->
                                if (song?.downloadState != DownloadState.STATE_PREPARING) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_PREPARING)
                                }
                            }
                        }
                        else -> {
                            Log.d("Check Downloaded", "Not Downloaded")
                        }
                    }
                }
            }
        }
    }

    @UnstableApi
    fun downloadPlaylist(list: ArrayList<Track>, id: String) {
        list.forEach { track ->
            Log.d("Check Track Download", track.toString())
            val trackWithThumbnail = track.addThumbnails()
            updateSongDownloadState(trackWithThumbnail.toSongEntity(), DownloadState.STATE_PREPARING)
            updatePlaylistDownloadState(id, DownloadState.STATE_PREPARING)
        }
        list.forEach { track ->
            updatePlaylistDownloadState(id, DownloadState.STATE_DOWNLOADING)
            val downloadRequest = DownloadRequest.Builder(track.videoId, track.videoId.toUri())
                .setData(track.title.toByteArray())
                .setCustomCacheKey(track.videoId)
                .build()
            DownloadService.sendAddDownload(
                getApplication(),
                MusicDownloadService::class.java,
                downloadRequest,
                false
            )
            getDownloadStateFromService(track.videoId)
        }
        getAllDownloadStateFromService(id)
    }

    fun clearPlaylistBrowse() {
        _playlistBrowse.value = null
    }

    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }
}