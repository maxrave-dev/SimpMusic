package com.maxrave.data.extension

import androidx.sqlite.db.SimpleSQLiteQuery

internal fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)