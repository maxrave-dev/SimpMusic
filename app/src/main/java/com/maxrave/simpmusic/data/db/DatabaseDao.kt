package com.maxrave.simpmusic.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.LocalPlaylistEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SongEntity
import java.time.LocalDateTime

@Dao
interface DatabaseDao {

    //Transaction request with multiple queries
    @Transaction
    suspend fun getAllRecentData(): List<Any> {
        val a = mutableListOf<Any>()
        a.addAll(getAllSongs())
        a.addAll(getAllArtists())
        a.addAll(getAllAlbums())
        a.addAll(getAllPlaylists())
        val sortedList = a.sortedWith<Any>(Comparator { p0, p1 ->
            val timeP0: LocalDateTime? = when (p0) {
                is SongEntity -> p0.inLibrary
                is ArtistEntity -> p0.inLibrary
                is AlbumEntity -> p0.inLibrary
                is PlaylistEntity -> p0.inLibrary
                else -> null
            }
            val timeP1: LocalDateTime? = when (p1) {
                is SongEntity -> p1.inLibrary
                is ArtistEntity -> p1.inLibrary
                is AlbumEntity -> p1.inLibrary
                is PlaylistEntity -> p1.inLibrary
                else -> null
            }
            if (timeP0 == null || timeP1 == null) {
                return@Comparator if (timeP0 == null && timeP1 == null) 0 else if (timeP0 == null) -1 else 1
            }
            timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
        })
        return sortedList.takeLast(20)
    }

    @Transaction
    suspend fun getAllDownloadedPlaylist(): List<Any> {
        val a = mutableListOf<Any>()
        a.addAll(getDownloadedAlbums())
        a.addAll(getDownloadedPlaylists())
        val sortedList = a.sortedWith<Any>(
            Comparator { p0, p1 ->
                val timeP0: LocalDateTime? = when (p0) {
                    is AlbumEntity -> p0.inLibrary
                    is PlaylistEntity -> p0.inLibrary
                    else -> null
                }
                val timeP1: LocalDateTime? = when (p1) {
                    is AlbumEntity -> p1.inLibrary
                    is PlaylistEntity -> p1.inLibrary
                    else -> null
                }
                if (timeP0 == null || timeP1 == null) {
                    return@Comparator if (timeP0 == null && timeP1 == null) 0 else if (timeP0 == null) -1 else 1
                }
                timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
            }
        )
        return sortedList
    }

    // Get search history
    @Query("SELECT * FROM search_history")
    suspend fun getSearchHistory(): List<SearchHistory>

    @Query("DELETE FROM search_history")
    suspend fun deleteSearchHistory()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(searchHistory: SearchHistory)

