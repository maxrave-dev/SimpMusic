package com.maxrave.simpmusic.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

@Database(entities = [SearchHistory::class, SongEntity::class, ArtistEntity::class, AlbumEntity::class, PlaylistEntity::class, LocalPlaylistEntity::class, LyricsEntity::class, FormatEntity::class, QueueEntity::class, SetVideoIdEntity::class, PairSongLocalPlaylist::class], version = 6, exportSchema = true, autoMigrations = [AutoMigration(from = 2, to = 3), AutoMigration(from = 1, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 2, to = 4), AutoMigration(from = 3, to = 5), AutoMigration(4, 5)])
@TypeConverters(Converters::class)
abstract class MusicDatabase: RoomDatabase() {
    abstract fun getDatabaseDao(): DatabaseDao
}