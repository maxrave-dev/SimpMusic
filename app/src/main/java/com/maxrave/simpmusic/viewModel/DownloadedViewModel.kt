package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadedViewModel @Inject constructor(application: Application, private val mainRepository: MainRepository): AndroidViewModel(application) {
    private var _listDownloadedSong: MutableLiveData<ArrayList<SongEntity>> = MutableLiveData()
    val listDownloadedSong: LiveData<ArrayList<SongEntity>> get() = _listDownloadedSong

    private var _songEntity: MutableLiveData<SongEntity> = MutableLiveData()
    val songEntity: LiveData<SongEntity> = _songEntity

    fun getListDownloadedSong() {
        viewModelScope.launch {
            mainRepository.getDownloadedSongs().collect{ downloadedSong ->
                _listDownloadedSong.value = downloadedSong as ArrayList<SongEntity>
            }
        }
    }
    fun getSongEntity(videoId: String) {
        viewModelScope.launch {
            mainRepository.getSongById(videoId).collect { values ->
                _songEntity.value = values
            }
        }
    }

    fun updateLikeStatus(videoId: String, likeStatus: Int) {
        viewModelScope.launch {
            mainRepository.updateLikeStatus(likeStatus = likeStatus, videoId = videoId)
        }
    }

    fun updateDownloadState(videoId: String, state: Int) {
        viewModelScope.launch {
            mainRepository.getSongById(videoId).collect { songEntity ->
                _songEntity.value = songEntity
            }
            mainRepository.updateDownloadState(videoId, state)
            getListDownloadedSong()
        }
    }
}