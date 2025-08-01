package org.simpmusic.aiservice

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.JsonSchema
import com.aallam.openai.api.chat.chatCompletionRequest
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost.Companion.Gemini
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.simpmusic.lyrics.domain.Lyrics

class AiService(
    private val aiHost: AIHost = AIHost.GEMINI,
    private val apiKey: String,
    private val customModelId: String? = null,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    private val openAI: OpenAI by lazy {
        when (aiHost) {
            AIHost.GEMINI -> OpenAI(host = Gemini, token = apiKey)
            AIHost.OPENAI -> OpenAI(token = apiKey)
        }
    }

    private val model by lazy {
        if (!customModelId.isNullOrEmpty()) {
            ModelId(customModelId)
        } else {
            when (aiHost) {
                AIHost.GEMINI -> ModelId("gemini-2.0-flash-lite")
                AIHost.OPENAI -> ModelId("gpt-4o")
            }
        }
    }

    suspend fun translateLyrics(
        inputLyrics: Lyrics,
        targetLanguage: String,
    ): Lyrics {
        val request =
            chatCompletionRequest {
                this.model = this@AiService.model
                responseFormat = ChatResponseFormat.jsonSchema(aiResponseJsonSchema)
                messages {
                    system {
                        content =
                            "You are a translation assistant. Translate the below JSON-serialized lyrics into the target language while preserving the exact same JSON structure. Only translate the actual text fields such as `words` and `syllables` (if present). Do not change keys, nesting, timestamps, or any other metadata.\\n\\nThe output must be valid JSON with the same structure as the input. Do not include explanations or extra commentary—only return the resulting JSON."
                    }
                    user {
                        content {
                            text("Target language: $targetLanguage")
                        }
                        content {
                            text("Input lyrics: ${json.encodeToString(inputLyrics)}")
                        }
                    }
                }
            }
        val completion: ChatCompletion = openAI.chatCompletion(request)
        val jsonContent =
            completion.choices
                .firstOrNull()
                ?.message
                ?.content ?: throw IllegalStateException("No response from AI")
        val jsonData =
            Regex(
                "```json\\s*([\\s\\S]*?)```",
            ).find(jsonContent)
                ?.groups
                ?.firstOrNull()
                ?.value ?: jsonContent
        val aiResponse =
            json.decodeFromString<Lyrics>(
                jsonData
                    .replace("```json", "")
                    .replace("```", ""),
            )
        return aiResponse
    }

    companion object {
        private val translationJsonSchema: JsonObject =
            buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("lyrics") {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("lines") {
                                put("type", "array")
                                putJsonObject("items") {
                                    put("type", "object")
                                    putJsonObject("properties") {
                                        putJsonObject("startTimeMs") {
                                            put("type", "string")
                                        }
                                        putJsonObject("endTimeMs") {
                                            put("type", "string")
                                        }
                                        putJsonObject("syllables") {
                                            put("type", "array")
                                            putJsonObject("items") {
                                                put("type", "string")
                                            }
                                        }
                                        putJsonObject("words") {
                                            put("type", "string")
                                        }
                                    }
                                    putJsonArray("required") {
                                        add("startTimeMs")
                                        add("endTimeMs")
                                        add("words")
                                        // `syllables` is optional if it's nullable
                                    }
                                }
                            }
                            putJsonObject("syncType") {
                                put("type", "string")
                            }
                        }
                        putJsonArray("required") {
                            add("lines")
                            add("syncType")
                        }
                    }
                }
                putJsonArray("required") {
                    add("lyrics")
                }
            }
        private val aiResponseJsonSchema =
            JsonSchema(
                name = "ai_translation_schema", // Give your schema a name
                schema = translationJsonSchema,
                strict = true, // Recommended for better adherence
            )
    }
}

enum class AIHost {
    GEMINI,
    OPENAI,
}