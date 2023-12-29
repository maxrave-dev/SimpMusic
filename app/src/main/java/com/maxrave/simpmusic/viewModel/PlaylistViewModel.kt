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
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    private val application: Application,
    private var dataStoreManager: DataStoreManager
): AndroidViewModel(application) {
    @Inject
    lateinit var downloadUtils: DownloadUtils

    var gradientDrawable: MutableLiveData<GradientDrawable?> = MutableLiveData()
    var loading = MutableLiveData<Boolean>()

    private val _playlistBrowse: MutableLiveData<Resource<PlaylistBrowse>?> = MutableLiveData()
    var playlistBrowse: LiveData<Resource<PlaylistBrowse>?> = _playlistBrowse

    private val _id: MutableLiveData<String> = MutableLiveData()
    var id: LiveData<String> = _id

    private val _isRadio: MutableLiveData<Boolean> = MutableLiveData()
    var isRadio: LiveData<Boolean> = _isRadio

    private var _playlistEntity: MutableLiveData<PlaylistEntity?> = MutableLiveData()
    var playlistEntity: LiveData<PlaylistEntity?> = _playlistEntity

    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var liked: MutableStateFlow<Boolean> = _liked


    private var _songEntity: MutableLiveData<SongEntity?> = MutableLiveData()
    val songEntity: LiveData<SongEntity?> = _songEntity
    private var _listLocalPlaylist: MutableLiveData<List<LocalPlaylistEntity>> = MutableLiveData()
    val listLocalPlaylist: LiveData<List<LocalPlaylistEntity>> = _listLocalPlaylist

    private var regionCode: String? = null
    private var language: String? = null

    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }

    fun updateId(id: String){
        _id.value = id
    }
    fun updateIsRadio(isRadio: Boolean) {
        _isRadio.value = isRadio
    }

    fun browsePlaylist(id: String) {
        loading.value = true
        viewModelScope.launch {
//            mainRepository.browsePlaylist(id, regionCode!!, SUPPORTED_LANGUAGE.serverCodes[SUPPORTED_LANGUAGE.codes.indexOf(language!!)]).collect{ values ->
//                _playlistBrowse.value = values
//            }
            mainRepository.getPlaylistData(id).collect {
                _playlistBrowse.value = it
            }
            withContext(Dispatchers.Main){
                loading.value = false
            }
        }
    }

    fun getRadio(radioId: String, videoId: String? = null, channelId: String? = null) {
        loading.value = true
        viewModelScope.launch {
            if (videoId != null) {
                mainRepository.getSongById(videoId).collectLatest { song ->
                    if (song != null) {
                        mainRepository.getRadio(radioId, song).collect {
                            _playlistBrowse.value = it
                        }
                        withContext(Dispatchers.Main) {
                            loading.value = false
                        }
                    }
                }
            }
            else if (channelId != null) {
                mainRepository.getArtistById(channelId).collectLatest { artist ->
                    mainRepository.getRadio(radioId = radioId, artist = artist).collect {
                        _playlistBrowse.value = it
                    }
                    withContext(Dispatchers.Main) {
                        loading.value = false
                    }
                }
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
    val listJob: MutableStateFlow<ArrayList<SongEntity>> = MutableStateFlow(arrayListOf())
    val playlistDownloadState: MutableStateFlow<Int> = MutableStateFlow(DownloadState.STATE_NOT_DOWNLOADED)


    fun updatePlaylistDownloadState(id: String, state: Int) {
        viewModelScope.launch {
            mainRepository.getPlaylist(id).collect { playlist ->
                _playlistEntity.value = playlist
                mainRepository.updatePlaylistDownloadState(id, state)
                playlistDownloadState.value = state
            }
        }
    }
//    fun downloading() {
//        _prevPlaylistDownloading.value = true
//    }
//    fun collectDownloadState(id: String) {
//        viewModelScope.launch {
//            listJob.collect { jobs->
//                    Log.w("PlaylistFragment", "ListJob: $jobs")
//                    if (jobs.isNotEmpty()){
//                        var count = 0
//                        jobs.forEach { job ->
//                            if (job.downloadState == DownloadState.STATE_DOWNLOADED) {
//                                count++
//                            }
//                            else if (job.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
//                                updatePlaylistDownloadState(id, DownloadState.STATE_NOT_DOWNLOADED)
//                                _prevPlaylistDownloading.value = false
//                            }
//                        }
//                        if (count == jobs.size) {
//                            updatePlaylistDownloadState(
//                                id,
//                                DownloadState.STATE_DOWNLOADED
//                            )
//                            _prevPlaylistDownloading.value = false
//                            Toast.makeText(
//                                context,
//                                context.getString(R.string.downloaded),
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    }
//            }
//        }
//    }

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
                                if (song?.downloadState != DownloadState.STATE_NOT_DOWNLOADED) {
                                    mainRepository.updateDownloadState(videoId, DownloadState.STATE_NOT_DOWNLOADED)
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

    fun clearPlaylistBrowse() {
        _playlistBrowse.value = null
        gradientDrawable.value = null
    }

    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
    }
    fun getSongEntity(song: SongEntity) {
        viewModelScope.launch {
            mainRepository.insertSong(song)
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
    fun updateDownloadState(videoId: String, state: Int) {
        viewModelScope.launch {
            mainRepository.updateDownloadState(videoId, state)
        }
    }

    fun insertSong(songEntity: SongEntity) {
        viewModelScope.launch {
            mainRepository.insertSong(songEntity)
        }
    }

    @UnstableApi
    fun downloadFullPlaylistState(id: String) {
        viewModelScope.launch {
            downloadUtils.downloads.collect { download ->
                playlistDownloadState.value =
                    if (listJob.value.all { download[it.videoId]?.state == Download.STATE_COMPLETED }) {
                        mainRepository.updatePlaylistDownloadState(
                            id,
                            DownloadState.STATE_DOWNLOADED
                        )
                        DownloadState.STATE_DOWNLOADED
                    } else if (listJob.value.all {
                            download[it.videoId]?.state == Download.STATE_QUEUED
                                    || download[it.videoId]?.state == Download.STATE_DOWNLOADING
                                    || download[it.videoId]?.state == Download.STATE_COMPLETED
                        }) {
                        mainRepository.updatePlaylistDownloadState(
                            id,
                            DownloadState.STATE_DOWNLOADING
                        )
                        DownloadState.STATE_DOWNLOADING
                    } else {
                        mainRepository.updatePlaylistDownloadState(id, DownloadState.STATE_NOT_DOWNLOADED)
                        DownloadState.STATE_NOT_DOWNLOADED
                    }
            }
        }
    }

    fun insertLocalPlaylist(localPlaylistEntity: LocalPlaylistEntity, listTrack: List<Track>) {
        viewModelScope.launch {
            mainRepository.insertLocalPlaylist(localPlaylistEntity)
            mainRepository.getLocalPlaylistByYoutubePlaylistId(localPlaylistEntity.youtubePlaylistId!!).collect { playlist ->
                if (playlist != null && playlist.youtubePlaylistId == localPlaylistEntity.youtubePlaylistId) {
                    for (track in listTrack) {
                        mainRepository.insertSong(track.toSongEntity())
                    }
                }
            }
            Toast.makeText(application, application.getString(R.string.added_local_playlist), Toast.LENGTH_SHORT).show()
        }
    }

    private var _localPlaylistIfYouTubePlaylist: MutableStateFlow<LocalPlaylistEntity?> = MutableStateFlow(null)
    var localPlaylistIfYouTubePlaylist: MutableStateFlow<LocalPlaylistEntity?> = _localPlaylistIfYouTubePlaylist

    fun checkSyncedPlaylist(value: String?) {
        viewModelScope.launch {
            if (value != null) {
                mainRepository.getLocalPlaylistByYoutubePlaylistId(value).collect {
                    _localPlaylistIfYouTubePlaylist.value = it
                }
            }
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

    fun insertRadioPlaylist(playlistEntity: PlaylistEntity) {
        viewModelScope.launch {
            mainRepository.insertRadioPlaylist(playlistEntity)
            mainRepository.getPlaylist(playlistEntity.id).collect{ values ->
                _playlistEntity.value = values
                if (values != null) {
                    _liked.value = values.liked
                }
            }
        }
    }

    fun updateInLibrary(videoId: String) {
        viewModelScope.launch {
            mainRepository.updateSongInLibrary(LocalDateTime.now(), videoId)
        }

    }

    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }
}