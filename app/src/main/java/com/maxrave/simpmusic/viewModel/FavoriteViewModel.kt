package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(application: Application, private val mainRepository: MainRepository): AndroidViewModel(application) {
    private var _listLikedSong: MutableLiveData<ArrayList<SongEntity>> = MutableLiveData()
    val listLikedSong: LiveData<ArrayList<SongEntity>> get() = _listLikedSong

    fun getListLikedSong() {
        viewModelScope.launch {
            mainRepository.getLikedSongs().collect{ likedSong ->
                _listLikedSong.value = likedSong as ArrayList<SongEntity>
            }
        }
    }
    fun updateLikeStatus(videoId: String, likeStatus: Int) {
        viewModelScope.launch {
            mainRepository.updateLikeStatus(likeStatus = likeStatus, videoId = videoId)
        }
    }
}