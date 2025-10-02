package com.maxrave.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OPEN_READONLY
import android.webkit.CookieManager
import com.maxrave.data.db.LocalDataSource
import com.maxrave.data.db.MusicDatabase
import com.maxrave.domain.data.entities.NotificationEntity
import com.maxrave.domain.data.model.cookie.CookieItem
import com.maxrave.domain.data.type.RecentlyType
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.logger.Logger
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
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
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
        youTube.cookiePath = File(context.filesDir, "ytdlp-cookie.txt").path.toPath()
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
                        Logger.d("YouTube", "New cookie")
                        localDataSource.getUsedGoogleAccount()?.netscapeCookie?.let {
                            writeTextToFile(it, File(context.filesDir, "ytdlp-cookie.txt").path)
                            Logger.w("YouTube", "Wrote cookie to file")
                        }
                    }
                }
            val pageIdJob =
                launch {
                    dataStoreManager.pageId.distinctUntilChanged().collectLatest { pageId ->
                        youTube.pageId = pageId.ifEmpty { null }
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
            pageIdJob.join()
            usingProxy.join()
            dataSyncIdJob.join()
            visitorDataJob.join()
            resetSpotifyToken.join()
            aiClientProviderJob.join()
            aiClientApiKeyJob.join()
            aiCustomModelIdJob.join()
        }

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                youTube.updateYtdlp()
            }
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

    override suspend fun writeTextToFile(
        text: String,
        filePath: String,
    ): Boolean {
        try {
            FileSystem.SYSTEM.sink(filePath.toPath()).buffer().use { sink ->
                sink.writeUtf8(text)
                sink.close()
                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Original from YTDLnis app
     */
    override suspend fun getCookiesFromInternalDatabase(url: String): CookieItem =
        withContext(Dispatchers.IO) {
            try {
                val projection =
                    arrayOf(
                        CookieItem.HOST,
                        CookieItem.EXPIRY,
                        CookieItem.PATH,
                        CookieItem.NAME,
                        CookieItem.VALUE,
                        CookieItem.SECURE,
                    )
                CookieManager.getInstance().flush()
                val cookieList = mutableListOf<CookieItem.Content>()
                val dbPath =
                    File("/data/data/${context.packageName}/").walkTopDown().find { it.name == "Cookies" }
                        ?: throw Exception("Cookies File not found!")

                val db =
                    SQLiteDatabase.openDatabase(
                        dbPath.absolutePath,
                        null,
                        OPEN_READONLY,
                    )
                db
                    .query(
                        "cookies",
                        projection,
                        null,
                        null,
                        null,
                        null,
                        null,
                    ).run {
                        while (moveToNext()) {
                            val expiry = getLong(getColumnIndexOrThrow(CookieItem.EXPIRY))
                            val name = getString(getColumnIndexOrThrow(CookieItem.NAME))
                            val value = getString(getColumnIndexOrThrow(CookieItem.VALUE))
                            val path = getString(getColumnIndexOrThrow(CookieItem.PATH))
                            val secure = getLong(getColumnIndexOrThrow(CookieItem.SECURE)) == 1L
                            val hostKey = getString(getColumnIndexOrThrow(CookieItem.HOST))

                            val host = if (hostKey[0] != '.') ".$hostKey" else hostKey
                            cookieList.add(
                                CookieItem.Content(
                                    domain = host,
                                    name = name,
                                    value = value,
                                    isSecure = secure,
                                    expiresUtc = expiry,
                                    hostKey = host,
                                    path = path,
                                ),
                            )
                        }
                        close()
                    }
                db.close()
                CookieItem(url, cookieList)
            } catch (e: Exception) {
                e.printStackTrace()
                CookieItem(url, emptyList())
            }
        }
}