package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@UnstableApi
class LogInViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    private val _spotifyStatus: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val spotifyStatus: StateFlow<Boolean> get() = _spotifyStatus

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
                    _spotifyStatus.value = true
                }
        }
    }

    fun setVisitorData(visitorData: String) {
        viewModelScope.launch {
            dataStoreManager.setVisitorData(visitorData)
        }
    }

    fun setDataSyncId(dataSyncId: String) {
        viewModelScope.launch {
            dataStoreManager.setDataSyncId(dataSyncId)
        }
    }
}