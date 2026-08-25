package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.AutoEqEntryEntity
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AutoEqRepository
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** What the picker is doing, so it can say so instead of looking empty. */
enum class AutoEqStatus {
    /** Showing whatever is cached; any refresh is happening quietly behind it. */
    READY,

    /** Nothing cached yet, so the first download has to finish before there is a list. */
    DOWNLOADING,

    /** The download failed and there is nothing cached to fall back on. */
    UNAVAILABLE,

    /** A profile was picked and its curve is on the way. */
    APPLYING,
}

class AutoEqViewModel(
    private val autoEqRepository: AutoEqRepository,
    private val dataStoreManager: DataStoreManager,
) : BaseViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<AutoEqEntryEntity>>(emptyList())
    val results: StateFlow<List<AutoEqEntryEntity>> = _results

    private val _status = MutableStateFlow(AutoEqStatus.READY)
    val status: StateFlow<AutoEqStatus> = _status

    /** Profiles whose curve is already on disk, so the list can mark what works without a network. */
    private val _cachedPaths = MutableStateFlow<Set<String>>(emptySet())
    val cachedPaths: StateFlow<Set<String>> = _cachedPaths

    init {
        viewModelScope.launch {
            // collectLatest cancels the pending delay on the next keystroke, which is the whole of
            // what debounce would do here — without depending on a preview flow operator.
            _query.collectLatest { term ->
                delay(SEARCH_DEBOUNCE_MS)
                _results.emit(autoEqRepository.search(term))
            }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /**
     * Show the cached list at once, then check for a newer index behind it.
     *
     * The refresh is deliberately not awaited before the first list appears: the index changes
     * rarely and the cache is almost always current, so making every open wait on the network
     * would pay a round trip for a result that is nearly always "nothing changed".
     */
    fun onOpen() {
        viewModelScope.launch {
            _cachedPaths.value = autoEqRepository.cachedCurvePaths()
            val cached = autoEqRepository.cachedCount()
            _status.value = if (cached == 0) AutoEqStatus.DOWNLOADING else AutoEqStatus.READY
            if (cached > 0) _results.emit(autoEqRepository.search(_query.value))

            val changed = autoEqRepository.refreshIndex()
            if (changed) _results.emit(autoEqRepository.search(_query.value))
            _status.value =
                if (autoEqRepository.cachedCount() == 0) AutoEqStatus.UNAVAILABLE else AutoEqStatus.READY
        }
    }

    /**
     * Fetch [entry]'s curve and hand it to the equalizer.
     *
     * Preamp before bands, for the same reason a preset does it in that order: the two are separate
     * keys, and the moment in between should be the new headroom under the old curve rather than a
     * boosted curve still running on the old headroom.
     */
    fun apply(
        entry: AutoEqEntryEntity,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            _status.value = AutoEqStatus.APPLYING
            val curve = autoEqRepository.loadCurve(entry)
            if (curve == null) {
                _status.value = AutoEqStatus.READY
                onResult(false)
                return@launch
            }
            dataStoreManager.setEqualizerPreamp(curve.preampDb)
            dataStoreManager.setEqualizerBands(curve.bandsDb)
            dataStoreManager.setEqualizerAutoEqProfile(labelFor(entry), curve.bandsDb)
            // A first pick has just put this curve on disk, so the mark has to move with it.
            _cachedPaths.value = autoEqRepository.cachedCurvePaths()
            _status.value = AutoEqStatus.READY
            onResult(true)
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 200L

        /** Source is part of the label because the same headphone is measured by several of them. */
        fun labelFor(entry: AutoEqEntryEntity): String = "${entry.name} · ${entry.source}"
    }
}
