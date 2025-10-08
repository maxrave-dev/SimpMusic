package com.maxrave.kotlinytmusicscraper.parser

import com.maxrave.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import com.maxrave.kotlinytmusicscraper.models.MusicShelfRenderer
import com.maxrave.kotlinytmusicscraper.models.SongItem
import com.maxrave.kotlinytmusicscraper.models.WatchEndpoint
import com.maxrave.kotlinytmusicscraper.models.getContinuation
import com.maxrave.kotlinytmusicscraper.models.response.BrowseResponse
import com.maxrave.kotlinytmusicscraper.models.response.LikeStatus
import com.maxrave.logger.Logger

fun BrowseResponse.fromPlaylistToTrack(): List<SongItem> =
    (
        (
            this.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?: this.contents?.twoColumnBrowseResultsRenderer?.tabs
        )?.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicPlaylistShelfRenderer
            ?.contents
            ?: this.contents
                ?.twoColumnBrowseResultsRenderer
                ?.secondaryContents
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.musicPlaylistShelfRenderer
                ?.contents
    )?.mapNotNull { content ->
        content.toSongItem()
    } ?: emptyList()

fun BrowseResponse.fromPlaylistToTrackWithSetVideoId(): List<Pair<SongItem, String>> =
    (
        (
            this.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?: this.contents?.twoColumnBrowseResultsRenderer?.tabs
        )?.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicPlaylistShelfRenderer
            ?.contents
            ?: this.contents
                ?.twoColumnBrowseResultsRenderer
                ?.secondaryContents
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.musicPlaylistShelfRenderer
                ?.contents
    )?.mapNotNull { content ->
        Pair(
            content.toSongItem() ?: return@mapNotNull null,
            content.toPlaylistItemData()?.playlistSetVideoId ?: return@mapNotNull null,
        )
    } ?: emptyList()

fun BrowseResponse.fromPlaylistContinuationToTracks(): List<SongItem> =
    (
        this.continuationContents
            ?.musicPlaylistShelfContinuation
            ?.contents
            ?: this.continuationContents
                ?.sectionListContinuation
                ?.contents
                ?.firstOrNull()
                ?.musicShelfRenderer
                ?.contents
    )?.mapNotNull { contents ->
        contents.toSongItem()
    } ?: emptyList()

fun BrowseResponse.fromPlaylistContinuationToTrackWithSetVideoId(): List<Pair<SongItem, String>> =
    (
        this.continuationContents
            ?.musicPlaylistShelfContinuation
            ?.contents
            ?: this.continuationContents
                ?.sectionListContinuation
                ?.contents
                ?.firstOrNull()
                ?.musicShelfRenderer
                ?.contents
    )?.mapNotNull { contents ->
        Pair(
            contents.toSongItem() ?: return@mapNotNull null,
            contents.toPlaylistItemData()?.playlistSetVideoId ?: return@mapNotNull null,
        )
    } ?: emptyList()

fun BrowseResponse.getPlaylistContinuation(): String? =
    this.onResponseReceivedActions
        ?.firstOrNull()
        ?.appendContinuationItemsAction
        ?.continuationItems
        ?.lastOrNull()
        ?.continuationItemRenderer
        ?.continuationEndpoint
        ?.continuationCommand
        ?.token
        ?: this.contents
            ?.twoColumnBrowseResultsRenderer
            ?.secondaryContents
            ?.sectionListRenderer
            ?.contents
            ?.lastOrNull()
            ?.musicPlaylistShelfRenderer
            ?.contents
            ?.lastOrNull()
            ?.continuationItemRenderer
            ?.continuationEndpoint
            ?.continuationCommand
            ?.token
        ?: this.contents
            ?.singleColumnBrowseResultsRenderer
            ?.tabs
            ?.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.continuations
            ?.getContinuation()
        ?: this.contents
            ?.twoColumnBrowseResultsRenderer
            ?.secondaryContents
            ?.sectionListRenderer
            ?.continuations
            ?.getContinuation()
        ?: this.contents
            ?.singleColumnBrowseResultsRenderer
            ?.tabs
            ?.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicPlaylistShelfRenderer
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
        ?: this.contents
            ?.twoColumnBrowseResultsRenderer
            ?.secondaryContents
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicPlaylistShelfRenderer
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
        ?: this.contents
            ?.twoColumnBrowseResultsRenderer
            ?.secondaryContents
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicPlaylistShelfRenderer
            ?.contents
            ?.lastOrNull()
            ?.continuationItemRenderer
            ?.continuationEndpoint
            ?.continuationCommand
            ?.token
        ?: this.continuationContents
            ?.musicPlaylistShelfContinuation
            ?.continuations
            ?.getContinuation()

fun BrowseResponse.getContinuePlaylistContinuation(): String? =
    this.continuationContents
        ?.musicPlaylistShelfContinuation
        ?.continuations
        ?.getContinuation()

fun BrowseResponse.getPlaylistRadioEndpoint(): WatchEndpoint? {
    val header =
        this.header?.musicDetailHeaderRenderer
            ?: this.header
                ?.musicEditablePlaylistDetailHeaderRenderer
                ?.header
                ?.musicDetailHeaderRenderer
    if (header != null) {
        return header.menu.menuRenderer.items
            .find {
                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
            }?.menuNavigationItemRenderer
            ?.navigationEndpoint
            ?.watchPlaylistEndpoint
    } else {
        return this.contents
            ?.twoColumnBrowseResultsRenderer
            ?.tabs
            ?.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicEditablePlaylistDetailHeaderRenderer
            ?.header
            ?.musicResponsiveHeaderRenderer
            ?.buttons
            ?.lastOrNull()
            ?.menuRenderer
            ?.items
            ?.find {
                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
            }?.menuNavigationItemRenderer
            ?.navigationEndpoint
            ?.watchPlaylistEndpoint
            ?: this.contents
                ?.twoColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.musicResponsiveHeaderRenderer
                ?.buttons
                ?.findLast { it.menuRenderer != null }
                ?.menuRenderer
                ?.items
                ?.find {
                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                }?.menuNavigationItemRenderer
                ?.navigationEndpoint
                ?.watchPlaylistEndpoint
    }
}

