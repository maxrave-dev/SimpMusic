package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.data.listentogether.ListenTogetherPrefs
import com.maxrave.domain.manager.DataStoreManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Listen Together preferences.
 *
 * Everything here is stored as a string through [DataStoreManager] because that is the only
 * generic accessor it exposes; booleans use the same "TRUE"/"FALSE" spelling the rest of the app
 * already writes, so the values stay readable if the store is ever inspected by hand.
 */
class ListenTogetherSettingsViewModel(
    private val dataStore: DataStoreManager,
) : ViewModel() {
    val serverUrl: StateFlow<String> =
        dataStore
            .getString(KEY_SERVER_URL)
            .map { it.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val usingCustomServer: StateFlow<Boolean> =
        dataStore
            .getString(KEY_SERVER_URL)
            .map { !it.isNullOrBlank() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val autoApproveJoins: StateFlow<Boolean> = boolFlow(KEY_AUTO_APPROVE_JOINS)
    val autoApproveSuggestions: StateFlow<Boolean> = boolFlow(KEY_AUTO_APPROVE_SUGGESTIONS)
    val followHostVolume: StateFlow<Boolean> = boolFlow(KEY_FOLLOW_HOST_VOLUME, default = true)

    val blockedNames: StateFlow<List<String>> =
        dataStore
            .getString(KEY_BLOCKLIST)
            .map { raw -> raw.orEmpty().split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Empty clears the override and falls back to [ListenTogetherClient.DEFAULT_SERVER_URL]. */
    fun setServerUrl(url: String) =
        viewModelScope.launch { dataStore.putString(KEY_SERVER_URL, url.trim()) }

    fun useDefaultServer() = viewModelScope.launch { dataStore.putString(KEY_SERVER_URL, "") }

    fun setAutoApproveJoins(value: Boolean) = putBool(KEY_AUTO_APPROVE_JOINS, value)

    fun setAutoApproveSuggestions(value: Boolean) = putBool(KEY_AUTO_APPROVE_SUGGESTIONS, value)

    fun setFollowHostVolume(value: Boolean) = putBool(KEY_FOLLOW_HOST_VOLUME, value)

    fun unblock(name: String) =
        viewModelScope.launch {
            val remaining = blockedNames.value.filterNot { it.equals(name, ignoreCase = true) }
            dataStore.putString(KEY_BLOCKLIST, remaining.joinToString(SEPARATOR))
        }

    private fun boolFlow(
        key: String,
        default: Boolean = false,
    ): StateFlow<Boolean> =
        dataStore
            .getString(key)
            .map { it?.equals(TRUE, ignoreCase = true) ?: default }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), default)

    private fun putBool(
        key: String,
        value: Boolean,
    ) = viewModelScope.launch { dataStore.putString(key, if (value) TRUE else FALSE) }

    companion object {
        // Single source of truth, shared with the DI module that builds the client — see
        // ListenTogetherPrefs for why these cannot be duplicated here.
        const val KEY_SERVER_URL = ListenTogetherPrefs.SERVER_URL
        const val KEY_AUTO_APPROVE_JOINS = ListenTogetherPrefs.AUTO_APPROVE_JOINS
        const val KEY_AUTO_APPROVE_SUGGESTIONS = ListenTogetherPrefs.AUTO_APPROVE_SUGGESTIONS
        const val KEY_FOLLOW_HOST_VOLUME = ListenTogetherPrefs.FOLLOW_HOST_VOLUME
        const val KEY_BLOCKLIST = ListenTogetherPrefs.BLOCKLIST

        private const val TRUE = ListenTogetherPrefs.TRUE
        private const val FALSE = ListenTogetherPrefs.FALSE
        private const val SEPARATOR = ListenTogetherPrefs.BLOCKLIST_SEPARATOR
    }
}
