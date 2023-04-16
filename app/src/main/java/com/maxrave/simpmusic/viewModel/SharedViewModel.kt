package com.maxrave.simpmusic.viewModel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.maxrave.simpmusic.data.model.mediaService.Song
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject
    constructor(private val mainRepository: MainRepository, private val musicServiceHandler: SimpleMediaServiceHandler, savedStateHandle: SavedStateHandle): ViewModel(){


    private val _mediaItems = MutableLiveData<Resource<ArrayList<Song>>>()
    val mediaItems: LiveData<Resource<ArrayList<Song>>> = _mediaItems


}