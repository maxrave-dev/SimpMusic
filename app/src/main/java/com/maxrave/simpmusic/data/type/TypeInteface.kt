package com.maxrave.simpmusic.data.type

/**
 * This is file which define the type of multiple data in interface type.
 * Eg: Combine all playlist data class to an interface, Combine all content data class for Home screen to an interface, etc
 */
interface HomeContentType

/**
 * All item can be used in Library screen
 * - Playlist
 * - Local Playlist
 * - Favorite Playlist
 * - Downloaded Playlist
 * - Recently Added: Song, Album, Artist, Playlist
 */
interface LibraryType

/**
 * I created this Type, may be we read this code, we will not understand why PlaylistType is HomeContentType
 * HomeContentType is used in Home screen, btw PlaylistType also used in Home screen, but PlaylistType is specific type for Playlist only
 * In Library Screen, I reused Home Content Composable, so the PlaylistType is inherited from HomeContentType
 */
interface PlaylistType :
    HomeContentType,
    LibraryType {
    enum class Type {
        YOUTUBE_PLAYLIST,
        RADIO,
        LOCAL,
        ALBUM,
        PODCAST
    }

    fun playlistType(): Type
}

interface ArtistType

interface RecentlyType : LibraryType {
    enum class Type {
        SONG,
        ALBUM,
        ARTIST,
        PLAYLIST,
    }

    fun objectType(): Type
}