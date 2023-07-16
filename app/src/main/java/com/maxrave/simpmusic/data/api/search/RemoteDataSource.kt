package com.maxrave.simpmusic.data.api.search

import android.provider.MediaStore.Video
import com.maxrave.simpmusic.common.Config
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
import com.maxrave.simpmusic.data.model.thumbnailUrl
import retrofit2.Response
import javax.inject.Inject

class RemoteDataSource @Inject constructor(private val searchService: SearchService) {
    suspend fun getThumbnails(songId: String): Response<ArrayList<thumbnailUrl>> = searchService.getThumbnails(songId)
    suspend fun searchAll(query: String): Response<ArrayList<Any>> = searchService.searchAll(query, Config.VN)

    suspend fun searchSongs(
        query: String,
        filter: String = "songs"
    ): Response<ArrayList<SongsResult>> = searchService.searchSongs(query, filter, Config.VN)

    suspend fun searchArtists(
        query: String,
        filter: String = "artists",
    ): Response<ArrayList<ArtistsResult>> = searchService.searchArtists(query, filter, Config.VN)

    suspend fun searchAlbums(
        query: String,
        filter: String = "albums",
    ): Response<ArrayList<AlbumsResult>> = searchService.searchAlbums(query, filter, Config.VN)

    suspend fun searchPlaylists(
        query: String,
        filter: String = "playlists",
    ): Response<ArrayList<PlaylistsResult>> = searchService.searchPlaylists(query, filter, Config.VN)

    suspend fun searchVideos(
        query: String,
        filter: String = "videos",
    ): Response<ArrayList<VideosResult>> = searchService.searchVideos(query, filter, Config.VN)

    suspend fun suggestQuery(query: String): Response<ArrayList<String>> = searchService.suggestQuery(query)

    suspend fun getHome(): Response<ArrayList<homeItem>> = searchService.getHome(Config.VN)

    suspend fun exploreMood(): Response<Mood> = searchService.exploreMood(Config.VN)
    suspend fun getMood(params: String): Response<MoodsMomentObject> = searchService.getMood(params, Config.VN)
    suspend fun getGenre(params: String): Response<GenreObject> = searchService.getGenre(params, Config.VN)

    suspend fun browseArtist(channelId: String): Response<ArtistBrowse> = searchService.browseArtist(channelId, Config.VN)

    suspend fun browseAlbum(browseId: String): Response<AlbumBrowse> = searchService.browseAlbum(browseId, Config.VN)

    suspend fun browsePlaylist(id: String): Response<PlaylistBrowse> = searchService.browsePlaylist(id, Config.VN)

    suspend fun exploreChart(regionCode: String): Response<Chart> = searchService.exploreChart(regionCode, Config.VN)

    suspend fun getMetadata(videoId: String): Response<MetadataSong> = searchService.getMetadata(videoId, Config.VN)
    suspend fun getLyrics(query: String): Response<Lyrics> = searchService.getLyrics(query, Config.VN)

    suspend fun getRelated(videoId: String): Response<ArrayList<Track>> = searchService.songsRelated(videoId, Config.VN)
    suspend fun getVideoRelated(videoId: String): Response<ArrayList<VideosResult>> = searchService.videosRelated(videoId, Config.VN)

}