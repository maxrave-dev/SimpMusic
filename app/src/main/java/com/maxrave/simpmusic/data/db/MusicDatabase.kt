package com.maxrave.simpmusic.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.maxrave.simpmusic.data.db.entities.AlbumEntity
import com.maxrave.simpmusic.data.db.entities.ArtistEntity
import com.maxrave.simpmusic.data.db.entities.PlaylistEntity
import com.maxrave.simpmusic.data.db.entities.SearchHistory
import com.maxrave.simpmusic.data.db.entities.SongEntity

@Database(entities = [SearchHistory::class, SongEntity::class, ArtistEntity::class, AlbumEntity::class, PlaylistEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MusicDatabase: RoomDatabase() {
    abstract fun getDatabaseDao(): DatabaseDao
}