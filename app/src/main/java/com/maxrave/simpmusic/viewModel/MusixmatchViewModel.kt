package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@UnstableApi
class MusixmatchViewModel(
    application: Application,
) : BaseViewModel(application) {
    fun login(
        userId: String,
        userToken: String,
    ) {
        viewModelScope.launch {
            runBlocking(Dispatchers.IO) {
                dataStoreManager.setMusixmatchUserToken(
                    userToken
                )
                val cookie =
                    "x-mxm-user-id=${userId.replace(":", "%3A")}; path=%2F; x-mxm-token-guid=${
                        userToken
                    }; mxm-encrypted-token="
                saveCookie(cookie)
            }
            makeToast(
                getString(
                    R.string.logged_in
                )
            )
        }
    }

    private suspend fun saveCookie(cookie: String) {
        dataStoreManager.setMusixmatchCookie(cookie)
        dataStoreManager.setMusixmatchLoggedIn(true)
    }
}