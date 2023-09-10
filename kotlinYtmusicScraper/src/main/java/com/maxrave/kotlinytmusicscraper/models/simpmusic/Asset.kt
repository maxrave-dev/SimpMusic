package com.maxrave.kotlinytmusicscraper.models.simpmusic


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Asset(
    @SerialName("browser_download_url")
    val browserDownloadUrl: String?,
    @SerialName("content_type")
    val contentType: String?,
    @SerialName("created_at")
    val createdAt: String?,
    @SerialName("download_count")
    val downloadCount: Int?,
    @SerialName("id")
    val id: Int?,
    @SerialName("label")
    val label: String?,
    @SerialName("name")
    val name: String?,
    @SerialName("node_id")
    val nodeId: String?,
    @SerialName("size")
    val size: Int?,
    @SerialName("state")
    val state: String?,
    @SerialName("updated_at")
    val updatedAt: String?,
    @SerialName("uploader")
    val uploader: Uploader?,
    @SerialName("url")
    val url: String?
)