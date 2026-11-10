package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.comments.YouTubeComment
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A track's waveform: one level per 1 / WAVEFORM_BARS_PER_SECOND seconds, each 0..1. */
class TrackWaveform(
    val videoId: String,
    val bars: FloatArray,
    val isComplete: Boolean = false,
)

/** The first page of a track's YouTube comments, as the SoundCloud style reads it. */
data class TrackComments(
    val videoId: String = "",
    val comments: List<YouTubeComment> = emptyList(),
    val count: String? = null,
)

/**
 * What the SoundCloud Now Playing style draws beyond the shared contract, for the track on screen:
 * its waveform and its comments (the ones naming a moment float over the waveform).
 *
 * Loading is driven by the screen ([load]) rather than by the player, so nothing is decoded or
 * fetched while the player is closed or another style is in use.
 */
class SoundCloudPlayerViewModel(
    private val songRepository: SongRepository,
) : BaseViewModel() {
    private val _waveform = MutableStateFlow<TrackWaveform?>(null)
    val waveform: StateFlow<TrackWaveform?> = _waveform.asStateFlow()

    private val _comments = MutableStateFlow(TrackComments())
    val comments: StateFlow<TrackComments> = _comments.asStateFlow()

    private var waveformVideoId: String? = null
    private var waveformJob: Job? = null
    private var commentsVideoId: String? = null
    private var commentsJob: Job? = null

    /** Loads [videoId]'s waveform and comments. Reopening the player on the same track reuses them. */
    fun load(videoId: String) {
        if (videoId.isBlank()) return
        loadWaveform(videoId)
        loadComments(videoId)
    }

    /**
     * The track is asked for the moment it shows up, which is often before the player has resolved its
     * stream: the format stored for it can still be a previous session's, already refused (403), and the
     * fresh one lands a second or so later. So a miss is asked again a few times while the track is
     * still on screen, and a track came back to without a waveform is tried afresh.
     */
    private fun loadWaveform(videoId: String) {
        if (_waveform.value?.let { it.videoId == videoId && it.isComplete } == true) return
        if (videoId == waveformVideoId && waveformJob?.isActive == true) return
        waveformVideoId = videoId
        waveformJob?.cancel()
        if (_waveform.value?.videoId != videoId) _waveform.value = TrackWaveform(videoId, floatArrayOf())
        waveformJob =
            viewModelScope.launch {
                WAVEFORM_ATTEMPT_DELAYS_MS.forEachIndexed { attempt, wait ->
                    delay(wait)
                    val bars =
                        try {
                            withContext(Dispatchers.IO) {
                                val loadingContext = currentCoroutineContext()
                                mediaPlayerHandler.player.loadWaveform(videoId) { partial ->
                                    if (partial.isNotEmpty()) {
                                        // A retry restarts decoding at zero; retain the longer prefix
                                        // already visible until the new pass catches up.
                                        _waveform.update { previous ->
                                            if (!loadingContext.isActive || previous == null || previous.videoId != videoId || partial.size < previous.bars.size) {
                                                previous
                                            } else {
                                                TrackWaveform(videoId, partial)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Logger.w(tag, "Waveform for $videoId threw: ${e.message}")
                            null
                        }
                    if (bars != null && bars.isNotEmpty()) {
                        _waveform.value = TrackWaveform(videoId, bars, isComplete = true)
                        return@launch
                    }
                    Logger.d(tag, "No waveform for $videoId yet, attempt ${attempt + 1} of ${WAVEFORM_ATTEMPT_DELAYS_MS.size}")
                }
                Logger.w(tag, "Gave up on a waveform for $videoId after ${WAVEFORM_ATTEMPT_DELAYS_MS.size} attempts")
            }
    }

    private fun loadComments(videoId: String) {
        if (videoId == commentsVideoId) return
        commentsVideoId = videoId
        commentsJob?.cancel()
        _comments.value = TrackComments(videoId = videoId)
        commentsJob =
            viewModelScope.launch {
                val result = songRepository.getComments(videoId).firstOrNull()
                val page = (if (result is Resource.Success) result.data else null) ?: return@launch
                _comments.value =
                    TrackComments(
                        videoId = videoId,
                        // YouTube sometimes repeats a comment; CommentsViewModel drops the repeats too.
                        comments = page.comments.distinctBy { it.id },
                        count = page.header?.countShort ?: page.header?.countText,
                    )
            }
    }

    private companion object {
        // Right away, then while the player resolves the stream (it took 1.3 s in the case that
        // showed this up), then twice more for a slow extraction.
        val WAVEFORM_ATTEMPT_DELAYS_MS = listOf(0L, 3_000L, 8_000L, 20_000L)
    }
}
