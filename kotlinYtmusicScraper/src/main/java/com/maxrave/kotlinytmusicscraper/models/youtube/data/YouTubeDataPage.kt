package com.maxrave.kotlinytmusicscraper.models.youtube.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouTubeDataPage(
    @SerialName("contents")
    val contents: Contents? = null
) {
    @Serializable
    data class Contents(
        @SerialName("twoColumnWatchNextResults")
        val twoColumnWatchNextResults: TwoColumnWatchNextResults? = null
    ) {
        @Serializable
        data class TwoColumnWatchNextResults(
            @SerialName("results")
            val results: Results? = null
        ) {
            @Serializable
            data class Results(
                @SerialName("results")
                val results: Results? = null
            ) {
                @Serializable
                data class Results(
                    @SerialName("contents")
                    val content: List<Content?>? = null
                ) {
                    @Serializable
                    data class Content(
                        @SerialName("videoSecondaryInfoRenderer")
                        val videoSecondaryInfoRenderer: VideoSecondaryInfoRenderer? = null
                    ) {
                        @Serializable
                        data class VideoSecondaryInfoRenderer(
                            @SerialName("owner")
                            val owner: Owner? = null
                        ) {
                            @Serializable
                            data class Owner(
                                @SerialName("videoOwnerRenderer")
                                val videoOwnerRenderer: VideoOwnerRenderer? = null
                            ) {
                                @Serializable
                                data class VideoOwnerRenderer(
                                    @SerialName("thumbnail")
                                    val thumbnail: Thumbnail? = null,
                                    @SerialName("subscriberCountText")
                                    val subscriberCountText: SubscriberCountText? = null,
                                    @SerialName("title")
                                    val title: Title? = null
                                ) {
                                    @Serializable
                                    data class Thumbnail(
                                        @SerialName("thumbnails")
                                        val thumbnails: List<com.maxrave.kotlinytmusicscraper.models.Thumbnail>? = null
                                    )
                                    @Serializable
                                    data class SubscriberCountText(
                                        @SerialName("simpleText")
                                        val simpleText: String? = null
                                    )
                                    @Serializable
                                    data class Title(
                                        @SerialName("runs")
                                        val runs: List<Run>? = null
                                    ) {
                                        @Serializable
                                        data class Run(
                                            @SerialName("text")
                                            val text: String? = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}
