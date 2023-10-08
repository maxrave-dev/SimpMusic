package com.maxrave.kotlinytmusicscraper.models.youtube


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoDetails(
    @SerialName("allowRatings")
    val allowRatings: Boolean? = null,
    @SerialName("author")
    val author: String? = null,
    @SerialName("channelId")
    val channelId: String? = null,
    @SerialName("isCrawlable")
    val isCrawlable: Boolean? = null,
    @SerialName("isLiveContent")
    val isLiveContent: Boolean? = null,
    @SerialName("isOwnerViewing")
    val isOwnerViewing: Boolean? = null,
    @SerialName("isPrivate")
    val isPrivate: Boolean? = null,
    @SerialName("isUnpluggedCorpus")
    val isUnpluggedCorpus: Boolean? = null,
    @SerialName("keywords")
    val keywords: List<String>? = null,
    @SerialName("lengthSeconds")
    val lengthSeconds: String? = null,
    @SerialName("shortDescription")
    val shortDescription: String? = null,
    @SerialName("thumbnail")
    val thumbnail: Thumbnails? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("videoId")
    val videoId: String? = null,
    @SerialName("viewCount")
    val viewCount: String?  = null
)