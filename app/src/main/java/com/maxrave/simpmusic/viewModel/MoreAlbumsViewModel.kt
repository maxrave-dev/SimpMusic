package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.maxrave.kotlinytmusicscraper.models.AlbumItem
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@UnstableApi
class MoreAlbumsViewModel(
    application: Application,
) : BaseViewModel(application) {
    private val _uiState = MutableStateFlow<MoreAlbumsUIState>(MoreAlbumsUIState.Loading)
    val uiState: StateFlow<MoreAlbumsUIState> get() = _uiState

    fun getAlbumMore(id: String) {
        viewModelScope.launch {
            _uiState.value = MoreAlbumsUIState.Loading
            mainRepository.getAlbumMore(id, ALBUM_PARAM).collect { data ->
                val items =
                    (data?.items?.firstOrNull()?.items ?: emptyList()).mapNotNull { item ->
                        item as? AlbumItem
                    }
                if (items.isNotEmpty()) {
                    _uiState.value =
                        MoreAlbumsUIState.Success(
                            title = data?.title ?: "",
                            albumItems = items,
                        )
                } else {
                    _uiState.value =
                        MoreAlbumsUIState.Error(
                            message = getString(R.string.error),
                        )
                }
            }
        }
    }

    fun getSingleMore(id: String) {
        viewModelScope.launch {
            _uiState.value = MoreAlbumsUIState.Loading
            mainRepository.getAlbumMore(id, SINGLE_PARAM).collect { data ->
                val items =
                    (data?.items?.firstOrNull()?.items ?: emptyList()).mapNotNull { item ->
                        item as? AlbumItem
                    }
                if (items.isNotEmpty()) {
                    _uiState.value =
                        MoreAlbumsUIState.Success(
                            title = data?.title ?: "",
                            albumItems = items,
                        )
                } else {
                    _uiState.value =
                        MoreAlbumsUIState.Error(
                            message = getString(R.string.error),
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
        val albumItems: List<AlbumItem>,
    ) : MoreAlbumsUIState()

    data class Error(
        val message: String,
    ) : MoreAlbumsUIState()

    object Loading : MoreAlbumsUIState()
}