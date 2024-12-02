package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.Resource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.inject
import java.time.LocalDateTime

@KoinViewModel
@UnstableApi
class AlbumViewModel(
    private val application: Application
): BaseViewModel(application) {

    private val downloadUtils: DownloadUtils by inject()

    override val tag: String = "AlbumViewModel"

    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    private var _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var loading: MutableStateFlow<Boolean> = _loading

    private var _albumBrowse: MutableStateFlow<Resource<AlbumBrowse>?> = MutableStateFlow(null)
    val albumBrowse: StateFlow<Resource<AlbumBrowse>?> = _albumBrowse

    private var _browseId: MutableStateFlow<String?> = MutableStateFlow(null)
    val browseId: StateFlow<String?> = _browseId

    private var _albumEntity: MutableStateFlow<AlbumEntity?> = MutableStateFlow(null)
    val albumEntity: StateFlow<AlbumEntity?> = _albumEntity

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
                Log.w("AlbumViewModel", "Browse Album $values")
                _albumBrowse.value = values
                withContext(Dispatchers.Main){
                    loading.value = false
                }
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
                    _albumEntity.value = album
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
    val listJob: MutableStateFlow<ArrayList<SongEntity>> = MutableStateFlow(arrayListOf())

    fun updatePlaylistDownloadState(id: String, state: Int) {
        viewModelScope.launch {
            mainRepository.getAlbum(id).collect { playlist ->
                _albumEntity.value = playlist
                mainRepository.updateAlbumDownloadState(id, state)
                albumDownloadState.value = state
            }
        }
    }
    fun updateDownloadState(videoId: String, state: Int) {
        viewModelScope.launch {
            mainRepository.updateDownloadState(videoId, state)
        }
    }

    @UnstableApi
    fun getDownloadStateFromService(videoId: String) {
    }

    private var _listTrack: MutableStateFlow<List<SongEntity>?> = MutableStateFlow(null)
    var listTrack: StateFlow<List<SongEntity>?> = _listTrack

    fun getListTrack(tracks: List<String>?) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(tracks!!).collect { values ->
                _listTrack.value = values
            }
        }
    }

    private var _listTrackForDownload: MutableStateFlow<List<SongEntity>?> = MutableStateFlow(null)
    var listTrackForDownload: StateFlow<List<SongEntity>?> = _listTrackForDownload

    fun getListTrackForDownload(tracks: List<String>?) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(tracks!!).collect { values ->
                _listTrackForDownload.value = values
            }
        }
    }

    fun clearAlbumBrowse() {
        _albumBrowse.value = null
        _albumEntity.value = null
    }

    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    fun insertSong(songEntity: SongEntity) {
        viewModelScope.launch {
            mainRepository.insertSong(songEntity).collect {
                println("Insert Song $it")
            }
        }
    }

    private var _songEntity: MutableLiveData<SongEntity?> = MutableLiveData(null)
    val songEntity: LiveData<SongEntity?> = _songEntity

    private var _listLocalPlaylist: MutableLiveData<List<LocalPlaylistEntity>> = MutableLiveData()
    val listLocalPlaylist: LiveData<List<LocalPlaylistEntity>> = _listLocalPlaylist

    fun getSongEntity(song: SongEntity) {
        viewModelScope.launch {
            mainRepository.insertSong(song).first().let {
                println("Insert song $it")
            }
            mainRepository.getSongById(song.videoId).collect { values ->
                _songEntity.value = values
            }
        }
    }
    fun updateLikeStatus(videoId: String, likeStatus: Int) {
        viewModelScope.launch {
            mainRepository.updateLikeStatus(likeStatus = likeStatus, videoId = videoId)
        }
    }
    fun getLocalPlaylist() {
        viewModelScope.launch {
            mainRepository.getAllLocalPlaylists().collect { values ->
                _listLocalPlaylist.postValue(values)
            }
        }
    }

    fun updateInLibrary(videoId: String) {
        viewModelScope.launch {
            mainRepository.updateSongInLibrary(LocalDateTime.now(), videoId)
        }
    }

    fun addToYouTubePlaylist(localPlaylistId: Long, youtubePlaylistId: String, videoId: String) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.Syncing)
            mainRepository.addYouTubePlaylistItem(youtubePlaylistId, videoId).collect { response ->
                if (response == "STATUS_SUCCEEDED") {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.Synced)
                    Toast.makeText(application, application.getString(R.string.added_to_youtube_playlist), Toast.LENGTH_SHORT).show()
                }
                else {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.NotSynced)
                    Toast.makeText(application, application.getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }

    fun updateLocalPlaylistTracks(list: List<String>, id: Long) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(list).collect { values ->
                var count = 0
                values.forEach { song ->
                    if (song.downloadState == DownloadState.STATE_DOWNLOADED){
                        count++
                    }
                }
                mainRepository.updateLocalPlaylistTracks(list, id)
                Toast.makeText(getApplication(), application.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
                if (count == values.size) {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_DOWNLOADED, id)
                }
                else {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_NOT_DOWNLOADED, id)
                }
            }
        }
    }

}