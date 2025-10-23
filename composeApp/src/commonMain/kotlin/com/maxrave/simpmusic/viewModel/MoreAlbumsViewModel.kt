package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.searchResult.albums.AlbumsResult
import com.maxrave.domain.repository.AlbumRepository
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.error

class MoreAlbumsViewModel(
    private val albumRepository: AlbumRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<MoreAlbumsUIState>(MoreAlbumsUIState.Loading)
    val uiState: StateFlow<MoreAlbumsUIState> get() = _uiState

    fun getAlbumMore(id: String) {
        viewModelScope.launch {
            _uiState.value = MoreAlbumsUIState.Loading
            albumRepository.getAlbumMore(id, ALBUM_PARAM).collect { data ->
                if (data != null && data.second.isNotEmpty()) {
                    _uiState.value =
                        MoreAlbumsUIState.Success(
                            title = data.first,
                            albumItems = data.second,
                        )
                } else {
                    _uiState.value =
                        MoreAlbumsUIState.Error(
                            message = getString(Res.string.error),
                        )
                }
            }
        }
    }

    fun getSingleMore(id: String) {
        viewModelScope.launch {
            _uiState.value = MoreAlbumsUIState.Loading
            albumRepository.getAlbumMore(id, SINGLE_PARAM).collect { data ->
                if (data != null && data.second.isNotEmpty()) {
                    _uiState.value =
                        MoreAlbumsUIState.Success(
                            title = data.first,
                            albumItems = data.second,
                        )
                } else {
                    _uiState.value =
                        MoreAlbumsUIState.Error(
                            message = getString(Res.string.error),
                        )
                }
            }
        }
    }

    companion object {
        const val ALBUM_PARAM = "ggMIegYIARoCAQI%3D"
        const val SINGLE_PARAM = "ggMIegYIAhoCAQI%3D"
    }
}

sealed class MoreAlbumsUIState {
    data class Success(
        val title: String,
        val albumItems: List<AlbumsResult>,
    ) : MoreAlbumsUIState()

    data class Error(
        val message: String,
    ) : MoreAlbumsUIState()

    object Loading : MoreAlbumsUIState()
}