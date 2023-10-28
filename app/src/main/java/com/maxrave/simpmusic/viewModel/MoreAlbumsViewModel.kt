package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.kotlinytmusicscraper.pages.BrowseResult
import com.maxrave.simpmusic.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreAlbumsViewModel @Inject constructor(application: Application, private val mainRepository: MainRepository): AndroidViewModel(application) {
    private var _browseResult: MutableStateFlow<BrowseResult?> = MutableStateFlow(null)
    val browseResult: StateFlow<BrowseResult?> = _browseResult

    fun getAlbumMore(id: String) {
        viewModelScope.launch {
            _browseResult.value = null
            mainRepository.getAlbumMore(id, ALBUM_PARAM).collect { data ->
                _browseResult.value = data
            }
        }
    }
    fun getSingleMore(id: String) {
        viewModelScope.launch {
            _browseResult.value = null
            mainRepository.getAlbumMore(id, SINGLE_PARAM).collect { data ->
                _browseResult.value = data
            }
        }
    }

    companion object {
        const val ALBUM_PARAM = "ggMIegYIARoCAQI%3D"
        const val SINGLE_PARAM = "ggMIegYIAhoCAQI%3D"
    }
}