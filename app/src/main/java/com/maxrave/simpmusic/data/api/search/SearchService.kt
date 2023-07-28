package com.maxrave.simpmusic.data.api.search

import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.artist.ArtistBrowse
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.explore.mood.genre.GenreObject
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.MoodsMomentObject
import com.maxrave.simpmusic.data.model.home.homeItem
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.data.model.streams.Streams
import com.maxrave.simpmusic.data.model.thumbnailUrl
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchService {
    //get_thumbnails
    @GET("thumbnails")
    suspend fun getThumbnails(@Query("songId") songId: String): Response<ArrayList<thumbnailUrl>>
    //song
    @GET("song")
    suspend fun getSong(@Query("videoId") videoId: String): Response<ArrayList<Streams>>
    //search
    @GET("search")
    suspend fun searchAll(@Query("q") query: String, @Query("r") region: String): Response<ArrayList<Any>>
    @GET("search")
    suspend fun searchSongs(@Query("q") query: String, @Query("f") filter: String = "songs", @Query("r") region: String): Response<ArrayList<SongsResult>>
    @GET("search")
    suspend fun searchArtists(@Query("q") query: String, @Query("f") filter: String = "artists", @Query("r") region: String): Response<ArrayList<ArtistsResult>>
    @GET("search")
    suspend fun searchAlbums(@Query("q") query: String, @Query("f") filter: String = "albums", @Query("r") region: String): Response<ArrayList<AlbumsResult>>
    @GET("search")
    suspend fun searchPlaylists(@Query("q") query: String, @Query("f") filter: String = "playlists", @Query("r") region: String): Response<ArrayList<PlaylistsResult>>
    @GET("search")
    suspend fun searchVideos(@Query("q") query: String, @Query("f") filter: String = "videos", @Query("r") region: String): Response<ArrayList<VideosResult>>

    //suggest query
    @GET("query")
    suspend fun suggestQuery(@Query("q") query: String): Response<ArrayList<String>>
    //songs related
    @GET("songs/related")
    suspend fun songsRelated(@Query("videoId") videoId: String, @Query("r") region: String): Response<ArrayList<Track>>
    @GET("videos/related")
    suspend fun videosRelated(@Query("videoId") videoId: String, @Query("r") region: String): Response<ArrayList<VideosResult>>

    //getHome
    @GET("home")
    suspend fun getHome(@Query("r") region: String): Response<ArrayList<homeItem>>

    //exploreMood
    @GET("explore/mood/title")
    suspend fun exploreMood(@Query("r") region: String): Response<Mood>
    @GET("explore/mood")
    suspend fun getMood(@Query("p") params: String, @Query("r") region: String): Response<MoodsMomentObject>
    @GET("explore/genre")
    suspend fun getGenre(@Query("p") params: String, @Query("r") region: String): Response<GenreObject>

    //Chart
    @GET("explore/charts")
    suspend fun exploreChart(@Query("cc") regionCode: String): Response<Chart>

    //browse
    //Artist
    @GET("browse/artists")
    suspend fun browseArtist(@Query("channelId") channelId: String, @Query("r") region: String): Response<ArtistBrowse>
    //Artist Album
    @GET("browse/artists")
    suspend fun browseArtistAlbum(@Query("channelId") channelId: String, @Query("params") params: String, @Query("r") region: String)
    //Album
    @GET("browse/albums")
    suspend fun browseAlbum(@Query("browseId") browseId: String, @Query("r") region: String): Response<AlbumBrowse>
    //Playlist
    @GET("playlists")
    suspend fun browsePlaylist(@Query("id") id: String, @Query("r") region: String): Response<PlaylistBrowse>

    //getMetadata
    @GET("/songs/metadata")
    suspend fun getMetadata(@Query("videoId") videoId: String, @Query("r") region: String): Response<MetadataSong>
    //getOnlyLyrics
    @GET("/songs/lyrics")
    suspend fun getLyrics(@Query("q") query: String, @Query("r") region: String): Response<Lyrics>

}
