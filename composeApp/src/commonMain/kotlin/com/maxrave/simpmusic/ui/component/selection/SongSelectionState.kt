package com.maxrave.simpmusic.ui.component.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.max_selection_reached

/**
 * Hard cap on how many songs can be selected at once. Anything that acts on the selection
 * (download, add to playlist, add to queue) fires one request per song, so the cap keeps a
 * single gesture from firing hundreds of them with no way to cancel.
 */
const val MAX_SONG_SELECTION = 25

/**
 * Selection state for a list of songs. Held by the screen, read by [com.maxrave.simpmusic.ui.component.SongFullWidthItems].
 *
 * Items are tracked by videoId rather than by list index: the lists this drives are paged and
 * reorderable, so an index stops pointing at the same song after a drag or a page load.
 */
@Stable
class SongSelectionState(
    private val limitMessage: String,
) {
    var isActive by mutableStateOf(false)
        private set

    private val _selected = mutableStateListOf<String>()
    val selected: List<String> get() = _selected
    val count: Int get() = _selected.size
    val isFull: Boolean get() = _selected.size >= MAX_SONG_SELECTION

    fun isSelected(videoId: String): Boolean = _selected.contains(videoId)

    /** Enters selection mode with [videoId] already picked. Long press calls this. */
    fun start(videoId: String) {
        if (isActive) return
        isActive = true
        add(videoId)
    }

    fun toggle(videoId: String) {
        if (!isActive) return
        if (_selected.remove(videoId)) {
            // Deselecting the last one leaves an action bar with nothing to act on, so leave the mode.
            if (_selected.isEmpty()) exit()
            return
        }
        add(videoId)
    }

    /**
     * Takes the first [MAX_SONG_SELECTION] of [videoIds], in the order the list shows them —
     * and doubles as deselect-all: pressing it again once everything it would pick is already
     * picked clears the selection instead of doing nothing.
     *
     * Clearing here deliberately stays in selection mode, unlike [toggle]. Tapping rows off one
     * by one reads as leaving; tapping this reads as starting the selection over.
     */
    fun toggleSelectAll(videoIds: List<String>) {
        if (!isActive) return
        val candidates = videoIds.filter { it.isNotBlank() }.distinct()
        val everythingReachableIsPicked =
            candidates.isNotEmpty() &&
                candidates.take(MAX_SONG_SELECTION).all { _selected.contains(it) }
        if (everythingReachableIsPicked) {
            _selected.clear()
            return
        }
        for (videoId in candidates) {
            if (!add(videoId)) return
        }
    }

    fun exit() {
        isActive = false
        _selected.clear()
    }

    /**
     * The only way into [_selected], which is what makes the cap hold: long press, tap and
     * select-all all land here, so the check cannot be bypassed by one of the three.
     * Returns false once the cap is reached, so select-all stops instead of toasting per song.
     */
    private fun add(videoId: String): Boolean {
        if (videoId.isBlank() || _selected.contains(videoId)) return true
        if (isFull) {
            showToast(limitMessage, ToastGravity.Bottom)
            return false
        }
        _selected.add(videoId)
        return true
    }
}

@Composable
fun rememberSongSelectionState(): SongSelectionState {
    val limitMessage = stringResource(Res.string.max_selection_reached, MAX_SONG_SELECTION)
    return remember(limitMessage) { SongSelectionState(limitMessage) }
}
