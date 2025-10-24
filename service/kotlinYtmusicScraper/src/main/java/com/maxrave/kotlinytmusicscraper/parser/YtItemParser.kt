package com.maxrave.kotlinytmusicscraper.parser

import com.maxrave.kotlinytmusicscraper.models.Badges
import com.maxrave.kotlinytmusicscraper.models.SongItem

fun List<Badges>.toSongBadges(): List<SongItem.SongBadges> =
    this.mapNotNull {
        when (it.musicInlineBadgeRenderer?.icon?.iconType) {
            "MUSIC_EXPLICIT_BADGE" -> SongItem.SongBadges.Explicit
            else -> null
        }
    }

fun String?.toDurationSeconds(): Int =
    this
        ?.let {
            if (it.contains(":")) {
                it.split(":")
            } else if (it.contains(".")) {
                it.split(".")
            } else {
                listOf(it)
            }
        }?.let { it[0].toInt() * 60 + it[1].toInt() } ?: 0