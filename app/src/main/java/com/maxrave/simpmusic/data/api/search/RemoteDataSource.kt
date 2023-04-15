package com.maxrave.simpmusic.data.api.search

import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.artist.ArtistBrowse
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.home.homeItem
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.thumbnailUrl
import retrofit2.Response
import javax.inject.Inject

class RemoteDataSource @Inject constructor(private val searchService: SearchService) {
    suspend fun getThumbnails(songId: String): Response<ArrayList<thumbnailUrl>> = searchService.getThumbnails(songId)
    suspend fun searchAll(query: String): Response<ArrayList<Any>> = searchService.searchAll(query)

    suspend fun searchSongs(
        query: String,
        filter: String = "songs"
    ): Response<ArrayList<SongsResult>> = searchService.searchSongs(query, filter = "songs")

    suspend fun searchArtists(
        query: String,
        filter: String = "artists",
    ): Response<ArrayList<ArtistsResult>> = searchService.searchArtists(query, filter = "artists")

    suspend fun searchAlbums(
        query: String,
        filter: String = "albums",
    ): Response<ArrayList<AlbumsResult>> = searchService.searchAlbums(query, filter = "albums")

    suspend fun searchPlaylists(
        query: String,
        filter: String = "playlists",
    ): Response<ArrayList<PlaylistsResult>> = searchService.searchPlaylists(query, filter = "playlists")

    suspend fun suggestQuery(query: String): Response<ArrayList<String>> = searchService.suggestQuery(query)

    suspend fun getHome(): Response<ArrayList<homeItem>> = searchService.getHome()

    suspend fun exploreMood(): Response<Mood> = searchService.exploreMood()

    suspend fun browseArtist(channelId: String): Response<ArtistBrowse> = searchService.browseArtist(channelId)

    suspend fun browseAlbum(browseId: String): Response<AlbumBrowse> = searchService.browseAlbum(browseId)

    suspend fun browsePlaylist(id: String): Response<PlaylistBrowse> = searchService.browsePlaylist(id)

    suspend fun exploreChart(regionCode: String): Response<Chart> = searchService.exploreChart(regionCode)
}