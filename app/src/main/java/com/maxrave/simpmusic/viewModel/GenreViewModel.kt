package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.model.explore.mood.genre.GenreObject
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GenreViewModel @Inject constructor(
    application: Application,
    private val mainRepository: MainRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val _genreObject: MutableStateFlow<GenreObject?> = MutableStateFlow(null)
    var genreObject: StateFlow<GenreObject?> = _genreObject

    var loading = MutableStateFlow(false)

    init {
        savedStateHandle.get<String>("params")?.let { params ->
            getGenre(params)
        }
    }

    private fun getGenre(params: String) {
        loading.value = true
        viewModelScope.launch {
            mainRepository.getGenreData(params).collect { values ->
                when (values) {
                    is Resource.Success -> {
                        _genreObject.value = values.data
                    }

                    is Resource.Error -> {
                        _genreObject.value = null
                    }
                }
            }
            withContext(Dispatchers.Main) {
                loading.value = false
            }
        }
    }
}
