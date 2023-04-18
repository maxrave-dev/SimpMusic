package com.maxrave.simpmusic.viewModel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import com.maxrave.simpmusic.data.model.mediaService.Song
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject
    constructor(private val mainRepository: MainRepository, private val musicServiceHandler: SimpleMediaServiceHandler, savedStateHandle: SavedStateHandle): ViewModel(){


    private val _mediaItems = MutableLiveData<Resource<ArrayList<Song>>>()
    val mediaItems: LiveData<Resource<ArrayList<Song>>> = _mediaItems

    private val _mediaSources = MutableLiveData<Resource<String>>()
    val mediaSources: LiveData<Resource<String>> = _mediaSources

    val playbackState = musicServiceHandler.simpleMediaState

    init {
        _mediaItems.postValue(Resource.Loading())
    }
    fun seekTo(position: Long) {

    }
    fun nextSong() {

    }
    @UnstableApi
    fun addMediaSource(mediaSource: MediaSource) {
        musicServiceHandler.addMediaSource(mediaSource)
    }
}