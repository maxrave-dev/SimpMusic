package com.maxrave.data.repository

import android.content.Context
import com.maxrave.data.db.LocalDataSource
import com.maxrave.data.db.MusicDatabase
import com.maxrave.domain.data.entities.NotificationEntity
import com.maxrave.domain.data.type.RecentlyType
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.spotify.Spotify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.simpmusic.aiservice.AIHost
import org.simpmusic.aiservice.AiClient
import java.io.File

internal class CommonRepositoryImpl(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val database: MusicDatabase,
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
    private val spotify: Spotify,
    private val aiClient: AiClient,
) : CommonRepository {
    override fun init(dataStoreManager: DataStoreManager) {
        youTube.setUpInterceptors(context)
        youTube.cachePath = File(context.cacheDir, "http-cache")
        youTube.cookiePath = File(context.filesDir, "cookie.txt").path.toPath()
        coroutineScope.launch {
            val resetSpotifyToken =
                launch {
                    dataStoreManager.setSpotifyClientToken("")
                    dataStoreManager.setSpotifyPersonalToken("")
                    dataStoreManager.setSpotifyClientTokenExpires(System.currentTimeMillis())
                    dataStoreManager.setSpotifyPersonalTokenExpires(System.currentTimeMillis())
                }
            val localeJob =
                launch {
                    combine(dataStoreManager.location, dataStoreManager.language) { location, language ->
                        Pair(location, language)
                    }.collectLatest { (location, language) ->
                        youTube.locale =
                            YouTubeLocale(
                                location,
                                try {
                                    language.substring(0..1)
                                } catch (e: Exception) {
                                    "en"
                                },
                            )
                    }
                }
            val ytCookieJob =
                launch {
                    dataStoreManager.cookie.distinctUntilChanged().collectLatest { cookie ->
                        if (cookie.isNotEmpty()) {
                            youTube.cookie = cookie
                            youTube.visitorData()?.let {
                                youTube.visitorData = it
                            }
                        } else {
                            youTube.cookie = null
                        }
                    }
                }
            val usingProxy =
                launch {
                    combine(
                        dataStoreManager.usingProxy,
                        dataStoreManager.proxyType,
                        dataStoreManager.proxyHost,
                        dataStoreManager.proxyPort,
                    ) { usingProxy, proxyType, proxyHost, proxyPort ->
                        Pair(usingProxy == DataStoreManager.TRUE, Triple(proxyType, proxyHost, proxyPort))
                    }.collectLatest { (usingProxy, data) ->
                        if (usingProxy) {
                            withContext(Dispatchers.IO) {
                                youTube.setProxy(
                                    data.first == DataStoreManager.ProxyType.PROXY_TYPE_HTTP,
                                    data.second,
                                    data.third,
                                )
                                spotify.setProxy(
                                    data.first == DataStoreManager.ProxyType.PROXY_TYPE_HTTP,
                                    data.second,
                                    data.third,
                                )
                            }
                        } else {
                            youTube.removeProxy()
                            spotify.removeProxy()
                        }
                    }
                }
            val dataSyncIdJob =
                launch {
                    dataStoreManager.dataSyncId.collectLatest { dataSyncId ->
                        youTube.dataSyncId = dataSyncId
                    }
                }
            val visitorDataJob =
                launch {
                    dataStoreManager.visitorData.collectLatest { visitorData ->
                        youTube.visitorData = visitorData
                    }
                }
            val aiClientProviderJob =
                launch {
                    dataStoreManager.aiProvider.collectLatest { provider ->
                        aiClient.host =
                            when (provider) {
                                DataStoreManager.AI_PROVIDER_GEMINI -> AIHost.GEMINI
                                DataStoreManager.AI_PROVIDER_OPENAI -> AIHost.OPENAI
                                else -> AIHost.GEMINI // Default to Gemini if not set
                            }
                    }
                }
            val aiClientApiKeyJob =
                launch {
                    dataStoreManager.aiApiKey.collectLatest { apiKey ->
                        aiClient.apiKey =
                            apiKey.ifEmpty {
                                null
                            }
                    }
                }
            val aiCustomModelIdJob =
                launch {
                    dataStoreManager.customModelId.collectLatest { modelId ->
                        aiClient.customModelId =
                            modelId.ifEmpty {
                                null
                            }
                    }
                }

            localeJob.join()
            ytCookieJob.join()
            usingProxy.join()
            dataSyncIdJob.join()
            visitorDataJob.join()
            resetSpotifyToken.join()
            aiClientProviderJob.join()
            aiClientApiKeyJob.join()
            aiCustomModelIdJob.join()
        }
    }

    // Database
    override fun closeDatabase() {
        if (database.isOpen) {
            database.close()
        }
    }

    override fun getDatabasePath() = database.openHelper.writableDatabase.path

    override fun databaseDaoCheckpoint() = localDataSource.checkpoint()

    // Recently data
    override fun getAllRecentData(): Flow<List<RecentlyType>> =
        flow {
            emit(localDataSource.getAllRecentData())
        }.flowOn(Dispatchers.IO)

    // Notifications
    override suspend fun insertNotification(notificationEntity: NotificationEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertNotification(notificationEntity)
        }

    override suspend fun getAllNotifications(): Flow<List<NotificationEntity>?> =
        flow {
            emit(localDataSource.getAllNotification())
        }.flowOn(Dispatchers.IO)

    override suspend fun deleteNotification(id: Long) =
        withContext(Dispatchers.IO) {
            localDataSource.deleteNotification(id)
        }
}