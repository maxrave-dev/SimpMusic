package com.maxrave.simpmusic.common

object Config {
    var BASE_URL = "https://ytmusicapi.herokuapp.com/"

    var NETWORK_ERROR = "NETWORK_ERROR"

    enum class SyncState {
        LINE_SYNCED,
        UNSYNCED,
        NOT_FOUND
    }
}