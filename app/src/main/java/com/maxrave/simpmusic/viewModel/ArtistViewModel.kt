package com.maxrave.simpmusic.viewModel

import android.graphics.drawable.GradientDrawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.model.browse.artist.ArtistBrowse
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(private val mainRepository: MainRepository): ViewModel(){
    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    private val _artistBrowse: MutableLiveData<Resource<ArtistBrowse>> = MutableLiveData()
    var artistBrowse: LiveData<Resource<ArtistBrowse>> = _artistBrowse
    var loading = MutableLiveData<Boolean>()

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

}