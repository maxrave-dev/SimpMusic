package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MostPlayedViewModel @Inject constructor(private val mainRepository: MainRepository): ViewModel() {
    private var _listMostPlayedSong: MutableLiveData<ArrayList<SongEntity>> = MutableLiveData()
    val listMostPlayedSong: LiveData<ArrayList<SongEntity>> get() = _listMostPlayedSong

    fun getListLikedSong() {
        viewModelScope.launch {
            mainRepository.getMostPlayedSongs().collect{ most ->
                _listMostPlayedSong.value = most as ArrayList<SongEntity>
            }
        }
    }
}