package com.maxrave.simpmusic.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.simpmusic.data.db.entities.NotificationEntity
import com.maxrave.simpmusic.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel
    @Inject
    constructor(
        private val application: Application,
        mainRepository: MainRepository,
    ) : AndroidViewModel(application) {
        private var _listNotification: MutableStateFlow<List<NotificationEntity>?> =
            MutableStateFlow(null)
        val listNotification: StateFlow<List<NotificationEntity>?> = _listNotification

        init {
            viewModelScope.launch {
                mainRepository.getAllNotifications().collect { notificationEntities ->
                    _listNotification.value = notificationEntities?.sortedByDescending {
                        it.time
                    }
                }
            }
        }
    }
