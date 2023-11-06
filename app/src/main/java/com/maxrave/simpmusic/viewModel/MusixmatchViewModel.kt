package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.musixmatch.MusixmatchCredential
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MusixmatchViewModel @Inject constructor(private val dataStore: DataStoreManager, private val mainRepository: MainRepository) : ViewModel() {
//    private val _status: MutableLiveData<Boolean> = MutableLiveData(false)
//    var status: LiveData<Boolean> = _status
//
//    fun saveCookie(cookie: String) {
//        viewModelScope.launch {
//            Log.d("LogInViewModel", "saveCookie: $cookie")
//            dataStore.setCookie(cookie)
//            dataStore.setLoggedIn(true)
//            YouTube.cookie = cookie
//            _status.postValue(true)
//        }
//    }
    var loading: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private var _data: MutableStateFlow<MusixmatchCredential?> = MutableStateFlow(null)
    val data: MutableStateFlow<MusixmatchCredential?> = _data
    fun loggin(email: String, password: String) {
        loading.value = true
        viewModelScope.launch {
            mainRepository.loginToMusixMatch(email, password).collect {
                _data.value = it
            }
        }
    }
    fun saveCookie(cookie: String) {
        viewModelScope.launch {
            dataStore.setMusixmatchCookie(cookie)
            dataStore.setMusixmatchLoggedIn(true)
            YouTube.musixMatchCookie = cookie
            withContext(Dispatchers.Main) {
                loading.value = false
            }
        }
    }
}