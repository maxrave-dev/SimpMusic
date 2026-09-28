package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.loginsync.LoginSyncHostState
import com.maxrave.domain.data.model.loginsync.NetAddress
import com.maxrave.domain.repository.LoginSyncHostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

// Counted from the QR appearing, and again from the phone connecting, so picking services on the
// phone never races the code's expiry.
private val SessionTimeout = 5.minutes

/** Desktop: shows the pairing QR and receives the phone's sign-ins. */
class LoginSyncHostViewModel(
    private val repository: LoginSyncHostRepository,
) : ViewModel() {
    val state: StateFlow<LoginSyncHostState> = repository.state

    private var session: Job? = null

    /** A fresh code — new key, new port. Also what "New code" does. */
    fun open() {
        session?.cancel()
        session =
            viewModelScope.launch {
                repository.start()
                // collectLatest restarts the timer on every change: a phone connecting, a new address.
                repository.state.collectLatest { state ->
                    when (state) {
                        is LoginSyncHostState.Waiting, LoginSyncHostState.Connected -> {
                            delay(SessionTimeout)
                            repository.expire()
                        }

                        is LoginSyncHostState.Done -> {
                            repository.stop()
                        }

                        else -> {}
                    }
                }
            }
    }

    fun select(address: NetAddress) = repository.select(address)

    fun close() {
        session?.cancel()
        session = null
        repository.stop()
    }

    override fun onCleared() {
        close()
    }
}
