package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.model.podcast.PodcastBrowse
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    private val application: Application
) : AndroidViewModel(application) {
    var gradientDrawable: MutableLiveData<GradientDrawable?> = MutableLiveData()
    var loading = MutableLiveData<Boolean>()
    var id = MutableLiveData<String>()

    private val _podcastBrowse: MutableLiveData<Resource<PodcastBrowse>?> = MutableLiveData()
    val podcastBrowse: MutableLiveData<Resource<PodcastBrowse>?> = _podcastBrowse

    fun clearPodcastBrowse() {
        _podcastBrowse.value = null
        gradientDrawable.value = null
    }

    fun getPodcastBrowse(id: String) {
        loading.value = true
        viewModelScope.launch {
            mainRepository.getPodcastData(id).collect {
                _podcastBrowse.value = it
                withContext(Dispatchers.Main) {
                    loading.value = false
                }
            }
        }
    }
}