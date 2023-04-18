package com.maxrave.simpmusic.data.api.search

import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.artist.ArtistBrowse
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.MoodsMomentObject
import com.maxrave.simpmusic.data.model.home.homeItem
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.thumbnailUrl
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchService {
    //get_thumbnails
    @GET("thumbnails")
    suspend fun getThumbnails(@Query("songId") songId: String): Response<ArrayList<thumbnailUrl>>
    //search
    @GET("search")
    suspend fun searchAll(@Query("q") query: String): Response<ArrayList<Any>>
    @GET("search")
    suspend fun searchSongs(@Query("q") query: String, @Query("f") filter: String = "songs"): Response<ArrayList<SongsResult>>
    @GET("search")
    suspend fun searchArtists(@Query("q") query: String, @Query("f") filter: String = "artists"): Response<ArrayList<ArtistsResult>>
    @GET("search")
    suspend fun searchAlbums(@Query("q") query: String, @Query("f") filter: String = "albums"): Response<ArrayList<AlbumsResult>>
    @GET("search")
    suspend fun searchPlaylists(@Query("q") query: String, @Query("f") filter: String = "playlists"): Response<ArrayList<PlaylistsResult>>

    //suggest query
    @GET("query")
    suspend fun suggestQuery(@Query("q") query: String): Response<ArrayList<String>>

    //getHome
    @GET("home")
    suspend fun getHome(): Response<ArrayList<homeItem>>

    //exploreMood
    @GET("explore/mood/title")
    suspend fun exploreMood(): Response<Mood>
    @GET("explore/mood")
    suspend fun getMood(@Query("p") params: String): Response<MoodsMomentObject>

    //Chart
    @GET("explore/charts")
    suspend fun exploreChart(@Query("cc") regionCode: String): Response<Chart>

    //browse
    //Artist
    @GET("browse/artists")
    suspend fun browseArtist(@Query("channelId") channelId: String): Response<ArtistBrowse>
    //Artist Album
    @GET("browse/artists")
    suspend fun browseArtistAlbum(@Query("channelId") channelId: String, @Query("params") params: String)
    //Album
    @GET("browse/albums")
    suspend fun browseAlbum(@Query("browseId") browseId: String): Response<AlbumBrowse>
    //Playlist
    @GET("playlists")
    suspend fun browsePlaylist(@Query("id") id: String): Response<PlaylistBrowse>


}
