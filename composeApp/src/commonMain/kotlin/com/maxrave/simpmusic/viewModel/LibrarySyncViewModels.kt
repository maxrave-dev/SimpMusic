package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.librarysync.LibrarySyncException
import com.maxrave.domain.data.model.librarysync.LibrarySyncPairingState
import com.maxrave.domain.data.model.librarysync.LibrarySyncProblem
import com.maxrave.domain.data.model.librarysync.LibrarySyncState
import com.maxrave.domain.data.model.librarysync.PendingSettings
import com.maxrave.domain.data.model.loginsync.DesktopInfo
import com.maxrave.domain.data.model.loginsync.NetAddress
import com.maxrave.domain.repository.LibrarySyncHostRepository
import com.maxrave.domain.repository.LibrarySyncPhoneRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Desktop: the pairing QR, the paired phones, and settings a phone sent (#2663). */
class LibrarySyncHostViewModel(
    private val repository: LibrarySyncHostRepository,
) : ViewModel() {
    val state: StateFlow<LibrarySyncState> = repository.state
    val pairing: StateFlow<LibrarySyncPairingState> = repository.pairing
    val pendingSettings: StateFlow<PendingSettings?> = repository.pendingSettings

    /** The dialog opened: checks the Windows network profile, and shows the QR straight away when no phone is paired. */
    fun open() {
        viewModelScope.launch {
            repository.checkNetworkProfile()
        }
        if (!repository.state.value.isPaired) startPairing()
    }

    fun startPairing() {
        viewModelScope.launch { repository.startPairing() }
    }

    fun select(address: NetAddress) = repository.selectPairingAddress(address)

    /** Back to the list; the QR stops being valid. */
    fun stopPairing() {
        viewModelScope.launch { repository.stopPairing() }
    }

    fun removePhone(phoneId: String) {
        viewModelScope.launch { repository.removePhone(phoneId) }
    }

    fun applySettings(restart: () -> Unit) {
        viewModelScope.launch {
            repository.applyPendingSettings()
            restart()
        }
    }

    fun dismissSettings() {
        viewModelScope.launch { repository.dismissPendingSettings() }
    }

    // The dialog can close with the ViewModel: the QR must still be withdrawn.
    fun close() {
        viewModelScope.launch(NonCancellable) { repository.stopPairing() }
    }

    override fun onCleared() {
        close()
    }
}

/** Android: scans the computer's QR, pairs, then shows the pairing's status (#2663). */
class LibrarySyncPhoneViewModel(
    private val repository: LibrarySyncPhoneRepository,
) : ViewModel() {
    /** Only the unpaired side has steps; once paired the screen shows [state]. */
    sealed interface Step {
        /** [blocked]: denied with "don't ask again", so only the system settings can grant it now. */
        data class NeedsPermission(
            val blocked: Boolean,
        ) : Step

        data object NoCamera : Step

        data object Scanning : Step

        data object Reaching : Step

        data class Confirm(
            val desktop: DesktopInfo,
        ) : Step

        data object Pairing : Step

        /** The computer removed this phone; the user decides when to scan again. */
        data object Revoked : Step

        data class Failed(
            val problem: LibrarySyncProblem,
            val detail: String?,
        ) : Step
    }

    val state: StateFlow<LibrarySyncState> = repository.state
    val pendingSettings: StateFlow<PendingSettings?> = repository.pendingSettings

    private val _step = MutableStateFlow<Step>(Step.Scanning)
    val step: StateFlow<Step> = _step.asStateFlow()

    /** [hasPermissions]: camera (and Nearby devices on Android 17+) already granted. */
    fun open(hasPermissions: Boolean) {
        val current = repository.state.value
        _step.value =
            when {
                current.isPaired -> Step.Scanning
                current.problem == LibrarySyncProblem.REVOKED -> Step.Revoked
                else -> scanOrAsk(hasPermissions)
            }
        if (current.isPaired) syncNow()
    }

    fun scan(hasPermissions: Boolean) {
        _step.value = scanOrAsk(hasPermissions)
    }

    fun onPermission(
        granted: Boolean,
        blocked: Boolean,
    ) {
        _step.value = if (granted) Step.Scanning else Step.NeedsPermission(blocked)
    }

    fun onCameraUnavailable() {
        _step.value = Step.NoCamera
    }

    fun onScanned(text: String) {
        // Any other QR (a Login Sync one included) is ignored and the camera keeps looking.
        if (_step.value != Step.Scanning || !repository.isPairingCode(text)) return
        _step.value = Step.Reaching
        viewModelScope.launch {
            _step.value =
                try {
                    Step.Confirm(repository.reach(text))
                } catch (e: LibrarySyncException) {
                    Step.Failed(e.problem, e.detail)
                }
        }
    }

    fun pair() {
        if (_step.value !is Step.Confirm) return
        _step.value = Step.Pairing
        viewModelScope.launch {
            try {
                repository.pair()
                // The paired status takes over the screen from here.
                _step.value = Step.Scanning
            } catch (e: LibrarySyncException) {
                _step.value = Step.Failed(e.problem, e.detail)
            }
        }
    }

    fun cancelConfirm(hasPermissions: Boolean) {
        repository.cancelPairing()
        _step.value = scanOrAsk(hasPermissions)
    }

    fun syncNow() {
        viewModelScope.launch { repository.syncNow() }
    }

    fun unpair(hasPermissions: Boolean) {
        viewModelScope.launch {
            repository.unpairAll()
            _step.value = scanOrAsk(hasPermissions)
        }
    }

    fun applySettings(restart: () -> Unit) {
        viewModelScope.launch {
            repository.applyPendingSettings()
            restart()
        }
    }

    fun dismissSettings() {
        viewModelScope.launch { repository.dismissPendingSettings() }
    }

    fun close() = repository.cancelPairing()

    override fun onCleared() = close()

    private fun scanOrAsk(hasPermissions: Boolean): Step = if (hasPermissions) Step.Scanning else Step.NeedsPermission(blocked = false)
}
