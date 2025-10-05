package com.maxrave.data.extension

import androidx.sqlite.db.SimpleSQLiteQuery

internal fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)

suspend fun <T> getFullDataFromDB(
    func: suspend (limit: Int, offset: Int) -> List<T>,
): List<T> {
    val a = mutableListOf<T>()
    var shouldContinue = true to 0
    while (shouldContinue.first) {
        val fetched = func(
            100,
            shouldContinue.second,
        )
        a.addAll(
            fetched,
        )
        shouldContinue = if (fetched.size < 100) {
            false to 0
        } else {
            true to shouldContinue.second + 100
        }
    }
    return a
}