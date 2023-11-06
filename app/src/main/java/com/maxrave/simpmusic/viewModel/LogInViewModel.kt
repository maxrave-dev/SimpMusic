package com.maxrave.simpmusic.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogInViewModel @Inject constructor(private val dataStore: DataStoreManager) : ViewModel() {
    private val _status: MutableLiveData<Boolean> = MutableLiveData(false)
    var status: LiveData<Boolean> = _status

    fun saveCookie(cookie: String) {
        viewModelScope.launch {
            Log.d("LogInViewModel", "saveCookie: $cookie")
            dataStore.setCookie(cookie)
            dataStore.setLoggedIn(true)
            YouTube.cookie = cookie
            _status.postValue(true)
        }
    }
}