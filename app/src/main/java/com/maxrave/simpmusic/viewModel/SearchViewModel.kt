package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.google.gson.Gson
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.extension.toQueryList
import com.maxrave.simpmusic.extension.toSongEntity
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import retrofit2.Response
import java.util.Collections
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val mainRepository: MainRepository, application: Application, private var dataStoreManager: DataStoreManager) : AndroidViewModel(application) {
    @Inject
    lateinit var downloadUtils: DownloadUtils

    var searchType: MutableLiveData<String> = MutableLiveData("all")
    var searchAllResult: MutableLiveData<ArrayList<Any>> = MutableLiveData()

    var searchHistory: MutableLiveData<ArrayList<String>> = MutableLiveData()
    var searchResult: MutableLiveData<ArrayList<Any>> = MutableLiveData()

    private val _songSearchResult: MutableLiveData<Resource<ArrayList<SongsResult>>> = MutableLiveData()
    val songsSearchResult: LiveData<Resource<ArrayList<SongsResult>>> = _songSearchResult

    private val _artistSearchResult: MutableLiveData<Resource<ArrayList<ArtistsResult>>> = MutableLiveData()
    val artistsSearchResult: LiveData<Resource<ArrayList<ArtistsResult>>> = _artistSearchResult

    private val _albumSearchResult: MutableLiveData<Resource<ArrayList<AlbumsResult>>> = MutableLiveData()
    val albumsSearchResult: LiveData<Resource<ArrayList<AlbumsResult>>> = _albumSearchResult

    private val _playlistSearchResult: MutableLiveData<Resource<ArrayList<PlaylistsResult>>> = MutableLiveData()
    val playlistSearchResult: LiveData<Resource<ArrayList<PlaylistsResult>>> = _playlistSearchResult

    private val _videoSearchResult: MutableLiveData<Resource<ArrayList<VideosResult>>> = MutableLiveData()
    val videoSearchResult: LiveData<Resource<ArrayList<VideosResult>>> = _videoSearchResult

    var loading = MutableLiveData<Boolean>()

    private val _suggestQuery: MutableLiveData<Resource<ArrayList<String>>> = MutableLiveData()
    val suggestQuery : LiveData<Resource<ArrayList<String>>> = _suggestQuery

    var errorMessage = MutableLiveData<String>()

    private val _songEntity: MutableLiveData<SongEntity> = MutableLiveData()
    val songEntity: LiveData<SongEntity> = _songEntity

    var regionCode: String? = null

    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
    }

    fun getSearchHistory() {
        viewModelScope.launch {
            mainRepository.getSearchHistory().collect{ values ->
                if (values.isNotEmpty()) {
                    values.toQueryList().let { list ->
                        searchHistory.value = list
                    }
                }
            }
        }
    }

    fun insertSearchHistory(query: String) {
        viewModelScope.launch {
            mainRepository.insertSearchHistory(SearchHistory(query = query))
        }
    }

    fun deleteSearchHistory() {
        viewModelScope.launch {
            mainRepository.deleteSearchHistory()
        }
    }

    fun searchSongs(query: String) {
        if (loading.value == false){
            loading.value = true
            viewModelScope.launch {
                mainRepository.searchSongs(query, "songs", regionCode!!).collect {values ->
                    _songSearchResult.value = values
                    Log.d("SearchViewModel", "searchSongs: ${_songSearchResult.value}")
                    withContext(Dispatchers.Main) {
                        loading.value = false
                    }
                }
            }
        }
    }
    fun searchAll(query: String) {
        searchAllResult.value?.clear()
        loading.value = true
        val temp = ArrayList<Any>()
        viewModelScope.launch {
            val job1 = launch {
                mainRepository.searchSongs(query, "songs", regionCode!!).collect {values ->
                    _songSearchResult.value = values
                    Log.d("SearchViewModel", "searchSongs: ${_songSearchResult.value}")
                }
            }
            val job2 = launch {
                mainRepository.searchArtists(query, "artists", regionCode!!).collect {values ->
                    _artistSearchResult.value = values
                    Log.d("SearchViewModel", "searchArtists: ${_artistSearchResult.value}")
                }
            }
            val job3 = launch {
                mainRepository.searchAlbums(query, "albums", regionCode!!).collect {values ->
                    _albumSearchResult.value = values
                    Log.d("SearchViewModel", "searchAlbums: ${_albumSearchResult.value}")
                }
            }
            val job4 = launch {
                mainRepository.searchPlaylists(query, "playlists", regionCode!!).collect {values ->
                    _playlistSearchResult.value = values
                    Log.d("SearchViewModel", "searchPlaylists: ${_playlistSearchResult.value}")
                }
            }
            val job5 = launch {
                mainRepository.searchVideos(query, "videos", regionCode!!).collect {values ->
                    _videoSearchResult.value = values
                    Log.d("SearchViewModel", "searchVideos: ${_videoSearchResult.value}")
                }
            }
            job1.join()
            job2.join()
            job3.join()
            job4.join()
            job5.join()
            withContext(Dispatchers.Main) {
                loading.value = false
            }
        }
    }
    fun suggestQuery(query: String){
            viewModelScope.launch { mainRepository.suggestQuery(query).collect {values ->
                _suggestQuery.value = values
                Log.d("SearchViewModel", "suggestQuery: ${_suggestQuery.value}")
            }
        }
    }
    fun updateSearchHistory(searchHistoryList: ArrayList<String>) {
        searchHistory.postValue(searchHistoryList)
    }



    override fun onCleared() {
        super.onCleared()
    }
    private fun onError(message: String) {
        errorMessage.value = message
        loading.value = false
    }

    fun searchAlbums(query: String) {
        if (loading.value == false){
            loading.value = true
            viewModelScope.launch { mainRepository.searchAlbums(query, "albums", regionCode!!).collect {values ->
                _albumSearchResult.value = values
                Log.d("SearchViewModel", "searchAlbums: ${_albumSearchResult.value}")
                withContext(Dispatchers.Main) {
                    loading.value = false
                }
            } }
        }
    }

    fun searchArtists(query: String) {
        if (loading.value == false){
            loading.value = true
            viewModelScope.launch { mainRepository.searchArtists(query, "artists", regionCode!!).collect {values ->
                _artistSearchResult.value = values
                Log.d("SearchViewModel", "searchArtists: ${_artistSearchResult.value}")
                withContext(Dispatchers.Main) {
                    loading.value = false
                }
            } }
        }
    }

    fun searchPlaylists(query: String) {
        if (loading.value == false){
            loading.value = true
            viewModelScope.launch { mainRepository.searchPlaylists(query, "playlists", regionCode!!).collect {values ->
                _playlistSearchResult.value = values
                Log.d("SearchViewModel", "searchPlaylists: ${_playlistSearchResult.value}")
                withContext(Dispatchers.Main) {
                    loading.value = false
                }
            } }
        }
    }
    fun searchVideos(query: String) {
        if (loading.value == false) {
            loading.value = true
            viewModelScope.launch {
                mainRepository.searchVideos(query, "videos", regionCode!!).collect { values ->
                    _videoSearchResult.value = values
                    Log.d("SearchViewModel", "searchVideos: ${_videoSearchResult.value}")
                    withContext(Dispatchers.Main) {
                        loading.value = false
                    }
                }
            }
        }
    }

    fun updateLikeStatus(videoId: String, b: Boolean) {
        viewModelScope.launch {
            if (b){
                mainRepository.updateLikeStatus(videoId, 1)
            }
            else {
                mainRepository.updateLikeStatus(videoId, 0)
            }
        }
    }

    fun getSongEntity(track: Track) {
        viewModelScope.launch {
            mainRepository.insertSong(track.toSongEntity())
            mainRepository.getSongById(track.videoId).collect { values ->
                _songEntity.value = values
            }
        }
    }

    private var _listLocalPlaylist: MutableLiveData<List<LocalPlaylistEntity>> = MutableLiveData()
    val localPlaylist: LiveData<List<LocalPlaylistEntity>> = _listLocalPlaylist
    fun getAllLocalPlaylist() {
        viewModelScope.launch {
            mainRepository.getAllLocalPlaylists().collect { values ->
                _listLocalPlaylist.postValue(values)
            }
        }
    }

    fun updateDownloadState(videoId: String, state: Int) {
        viewModelScope.launch {
            mainRepository.getSongById(videoId).collect { songEntity ->
                _songEntity.value = songEntity
            }
            mainRepository.updateDownloadState(videoId, state)
        }
    }

    private var _downloadState: MutableStateFlow<Download?> = MutableStateFlow(null)
    var downloadState: StateFlow<Download?> = _downloadState.asStateFlow()

    @UnstableApi
    fun getDownloadStateFromService(videoId: String) {
        viewModelScope.launch {
            downloadState = downloadUtils.getDownload(videoId).stateIn(viewModelScope)
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
                    }
                }
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
                Toast.makeText(getApplication(), "Added to playlist", Toast.LENGTH_SHORT).show()
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