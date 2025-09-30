package org.simpmusic.aiservice

import com.maxrave.domain.data.model.metadata.Lyrics

class AiClient {
    private var aiService: AiService? = null
    var host = AIHost.GEMINI
        set(value) {
            field = value
            apiKey?.let {
                aiService =
                    AiService(
                        aiHost = value,
                        apiKey = it,
                    )
            }
        }
    var apiKey: String? = null
        set(value) {
            field = value
            aiService =
                if (value != null) {
                    AiService(
                        aiHost = host,
                        apiKey = value,
                    )
                } else {
                    null
                }
        }
    var customModelId: String? = null
        set(value) {
            field = value
            aiService =
                if (apiKey != null) {
                    AiService(
                        aiHost = host,
                        apiKey = apiKey!!,
                        customModelId = value,
                    )
                } else {
                    null
                }
        }

    suspend fun translateLyrics(
        inputLyrics: Lyrics,
        targetLanguage: String,
    ): Result<Lyrics> =
        runCatching {
            aiService?.translateLyrics(inputLyrics, targetLanguage).also { data ->
                if (data?.lines?.map { it.words }?.containsAll(
                        inputLyrics.lines?.map { it.words } ?: emptyList(),
                    ) == true
                ) {
                    throw IllegalStateException("Translation failed or returned empty lyrics.")
                }
            }
                ?: throw IllegalStateException("AI service is not initialized. Please set host and apiKey.")
        }
}