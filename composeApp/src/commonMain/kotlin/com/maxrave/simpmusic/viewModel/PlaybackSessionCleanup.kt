package com.maxrave.simpmusic.viewModel

/**
 * Stops active playback, clears the in-memory player queue, and wipes the persisted session keys
 * (`recentMediaId`, `recentPosition`, `playlistFromSaved`, and `SavedQueue`) so `mayBeRestoreQueue`
 * does not resurrect a dismissed playback bar on the next launch (#2792).
 *
 * Order matters: [stopPlayerEvent] must complete first so the progress ticker stops and
 * `nowPlayingState.songEntity` is cleared before [resetQueue] fires `onMediaItemTransition`,
 * preventing `mayBeSaveRecentSong` / `mayBeSaveRecentPosition` from writing the song back.
 */
internal suspend fun stopAndClearPersistedPlayback(
    stopPlayerEvent: suspend () -> Unit,
    resetQueue: () -> Unit,
    clearRecentSong: suspend (mediaId: String, position: Long) -> Unit,
    clearSavedPlaylist: suspend (playlist: String) -> Unit,
    removeSavedQueue: suspend () -> Unit,
) {
    stopPlayerEvent()
    resetQueue()
    clearRecentSong("", 0L)
    clearSavedPlaylist("")
    removeSavedQueue()
}
