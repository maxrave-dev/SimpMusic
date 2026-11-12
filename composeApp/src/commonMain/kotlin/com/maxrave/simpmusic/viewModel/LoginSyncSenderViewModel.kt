package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.librarysync.LibrarySyncException
import com.maxrave.domain.data.model.librarysync.LibrarySyncProblem
import com.maxrave.domain.data.model.loginsync.DesktopInfo
import com.maxrave.domain.data.model.loginsync.LoginSyncException
import com.maxrave.domain.data.model.loginsync.LoginSyncService
import com.maxrave.domain.repository.LibrarySyncPhoneRepository
import com.maxrave.domain.repository.LoginSyncSenderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Android: scans the Desktop's QR, asks the user to trust it, sends the chosen sign-ins. Paired for
 * library sync, it skips the QR and sends over the paired channel, where the computer's user decides.
 */
class LoginSyncSenderViewModel(
    private val repository: LoginSyncSenderRepository,
    private val librarySync: LibrarySyncPhoneRepository,
) : ViewModel() {
    sealed interface Step {
        data object Loading : Step

        data object NothingSignedIn : Step

        /** [blocked]: denied with "don't ask again", so only the system settings can grant it now. */
        data class NeedsCamera(
            val blocked: Boolean,
        ) : Step

        data object NoCamera : Step

        data object Scanning : Step

        data object Connecting : Step

        data class Trust(
            val desktop: DesktopInfo,
        ) : Step

        data class Choose(
            val desktop: DesktopInfo,
        ) : Step

        data object Sending : Step

        /** Paired: the computer is asking its user (D4). */
        data class Waiting(
            val desktop: DesktopInfo,
        ) : Step

        /** Paired: the computer's user ignored the sign-ins, or did not answer in time. */
        data class Declined(
            val desktop: DesktopInfo,
        ) : Step

        data class Done(
            val desktop: DesktopInfo,
            val services: List<LoginSyncService>,
        ) : Step

        data class Failed(
            val reason: LoginSyncException.Reason,
            val detail: String?,
        ) : Step
    }

    data class UiState(
        val step: Step = Step.Loading,
        val signedIn: List<LoginSyncService> = emptyList(),
        val selected: Set<LoginSyncService> = emptySet(),
        /** The computer this phone is paired with for library sync, if any. */
        val pairedTo: DesktopInfo? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private fun step(step: Step) = _uiState.update { it.copy(step = step) }

    /** The sheet opened. [hasCamera]: the camera permission is already granted. */
    fun open(hasCamera: Boolean) {
        val pairedTo = librarySync.state.value.peers.firstOrNull()?.let { DesktopInfo(it.name, it.os) }
        _uiState.value = UiState(pairedTo = pairedTo)
        viewModelScope.launch {
            val signedIn = repository.signedInServices()
            _uiState.value =
                UiState(
                    pairedTo = pairedTo,
                    step =
                        when {
                            signedIn.isEmpty() -> Step.NothingSignedIn
                            pairedTo != null -> Step.Choose(pairedTo)
                            hasCamera -> Step.Scanning
                            else -> Step.NeedsCamera(blocked = false)
                        },
                    signedIn = signedIn,
                    selected = signedIn.toSet(),
                )
        }
    }

    fun onCameraPermission(
        granted: Boolean,
        blocked: Boolean,
    ) = step(if (granted) Step.Scanning else Step.NeedsCamera(blocked))

    fun onCameraUnavailable() = step(Step.NoCamera)

    fun onScanned(text: String) {
        // Any other QR is ignored and the camera keeps looking.
        if (_uiState.value.step != Step.Scanning || !repository.isInvite(text)) return
        step(Step.Connecting)
        viewModelScope.launch {
            step(
                try {
                    Step.Trust(repository.connect(text))
                } catch (e: LoginSyncException) {
                    Step.Failed(e.reason, e.detail)
                },
            )
        }
    }

    fun trust() {
        val trust = _uiState.value.step as? Step.Trust ?: return
        step(Step.Choose(trust.desktop))
    }

    fun toggle(service: LoginSyncService) =
        _uiState.update { it.copy(selected = if (service in it.selected) it.selected - service else it.selected + service) }

    fun send() {
        val choose = _uiState.value.step as? Step.Choose ?: return
        val services = _uiState.value.selected
        if (services.isEmpty()) return
        if (_uiState.value.pairedTo != null) return sendPaired(choose.desktop, services)
        step(Step.Sending)
        viewModelScope.launch {
            step(
                try {
                    Step.Done(choose.desktop, repository.send(services))
                } catch (e: LoginSyncException) {
                    Step.Failed(e.reason, e.detail)
                },
            )
        }
    }

    private fun sendPaired(
        desktop: DesktopInfo,
        services: Set<LoginSyncService>,
    ) {
        step(Step.Waiting(desktop))
        viewModelScope.launch {
            step(
                try {
                    val taken = librarySync.sendLogins(services)
                    if (taken.isEmpty()) Step.Declined(desktop) else Step.Done(desktop, taken)
                } catch (e: LibrarySyncException) {
                    val reason = if (e.problem == LibrarySyncProblem.UNREACHABLE) LoginSyncException.Reason.UNREACHABLE else LoginSyncException.Reason.FAILED
                    Step.Failed(reason, e.detail)
                },
            )
        }
    }

    fun retry(hasCamera: Boolean) {
        repository.disconnect()
        val pairedTo = _uiState.value.pairedTo
        step(
            when {
                pairedTo != null -> Step.Choose(pairedTo)
                hasCamera -> Step.Scanning
                else -> Step.NeedsCamera(blocked = false)
            },
        )
    }

    /** The sheet is gone. A computer still waiting on this phone is put back on its QR. */
    fun close() = repository.disconnect()

    override fun onCleared() = close()
}
