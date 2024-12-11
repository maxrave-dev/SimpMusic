package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.musixmatch.MusixmatchCredential
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class MusixmatchViewModel(
    application: Application,
) : BaseViewModel(application) {
    override val tag: String
        get() = "MusixmatchViewModel"

    var loading: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    private var _data: MutableStateFlow<MusixmatchCredential?> = MutableStateFlow(null)
    val data: MutableStateFlow<MusixmatchCredential?> = _data

    fun login(
        email: String,
        password: String,
    ) {
        loading.value = true
        viewModelScope.launch {
            mainRepository.loginToMusixMatch(email, password).collect {
                _data.value = it
            }
        }
    }

    fun saveCookie(cookie: String) {
        viewModelScope.launch {
            dataStoreManager.setMusixmatchCookie(cookie)
            dataStoreManager.setMusixmatchLoggedIn(true)
            YouTube.musixMatchCookie = cookie
            withContext(Dispatchers.Main) {
                loading.value = false
            }
        }
    }
}