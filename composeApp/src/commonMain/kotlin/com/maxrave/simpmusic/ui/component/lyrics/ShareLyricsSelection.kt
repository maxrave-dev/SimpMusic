package com.maxrave.simpmusic.ui.component.lyrics

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** How many lines one share card may carry. */
const val MAX_SHARE_LYRIC_LINES = 3

/**
 * The set of lyric lines picked for a share card, held as an [IntRange] rather than a set of
 * indices.
 *
 * Holding a range is what ENFORCES the rule that the lines must be next to each other — there is
 * no state this class can be in that represents a gap, so no validation pass is needed and none
 * can be forgotten. Spotify applies the same rule for a content reason: three lines lifted from
 * three different verses read as a broken sentence, and someone looking at the finished card has
 * no way to tell they were ever far apart.
 *
 * The tap rules follow from that. Tapping a line far from the current pick starts a NEW pick
 * there instead of refusing — refusing would be the only other option, and it teaches nothing.
 */
@Stable
class ShareLyricsSelection(
    initialIndex: Int? = null,
) {
    var range by mutableStateOf(initialIndex?.let { it..it })
        private set

    val count: Int get() = range?.count() ?: 0

    val isEmpty: Boolean get() = range == null

    fun isSelected(index: Int): Boolean = range?.contains(index) == true

    /**
     * Applies a tap on [index].
     *
     * [onLimitReached] fires only when the tap WOULD have extended a full selection — the one case
     * where nothing visibly happens, so the UI has to explain itself.
     */
    fun toggle(
        index: Int,
        onLimitReached: () -> Unit = {},
    ) {
        val current = range
        if (current == null) {
            range = index..index
            return
        }

        when {
            // Shrink from either end. A one-line selection tapped again clears entirely.
            index == current.first && index == current.last -> range = null
            index == current.first -> range = (current.first + 1)..current.last
            index == current.last -> range = current.first..(current.last - 1)

            // A tap in the middle of a longer pick is ambiguous — which end did they mean to move?
            // Restarting from that line is the reading with no hidden rule behind it.
            current.contains(index) -> range = index..index

            // Grow, if there is room.
            index == current.first - 1 || index == current.last + 1 -> {
                if (current.count() >= MAX_SHARE_LYRIC_LINES) {
                    onLimitReached()
                } else {
                    range = if (index < current.first) index..current.last else current.first..index
                }
            }

            // Anywhere else: start again from there.
            else -> range = index..index
        }
    }
}
