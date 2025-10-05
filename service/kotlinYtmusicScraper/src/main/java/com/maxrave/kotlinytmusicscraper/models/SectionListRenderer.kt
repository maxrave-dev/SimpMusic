package com.maxrave.kotlinytmusicscraper.models

import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class SectionListRenderer(
    val header: Header?,
    val contents: List<Content>?,
    val continuations: List<Continuation>?,
) {
    @Serializable
    data class Header(
        val chipCloudRenderer: ChipCloudRenderer?,
    ) {
        @Serializable
        data class ChipCloudRenderer(
            val chips: List<Chip>,
        ) {
            @Serializable
            data class Chip(
                val chipCloudChipRenderer: ChipCloudChipRenderer,
            ) {
                @Serializable
                data class ChipCloudChipRenderer(
                    val isSelected: Boolean? = null,
                    val navigationEndpoint: NavigationEndpoint,
                    // The close button doesn't have the following two fields
                    val text: Runs?,
                    val uniqueId: String?,
                )
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class Content(
        @JsonNames("musicImmersiveCarouselShelfRenderer")
        val musicCarouselShelfRenderer: MusicCarouselShelfRenderer?,
        val musicShelfRenderer: MusicShelfRenderer?,
        val musicCardShelfRenderer: MusicCardShelfRenderer?,
        val musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer?,
        val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?,
        val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer?,
        val musicEditablePlaylistDetailHeaderRenderer: BrowseResponse.Header.MusicEditablePlaylistDetailHeaderRenderer?,
        val gridRenderer: GridRenderer?,
    ) {
        @Serializable
        data class MusicResponsiveHeaderRenderer(
            val description: Description?,
            val straplineTextOne: StraplineTextOne?,
            val straplineThumbnail: StraplineThumbnail?,
            val subtitle: MusicShelfRenderer.Content.MusicMultiRowListItemRenderer.Subtitle?,
            val thumbnail: ThumbnailRenderer?,
            val title: Title?,
            val secondSubtitle: MusicShelfRenderer.Content.MusicMultiRowListItemRenderer.Subtitle?,
            val facepile: Facepile?,
            val buttons: List<Button>?,
        ) {
            @Serializable
            data class Facepile(
                val avatarStackViewModel: AvatarStackViewModel?,
            ) {
                @Serializable
                data class AvatarStackViewModel(
                    val avatars: List<Avatar>?,
                    val rendererContext: RendererContext?,
                    val text: Text?,
                ) {
                    @Serializable
                    data class Text(
                        val content: String?,
                    )

                    @Serializable
                    data class RendererContext(
                        val commandContext: CommandContext?,
                    ) {
                        @Serializable
                        data class CommandContext(
                            val onTap: OnTap?,
                        ) {
                            @Serializable
                            data class OnTap(
                                val innertubeCommand: InnertubeCommand?,
                            ) {
                                @Serializable
                                data class InnertubeCommand(
                                    val browseEndpoint: BrowseEndpoint?,
                                )
                            }
                        }
                    }

                    @Serializable
                    data class Avatar(
                        val avatarViewModel: AvatarViewModel?,
                    ) {
                        @Serializable
                        data class AvatarViewModel(
                            val image: Image?,
                        ) {
                            @Serializable
                            data class Image(
                                val sources: List<Source>?,
                            ) {
                                @Serializable
                                data class Source(
                                    val url: String?,
                                )
                            }
                        }
                    }
                }
            }

            @Serializable
            data class Description(
                val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?,
            )

            @Serializable
            data class StraplineTextOne(
                val runs: List<Run>?,
            )

            @Serializable
            data class StraplineThumbnail(
                val musicThumbnailRenderer: ThumbnailRenderer.MusicThumbnailRenderer?,
            )

            @Serializable
            data class Title(
                val runs: List<Run>?,
            )
        }
    }
}