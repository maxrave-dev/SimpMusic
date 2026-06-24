package com.maxrave.simpmusic.ui.screen.player

import com.maxrave.domain.data.model.browse.album.Track

/**
 * Pure helpers for the Spotify-style artwork pager on [NowPlayingScreen].
 *
 * Extracted to keep the sync/dispatch logic unit-testable without Compose runtime.
 */

/**
 * Maps the currently playing track back to its position in the queue (`listTracks`).
 *
 * - Uses `videoId` (already prefix-stripped by `MediaServiceHandlerImpl#getDataOfNowPlayingState`).
 *   Do **not** pass `nowPlayingState.mediaItem.mediaId` — for video items it carries the
 *   `MERGING_DATA_TYPE.VIDEO` ("Video") prefix and the lookup will silently fail.
 * - Uses `indexOfLast` to mirror `MediaServiceHandlerImpl#currentOrderIndex`.
 * - Coerces to `0` when the track isn't found, so the pager stays on the first slot
 *   instead of throwing during a transient queue/now-playing mismatch.
 */
internal fun deriveOrderIndex(
    queue: List<Track>,
    nowPlayingVideoId: String?,
): Int {
    if (queue.isEmpty() || nowPlayingVideoId.isNullOrEmpty()) return 0
    return queue
        .indexOfLast { it.videoId == nowPlayingVideoId }
        .coerceAtLeast(0)
}

/**
 * What the player should do when the pager settles on a new page.
 *
 * - [Next] / [Previous] keep the existing crossfade flow on Android intact
 *   (CrossfadeExoPlayerAdapter relies on `player.next()` / `player.previous()`).
 * - [Skip] jumps to a non-adjacent index via `playMediaItemInMediaSource`, which
 *   internally handles unshuffling.
 */
internal sealed interface ArtworkSeekAction {
    data object Next : ArtworkSeekAction

    data object Previous : ArtworkSeekAction

    data class Skip(val index: Int) : ArtworkSeekAction

    /** Same page — caller should ignore (the LaunchedEffect filter normally prevents this). */
    data object NoOp : ArtworkSeekAction
}

internal fun computeSeekAction(
    newPage: Int,
    currentOrderIndex: Int,
): ArtworkSeekAction =
    when (newPage - currentOrderIndex) {
        0 -> ArtworkSeekAction.NoOp
        1 -> ArtworkSeekAction.Next
        -1 -> ArtworkSeekAction.Previous
        else -> ArtworkSeekAction.Skip(newPage)
    }
