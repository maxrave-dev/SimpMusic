package com.maxrave.simpmusic.data.repository

import com.maxrave.simpmusic.data.model.home.chart.Chart
import com.maxrave.simpmusic.data.api.BaseApiResponse
import com.maxrave.simpmusic.data.api.search.RemoteDataSource
import com.maxrave.simpmusic.data.db.LocalDataSource
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.AlbumBrowse
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.model.explore.mood.Mood
import com.maxrave.simpmusic.data.model.home.homeItem
import com.maxrave.simpmusic.data.model.searchResult.albums.AlbumsResult
import com.maxrave.simpmusic.data.model.searchResult.artists.ArtistsResult
import com.maxrave.simpmusic.data.model.searchResult.playlists.PlaylistsResult
import com.maxrave.simpmusic.data.model.searchResult.songs.SongsResult
import com.maxrave.simpmusic.data.model.browse.artist.ArtistBrowse
import com.maxrave.simpmusic.data.model.browse.playlist.PlaylistBrowse
import com.maxrave.simpmusic.data.model.explore.mood.genre.GenreObject
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.MoodsMomentObject
import com.maxrave.simpmusic.data.model.metadata.Lyrics
import com.maxrave.simpmusic.data.model.metadata.MetadataSong
import com.maxrave.simpmusic.data.model.searchResult.videos.VideosResult
import com.maxrave.simpmusic.data.model.thumbnailUrl
import com.maxrave.simpmusic.utils.Resource
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@ActivityRetainedScoped
class MainRepository @Inject constructor(private val remoteDataSource: RemoteDataSource, private val localDataSource: LocalDataSource): BaseApiResponse() {
    suspend fun getThumbnails(songId: String): Flow<Resource<ArrayList<thumbnailUrl>>> = flow { emit(safeApiCall { remoteDataSource.getThumbnails(songId) }) }.flowOn(Dispatchers.IO)
    //search
    suspend fun searchAll(query: String) = remoteDataSource.searchAll(query)
    suspend fun searchSongs(query: String, filter: String = "songs"): Flow<Resource<ArrayList<SongsResult>>> = flow<Resource<ArrayList<SongsResult>>> { emit(safeApiCall { remoteDataSource.searchSongs(query, filter) }) }.flowOn(Dispatchers.IO)
    suspend fun searchArtists(query: String, filter: String = "artists"): Flow<Resource<ArrayList<ArtistsResult>>> = flow<Resource<ArrayList<ArtistsResult>>> { emit(safeApiCall { remoteDataSource.searchArtists(query, filter) }) }.flowOn(Dispatchers.IO)
    suspend fun searchAlbums(query: String, filter: String = "albums"): Flow<Resource<ArrayList<AlbumsResult>>> = flow<Resource<ArrayList<AlbumsResult>>> { emit(safeApiCall { remoteDataSource.searchAlbums(query, filter) }) }.flowOn(Dispatchers.IO)
    suspend fun searchPlaylists(query: String, filter: String = "playlists"): Flow<Resource<ArrayList<PlaylistsResult>>> = flow<Resource<ArrayList<PlaylistsResult>>> { emit(safeApiCall { remoteDataSource.searchPlaylists(query, filter) }) }.flowOn(Dispatchers.IO)
    suspend fun searchVideos(query: String, filter: String = "videos"): Flow<Resource<ArrayList<VideosResult>>> = flow<Resource<ArrayList<VideosResult>>> { emit(safeApiCall { remoteDataSource.searchVideos(query, filter) }) }.flowOn(Dispatchers.IO)

    //suggest query
    suspend fun suggestQuery(query: String): Flow<Resource<ArrayList<String>>> = flow<Resource<ArrayList<String>>> { emit(safeApiCall { remoteDataSource.suggestQuery(query) }) }.flowOn(Dispatchers.IO)

    //getHome
    suspend fun getHome() : Flow<Resource<ArrayList<homeItem>>> =  flow<Resource<ArrayList<homeItem>>>{emit(safeApiCall { remoteDataSource.getHome() })  }.flowOn(Dispatchers.IO)

