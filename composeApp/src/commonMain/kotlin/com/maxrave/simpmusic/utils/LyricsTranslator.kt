package com.maxrave.simpmusic.utils

import com.maxrave.domain.data.model.metadata.Line
import com.maxrave.domain.data.model.metadata.Lyrics
import com.maxrave.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object LyricsTranslator {
    private const val TAG = "LyricsTranslator"
    private val client = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun translateLyrics(
        lyrics: Lyrics,
        targetLanguage: String,
    ): Result<Lyrics> = runCatching {
        val originalLines = lyrics.lines.orEmpty()
        if (originalLines.isEmpty()) {
            throw IllegalArgumentException("Empty lyrics")
        }

        val nonBlankLines = originalLines.mapIndexedNotNull { index, line ->
            val w = line.words.trim()
            if (w.isNotEmpty() && w != "♫") index to w else null
        }

        if (nonBlankLines.isEmpty()) {
            throw IllegalArgumentException("No translatable words")
        }

        val textToTranslate = nonBlankLines.joinToString("\n") { it.second }

        // Try Google Translate public client with POST (avoids URL length limits & 403 blocks)
        val translatedText = translateWithGoogle(textToTranslate, targetLanguage)
            ?: translateWithMyMemory(textToTranslate, targetLanguage)
            ?: throw IllegalStateException("All translation providers failed")

        val translatedLinesList = translatedText.lines()
        val indexToTranslatedWord = mutableMapOf<Int, String>()
        nonBlankLines.forEachIndexed { i, (origIndex, _) ->
            val trans = translatedLinesList.getOrNull(i)?.trim()
            if (!trans.isNullOrEmpty()) {
                indexToTranslatedWord[origIndex] = trans
            }
        }

        val resultLines = originalLines.mapIndexed { index, origLine ->
            val translatedWord = indexToTranslatedWord[index] ?: origLine.words
            Line(
                endTimeMs = origLine.endTimeMs,
                startTimeMs = origLine.startTimeMs,
                words = translatedWord,
                syllables = emptyList(),
            )
        }

        Lyrics(
            error = false,
            lines = ArrayList(resultLines),
            syncType = lyrics.syncType,
        )
    }

    private suspend fun translateWithGoogle(text: String, targetLanguage: String): String? {
        return runCatching {
            val response = client.submitForm(
                url = "https://translate.googleapis.com/translate_a/single?client=gtx&dt=t",
                formParameters = Parameters.build {
                    append("sl", "auto")
                    append("tl", targetLanguage)
                    append("q", text)
                },
            ) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            }.bodyAsText()

            val jsonElement = json.parseToJsonElement(response)
            val parts = jsonElement.jsonArray[0].jsonArray
            val sb = StringBuilder()
            for (p in parts) {
                val partStr = p.jsonArray.getOrNull(0)?.jsonPrimitive?.content
                if (partStr != null) {
                    sb.append(partStr)
                }
            }
            sb.toString()
        }.onFailure {
            Logger.w(TAG, "Google translate failed: ${it.message}")
        }.getOrNull()
    }

    private suspend fun translateWithMyMemory(text: String, targetLanguage: String): String? {
        return runCatching {
            val encoded = text.encodeURLParameter()
            val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=autodetect|$targetLanguage"
            val response = client.get(url) {
                header("User-Agent", "SimpMusic/2.5.0")
            }.bodyAsText()

            val jsonElement = json.parseToJsonElement(response)
            jsonElement.jsonObject["responseData"]?.jsonObject?.get("translatedText")?.jsonPrimitive?.content
        }.onFailure {
            Logger.w(TAG, "MyMemory translate failed: ${it.message}")
        }.getOrNull()
    }
}