fun BrowseResponse.getPlaylistShuffleEndpoint(): WatchEndpoint? {
    val header =
        this.header?.musicDetailHeaderRenderer
            ?: this.header
                ?.musicEditablePlaylistDetailHeaderRenderer
                ?.header
                ?.musicDetailHeaderRenderer
    if (header != null) {
        return header.menu.menuRenderer.topLevelButtons
            ?.firstOrNull()
            ?.buttonRenderer
            ?.navigationEndpoint
            ?.watchPlaylistEndpoint
    } else {
        return this.contents
            ?.twoColumnBrowseResultsRenderer
            ?.tabs
            ?.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.contents
            ?.firstOrNull()
            ?.musicEditablePlaylistDetailHeaderRenderer
            ?.header
            ?.musicResponsiveHeaderRenderer
            ?.buttons
            ?.lastOrNull()
            ?.menuRenderer
            ?.items
            ?.find {
                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
            }?.menuNavigationItemRenderer
            ?.navigationEndpoint
            ?.watchPlaylistEndpoint
            ?: this.contents
                ?.twoColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.musicResponsiveHeaderRenderer
                ?.buttons
                ?.findLast { it.menuRenderer != null }
                ?.menuRenderer
                ?.items
                ?.find {
                    it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                }?.menuNavigationItemRenderer
                ?.navigationEndpoint
                ?.watchPlaylistEndpoint
    }
}

fun MusicShelfRenderer.Content.toPlaylistItemData(): MusicResponsiveListItemRenderer.PlaylistItemData? =
    this.musicResponsiveListItemRenderer?.playlistItemData

fun MusicShelfRenderer.Content.toSongItem(): SongItem? {
    val flexColumns = this.musicResponsiveListItemRenderer?.flexColumns
    val fixedColumns = this.musicResponsiveListItemRenderer?.fixedColumns
    val menu = this.musicResponsiveListItemRenderer?.menu
    return SongItem(
        id =
            flexColumns
                ?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.firstOrNull()
                ?.navigationEndpoint
                ?.watchEndpoint
                ?.videoId ?: return null,
        title =
            flexColumns
                .firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.firstOrNull()
                ?.text ?: return null,
        artists =
            flexColumns
                .apply {
                    Logger.w(
                        "PlaylistParser",
                        "Artists: ${this.map {
                            it.musicResponsiveListItemFlexColumnRenderer.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                        }}",
                    )
                }.filter {
                    it.musicResponsiveListItemFlexColumnRenderer.isArtist()
                }.apply {
                    Logger.w(
                        "PlaylistParser",
                        "Artists after filter: ${this.map {
                            it.musicResponsiveListItemFlexColumnRenderer.text
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                        }}",
                    )
                }.mapNotNull { it.musicResponsiveListItemFlexColumnRenderer.toArtist() },
        album =
            flexColumns
                .find {
                    it.musicResponsiveListItemFlexColumnRenderer.isAlbum()
                }?.musicResponsiveListItemFlexColumnRenderer
                ?.toAlbum(),
        duration =
            fixedColumns
                ?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.firstOrNull()
                ?.text
                .toDurationSeconds(),
        thumbnail =
            this.musicResponsiveListItemRenderer
                ?.thumbnail
                ?.musicThumbnailRenderer
                ?.getThumbnailUrl() ?: "",
        endpoint =
            flexColumns
                .first()
                .musicResponsiveListItemFlexColumnRenderer
                .text
                ?.runs
                ?.firstOrNull()
                ?.navigationEndpoint
                ?.watchEndpoint,
        explicit =
            this.musicResponsiveListItemRenderer?.badges?.toSongBadges()?.contains(
                SongItem.SongBadges.Explicit,
            ) ?: false,
        thumbnails =
            this.musicResponsiveListItemRenderer
                ?.thumbnail
                ?.musicThumbnailRenderer
                ?.thumbnail,
        likeStatus =
            menu
                ?.menuRenderer
                ?.topLevelButtons
                ?.firstOrNull()
                ?.likeButtonRenderer
                ?.toLikeStatus()
                ?: LikeStatus.INDIFFERENT,
        badges = this.musicResponsiveListItemRenderer?.badges?.toSongBadges(),
    )
}

/**
 * Check if the browse response has reload params, if true, this has the suggestion tracks
 */
fun BrowseResponse.hasReloadParams(): Boolean =
    this.continuationContents
        ?.sectionListContinuation
        ?.contents
        ?.firstOrNull()
        ?.musicShelfRenderer
        ?.continuations
        ?.firstOrNull()
        ?.reloadContinuationData
        ?.continuation != null

fun BrowseResponse.getReloadParams(): String? =
    this.continuationContents
        ?.sectionListContinuation
        ?.contents
        ?.firstOrNull()
        ?.musicShelfRenderer
        ?.continuations
        ?.firstOrNull()
        ?.reloadContinuationData
        ?.continuation

fun BrowseResponse.getSuggestionSongItems(): List<SongItem> =
    if (hasReloadParams()) {
        this.continuationContents
            ?.sectionListContinuation
            ?.contents
            ?.firstOrNull()
            ?.musicShelfRenderer
            ?.contents
            ?.mapNotNull { content ->
                content.toSongItem()
            } ?: emptyList()
    } else {
        emptyList()
    }