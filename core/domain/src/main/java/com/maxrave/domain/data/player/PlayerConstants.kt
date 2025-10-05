package com.maxrave.domain.data.player

/**
 * Player constants to avoid Media3 dependencies
 */
object PlayerConstants {
    // Playback states
    const val STATE_IDLE = 1
    const val STATE_BUFFERING = 2
    const val STATE_READY = 3
    const val STATE_ENDED = 4

    // Repeat modes
    const val REPEAT_MODE_OFF = 0
    const val REPEAT_MODE_ONE = 1
    const val REPEAT_MODE_ALL = 2

    // Media item transition reasons
    const val MEDIA_ITEM_TRANSITION_REASON_REPEAT = 0
    const val MEDIA_ITEM_TRANSITION_REASON_AUTO = 1
    const val MEDIA_ITEM_TRANSITION_REASON_SEEK = 2
    const val MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED = 3

    // Player error
    const val ERROR_CODE_TIMEOUT = 1003

    // Audio session
    const val AUDIO_SESSION_ID_UNSET = 0

    // Selection flags
    const val SELECTION_FLAG_DEFAULT = 1
}