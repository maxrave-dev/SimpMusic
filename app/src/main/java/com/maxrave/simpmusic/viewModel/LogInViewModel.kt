package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.launch

class LogInViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    override val tag: String
        get() = "LogInViewModel"

    private val _spotifyStatus: MutableLiveData<Boolean> = MutableLiveData(false)
    var spotifyStatus: LiveData<Boolean> = _spotifyStatus

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