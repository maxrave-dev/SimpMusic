package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class LogInViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    override val tag: String
        get() = "LogInViewModel"

    private val _status: MutableLiveData<Boolean> = MutableLiveData(false)
    var status: LiveData<Boolean> = _status

    private val _spotifyStatus: MutableLiveData<Boolean> = MutableLiveData(false)
    var spotifyStatus: LiveData<Boolean> = _spotifyStatus

    fun saveCookie(cookie: String) {
        viewModelScope.launch {
            Log.d("LogInViewModel", "saveCookie: $cookie")
            dataStoreManager.setCookie(cookie)
            dataStoreManager.setLoggedIn(true)
            _status.postValue(true)
        }
    }

    fun saveSpotifySpdc(cookie: String) {
        viewModelScope.launch {
            cookie
                .split("; ")
                .filter { it.isNotEmpty() }
                .associate {
                    val (key, value) = it.split("=")
                    key to value
                }.let {
                    dataStoreManager.setSpdc(it["sp_dc"] ?: "")
                    _spotifyStatus.postValue(true)
                }
        }
    }
}