    //exploreMood
    suspend fun exploreMood(): Flow<Resource<Mood>> = flow<Resource<Mood>> { emit(safeApiCall { remoteDataSource.exploreMood() }) }.flowOn(Dispatchers.IO)
    suspend fun getMood(params: String): Flow<Resource<MoodsMomentObject>> = flow<Resource<MoodsMomentObject>> { emit(safeApiCall { remoteDataSource.getMood(params) }) }.flowOn(Dispatchers.IO)
    suspend fun getGenre(params: String): Flow<Resource<GenreObject>> = flow<Resource<GenreObject>> { emit(safeApiCall { remoteDataSource.getGenre(params) }) }.flowOn(Dispatchers.IO)

    //browse
    //artist
    suspend fun browseArtist(channelId: String): Flow<Resource<ArtistBrowse>> = flow<Resource<ArtistBrowse>> { emit(safeApiCall { remoteDataSource.browseArtist(channelId) }) }.flowOn(Dispatchers.IO)
    //album
    suspend fun browseAlbum(browseId: String): Flow<Resource<AlbumBrowse>> = flow<Resource<AlbumBrowse>> { emit(safeApiCall { remoteDataSource.browseAlbum(browseId) }) }.flowOn(Dispatchers.IO)
    //playlist
    suspend fun browsePlaylist(id: String): Flow<Resource<PlaylistBrowse>> = flow<Resource<PlaylistBrowse>> { emit(safeApiCall { remoteDataSource.browsePlaylist(id) }) }.flowOn(Dispatchers.IO)
    //chart
    suspend fun exploreChart(regionCode: String): Flow<Resource<Chart>> = flow<Resource<Chart>> { emit(safeApiCall { remoteDataSource.exploreChart(regionCode) }) }.flowOn(Dispatchers.IO)
    //metadata
    suspend fun getMetadata(videoId: String): Flow<Resource<MetadataSong>> = flow<Resource<MetadataSong>> { emit(safeApiCall { remoteDataSource.getMetadata(videoId) }) }.flowOn(Dispatchers.IO)
    suspend fun getLyrics(query: String): Flow<Resource<Lyrics>> = flow<Resource<Lyrics>> { emit(safeApiCall { remoteDataSource.getLyrics(query) }) }.flowOn(Dispatchers.IO)
    //related
    suspend fun getRelated(videoId: String): Flow<Resource<ArrayList<Track>>> = flow<Resource<ArrayList<Track>>> { emit(safeApiCall { remoteDataSource.getRelated(videoId) }) }.flowOn(Dispatchers.IO)
    suspend fun getVideoRelated(videoId: String): Flow<Resource<ArrayList<VideosResult>>> = flow<Resource<ArrayList<VideosResult>>> { emit(safeApiCall { remoteDataSource.getVideoRelated(videoId) }) }.flowOn(Dispatchers.IO)


    //Database
    suspend fun getSearchHistory(): Flow<List<SearchHistory>> = flow { emit(localDataSource.getSearchHistory()) }.flowOn(Dispatchers.IO)
    suspend fun insertSearchHistory(searchHistory: SearchHistory) = withContext(Dispatchers.IO) { localDataSource.insertSearchHistory(searchHistory) }
    suspend fun deleteSearchHistory() = withContext(Dispatchers.IO) { localDataSource.deleteSearchHistory() }

    suspend fun getAllSongs(): Flow<List<SongEntity>> = flow { emit(localDataSource.getAllSongs()) }.flowOn(Dispatchers.IO)
    suspend fun getSongsByListVideoId(listVideoId: List<String>): Flow<List<SongEntity>> = flow { emit(localDataSource.getSongByListVideoId(listVideoId)) }.flowOn(Dispatchers.IO)
    suspend fun getLikedSongs(): Flow<List<SongEntity>> = flow { emit(localDataSource.getLikedSongs()) }.flowOn(Dispatchers.IO)
    suspend fun getLibrarySongs(): Flow<List<SongEntity>> = flow { emit(localDataSource.getLibrarySongs()) }.flowOn(Dispatchers.IO)
    suspend fun getSongById(id: String): Flow<SongEntity> = flow { emit(localDataSource.getSong(id)) }.flowOn(Dispatchers.IO)
    suspend fun insertSong(songEntity: SongEntity) = withContext(Dispatchers.IO) { localDataSource.insertSong(songEntity) }
    suspend fun updateListenCount(videoId: String) = withContext(Dispatchers.IO) { localDataSource.updateListenCount(videoId) }
    suspend fun updateLikeStatus(videoId: String, likeStatus: Int) = withContext(Dispatchers.Main) { localDataSource.updateLiked(likeStatus, videoId) }
    suspend fun updateSongInLibrary(inLibrary: LocalDateTime, videoId: String) = withContext(Dispatchers.Main) { localDataSource.updateSongInLibrary(inLibrary, videoId) }
    suspend fun getMostPlayedSongs(): Flow<List<SongEntity>> = flow { emit(localDataSource.getMostPlayedSongs()) }.flowOn(Dispatchers.IO)
    suspend fun updateDownloadState(videoId: String, downloadState: Int) = withContext(Dispatchers.Main) { localDataSource.updateDownloadState(downloadState, videoId) }

