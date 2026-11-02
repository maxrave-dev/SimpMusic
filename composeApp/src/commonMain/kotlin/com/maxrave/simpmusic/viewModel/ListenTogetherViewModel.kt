package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.data.listentogether.ListenTogetherPlaybackBridge
import com.maxrave.data.listentogether.ListenTogetherPrefs
import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.data.model.listentogether.RoomConnection
import com.maxrave.domain.repository.ListenTogetherRepository
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.utils.ROOM_CODE_LENGTH
import com.maxrave.simpmusic.utils.RoomFormProblem
import com.maxrave.simpmusic.utils.awaitConnected
import com.maxrave.simpmusic.utils.roomCodeFromText
import com.maxrave.simpmusic.utils.roomFormProblems
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Screen state for Listen Together.
 *
 * The session owns everything the server says; this holds only what the user has typed but not yet
 * sent, so a half-filled room code survives a recomposition without being confused for room state.
 */
class ListenTogetherViewModel(
    private val repository: ListenTogetherRepository,
    private val dataStore: DataStoreManager,
    bridge: ListenTogetherPlaybackBridge,
) : ViewModel() {
    init {
        // The bridge is a singleton that outlives this screen; starting it here is simply the
        // first moment anything asks for it. Koin's `createdAtStart` does not fire for modules
        // added with loadKoinModules, so without an explicit injection it is never constructed
        // at all — and then a room syncs membership while no audio follows anyone.
        bridge.start()

        // Host conveniences live in settings but are applied by the session, so they are mirrored
        // onto it for as long as this screen exists.
        viewModelScope.launch {
            dataStore.getString(ListenTogetherPrefs.AUTO_APPROVE_JOINS).collect {
                repository.autoApproveJoins = it == ListenTogetherPrefs.TRUE
            }
        }
        viewModelScope.launch {
            dataStore.getString(ListenTogetherPrefs.AUTO_APPROVE_SUGGESTIONS).collect {
                repository.autoApproveSuggestions = it == ListenTogetherPrefs.TRUE
            }
        }
    }

    val state: StateFlow<ListenTogetherRoom> =
        repository.room.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListenTogetherRoom())

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _roomCodeInput = MutableStateFlow("")
    val roomCodeInput: StateFlow<String> = _roomCodeInput.asStateFlow()

    private val _formProblems = MutableStateFlow(emptySet<RoomFormProblem>())

    /** What the form says is wrong. Empty until the user has tried to go ahead, or a link has arrived. */
    val formProblems: StateFlow<Set<RoomFormProblem>> = _formProblems.asStateFlow()

    private val _focusNameField = MutableStateFlow(false)
    val focusNameField: StateFlow<Boolean> = _focusNameField.asStateFlow()

    private var pendingAction: Job? = null
    private val _connectingForAction = MutableStateFlow(false)

    /** A tap on Create or Join is waiting for the connection it started. */
    val connectingForAction: StateFlow<Boolean> = _connectingForAction.asStateFlow()

    fun onDisplayNameChange(value: String) {
        // metroserver caps usernames at 50; trimming here means the server never has to reject it.
        _displayName.value = value.take(MAX_USERNAME_LENGTH)
        if (value.isNotBlank()) _formProblems.update { it - RoomFormProblem.NAME_MISSING }
    }

    fun onNameFieldFocused() {
        _focusNameField.value = false
    }

    fun onRoomCodeChange(value: String) {
        // Longer than a code, so it may be a whole link or share message pasted in. Filtering that
        // down to its first eight letters would leave a wrong code, like SIMPMUSI.
        val pasted = if (value.length > ROOM_CODE_LENGTH) roomCodeFromText(value) else null
        val code = pasted ?: value.uppercase().filter { it.isLetterOrDigit() }.take(ROOM_CODE_LENGTH)
        _roomCodeInput.value = code
        _formProblems.update { problems ->
            val fixed = if (code.length == ROOM_CODE_LENGTH) setOf(RoomFormProblem.CODE_INCOMPLETE) else emptySet()
            problems - fixed - RoomFormProblem.QR_UNREADABLE
        }
    }

    fun connect() = repository.connect()

    fun disconnect() {
        pendingAction?.cancel()
        repository.disconnect()
    }

    fun createRoom() {
        val name = _displayName.value.trim()
        if (!formIsReady(roomFormProblems(name))) return
        whenConnected { repository.createRoom(name) }
    }

    fun joinRoom() {
        val name = _displayName.value.trim()
        val code = _roomCodeInput.value
        if (!formIsReady(roomFormProblems(name, code))) return
        whenConnected { repository.joinRoom(code, name) }
    }

    private fun formIsReady(problems: Set<RoomFormProblem>): Boolean {
        _formProblems.value = problems
        if (RoomFormProblem.NAME_MISSING in problems) _focusNameField.value = true
        return problems.isEmpty()
    }

    /**
     * Runs [action] on a live connection, connecting first if needed: the session drops a create
     * message sent while disconnected, with no error, so a tap has to wait for the socket.
     */
    private fun whenConnected(action: () -> Unit) {
        if (repository.room.value.isConnected) {
            action()
            return
        }
        if (pendingAction?.isActive == true) return
        pendingAction =
            viewModelScope.launch {
                _connectingForAction.value = true
                try {
                    if (repository.room.value.connection !is RoomConnection.Connecting) repository.connect()
                    val connected =
                        withTimeoutOrNull(CONNECT_WAIT_MS) {
                            repository.room.map { it.connection }.awaitConnected()
                        }
                    if (connected == true) action()
                } finally {
                    _connectingForAction.value = false
                }
            }
    }

    /**
     * A room code from a QR code or an app link. Fills in the form and connects, but never joins by
     * itself: the code came from outside the app, and joining shows the user's name to that room.
     *
     * @return false if the user is already in a different room, so nothing changed.
     */
    fun onRoomCodeReceived(code: String): Boolean {
        val room = repository.room.value
        if (room.inRoom) return room.roomCode == code
        _roomCodeInput.value = code
        _formProblems.update { it - RoomFormProblem.CODE_INCOMPLETE - RoomFormProblem.QR_UNREADABLE }
        if (_displayName.value.isBlank()) {
            _formProblems.update { it + RoomFormProblem.NAME_MISSING }
            _focusNameField.value = true
        }
        if (room.connection is RoomConnection.Disconnected || room.connection is RoomConnection.Failed) {
            repository.connect()
        }
        return true
    }

    fun onQrUnreadable() {
        _formProblems.update { it + RoomFormProblem.QR_UNREADABLE }
    }

    fun leaveRoom() {
        repository.leaveRoom()
        _roomCodeInput.value = ""
    }

    fun approveJoin(userId: String) {
        repository.approveJoin(userId)
    }

    fun rejectJoin(userId: String) {
        repository.rejectJoin(userId)
    }

    fun approveSuggestion(id: String) {
        repository.approveSuggestion(id)
    }

    fun rejectSuggestion(id: String) {
        repository.rejectSuggestion(id)
    }

    fun kickUser(userId: String) {
        repository.kickUser(userId)
    }

    /**
     * Blocks by NAME and then kicks.
     *
     * The protocol has no ban and the server mints a new user id per connection, so the id cannot
     * be blocked on — the name is the only thing that survives a reconnect, and it is changeable.
     * A convenience, not a security control.
     */
    fun blockAndKick(
        userId: String,
        username: String,
    ) {
        viewModelScope.launch {
            val current =
                dataStore
                    .getString(ListenTogetherPrefs.BLOCKLIST)
                    .first()
                    .orEmpty()
                    .split(ListenTogetherPrefs.BLOCKLIST_SEPARATOR)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            if (current.none { it.equals(username, ignoreCase = true) }) {
                dataStore.putString(
                    ListenTogetherPrefs.BLOCKLIST,
                    (current + username).joinToString(ListenTogetherPrefs.BLOCKLIST_SEPARATOR),
                )
            }
            repository.kickUser(userId)
        }
    }

    fun transferHost(userId: String) {
        repository.transferHost(userId)
    }

    fun cancelJoin() = repository.cancelJoin()

    fun clearError() = repository.clearError()

    companion object {
        private const val MAX_USERNAME_LENGTH = 50
        private const val CONNECT_WAIT_MS = 30_000L
    }
}
