package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.radiobuilder.RadioBuilder
import com.maxrave.domain.data.model.radiobuilder.RadioChipGroup
import com.maxrave.domain.data.model.radiobuilder.RadioSeed
import com.maxrave.domain.data.model.radiobuilder.RadioSeedKind
import com.maxrave.domain.repository.AnalyticsRepository
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.error

/**
 * YouTube Music's radio builder, filled in with this device's listening: pick artists, songs and
 * genres, optionally tune, and Done opens the ONE tuned radio they make in the Playlist page's radio
 * mode. A web sign-in gets only artists from YouTube, so the grid adds the top of the local Replay Mix
 * (`RDATv<videoId>`) and the genres YouTube puts on their radios (`RDATG<id>E`), as the iOS app shows.
 */
class RadioBuilderViewModel(
    private val homeRepository: HomeRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val songRepository: SongRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<RadioBuilderUIState>(RadioBuilderUIState.Loading)
    val uiState: StateFlow<RadioBuilderUIState> get() = _uiState

    /** Seed tokens, in the order they were picked. */
    private val _selectedSeeds = MutableStateFlow<List<String>>(emptyList())
    val selectedSeeds: StateFlow<List<String>> get() = _selectedSeeds

    /** The picked chip id per group, groups numbered across sections in page order. */
    private val _selectedChips = MutableStateFlow<Map<Int, String>>(emptyMap())
    val selectedChips: StateFlow<Map<Int, String>> get() = _selectedChips

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = RadioBuilderUIState.Loading
            homeRepository.getRadioBuilder().collect { resource ->
                val page = resource.data
                if (resource is Resource.Success && page != null) {
                    _selectedSeeds.value = emptyList()
                    _selectedChips.value =
                        page
                            .groups()
                            .mapIndexedNotNull { index, group -> group.initialChipId?.let { index to it } }
                            .toMap()
                    _uiState.value = RadioBuilderUIState.Success(page.copy(seeds = withLocalSeeds(page.seeds)))
                } else {
                    _uiState.value = RadioBuilderUIState.Error(resource.message ?: getString(Res.string.error))
                }
            }
        }
    }

    /**
     * The grid: YouTube's artists, the top [LOCAL_SONGS] of the local Replay Mix (this week's most
     * played, then this month's) and the genres of its top [GENRE_SOURCES], shuffled together. A source
     * that fails to read is left out.
     */
    private suspend fun withLocalSeeds(youTube: List<RadioSeed>): List<RadioSeed> {
        val songs =
            runCatching { loadReplayMixSongs(analyticsRepository, songRepository) }
                .getOrDefault(emptyList())
        val songSeeds =
            songs.take(LOCAL_SONGS).map { song ->
                RadioSeed(
                    title = song.title,
                    thumbnail = song.thumbnails,
                    token = "RDATv${song.videoId}",
                    kind = RadioSeedKind.SONG,
                    subtitle = song.artistName?.joinToString(", "),
                )
            }
        val genres =
            runCatching { homeRepository.radioGenres(songs.take(GENRE_SOURCES).map { it.videoId }) }
                .getOrDefault(emptyList())
        log("Radio builder grid: ${youTube.size} artists, ${songSeeds.size} songs, ${genres.size} genres")
        // Artists, songs and genres shuffled together, as the iOS grid mixes them.
        return (youTube + songSeeds + genres).distinctBy { it.token }.shuffled()
    }

    fun toggleSeed(token: String) {
        val picking = token !in _selectedSeeds.value
        _selectedSeeds.value =
            _selectedSeeds.value.let { if (picking) it + token else it - token }
        if (picking) revealRelated(token)
    }

    /**
     * Picking an artist brings its related artists into the grid right after it, as the web page does.
     * They stay when it is unpicked, since some of them may be picked by then.
     */
    private fun revealRelated(token: String) {
        val page = (_uiState.value as? RadioBuilderUIState.Success)?.page ?: return
        val shown = page.seeds.mapTo(HashSet()) { it.token }
        val related = page.related[token].orEmpty().filterNot { it.token in shown }.distinctBy { it.token }
        val at = page.seeds.indexOfFirst { it.token == token }
        if (related.isEmpty() || at < 0) return
        _uiState.value =
            RadioBuilderUIState.Success(page.copy(seeds = page.seeds.toMutableList().apply { addAll(at + 1, related) }))
    }

    /** A required group always keeps one chip; tapping the picked chip of another clears it. */
    fun pickChip(
        groupIndex: Int,
        chipId: String,
        required: Boolean,
    ) {
        val current = _selectedChips.value
        _selectedChips.value =
            if (current[groupIndex] == chipId && !required) current - groupIndex else current + (groupIndex to chipId)
    }

    /** The one tuned radio id every pick and chip make. Null when nothing is picked. */
    fun radioId(): String? {
        val page = (_uiState.value as? RadioBuilderUIState.Success)?.page ?: return null
        val chips = _selectedChips.value
        val id =
            homeRepository.radioId(
                seedTokens = _selectedSeeds.value,
                selectedChipIds = page.groups().indices.mapNotNull { chips[it] },
            ) ?: return null
        log("Radio builder: ${_selectedSeeds.value.size} seeds, ${chips.size} chips")
        return id
    }

    private companion object {
        const val LOCAL_SONGS = 24
        const val GENRE_SOURCES = 5
    }
}

/** Every chip group of the page, sections in order; the index is a group's key in the selection. */
fun RadioBuilder.groups(): List<RadioChipGroup> = sections.flatMap { it.groups }

sealed class RadioBuilderUIState {
    data class Success(
        val page: RadioBuilder,
    ) : RadioBuilderUIState()

    data class Error(
        val message: String,
    ) : RadioBuilderUIState()

    object Loading : RadioBuilderUIState()
}
