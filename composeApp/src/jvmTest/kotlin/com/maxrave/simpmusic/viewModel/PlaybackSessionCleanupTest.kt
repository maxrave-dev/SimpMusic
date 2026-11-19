package com.maxrave.simpmusic.viewModel

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaybackSessionCleanupTest {
    @Test
    fun stopsPlayerAndResetsQueueBeforeClearingPersistedState() =
        runBlocking {
            val events = mutableListOf<String>()
            stopAndClearPersistedPlayback(
                stopPlayerEvent = { events.add("stop") },
                resetQueue = { events.add("resetQueue") },
                clearRecentSong = { mediaId, position -> events.add("clearRecentSong:$mediaId:$position") },
                clearSavedPlaylist = { playlist -> events.add("clearSavedPlaylist:$playlist") },
                removeSavedQueue = { events.add("removeSavedQueue") },
            )
            assertEquals(
                listOf(
                    "stop",
                    "resetQueue",
                    "clearRecentSong::0",
                    "clearSavedPlaylist:",
                    "removeSavedQueue",
                ),
                events,
            )
        }

    @Test
    fun transitionCallbackDuringQueueResetCannotResurrectClearedSong() =
        runBlocking {
            var activeVideoId: String? = "dQw4w9WgXcQ"
            var queueInitialized = true
            var persistedMediaId = "dQw4w9WgXcQ"
            var persistedPosition = 42_000L
            var persistedPlaylist = "Favorites"
            val persistedQueue = mutableListOf("dQw4w9WgXcQ", "track2")

            val simulateMayBeSaveRecentSong = {
                val id = activeVideoId
                if (id != null && queueInitialized) {
                    persistedMediaId = id
                    persistedPosition = 45_000L
                }
            }

            stopAndClearPersistedPlayback(
                stopPlayerEvent = {
                    activeVideoId = null
                },
                resetQueue = {
                    queueInitialized = false
                    // Mirrors player.clearMediaItems() firing onMediaItemTransition -> mayBeSaveRecentSong()
                    simulateMayBeSaveRecentSong()
                },
                clearRecentSong = { mediaId, position ->
                    persistedMediaId = mediaId
                    persistedPosition = position
                },
                clearSavedPlaylist = { playlist ->
                    persistedPlaylist = playlist
                },
                removeSavedQueue = {
                    persistedQueue.clear()
                },
            )

            assertEquals("", persistedMediaId)
            assertEquals(0L, persistedPosition)
            assertEquals("", persistedPlaylist)
            assertTrue(persistedQueue.isEmpty())
        }
}
