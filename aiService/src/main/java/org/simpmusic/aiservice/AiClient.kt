package org.simpmusic.aiservice

import com.maxrave.lyricsproviders.models.lyrics.Lyrics

class AiClient() {
    private var aiService: AiService? = null
    var host = AIHost.GEMINI
        set(value) {
            field = value
            apiKey?.let {
                aiService = AiService(
                    aiHost = value,
                    apiKey = it
                )
            }
        }
    var apiKey: String? = null
        set(value) {
            field = value
            aiService = if (value != null) {
                AiService(
                    aiHost = host,
                    apiKey = value
                )
            } else {
                null
            }
        }

    suspend fun translateLyrics(
        inputLyrics: Lyrics,
        targetLanguage: String
    ): Result<Lyrics> = runCatching {
        aiService?.translateLyrics(inputLyrics, targetLanguage)
            ?: throw IllegalStateException("AI service is not initialized. Please set host and apiKey.")
    }
}