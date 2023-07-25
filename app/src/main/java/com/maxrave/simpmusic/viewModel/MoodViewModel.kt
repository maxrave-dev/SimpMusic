package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.MoodsMomentObject
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MoodViewModel @Inject constructor(private val mainRepository: MainRepository, application: Application, private var dataStoreManager: DataStoreManager) : AndroidViewModel(application) {
    private val _moodsMomentObject: MutableLiveData<Resource<MoodsMomentObject>> = MutableLiveData()
    var moodsMomentObject: LiveData<Resource<MoodsMomentObject>> = _moodsMomentObject
    var loading = MutableLiveData<Boolean>()

    private var regionCode: String? = null
    init {
        regionCode = runBlocking { dataStoreManager.location.first() }
    }

    fun getMood(params: String){
        loading.value = true
        viewModelScope.launch {
            mainRepository.getMood(params, regionCode!!).collect{values ->
                _moodsMomentObject.value = values
            }
            withContext(Dispatchers.Main){
                loading.value = false
            }
        }
    }
}