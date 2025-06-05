package org.simpmusic.aiservice

import com.maxrave.lyricsproviders.models.lyrics.Line
import com.maxrave.lyricsproviders.models.lyrics.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class AiClientTest {
//    private val aiClient = AiClient()
//    @Before
//    fun setUp() {
//        aiClient.host = AIHost.GEMINI
//        aiClient.apiKey = ""
//    }
//
//    @Test
//    fun testTranslateLyrics() {
//        // Example lyrics input
//        val inputLyrics = Lyrics(
//            lyrics = Lyrics.LyricsX(
//                lines = listOf(
//                    Line(
//                        startTimeMs = "0",
//                        endTimeMs = "1000",
//                        words = "Hello world",
//                        syllables = listOf("Hel", "lo", "world")
//                    ),
//                    Line(
//                        startTimeMs = "1000",
//                        endTimeMs = "2000",
//                        words = "This is a test",
//                        syllables = listOf("This", "is", "a", "test")
//                    )
//                ),
//                syncType = "LINE_SYNCED"
//            )
//        )
//        val targetLanguage = "vi" // Vietnamese
//
//        runBlocking(Dispatchers.IO) {
//            // Call the translateLyrics method
//            aiClient.translateLyrics(inputLyrics, targetLanguage).onSuccess {
//                // Print the translated lyrics
//                println("Translated Lyrics: $it")
//            }.onFailure { exception ->
//                // Print the error message
//                println("Error translating lyrics: ${exception.message}")
//            }
//        }
//    }
}