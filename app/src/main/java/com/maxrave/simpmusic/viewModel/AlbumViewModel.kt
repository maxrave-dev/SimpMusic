package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.searchResult.songs.Thumbnail
import com.maxrave.simpmusic.data.model.thumbnailUrl
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(private val mainRepository: MainRepository, application: Application): AndroidViewModel(application) {
    var gradientDrawable: MutableLiveData<GradientDrawable> = MutableLiveData()
    var loading = MutableLiveData<Boolean>()

    private val _albumBrowse: MutableLiveData<Resource<AlbumBrowse>> = MutableLiveData()
    var albumBrowse: LiveData<Resource<AlbumBrowse>> = _albumBrowse

    private val _browseId: MutableLiveData<String> = MutableLiveData()
    var browseId: LiveData<String> = _browseId

    private var _albumEntity: MutableLiveData<AlbumEntity> = MutableLiveData()
    var albumEntity: LiveData<AlbumEntity> = _albumEntity

    private var _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var liked: MutableStateFlow<Boolean> = _liked

    fun updateBrowseId(browseId: String){
        _browseId.value = browseId
    }
    fun browseAlbum(channelId: String){
        loading.value = true
        viewModelScope.launch {
            mainRepository.browseAlbum(channelId).collect{ values ->
                _albumBrowse.value = values
            }
            withContext(Dispatchers.Main){
                loading.value = false
            }
        }
    }

    fun insertAlbum(albumEntity: AlbumEntity){
        viewModelScope.launch {
            mainRepository.insertAlbum(albumEntity)
            mainRepository.getAlbum(albumEntity.browseId).collect{ values ->
                _albumEntity.value = values
                _liked.value = values.liked
            }
        }
    }

    fun updateAlbumLiked(liked: Boolean, browseId: String){
        viewModelScope.launch {
            val tempLiked = if(liked) 1 else 0
            mainRepository.updateAlbumLiked(browseId, tempLiked)
            mainRepository.getAlbum(browseId).collect{ values ->
                _albumEntity.value = values
                _liked.value = values.liked
            }
        }
    }
}