package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.simpmusic.lastfm.authorizeUrl
import org.simpmusic.lastfm.completeLogin

class LogInViewModel(
    private val dataStoreManager: DataStoreManager,
) : BaseViewModel() {
    private val _spotifyStatus: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val spotifyStatus: StateFlow<Boolean> get() = _spotifyStatus

    private val _fullSpotifyCookies: MutableStateFlow<List<Pair<String, String?>>> = MutableStateFlow(emptyList())
    val fullSpotifyCookies: StateFlow<List<Pair<String, String?>>> get() = _fullSpotifyCookies.asStateFlow()

    private val _fullYouTubeCookies: MutableStateFlow<List<Pair<String, String?>>> = MutableStateFlow(emptyList())
    val fullYouTubeCookies: StateFlow<List<Pair<String, String?>>> get() = _fullYouTubeCookies.asStateFlow()

    fun saveSpotifySpdc(cookie: String) {
        viewModelScope.launch {
            cookie
                .split("; ")
                .filter { it.isNotEmpty() }
                .associate {
                    val (key, value) = it.split("=")
                    key to value
                }.let {
                    dataStoreManager.setSpdc(it["sp_dc"] ?: "")
                    _spotifyStatus.value = true
                }
        }
    }

    fun setVisitorData(visitorData: String) {
        viewModelScope.launch {
            dataStoreManager.setVisitorData(visitorData)
        }
    }

    fun setDataSyncId(dataSyncId: String) {
        viewModelScope.launch {
            dataStoreManager.setDataSyncId(dataSyncId)
        }
    }

    fun setFullSpotifyCookies(cookies: List<Pair<String, String?>>) {
        viewModelScope.launch {
            _fullSpotifyCookies.value = cookies
        }
    }

    fun setFullYouTubeCookies(cookies: List<Pair<String, String?>>) {
        viewModelScope.launch {
            _fullYouTubeCookies.value = cookies
        }
    }

    fun saveDiscordToken(token: String) {
        viewModelScope.launch {
            dataStoreManager.setDiscordToken(token)
        }
    }

    private val _lastfmState: MutableStateFlow<LastfmLoginState> = MutableStateFlow(LastfmLoginState.Idle)
    val lastfmState: StateFlow<LastfmLoginState> get() = _lastfmState.asStateFlow()

    /**
     * True once a session key is stored, whoever put it there.
     *
     * The callback can be handled outside this screen — [SharedViewModel.completeLastfmLogin] takes
     * it when the browser returns — so the screen watches the stored result instead of only its own
     * state. That is what lets it close itself in both paths: the redirect, and the pasted link.
     */
    val lastfmLoggedIn: StateFlow<Boolean> =
        dataStoreManager.lastfmSessionKey
            .map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Sends the user to Last.fm to approve access.
     *
     * Nothing is fetched first: in the web flow Last.fm mints the token itself and hands it back on
     * the callback, so the app has no token to hold on to until the user returns.
     */
    fun startLastfmLogin() {
        val url = authorizeUrl()
        _lastfmState.value =
            if (url != null) {
                LastfmLoginState.AwaitingApproval(authorizeUrl = url)
            } else {
                LastfmLoginState.Failed
            }
    }

    /** Exchanges the token that arrived on the callback for a session key. */
    fun completeLastfmLogin(token: String) {
        viewModelScope.launch {
            if (token.isEmpty()) return@launch
            _lastfmState.value = LastfmLoginState.CompletingLogin
            val session = completeLogin(token)
            if (session != null) {
                dataStoreManager.setLastfmSession(
                    sessionKey = session.sessionKey,
                    username = session.username,
                )
                _lastfmState.value = LastfmLoginState.LoggedIn(session.username)
            } else {
                _lastfmState.value = LastfmLoginState.Failed
            }
        }
    }

    /**
     * Finishes the login from whatever the user pasted back.
     *
     * The callback deep link cannot be relied on: a Linux desktop may have no handler registered for
     * the scheme, a browser may refuse to hand off to a local app, and the approval may even have
     * happened on a different device. In all of those the user can still see the redirect target in
     * their address bar, so accept it directly.
     *
     * Takes either the whole callback URL (`wordbyword://lastfm-auth?token=abc`) or a bare token,
     * because which of the two a user manages to copy is not something to be strict about.
     */
    fun completeLastfmLoginFromCallback(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val token =
            if (trimmed.contains("token=")) {
                trimmed.substringAfter("token=").substringBefore("&").trim()
            } else {
                trimmed
            }
        if (token.isEmpty()) {
            _lastfmState.value = LastfmLoginState.Failed
            return
        }
        completeLastfmLogin(token)
    }

    fun resetLastfmState() {
        _lastfmState.value = LastfmLoginState.Idle
    }
}

sealed interface LastfmLoginState {
    data object Idle : LastfmLoginState

    /** The browser is open and the user has not come back yet. */
    data class AwaitingApproval(
        val authorizeUrl: String,
    ) : LastfmLoginState

    data object CompletingLogin : LastfmLoginState

    data class LoggedIn(
        val username: String,
    ) : LastfmLoginState

    data object Failed : LastfmLoginState
}