    suspend fun getAllArtists(): Flow<List<ArtistEntity>> = flow { emit(localDataSource.getAllArtists()) }.flowOn(Dispatchers.IO)
    suspend fun getArtistById(id: String): Flow<ArtistEntity> = flow { emit(localDataSource.getArtist(id)) }.flowOn(Dispatchers.IO)
    suspend fun insertArtist(artistEntity: ArtistEntity) = withContext(Dispatchers.IO) { localDataSource.insertArtist(artistEntity) }
    suspend fun updateFollowedStatus(channelId: String, followedStatus: Int) = withContext(Dispatchers.Main) { localDataSource.updateFollowed(followedStatus, channelId) }
    suspend fun getFollowedArtists(): Flow<List<ArtistEntity>> = flow { emit(localDataSource.getFollowedArtists()) }.flowOn(Dispatchers.IO)
    suspend fun updateArtistInLibrary(inLibrary: LocalDateTime, channelId: String) = withContext(Dispatchers.Main) { localDataSource.updateArtistInLibrary(inLibrary, channelId) }

    suspend fun getAllAlbums(): Flow<List<AlbumEntity>> = flow { emit(localDataSource.getAllAlbums()) }.flowOn(Dispatchers.IO)
    suspend fun getAlbum(id: String): Flow<AlbumEntity> = flow { emit(localDataSource.getAlbum(id)) }.flowOn(Dispatchers.IO)
    suspend fun getLikedAlbums(): Flow<List<AlbumEntity>> = flow { emit(localDataSource.getLikedAlbums()) }.flowOn(Dispatchers.IO)
    suspend fun insertAlbum(albumEntity: AlbumEntity) = withContext(Dispatchers.IO) { localDataSource.insertAlbum(albumEntity) }
    suspend fun updateAlbumLiked(albumId: String, likeStatus: Int) = withContext(Dispatchers.Main) { localDataSource.updateAlbumLiked(likeStatus, albumId) }
    suspend fun updateAlbumInLibrary(inLibrary: LocalDateTime, albumId: String) = withContext(Dispatchers.Main) { localDataSource.updateAlbumInLibrary(inLibrary, albumId) }
    suspend fun updateAlbumDownloadState(albumId: String, downloadState: Int) = withContext(Dispatchers.Main) { localDataSource.updateAlbumDownloadState(downloadState, albumId) }

    suspend fun getAllPlaylists(): Flow<List<PlaylistEntity>> = flow { emit(localDataSource.getAllPlaylists()) }.flowOn(Dispatchers.IO)
    suspend fun getPlaylist(id: String): Flow<PlaylistEntity> = flow { emit(localDataSource.getPlaylist(id)) }.flowOn(Dispatchers.IO)
    suspend fun getLikedPlaylists(): Flow<List<PlaylistEntity>> = flow { emit(localDataSource.getLikedPlaylists()) }.flowOn(Dispatchers.IO)
    suspend fun insertPlaylist(playlistEntity: PlaylistEntity) = withContext(Dispatchers.IO) { localDataSource.insertPlaylist(playlistEntity) }
    suspend fun updatePlaylistLiked(playlistId: String, likeStatus: Int) = withContext(Dispatchers.Main) { localDataSource.updatePlaylistLiked(likeStatus, playlistId) }
    suspend fun updatePlaylistInLibrary(inLibrary: LocalDateTime, playlistId: String) = withContext(Dispatchers.Main) { localDataSource.updatePlaylistInLibrary(inLibrary, playlistId) }
    suspend fun updatePlaylistDownloadState(playlistId: String, downloadState: Int) = withContext(Dispatchers.Main) { localDataSource.updatePlaylistDownloadState(downloadState, playlistId) }

}