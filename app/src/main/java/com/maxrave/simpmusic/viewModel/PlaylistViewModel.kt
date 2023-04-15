package com.maxrave.simpmusic.viewModel

import android.graphics.drawable.GradientDrawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(private val mainRepository: MainRepository): ViewModel() {
    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    var loading = MutableLiveData<Boolean>()

    private val _playlistBrowse: MutableLiveData<Resource<PlaylistBrowse>> = MutableLiveData()
    var playlistBrowse: LiveData<Resource<PlaylistBrowse>> = _playlistBrowse


    fun browsePlaylist(id: String) {
        loading.value = true
        var job = viewModelScope.launch {
            mainRepository.browsePlaylist(id).collect{values ->
                _playlistBrowse.value = values
            }
            withContext(Dispatchers.Main){
                loading.value = false
            }
        }
    }
}