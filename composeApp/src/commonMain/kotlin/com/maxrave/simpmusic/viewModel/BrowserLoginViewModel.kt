package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.repository.BrowserCookies
import com.maxrave.domain.repository.BrowserLoginException
import com.maxrave.domain.repository.BrowserLoginFailure
import com.maxrave.domain.repository.BrowserLoginRepository
import com.maxrave.domain.repository.BrowserLoginTarget
import com.maxrave.domain.repository.BrowserProfile
import com.maxrave.domain.repository.BrowserProfileStatus
import com.maxrave.logger.LogLevel
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrowserLoginViewModel(
    private val repository: BrowserLoginRepository,
) : BaseViewModel() {
    private val _state = MutableStateFlow(BrowserLoginUiState())
    val state = _state.asStateFlow()
    private var operation: Job? = null

    fun refresh() = refresh(_state.value.target)

    fun refresh(target: BrowserLoginTarget) {
        if (_state.value.importingId != null || _state.value.completed && _state.value.target == target) return
        operation?.cancel()
        _state.value = _state.value.copy(
            loading = true,
            error = null,
            rejectedSession = false,
            failedProfileId = null,
            awaitingFileAccess = false,
            profiles = if (_state.value.target == target) _state.value.profiles else emptyList(),
            target = target,
            completed = false,
        )
        operation = viewModelScope.launch {
            try {
                val profiles = repository.profiles(target)
                ensureActive()
                _state.value = _state.value.copy(loading = false, profiles = profiles)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val site = error.stackTrace.firstOrNull { it.className.startsWith("com.maxrave.data.browserlogin.") }
                val location = site?.let { "${it.className}.${it.methodName}:${it.lineNumber}" } ?: "unknown"
                // Never pass the throwable/message: native file errors can contain private paths.
                log("Browser discovery failed: ${error::class.simpleName} at $location", LogLevel.WARN)
                _state.value = _state.value.copy(loading = false, error = BrowserLoginFailure.INVALID_DATA)
            }
        }
    }

    fun importProfile(profile: BrowserProfile, target: BrowserLoginTarget, addAccount: suspend (BrowserCookies) -> Boolean) {
        if (_state.value.target != target || _state.value.loading || _state.value.importingId != null || _state.value.completed) return
        _state.value = _state.value.copy(importingId = profile.id, error = null, rejectedSession = false, failedProfileId = null)
        operation = viewModelScope.launch {
            try {
                val cookies = repository.cookies(profile, target)
                ensureActive()
                // Keep credentials local to this call, never in StateFlow, SavedState or a log.
                val accepted = addAccount(cookies)
                ensureActive()
                _state.value = _state.value.copy(completed = accepted, rejectedSession = !accepted)
            } catch (error: CancellationException) {
                throw error
            } catch (error: BrowserLoginException) {
                _state.value = _state.value.copy(error = error.reason, failedProfileId = profile.id)
            } catch (_: Exception) {
                _state.value = _state.value.copy(error = BrowserLoginFailure.INVALID_DATA, failedProfileId = profile.id)
            } finally {
                _state.value = _state.value.copy(importingId = null)
            }
        }
    }

    fun onFileAccessRequested() {
        _state.value = _state.value.copy(awaitingFileAccess = true)
    }

    fun onResumeAfterFileAccess() {
        if (_state.value.awaitingFileAccess) refresh()
    }
}

data class BrowserLoginUiState(
    val loading: Boolean = true,
    val profiles: List<BrowserProfile> = emptyList(),
    val importingId: String? = null,
    val error: BrowserLoginFailure? = null,
    val rejectedSession: Boolean = false,
    val completed: Boolean = false,
    val failedProfileId: String? = null,
    val awaitingFileAccess: Boolean = false,
    val target: BrowserLoginTarget = BrowserLoginTarget.YOUTUBE,
)

internal fun BrowserLoginUiState.requiresFileAccess(profile: BrowserProfile, isMacOS: Boolean): Boolean =
    isMacOS && (profile.status == BrowserProfileStatus.PERMISSION_REQUIRED ||
        error == BrowserLoginFailure.FILE_ACCESS_DENIED && failedProfileId == profile.id)
