package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(private val mainRepository: MainRepository, application: Application): AndroidViewModel(application) {
    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    var loading = MutableLiveData<Boolean>()

    private val _playlistBrowse: MutableLiveData<Resource<PlaylistBrowse>> = MutableLiveData()
    var playlistBrowse: LiveData<Resource<PlaylistBrowse>> = _playlistBrowse

    private val _id: MutableLiveData<String> = MutableLiveData()
    var id: LiveData<String> = _id

    private var _playlistEntity: MutableLiveData<PlaylistEntity> = MutableLiveData()
    var playlistEntity: LiveData<PlaylistEntity> = _playlistEntity

    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var liked: MutableStateFlow<Boolean> = _liked

    fun updateId(id: String){
        _id.value = id
    }

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

    fun insertPlaylist(playlistEntity: PlaylistEntity){
        viewModelScope.launch {
            mainRepository.insertPlaylist(playlistEntity)
            mainRepository.getPlaylist(playlistEntity.id).collect{ values ->
                _playlistEntity.value = values
                _liked.value = values.liked
            }
        }
    }

    fun updatePlaylistLiked(liked: Boolean, id: String){
        viewModelScope.launch {
            val tempLiked = if(liked) 1 else 0
            mainRepository.updatePlaylistLiked(id, tempLiked)
            mainRepository.getPlaylist(id).collect{ values ->
                _playlistEntity.value = values
                _liked.value = values.liked
            }
        }
    }
}