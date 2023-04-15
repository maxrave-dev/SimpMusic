package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import retrofit2.Response
import java.util.Collections
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val mainRepository: MainRepository, application: Application) : AndroidViewModel(application) {
    var searchType: MutableLiveData<String> = MutableLiveData()
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

    var loading = MutableLiveData<Boolean>()

    private val _suggestQuery: MutableLiveData<Resource<ArrayList<String>>> = MutableLiveData()
    val suggestQuery : LiveData<Resource<ArrayList<String>>> = _suggestQuery

    var errorMessage = MutableLiveData<String>()


    fun searchSongs(query: String) {
//        songsSearchResult.value?.clear()
//        loading.value = true
//        var job = coroutineScope.launch {
//            val response = mainRepository.searchSongs(query)
//            withContext(Dispatchers.Main) {
//                if (response.isSuccessful) {
//                    songsSearchResult.value = response.body()
//                    Log.d("SearchViewModel", "searchSongs: ${songsSearchResult.value}")
//                    loading.value = false
//                }
//                else {
//                    onError("Error : ${response.message()} ")
//                    loading.value = false
//                }
//            }
//        }
        var job = viewModelScope.launch {
            mainRepository.searchSongs(query).collect {values ->
                _songSearchResult.value = values
                Log.d("SearchViewModel", "searchSongs: ${_songSearchResult.value}")
            }
        }
    }
    fun searchAll(query: String) {
        searchAllResult.value?.clear()
        loading.value = true
        val temp = ArrayList<Any>()
        val parentJob = viewModelScope.launch {
            val job1 = launch {
                mainRepository.searchSongs(query).collect {values ->
                    _songSearchResult.value = values
                    Log.d("SearchViewModel", "searchSongs: ${_songSearchResult.value}")
                }
            }
            val job2 = launch {
                mainRepository.searchArtists(query).collect {values ->
                    _artistSearchResult.value = values
                    Log.d("SearchViewModel", "searchArtists: ${_artistSearchResult.value}")
                }
            }
            val job3 = launch {
                mainRepository.searchAlbums(query).collect {values ->
                    _albumSearchResult.value = values
                    Log.d("SearchViewModel", "searchAlbums: ${_albumSearchResult.value}")
                }
            }
            val job4 = launch {
                mainRepository.searchPlaylists(query).collect {values ->
                    _playlistSearchResult.value = values
                    Log.d("SearchViewModel", "searchPlaylists: ${_playlistSearchResult.value}")
                }
            }
            job1.join()
            job2.join()
            job3.join()
            job4.join()
            withContext(Dispatchers.Main) {
                loading.value = false
            }
        }
    }
    fun suggestQuery(query: String){
        var job = viewModelScope.launch { mainRepository.suggestQuery(query).collect {values ->
            _suggestQuery.value = values
            Log.d("SearchViewModel", "suggestQuery: ${_suggestQuery.value}")
        } }
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
        var job = viewModelScope.launch { mainRepository.searchAlbums(query).collect {values ->
            _albumSearchResult.value = values
            Log.d("SearchViewModel", "searchAlbums: ${_albumSearchResult.value}")
        } }
    }

    fun searchArtists(query: String) {
        var job = viewModelScope.launch { mainRepository.searchArtists(query).collect {values ->
            _artistSearchResult.value = values
            Log.d("SearchViewModel", "searchArtists: ${_artistSearchResult.value}")
        } }
    }

    fun searchPlaylists(query: String) {
        var job = viewModelScope.launch { mainRepository.searchPlaylists(query).collect {values ->
            _playlistSearchResult.value = values
            Log.d("SearchViewModel", "searchPlaylists: ${_playlistSearchResult.value}")
        } }
    }
}