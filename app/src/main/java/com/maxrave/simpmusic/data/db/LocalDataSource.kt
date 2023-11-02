package com.maxrave.simpmusic.data.db

import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.FormatEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.LyricsEntity
import com.maxrave.simpmusic.data.db.entities.PairSongLocalPlaylist
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.QueueEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SetVideoIdEntity
import com.maxrave.simpmusic.data.db.entities.SongEntity
import java.time.LocalDateTime
import javax.inject.Inject

class LocalDataSource @Inject constructor(private val databaseDao: DatabaseDao) {
    suspend fun getAllRecentData() = databaseDao.getAllRecentData()
    suspend fun getAllDownloadedPlaylist() = databaseDao.getAllDownloadedPlaylist()

    suspend fun getSearchHistory() = databaseDao.getSearchHistory()

    suspend fun deleteSearchHistory() = databaseDao.deleteSearchHistory()

    suspend fun insertSearchHistory(searchHistory: SearchHistory) = databaseDao.insertSearchHistory(searchHistory)

    suspend fun getAllSongs() = databaseDao.getAllSongs()
    suspend fun getRecentSongs(limit: Int, offset: Int) = databaseDao.getRecentSongs(limit, offset)
    suspend fun getSongByListVideoId(primaryKeyList: List<String>) = databaseDao.getSongByListVideoId(primaryKeyList)
    suspend fun getDownloadedSongs() = databaseDao.getDownloadedSongs()
    suspend fun getDownloadingSongs() = databaseDao.getDownloadingSongs()
    suspend fun getLikedSongs() = databaseDao.getLikedSongs()
    suspend fun getLibrarySongs() = databaseDao.getLibrarySongs()
    suspend fun getSong(videoId: String) = databaseDao.getSong(videoId)
    suspend fun insertSong(song: SongEntity) = databaseDao.insertSong(song)
    suspend fun updateListenCount(videoId: String) = databaseDao.updateTotalPlayTime(videoId)
    suspend fun updateLiked(liked: Int, videoId: String) = databaseDao.updateLiked(liked, videoId)
    suspend fun updateDurationSeconds(durationSeconds: Int, videoId: String) = databaseDao.updateDurationSeconds(durationSeconds, videoId)
    suspend fun updateSongInLibrary(inLibrary: LocalDateTime, videoId: String) = databaseDao.updateSongInLibrary(inLibrary, videoId)
    suspend fun getMostPlayedSongs() = databaseDao.getMostPlayedSongs()
    suspend fun updateDownloadState(downloadState: Int, videoId: String) = databaseDao.updateDownloadState(downloadState, videoId)

    suspend fun getAllArtists() = databaseDao.getAllArtists()
    suspend fun insertArtist(artist: ArtistEntity) = databaseDao.insertArtist(artist)
    suspend fun updateFollowed(followed: Int, channelId: String) = databaseDao.updateFollowed(followed, channelId)
    suspend fun getArtist(channelId: String) = databaseDao.getArtist(channelId)
    suspend fun getFollowedArtists() = databaseDao.getFollowedArtists()
    suspend fun updateArtistInLibrary(inLibrary: LocalDateTime, channelId: String) = databaseDao.updateArtistInLibrary(inLibrary, channelId)

    suspend fun getAllAlbums() = databaseDao.getAllAlbums()
    suspend fun insertAlbum(album: AlbumEntity) = databaseDao.insertAlbum(album)
    suspend fun updateAlbumLiked(liked: Int, albumId: String) = databaseDao.updateAlbumLiked(liked, albumId)
    suspend fun getAlbum(albumId: String) = databaseDao.getAlbum(albumId)
    suspend fun getLikedAlbums() = databaseDao.getLikedAlbums()
    suspend fun updateAlbumInLibrary(inLibrary: LocalDateTime, albumId: String) = databaseDao.updateAlbumInLibrary(inLibrary, albumId)
    suspend fun updateAlbumDownloadState(downloadState: Int, albumId: String) = databaseDao.updateAlbumDownloadState(downloadState, albumId)

