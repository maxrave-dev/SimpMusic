package com.maxrave.kotlinytmusicscraper.models.musixmatch
import kotlinx.serialization.Serializable

@Serializable
data class MusixmatchTranslationLyricsResponse (
    val message: Message
) {
    @Serializable
    data class Message(
        val body: Body,
        val header: Header
    ) {
        @Serializable
        data class Body(
            val translations_list: List<Translation>
        ) {
            @Serializable
            data class Translation(
                val translation: TranslationData
            )
            {
                @Serializable
                data class TranslationData(
                    val snippet: String,
                    val matched_line: String,
                    val subtitle_matched_line: String,
                    val description: String,
                )
            }
        }

        @Serializable
        data class Header(
            val status_code: Int
        )
    }
}