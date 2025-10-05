package com.maxrave.data.db

import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ktlint:standard:class-naming")
@DeleteTable.Entries(
    DeleteTable(
        tableName = "format",
    ),
)
internal class AutoMigration7_8 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        db.execSQL("DROP TABLE IF EXISTS `format`")
    }
}

@Suppress("ktlint:standard:class-naming")
@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "local_playlist",
        columnName = "synced_with_youtube_playlist",
    ),
)
internal class AutoMigration11_12 : AutoMigrationSpec