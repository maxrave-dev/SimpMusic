package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.di.DownloadCache
import com.maxrave.simpmusic.di.PlayerCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private var dataStoreManager: DataStoreManager,
    private var mainRepository: MainRepository,
    @PlayerCache private val playerCache: SimpleCache,
    @DownloadCache private val downloadCache: SimpleCache
    ) : AndroidViewModel(application) {


    private var _location: MutableLiveData<String> = MutableLiveData()
    val location: LiveData<String> = _location
    fun getLocation() {
        viewModelScope.launch {
            dataStoreManager.location.collect { location ->
                _location.postValue(location)
            }
        }
    }

    fun changeLocation(location: String) {
        viewModelScope.launch {
            dataStoreManager.setLocation(location)
            getLocation()
        }
    }

    private val _cacheSize: MutableLiveData<Long> = MutableLiveData()
    var cacheSize: LiveData<Long> = _cacheSize

    @UnstableApi
    fun getPlayerCacheSize() {
        _cacheSize.value = playerCache.cacheSpace
    }

    @UnstableApi
    fun clearPlayerCache() {
        viewModelScope.launch {
            playerCache.keys.forEach { key ->
                playerCache.removeResource(key)
            }
            Toast.makeText(getApplication(), "Player cache cleared", Toast.LENGTH_SHORT).show()
            _cacheSize.value = playerCache.cacheSpace
        }
    }

    private val _downloadedCacheSize: MutableLiveData<Long> = MutableLiveData()
    var downloadedCacheSize: LiveData<Long> = _downloadedCacheSize

    @UnstableApi
    fun getDownloadedCacheSize() {
        _downloadedCacheSize.value = downloadCache.cacheSpace
    }

    @UnstableApi
    fun clearDownloadedCache() {
        viewModelScope.launch {
            playerCache.keys.forEach { key ->
                playerCache.removeResource(key)
            }
            mainRepository.getDownloadedSongs().collect {songs ->
                songs.forEach { song ->
                    mainRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
            Toast.makeText(getApplication(), "Player cache cleared", Toast.LENGTH_SHORT).show()
            _cacheSize.value = playerCache.cacheSpace
        }
    }
}