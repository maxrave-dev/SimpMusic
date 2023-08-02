package com.maxrave.simpmusic.data.api.search

import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.browse.artist.ArtistBrowse
import com.maxrave.simpmusic.data.model.browse.artist.ChannelId
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.explore.mood.genre.GenreObject
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.MoodsMomentObject
import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.model.home.homeItem
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.data.model.songfull.SongFull
import com.maxrave.simpmusic.data.model.streams.Streams
import com.maxrave.simpmusic.data.model.thumbnailUrl
import retrofit2.Response
import javax.inject.Inject

class RemoteDataSource @Inject constructor(private val searchService: SearchService, private val dataStoreManager: DataStoreManager) {
    suspend fun getSong(videoId: String): Response<ArrayList<Streams>> = searchService.getSong(videoId)
    suspend fun getSongFull(videoId: String): Response<SongFull> = searchService.getSongFull(videoId)

    suspend fun getThumbnails(songId: String): Response<ArrayList<thumbnailUrl>> = searchService.getThumbnails(songId)
    suspend fun searchAll(query: String, regionCode: String, language: String): Response<ArrayList<Any>> = searchService.searchAll(query, regionCode, language)

    suspend fun searchSongs(
        query: String,
        filter: String = "songs",
        regionCode: String,
        language: String
    ): Response<ArrayList<SongsResult>> = searchService.searchSongs(query, filter, regionCode, language)

    suspend fun searchArtists(
        query: String,
        filter: String = "artists",
        regionCode: String,
        language: String
    ): Response<ArrayList<ArtistsResult>> = searchService.searchArtists(query, filter, regionCode, language)

    suspend fun searchAlbums(
        query: String,
        filter: String = "albums",
        regionCode: String,
        language: String
    ): Response<ArrayList<AlbumsResult>> = searchService.searchAlbums(query, filter, regionCode, language)

    suspend fun searchPlaylists(
        query: String,
        filter: String = "playlists",
        regionCode: String,
        language: String
    ): Response<ArrayList<PlaylistsResult>> = searchService.searchPlaylists(query, filter, regionCode, language)

    suspend fun searchVideos(
        query: String,
        filter: String = "videos",
        regionCode: String,
        language: String
    ): Response<ArrayList<VideosResult>> = searchService.searchVideos(query, filter, regionCode, language)

    suspend fun suggestQuery(query: String): Response<ArrayList<String>> = searchService.suggestQuery(query)

    suspend fun getHome(regionCode: String, language: String): Response<ArrayList<homeItem>> = searchService.getHome(regionCode, language)

    suspend fun exploreMood(regionCode: String, language: String): Response<Mood> = searchService.exploreMood(regionCode, language)
    suspend fun getMood(params: String, regionCode: String, language: String): Response<MoodsMomentObject> = searchService.getMood(params, regionCode, language)
    suspend fun getGenre(params: String, regionCode: String, language: String): Response<GenreObject> = searchService.getGenre(params, regionCode, language)

    suspend fun browseArtist(channelId: String, regionCode: String, language: String): Response<ArtistBrowse> = searchService.browseArtist(channelId, regionCode, language)

    suspend fun browseAlbum(browseId: String, regionCode: String, language: String): Response<AlbumBrowse> = searchService.browseAlbum(browseId, regionCode, language)

    suspend fun browsePlaylist(id: String, regionCode: String, language: String): Response<PlaylistBrowse> = searchService.browsePlaylist(id, regionCode, language)

    suspend fun exploreChart(regionCode: String, language: String): Response<Chart> = searchService.exploreChart(regionCode, language)

    suspend fun getMetadata(videoId: String, regionCode: String, language: String): Response<MetadataSong> = searchService.getMetadata(videoId, regionCode, language)
    suspend fun getLyrics(query: String, regionCode: String = Config.VN): Response<Lyrics> = searchService.getLyrics(query, regionCode)

    suspend fun getRelated(videoId: String, regionCode: String, language: String): Response<ArrayList<Track>> = searchService.songsRelated(videoId, regionCode, language)
    suspend fun getVideoRelated(videoId: String, regionCode: String, language: String): Response<ArrayList<VideosResult>> = searchService.videosRelated(videoId, regionCode, language)

    suspend fun convertNameToId(name: String): Response<ChannelId> = searchService.convertNameToId(name)
}