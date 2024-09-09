package com.maxrave.simpmusic.viewModel.base

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.repository.MainRepository
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseViewModel(
    private val application: Application,
) : AndroidViewModel(application),
    KoinComponent {
    protected val dataStoreManager: DataStoreManager by inject()
    protected val mainRepository: MainRepository by inject()

    /**
     * Tag for logging
     */
    abstract val tag: String

    /**
     * Cancel all jobs
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    protected fun makeToast(message: String?) {
        Toast.makeText(application, message ?: "NO MESSAGE", Toast.LENGTH_SHORT).show()
    }
}