package com.maxrave.kotlinytmusicscraper.models

import kotlinx.serialization.Serializable

@Serializable
data class ReturnYouTubeDislikeResponse(
    val id: String? = null,
    val dateCreated: String? = null,
    val likes: Int? = null,
    val dislikes: Int? = null,
    val rating: Double? = null,
    val viewCount: Int? = null,
    val deleted: Boolean? = null,
)
//  "id": "WPb2j3GU8Kc",
//  "dateCreated": "2024-02-05T04:55:16.031426Z",
//  "likes": 314,
//  "dislikes": 2,
//  "rating": 4.974683544303797,
//  "viewCount": 109972,
//  "deleted": false