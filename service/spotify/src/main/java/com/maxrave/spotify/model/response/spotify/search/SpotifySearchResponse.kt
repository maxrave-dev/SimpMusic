package com.maxrave.spotify.model.response.spotify.search

import kotlinx.serialization.Serializable

@Serializable
data class SpotifySearchResponse(
    val data: Data? = null,
) {
    @Serializable
    data class Data(
        val searchV2: Search,
    ) {
        @Serializable
        data class Search(
            val query: String? = null,
            val tracksV2: TracksV2? = null,
        ) {
            @Serializable
            data class TracksV2(
                val items: List<Items>? = null,
                val pagingInfo: PagingInfo? = null,
                val totalCount: Int? = null,
            ) {
                @Serializable
                data class PagingInfo(
                    val limit: Int? = null,
                    val nextOffset: Int? = null,
                )

                @Serializable
                data class Items(
                    val item: Item? = null,
                ) {
                    @Serializable
                    data class Item(
                        val data: DataX? = null,
                    ) {
                        @Serializable
                        data class DataX(
                            val id: String? = null,
                            val name: String? = null,
                            val artists: Artists? = null,
                            val duration: Duration? = null,
                        ) {
                            @Serializable
                            data class Duration(
                                val totalMilliseconds: Int? = null,
                            )

                            @Serializable
                            data class Artists(
                                val items: List<ItemX>? = null,
                            ) {
                                @Serializable
                                data class ItemX(
                                    val profile: Profile? = null,
                                    val uri: String? = null,
                                ) {
                                    @Serializable
                                    data class Profile(
                                        val name: String? = null,
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