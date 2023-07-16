package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.model.browse.artist.ArtistBrowse
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(application: Application, private val mainRepository: MainRepository): AndroidViewModel(application){
    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    private val _artistBrowse: MutableLiveData<Resource<ArtistBrowse>> = MutableLiveData()
    var artistBrowse: LiveData<Resource<ArtistBrowse>> = _artistBrowse
    var loading = MutableLiveData<Boolean>()
    private var _artistEntity: MutableLiveData<ArtistEntity> = MutableLiveData()
    var artistEntity: LiveData<ArtistEntity> = _artistEntity
    private var _followed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var followed: StateFlow<Boolean> = _followed

    fun browseArtist(channelId: String){
        loading.value = true
        var job = viewModelScope.launch {
            mainRepository.browseArtist(channelId).collect { values ->
                _artistBrowse.value = values
            }
            withContext(Dispatchers.Main){
                loading.value = false
            }
        }
    }

    fun insertArtist(artist: ArtistEntity){
        viewModelScope.launch {
            mainRepository.insertArtist(artist)
            mainRepository.updateArtistInLibrary(LocalDateTime.now(), artist.channelId)
            mainRepository.getArtistById(artist.channelId).collect{
                _artistEntity.value = it
                _followed.value = it.followed
                Log.d("ArtistViewModel", "insertArtist: ${it.followed}")
            }
        }
    }

    fun updateFollowed(followed: Int, channelId: String){
        viewModelScope.launch {
            _followed.value = followed == 1
            mainRepository.updateFollowedStatus(channelId, followed)
            Log.d("ArtistViewModel", "updateFollowed: ${_followed.value}")
        }
    }

    override fun onCleared() {
    }

}