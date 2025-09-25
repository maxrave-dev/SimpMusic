package com.maxrave.domain.repository

import com.maxrave.domain.data.entities.NotificationEntity
import com.maxrave.domain.data.model.cookie.CookieItem
import com.maxrave.domain.data.type.RecentlyType
import com.maxrave.domain.manager.DataStoreManager
import kotlinx.coroutines.flow.Flow

interface CommonRepository {
    fun init(dataStoreManager: DataStoreManager)

    // Database
    fun closeDatabase()

    fun getDatabasePath(): String?

    fun databaseDaoCheckpoint()

    // Recently data
    fun getAllRecentData(): Flow<List<RecentlyType>>

    // Notifications
    suspend fun insertNotification(notificationEntity: NotificationEntity)

    suspend fun getAllNotifications(): Flow<List<NotificationEntity>?>

    suspend fun deleteNotification(id: Long)

    suspend fun writeTextToFile(text: String, filePath: String): Boolean

    suspend fun getCookiesFromInternalDatabase(url: String): CookieItem
}