package com.maxrave.simpmusic.data.db

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@DeleteTable.Entries(
    DeleteTable(
        tableName = "format"
    )
)
class AutoMigration7_8 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        db.execSQL("DROP TABLE IF EXISTS `format`")
    }
}