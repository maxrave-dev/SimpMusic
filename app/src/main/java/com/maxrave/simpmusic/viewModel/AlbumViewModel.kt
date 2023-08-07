package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail
import com.maxrave.simpmusic.data.model.thumbnailUrl
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.addThumbnails
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(private var dataStoreManager: DataStoreManager, private val mainRepository: MainRepository, application: Application): AndroidViewModel(application) {
    @Inject
    lateinit var downloadUtils: DownloadUtils

    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    var loading = MutableLiveData<Boolean>()

    private val _albumBrowse: MutableLiveData<Resource<AlbumBrowse>?> = MutableLiveData()
    var albumBrowse: LiveData<Resource<AlbumBrowse>?> = _albumBrowse

    private val _browseId: MutableLiveData<String> = MutableLiveData()
    var browseId: LiveData<String> = _browseId

    private var _albumEntity: MutableLiveData<AlbumEntity> = MutableLiveData()
    var albumEntity: LiveData<AlbumEntity> = _albumEntity

    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var liked: MutableStateFlow<Boolean> = _liked
    private var regionCode: String? = null
    private var language: String? = null

    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    fun updateBrowseId(browseId: String){
        _browseId.value = browseId
    }
    fun browseAlbum(browseId: String){
        loading.value = true
        viewModelScope.launch {
            mainRepository.getAlbumData(browseId).collect { values ->
                _albumBrowse.value = values
            }
            withContext(Dispatchers.Main){
                loading.value = false
            }
        }
    }

    fun insertAlbum(albumEntity: AlbumEntity){
        viewModelScope.launch {
            mainRepository.insertAlbum(albumEntity)
            mainRepository.getAlbum(albumEntity.browseId).collect{ values ->
                _liked.value = values.liked
                val list = values.tracks
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
                    updateAlbumDownloadState(albumEntity.browseId, DownloadState.STATE_DOWNLOADED)
                }
                else {
                    updateAlbumDownloadState(albumEntity.browseId, DownloadState.STATE_NOT_DOWNLOADED)
                }
                mainRepository.getAlbum(albumEntity.browseId).collect { album ->
                    _albumEntity.value = values
                }
            }
        }
    }

    fun getAlbum(browseId: String){
        viewModelScope.launch {
            mainRepository.getAlbum(browseId).collect{ values ->
                _liked.value = values.liked
                val list = values.tracks
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
                    updateAlbumDownloadState(browseId, DownloadState.STATE_DOWNLOADED)
                }
                else {
                    updateAlbumDownloadState(browseId, DownloadState.STATE_NOT_DOWNLOADED)
                }
                mainRepository.getAlbum(browseId).collect { album ->
                    _albumEntity.value = album
                }
            }
        }
    }

    fun updateAlbumLiked(liked: Boolean, browseId: String){
        viewModelScope.launch {
            val tempLiked = if(liked) 1 else 0
            mainRepository.updateAlbumLiked(browseId, tempLiked)
            mainRepository.getAlbum(browseId).collect{ values ->
                _albumEntity.value = values
                _liked.value = values.liked
            }
        }
    }
    val albumDownloadState: MutableStateFlow<Int> = MutableStateFlow(DownloadState.STATE_NOT_DOWNLOADED)

    private fun updateSongDownloadState(song: SongEntity, state: Int) {
        viewModelScope.launch {
            mainRepository.insertSong(song)
            mainRepository.getSongById(song.videoId).collect {
                mainRepository.updateDownloadState(song.videoId, state)
            }
        }
    }


    private fun updateAlbumDownloadState(browseId: String, state: Int) {
        viewModelScope.launch {
            mainRepository.getAlbum(browseId).collect { album ->
                _albumEntity.value = album
                mainRepository.updateAlbumDownloadState(browseId, state)
                albumDownloadState.value = state
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
                updateAlbumDownloadState(browseId.value!!, DownloadState.STATE_DOWNLOADED)
            }
            mainRepository.getAlbum(browseId.value!!).collect { album ->
                if (albumEntity.value?.downloadState != album.downloadState) {
                    _albumEntity.value = album
                }
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
                            updateAlbumDownloadState(browseId, DownloadState.STATE_NOT_DOWNLOADED)
                        }
                    }
                    if (count == down.size) {
                        mainRepository.getAlbum(browseId).collect{ album ->
                            mainRepository.getSongsByListVideoId(album.tracks!!).collect{ tracks ->
                                tracks.forEach { track ->
                                    if (track.downloadState != DownloadState.STATE_DOWNLOADED) {
                                        mainRepository.updateDownloadState(track.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                                        Toast.makeText(getApplication(), "Download Failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        updateAlbumDownloadState(browseId, DownloadState.STATE_DOWNLOADED)
                        Toast.makeText(getApplication(), "Download Completed", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        updateAlbumDownloadState(browseId, DownloadState.STATE_DOWNLOADING)
                    }
                }
                else {
                    updateAlbumDownloadState(browseId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
        }
    }

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
                                if (song?.downloadState != DownloadState.STATE_DOWNLOADING) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_DOWNLOADING)
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
    fun downloadAlbum(list: ArrayList<Track>, browseId: String) {
        list.forEach { track ->
            Log.d("Check Track Download", track.toString())
            val trackWithThumbnail = track.addThumbnails()
            updateSongDownloadState(trackWithThumbnail.toSongEntity(), DownloadState.STATE_PREPARING)
            updateAlbumDownloadState(browseId, DownloadState.STATE_PREPARING)
        }
        list.forEach { track ->
            updateAlbumDownloadState(browseId, DownloadState.STATE_DOWNLOADING)
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
        getAllDownloadStateFromService(browseId)
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

    fun clearAlbumBrowse() {
        _albumBrowse.value = null
    }

    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }
}