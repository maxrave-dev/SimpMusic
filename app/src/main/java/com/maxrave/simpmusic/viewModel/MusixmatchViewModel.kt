package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@UnstableApi

class MusixmatchViewModel(
    application: Application,
) : BaseViewModel(application) {
    override val tag: String
        get() = "MusixmatchViewModel"

    var loading: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    fun login(
        email: String,
        password: String,
    ) {
        loading.value = true
        viewModelScope.launch {
            mainRepository.loginToMusixMatch(email, password).collect { data ->
                if (data != null) {
                    Log.w("MusixmatchFragment", data.toString())
                    if (data.message.body.firstOrNull()
                            ?.credential?.error == null &&
                        data.message.body.firstOrNull()
                            ?.credential?.account != null
                    ) {
                        mainRepository.getMusixmatchCookie()?.let { saveCookie(it) }
                    } else {
                        makeToast(data.message.body.firstOrNull()
                            ?.credential?.error
                            ?.description ?: getString(R.string.error))
                    }
                } else {
                    makeToast(getString(R.string.error))
                }
            }
        }
    }

    private fun saveCookie(cookie: String) {
        viewModelScope.launch {
            dataStoreManager.setMusixmatchCookie(cookie)
            dataStoreManager.setMusixmatchLoggedIn(true)
            withContext(Dispatchers.Main) {
                loading.value = false
            }
        }
    }
}