    //Song
    @Query("SELECT * FROM song ORDER BY inLibrary DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecentSongs(limit: Int, offset: Int): List<SongEntity>

    @Query("SELECT * FROM song")
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT * FROM song WHERE liked = 1")
    suspend fun getLikedSongs(): List<SongEntity>

    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL")
    suspend fun getLibrarySongs(): List<SongEntity>

    @Query("SELECT * FROM song WHERE videoId = :videoId")
    suspend fun getSong(videoId: String): SongEntity?

    @Query("UPDATE song SET totalPlayTime = totalPlayTime + 1 WHERE videoId = :videoId")
    suspend fun updateTotalPlayTime(videoId: String)

    @Query("UPDATE song SET inLibrary = :inLibrary WHERE videoId = :videoId")
    suspend fun updateSongInLibrary(inLibrary: LocalDateTime, videoId: String)

    @Query("UPDATE song SET liked = :liked WHERE videoId = :videoId")
    suspend fun updateLiked(liked: Int, videoId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: SongEntity)

    @Query("SELECT * FROM song WHERE totalPlayTime > 1 ORDER BY totalPlayTime DESC LIMIT 20")
    suspend fun getMostPlayedSongs(): List<SongEntity>

    @Query("UPDATE song SET downloadState = :downloadState WHERE videoId = :videoId")
    suspend fun updateDownloadState(downloadState: Int, videoId: String)

    @Query("SELECT * FROM song WHERE downloadState = 3")
    suspend fun getDownloadedSongs(): List<SongEntity>

    @Query("SELECT * FROM song WHERE videoId IN (:primaryKeyList)")
    fun getSongByListVideoId(primaryKeyList: List<String>): List<SongEntity>

    //Artist
    @Query("SELECT * FROM artist")
    suspend fun getAllArtists(): List<ArtistEntity>

    @Query("SELECT * FROM artist WHERE channelId = :channelId")
    suspend fun getArtist(channelId: String): ArtistEntity

    @Query("SELECT * FROM artist WHERE followed = 1")
    suspend fun getFollowedArtists(): List<ArtistEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArtist(artist: ArtistEntity)

    @Query("UPDATE artist SET followed = :followed WHERE channelId = :channelId")
    suspend fun updateFollowed(followed: Int, channelId: String)

    @Query("UPDATE artist SET inLibrary = :inLibrary WHERE channelId = :channelId")
    suspend fun updateArtistInLibrary(inLibrary: LocalDateTime, channelId: String)

    //Album
    @Query("SELECT * FROM album")
    suspend fun getAllAlbums(): List<AlbumEntity>

    @Query("SELECT * FROM album WHERE browseId = :browseId")
    suspend fun getAlbum(browseId: String): AlbumEntity

    @Query("SELECT * FROM album WHERE liked = 1")
    suspend fun getLikedAlbums(): List<AlbumEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Query("UPDATE album SET liked = :liked WHERE browseId = :browseId")
    suspend fun updateAlbumLiked(liked: Int, browseId: String)

    @Query("UPDATE album SET inLibrary = :inLibrary WHERE browseId = :browseId")
    suspend fun updateAlbumInLibrary(inLibrary: LocalDateTime, browseId: String)

    @Query("UPDATE album SET downloadState = :downloadState WHERE browseId = :browseId")
    suspend fun updateAlbumDownloadState(downloadState: Int, browseId: String)

    @Query("SELECT * FROM album WHERE downloadState = 3")
    suspend fun getDownloadedAlbums(): List<AlbumEntity>

    //Playlist
    @Query("SELECT * FROM playlist")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist WHERE id = :playlistId")
    suspend fun getPlaylist(playlistId: String): PlaylistEntity?

    @Query("SELECT * FROM playlist WHERE liked = 1")
    suspend fun getLikedPlaylists(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlist SET liked = :liked WHERE id = :playlistId")
    suspend fun updatePlaylistLiked(liked: Int, playlistId: String)

    @Query("UPDATE playlist SET inLibrary = :inLibrary WHERE id = :playlistId")
    suspend fun updatePlaylistInLibrary(inLibrary: LocalDateTime, playlistId: String)

    @Query("UPDATE playlist SET downloadState = :downloadState WHERE id = :playlistId")
    suspend fun updatePlaylistDownloadState(downloadState: Int, playlistId: String)

    @Query("SELECT * FROM playlist WHERE downloadState = 3")
    suspend fun getDownloadedPlaylists(): List<PlaylistEntity>

    //Local Playlist
    @Query("SELECT * FROM local_playlist")
    suspend fun getAllLocalPlaylists(): List<LocalPlaylistEntity>

    @Query("SELECT * FROM local_playlist WHERE id = :id")
    suspend fun getLocalPlaylist(id: Long): LocalPlaylistEntity

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocalPlaylist(localPlaylist: LocalPlaylistEntity)

    @Query("DELETE FROM local_playlist WHERE id = :id")
    suspend fun deleteLocalPlaylist(id: Long)

    @Query("UPDATE local_playlist SET title = :title WHERE id = :id")
    suspend fun updateLocalPlaylistTitle(title: String, id: Long)

    @Query("UPDATE local_playlist SET tracks = :tracks WHERE id = :id")
    suspend fun updateLocalPlaylistTracks(tracks: List<String>, id: Long)

    @Query("UPDATE local_playlist SET thumbnail = :thumbnail WHERE id = :id")
    suspend fun updateLocalPlaylistThumbnail(thumbnail: String, id: Long)

    @Query("UPDATE local_playlist SET inLibrary = :inLibrary WHERE id = :id")
    suspend fun updateLocalPlaylistInLibrary(inLibrary: LocalDateTime, id: Long)

    @Query("UPDATE local_playlist SET downloadState = :downloadState WHERE id = :id")
    suspend fun updateLocalPlaylistDownloadState(downloadState: Int, id: Long)

    @Query("SELECT * FROM local_playlist WHERE downloadState = 3")
    suspend fun getDownloadedLocalPlaylists(): List<LocalPlaylistEntity>

}