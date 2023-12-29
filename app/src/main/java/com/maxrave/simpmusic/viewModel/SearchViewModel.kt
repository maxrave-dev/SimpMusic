package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.maxrave.kotlinytmusicscraper.models.SearchSuggestions
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
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
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val mainRepository: MainRepository, private val application: Application, private var dataStoreManager: DataStoreManager) : AndroidViewModel(application) {
    @Inject
    lateinit var downloadUtils: DownloadUtils

    var searchType: MutableLiveData<String> = MutableLiveData("all")
    var searchAllResult: MutableLiveData<ArrayList<Any>> = MutableLiveData()

    var searchHistory: MutableLiveData<ArrayList<String>> = MutableLiveData()
    var searchResult: MutableLiveData<ArrayList<Any>> = MutableLiveData()

    private var _songSearchResult: MutableLiveData<Resource<ArrayList<SongsResult>>> = MutableLiveData()
    val songsSearchResult: LiveData<Resource<ArrayList<SongsResult>>> = _songSearchResult

    private var _artistSearchResult: MutableLiveData<Resource<ArrayList<ArtistsResult>>> = MutableLiveData()
    val artistsSearchResult: LiveData<Resource<ArrayList<ArtistsResult>>> = _artistSearchResult

    private var _albumSearchResult: MutableLiveData<Resource<ArrayList<AlbumsResult>>> =
        MutableLiveData()
    val albumsSearchResult: LiveData<Resource<ArrayList<AlbumsResult>>> = _albumSearchResult

    private var _playlistSearchResult: MutableLiveData<Resource<ArrayList<PlaylistsResult>>> =
        MutableLiveData()
    val playlistSearchResult: LiveData<Resource<ArrayList<PlaylistsResult>>> = _playlistSearchResult

    private var _videoSearchResult: MutableLiveData<Resource<ArrayList<VideosResult>>> =
        MutableLiveData()
    val videoSearchResult: LiveData<Resource<ArrayList<VideosResult>>> = _videoSearchResult

    private var _featuredPlaylistSearchResult: MutableLiveData<Resource<ArrayList<PlaylistsResult>>> =
        MutableLiveData()
    val featuredPlaylistSearchResult: LiveData<Resource<ArrayList<PlaylistsResult>>> =
        _featuredPlaylistSearchResult

    private var _podcastSearchResult: MutableLiveData<Resource<ArrayList<PlaylistsResult>>> =
        MutableLiveData()
    val podcastSearchResult: LiveData<Resource<ArrayList<PlaylistsResult>>> = _podcastSearchResult

    var loading = MutableLiveData<Boolean>()

    private var _suggestQuery: MutableLiveData<Resource<SearchSuggestions>> = MutableLiveData()
    val suggestQuery: LiveData<Resource<SearchSuggestions>> = _suggestQuery

    var errorMessage = MutableLiveData<String>()

    private val _songEntity: MutableLiveData<SongEntity?> = MutableLiveData()
    val songEntity: LiveData<SongEntity?> = _songEntity

    var regionCode: String? = null
    var language: String? = null

    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
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
//                mainRepository.searchSongs(query, "songs", regionCode!!, SUPPORTED_LANGUAGE.serverCodes[SUPPORTED_LANGUAGE.codes.indexOf(language!!)]).collect { values ->
//                    _songSearchResult.value = values
//                    Log.d("SearchViewModel", "searchSongs: ${_songSearchResult.value}")
//                    withContext(Dispatchers.Main) {
//                        loading.value = false
//                    }
//                }
                mainRepository.getSearchDataSong(query).collect {
                    _songSearchResult.value = it
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
        viewModelScope.launch {
            val job1 = launch {
                mainRepository.getSearchDataSong(query).collect {values ->
                    _songSearchResult.value = values
                    Log.d("SearchViewModel", "searchSongs: ${_songSearchResult.value}")
                }
            }
            val job2 = launch {
                mainRepository.getSearchDataArtist(query).collect {values ->
                    _artistSearchResult.value = values
                    Log.d("SearchViewModel", "searchArtists: ${_artistSearchResult.value}")
                }
            }
            val job3 = launch {
                mainRepository.getSearchDataAlbum(query).collect {values ->
                    _albumSearchResult.value = values
                    Log.d("SearchViewModel", "searchAlbums: ${_albumSearchResult.value}")
                }
            }
            val job4 = launch {
                mainRepository.getSearchDataPlaylist(query).collect {values ->
                    _playlistSearchResult.value = values
                    Log.d("SearchViewModel", "searchPlaylists: ${_playlistSearchResult.value}")
                }
            }
            val job5 = launch {
                mainRepository.getSearchDataVideo(query).collect { values ->
                    _videoSearchResult.value = values
                    Log.d("SearchViewModel", "searchVideos: ${_videoSearchResult.value}")
                }
            }
            val job6 = launch {
                mainRepository.getSearchDataFeaturedPlaylist(query).collect { values ->
                    Log.d("SearchViewModel", "featured: $values")
                    _featuredPlaylistSearchResult.value = values
                }
            }
            val job7 = launch {
                mainRepository.getSearchDataPodcast(query).collect { values ->
                    Log.d("SearchViewModel", "podcast: ${values.data.toString()}")
                    _podcastSearchResult.value = values
                }
            }
            job1.join()
            job2.join()
            job3.join()
            job4.join()
            job5.join()
            job6.join()
            job7.join()
            withContext(Dispatchers.Main) {
                loading.value = false
            }
        }
    }
    fun suggestQuery(query: String){
            viewModelScope.launch {
                mainRepository.getSuggestQuery(query).collect{ values ->
                    Log.d("SearchViewModel", "suggestQuery: $values")
                    _suggestQuery.value = values
                }
        }
    }


    fun searchAlbums(query: String) {
        if (loading.value == false) {
            loading.value = true
            viewModelScope.launch {
                mainRepository.getSearchDataAlbum(query).collect { values ->
                    _albumSearchResult.value = values
                    Log.d("SearchViewModel", "searchAlbums: ${_albumSearchResult.value}")
                    withContext(Dispatchers.Main) {
                        loading.value = false
                    }
                }
            }
        }
    }

    fun searchFeaturedPlaylist(query: String) {
        if (loading.value == false) {
            loading.value = true
            viewModelScope.launch {
                mainRepository.getSearchDataFeaturedPlaylist(query).collect { values ->
                    _featuredPlaylistSearchResult.value = values
                    Log.d(
                        "SearchViewModel",
                        "searchFeaturedPlaylist: ${_featuredPlaylistSearchResult.value}"
                    )
                    withContext(Dispatchers.Main) {
                        loading.value = false
                    }
                }
            }
        }
    }

    fun searchPodcast(query: String) {
        if (loading.value == false) {
            loading.value = true
            viewModelScope.launch {
                mainRepository.getSearchDataPodcast(query).collect { values ->
                    _podcastSearchResult.value = values
                    Log.d("SearchViewModel", "searchPodcast: ${_podcastSearchResult.value}")
                    withContext(Dispatchers.Main) {
                        loading.value = false
                    }
                }
            }
        }
    }

    fun searchArtists(query: String) {
        if (loading.value == false) {
            loading.value = true
            viewModelScope.launch {
                mainRepository.getSearchDataArtist(query).collect { values ->
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
            viewModelScope.launch { mainRepository.getSearchDataPlaylist(query).collect {values ->
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
                mainRepository.getSearchDataVideo(query).collect { values ->
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
                Toast.makeText(getApplication(), application.getString (R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
                if (count == values.size) {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_DOWNLOADED, id)
                }
                else {
                    mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_NOT_DOWNLOADED, id)
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
                    Toast.makeText(getApplication(), application.getString(R.string.added_to_youtube_playlist), Toast.LENGTH_SHORT).show()
                }
                else {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.NotSynced)
                    Toast.makeText(getApplication(), application.getString(R.string.error), Toast.LENGTH_SHORT).show()
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