    suspend fun getAllPlaylists() = databaseDao.getAllPlaylists()
    suspend fun insertPlaylist(playlist: PlaylistEntity) = databaseDao.insertPlaylist(playlist)
    suspend fun insertRadioPlaylist(playlist: PlaylistEntity) = databaseDao.insertRadioPlaylist(playlist)
    suspend fun updatePlaylistLiked(liked: Int, playlistId: String) = databaseDao.updatePlaylistLiked(liked, playlistId)
    suspend fun getPlaylist(playlistId: String) = databaseDao.getPlaylist(playlistId)
    suspend fun getLikedPlaylists() = databaseDao.getLikedPlaylists()
    suspend fun updatePlaylistInLibrary(inLibrary: LocalDateTime, playlistId: String) = databaseDao.updatePlaylistInLibrary(inLibrary, playlistId)
    suspend fun updatePlaylistDownloadState(downloadState: Int, playlistId: String) = databaseDao.updatePlaylistDownloadState(downloadState, playlistId)

    suspend fun getAllLocalPlaylists() = databaseDao.getAllLocalPlaylists()
    suspend fun getLocalPlaylist(id: Long) = databaseDao.getLocalPlaylist(id)
    suspend fun insertLocalPlaylist(localPlaylist: LocalPlaylistEntity) = databaseDao.insertLocalPlaylist(localPlaylist)
    suspend fun deleteLocalPlaylist(id: Long) = databaseDao.deleteLocalPlaylist(id)
    suspend fun updateLocalPlaylistTitle(title: String, id: Long) = databaseDao.updateLocalPlaylistTitle(title, id)
    suspend fun updateLocalPlaylistThumbnail(thumbnail: String, id: Long) = databaseDao.updateLocalPlaylistThumbnail(thumbnail, id)
    suspend fun updateLocalPlaylistTracks(tracks: List<String>, id: Long) = databaseDao.updateLocalPlaylistTracks(tracks, id)
    suspend fun updateLocalPlaylistInLibrary(inLibrary: LocalDateTime, id: Long) = databaseDao.updateLocalPlaylistInLibrary(inLibrary, id)
    suspend fun updateLocalPlaylistDownloadState(downloadState: Int, id: Long) = databaseDao.updateLocalPlaylistDownloadState(downloadState, id)
    suspend fun getDownloadedLocalPlaylists() = databaseDao.getDownloadedLocalPlaylists()
    suspend fun updateLocalPlaylistYouTubePlaylistId(id: Long, ytId: String?) = databaseDao.updateLocalPlaylistYouTubePlaylistId(id, ytId)
    suspend fun updateLocalPlaylistYouTubePlaylistSynced(id: Long, synced: Int) = databaseDao.updateLocalPlaylistYouTubePlaylistSynced(id, synced)
    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(id: Long, syncState: Int) = databaseDao.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)

    suspend fun getSavedLyrics(videoId: String) = databaseDao.getLyrics(videoId)
    suspend fun insertLyrics(lyrics: LyricsEntity) = databaseDao.insertLyrics(lyrics)
    suspend fun getPreparingSongs() = databaseDao.getPreparingSongs()

    suspend fun insertFormat(format: FormatEntity) = databaseDao.insertFormat(format)
    suspend fun getFormat(videoId: String) = databaseDao.getFormat(videoId)

    suspend fun recoverQueue(queueEntity: QueueEntity) = databaseDao.recoverQueue(queueEntity)
    suspend fun getQueue() = databaseDao.getQueue()
    suspend fun deleteQueue() = databaseDao.deleteQueue()

    suspend fun getLocalPlaylistByYoutubePlaylistId(playlistId: String) = databaseDao.getLocalPlaylistByYoutubePlaylistId(playlistId)

    suspend fun insertSetVideoId(setVideoIdEntity: SetVideoIdEntity) = databaseDao.insertSetVideoId(setVideoIdEntity)
    suspend fun getSetVideoId(videoId: String) = databaseDao.getSetVideoId(videoId)

    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) = databaseDao.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
    suspend fun getPlaylistPairSong(playlistId: Long) = databaseDao.getPlaylistPairSong(playlistId)
    suspend fun deletePairSongLocalPlaylist(playlistId: Long, videoId: String) = databaseDao.deletePairSongLocalPlaylist(playlistId, videoId)
}