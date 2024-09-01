package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class FollowedViewModel(application: Application): BaseViewModel(application) {

    override val tag: String
        get() = "FollowedViewModel"

    private var _listFollowedArtist: MutableLiveData<ArrayList<ArtistEntity>> = MutableLiveData()
    val listFollowedArtist: LiveData<ArrayList<ArtistEntity>> get() = _listFollowedArtist

    fun getListLikedSong() {
        viewModelScope.launch {
            mainRepository.getFollowedArtists().collect{ likedSong ->
                _listFollowedArtist.value = likedSong as ArrayList<ArtistEntity>
            }
        }
    }
}