package com.maxrave.domain.data.model.browse.album

import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.FeedbackTokens
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val album: Album?,
    val artists: List<Artist>?,
    val duration: String?,
    val durationSeconds: Int?,
    val isAvailable: Boolean,
    val isExplicit: Boolean,
    val likeStatus: String?,
    val thumbnails: List<Thumbnail>?,
    val title: String,
    val videoId: String,
    val videoType: String?,
    val category: String?,
    val feedbackTokens: FeedbackTokens?,
    val resultType: String?,
    val year: String? = null,
)