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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.error

/**
 * YouTube Music's radio builder, filled in with this device's listening: pick artists, songs and
 * genres, optionally tune, and Done opens the ONE tuned radio they make in the Playlist page's radio
 * mode. A web sign-in gets only artists from YouTube, so the grid adds the songs played most lately
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
     * The grid: YouTube's artists, the songs played most in the last [LOCAL_DAYS] days and the genres
     * of the top [GENRE_SOURCES] of them, shuffled together. A source that fails to read is left out.
     */
    private suspend fun withLocalSeeds(youTube: List<RadioSeed>): List<RadioSeed> {
        val top =
            runCatching { analyticsRepository.queryTopPlayedSongsLastXDays(LOCAL_DAYS).first() }
                .getOrDefault(emptyList())
        val songsById =
            runCatching { songRepository.getSongsByListVideoId(top.map { it.videoId }).first() }
                .getOrDefault(emptyList())
                .associateBy { it.videoId }
        val songs = top.mapNotNull { songsById[it.videoId] }
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
        _selectedSeeds.value =
            _selectedSeeds.value.let { if (token in it) it - token else it + token }
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
        const val LOCAL_DAYS = 30
        const val LOCAL_SONGS = 12
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
