package com.maxrave.data.dataStore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.maxrave.common.SELECTED_LANGUAGE
import com.maxrave.common.SETTINGS_FILENAME
import com.maxrave.common.SPONSOR_BLOCK
import com.maxrave.common.SUPPORTED_LANGUAGE
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.AI_PROVIDER_GEMINI
import com.maxrave.domain.manager.DataStoreManager.Values.FALSE
import com.maxrave.domain.manager.DataStoreManager.Values.GITHUB
import com.maxrave.domain.manager.DataStoreManager.Values.LOCAL_PLAYLIST_FILTER_OLDER_FIRST
import com.maxrave.domain.manager.DataStoreManager.Values.PROXY_TYPE_HTTP
import com.maxrave.domain.manager.DataStoreManager.Values.PROXY_TYPE_SOCKS
import com.maxrave.domain.manager.DataStoreManager.Values.REPEAT_ALL
import com.maxrave.domain.manager.DataStoreManager.Values.REPEAT_MODE_OFF
import com.maxrave.domain.manager.DataStoreManager.Values.REPEAT_ONE
import com.maxrave.domain.manager.DataStoreManager.Values.SIMPMUSIC
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Proxy
import com.maxrave.common.QUALITY as COMMON_QUALITY

val Context.dataStore by preferencesDataStore(name = SETTINGS_FILENAME)

internal class DataStoreManagerImpl(
    private val settingsDataStore: DataStore<Preferences>,
) : DataStoreManager {
    override val appVersion: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[APP_VERSION] ?: ""
        }

    override suspend fun setAppVersion(version: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[APP_VERSION] = version
            }
        }
    }

    override val openAppTime: Flow<Int> =
        settingsDataStore.data.map { preferences ->
            preferences[OPEN_APP_TIME] ?: 0
        }

    override suspend fun openApp() {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[OPEN_APP_TIME] = openAppTime.first() + 1
            }
        }
    }

    override suspend fun resetOpenAppTime() {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[OPEN_APP_TIME] = 0
            }
        }
    }

    override suspend fun doneOpenAppTime() {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[OPEN_APP_TIME] = 31
            }
        }
    }

    override val location: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[LOCATION] ?: "VN"
        }

    override suspend fun setLocation(location: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[LOCATION] = location
            }
        }
    }

    override val quality: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[QUALITY] ?: COMMON_QUALITY.items[0].toString()
        }

    override suspend fun setQuality(quality: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[QUALITY] = quality
            }
        }
    }

    override val language: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[stringPreferencesKey(SELECTED_LANGUAGE)] ?: SUPPORTED_LANGUAGE.codes.first()
        }

    override fun getString(key: String): Flow<String?> =
        settingsDataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)]
        }

    override suspend fun putString(
        key: String,
        value: String,
    ) {
        settingsDataStore.edit { settings ->
            settings[stringPreferencesKey(key)] = value
        }
    }

    override val loggedIn: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[LOGGED_IN] ?: FALSE
        }

    override val cookie: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[COOKIE] ?: ""
        }

    override val pageId: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[PAGE_ID] ?: ""
        }

    override suspend fun setCookie(
        cookie: String,
        pageId: String?,
    ) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[COOKIE] = cookie
                settings[PAGE_ID] = pageId ?: ""
            }
        }
    }

    override suspend fun setLoggedIn(logged: Boolean) {
        withContext(Dispatchers.IO) {
            if (logged) {
                settingsDataStore.edit { settings ->
                    settings[LOGGED_IN] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[LOGGED_IN] = FALSE
                }
            }
        }
    }

    override val normalizeVolume: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[NORMALIZE_VOLUME] ?: FALSE
        }

    override suspend fun setNormalizeVolume(normalize: Boolean) {
        withContext(Dispatchers.IO) {
            if (normalize) {
                settingsDataStore.edit { settings ->
                    settings[NORMALIZE_VOLUME] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[NORMALIZE_VOLUME] = FALSE
                }
            }
        }
    }

    override val skipSilent: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SKIP_SILENT] ?: FALSE
        }

    override suspend fun setSkipSilent(skip: Boolean) {
        withContext(Dispatchers.IO) {
            if (skip) {
                settingsDataStore.edit { settings ->
                    settings[SKIP_SILENT] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SKIP_SILENT] = FALSE
                }
            }
        }
    }

    override val saveStateOfPlayback: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SAVE_STATE_OF_PLAYBACK] ?: FALSE
        }

    override suspend fun setSaveStateOfPlayback(save: Boolean) {
        withContext(Dispatchers.IO) {
            if (save) {
                settingsDataStore.edit { settings ->
                    settings[SAVE_STATE_OF_PLAYBACK] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SAVE_STATE_OF_PLAYBACK] = FALSE
                }
            }
        }
    }

    override val shuffleKey: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SHUFFLE_KEY] ?: FALSE
        }
    override val repeatKey: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[REPEAT_KEY] ?: REPEAT_MODE_OFF
        }

    override suspend fun recoverShuffleAndRepeatKey(
        shuffle: Boolean,
        repeat: Int,
    ) {
        withContext(Dispatchers.IO) {
            if (shuffle) {
                settingsDataStore.edit { settings ->
                    settings[SHUFFLE_KEY] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SHUFFLE_KEY] = FALSE
                }
            }
            settingsDataStore.edit { settings ->
                settings[REPEAT_KEY] =
                    when (repeat) {
                        1 -> REPEAT_ONE
                        2 -> REPEAT_ALL
                        0 -> REPEAT_MODE_OFF
                        else -> REPEAT_MODE_OFF
                    }
            }
        }
    }

    override val saveRecentSongAndQueue: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SAVE_RECENT_SONG] ?: FALSE
        }

    override suspend fun setSaveRecentSongAndQueue(save: Boolean) {
        withContext(Dispatchers.IO) {
            if (save) {
                settingsDataStore.edit { settings ->
                    settings[SAVE_RECENT_SONG] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SAVE_RECENT_SONG] = FALSE
                }
            }
        }
    }

    override val recentMediaId =
        settingsDataStore.data.map { preferences ->
            preferences[RECENT_SONG_MEDIA_ID_KEY] ?: ""
        }
    override val recentPosition =
        settingsDataStore.data.map { preferences ->
            preferences[RECENT_SONG_POSITION_KEY] ?: "0"
        }

    override suspend fun saveRecentSong(
        mediaId: String,
        position: Long,
    ) {
        Logger.w("saveRecentSong", "$mediaId $position")
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[RECENT_SONG_MEDIA_ID_KEY] = mediaId
                settings[RECENT_SONG_POSITION_KEY] = position.toString()
            }
        }
    }

    override val playlistFromSaved =
        settingsDataStore.data.map { preferences ->
            preferences[FROM_SAVED_PLAYLIST] ?: ""
        }

    override suspend fun setPlaylistFromSaved(playlist: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[FROM_SAVED_PLAYLIST] = playlist
            }
        }
    }

    override val sendBackToGoogle =
        settingsDataStore.data.map { preferences ->
            preferences[SEND_BACK_TO_GOOGLE] ?: FALSE
        }

    override suspend fun setSendBackToGoogle(send: Boolean) {
        withContext(Dispatchers.IO) {
            if (send) {
                settingsDataStore.edit { settings ->
                    settings[SEND_BACK_TO_GOOGLE] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SEND_BACK_TO_GOOGLE] = FALSE
                }
            }
        }
    }

    override val sponsorBlockEnabled =
        settingsDataStore.data.map { preferences ->
            preferences[SPONSOR_BLOCK_ENABLED] ?: FALSE
        }

    override suspend fun setSponsorBlockEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (enabled) {
                settingsDataStore.edit { settings ->
                    settings[SPONSOR_BLOCK_ENABLED] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SPONSOR_BLOCK_ENABLED] = FALSE
                }
            }
        }
    }

    override suspend fun getSponsorBlockCategories(): ArrayList<String> {
        val list: ArrayList<String> = arrayListOf()
        for (category in SPONSOR_BLOCK.list) {
            if (getString(category.toString()).first() == TRUE) list.add(category.toString())
        }
        return list
    }

    override suspend fun setSponsorBlockCategories(categories: ArrayList<String>) {
        withContext(Dispatchers.IO) {
            Logger.w("setSponsorBlockCategories", categories.toString())
            for (category in categories) {
                settingsDataStore.edit { settings ->
                    settings[stringPreferencesKey(category)] = TRUE
                }
            }
            SPONSOR_BLOCK.list.filter { !categories.contains(it) }.forEach { category ->
                settingsDataStore.edit { settings ->
                    settings[stringPreferencesKey(category.toString())] = FALSE
                }
            }
        }
    }

    override val enableTranslateLyric =
        settingsDataStore.data.map { preferences ->
            preferences[USE_TRANSLATION_LANGUAGE] ?: FALSE
        }

    override suspend fun setEnableTranslateLyric(enable: Boolean) {
        withContext(Dispatchers.IO) {
            if (enable) {
                settingsDataStore.edit { settings ->
                    settings[USE_TRANSLATION_LANGUAGE] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[USE_TRANSLATION_LANGUAGE] = FALSE
                }
            }
        }
    }

    override val lyricsProvider =
        settingsDataStore.data.map { preferences ->
            preferences[LYRICS_PROVIDER] ?: SIMPMUSIC
        }

    override suspend fun setLyricsProvider(provider: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[LYRICS_PROVIDER] = provider
            }
        }
    }

    override val translationLanguage =
        settingsDataStore.data.map { preferences ->
            val languageValue = language.first()
            preferences[TRANSLATION_LANGUAGE] ?: if (languageValue.length >= 2) {
                languageValue
                    .substring(0..1)
            } else {
                "en"
            }
        }

    override suspend fun setTranslationLanguage(language: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[TRANSLATION_LANGUAGE] = language
            }
        }
    }

    override val maxSongCacheSize =
        settingsDataStore.data.map { preferences ->
            preferences[MAX_SONG_CACHE_SIZE] ?: -1
        }

    override suspend fun setMaxSongCacheSize(size: Int) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[MAX_SONG_CACHE_SIZE] = size
            }
        }
    }

    override val watchVideoInsteadOfPlayingAudio =
        settingsDataStore.data.map { preferences ->
            preferences[WATCH_VIDEO_INSTEAD_OF_PLAYING_AUDIO] ?: FALSE
        }

    override suspend fun setWatchVideoInsteadOfPlayingAudio(watch: Boolean) {
        withContext(Dispatchers.IO) {
            if (watch) {
                settingsDataStore.edit { settings ->
                    settings[WATCH_VIDEO_INSTEAD_OF_PLAYING_AUDIO] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[WATCH_VIDEO_INSTEAD_OF_PLAYING_AUDIO] = FALSE
                }
            }
        }
    }

    override val videoQuality =
        settingsDataStore.data.map { preferences ->
            preferences[VIDEO_QUALITY] ?: "720p"
        }

    override suspend fun setVideoQuality(quality: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[VIDEO_QUALITY] = quality
            }
        }
    }

    override val spdc =
        settingsDataStore.data.map { preferences ->
            preferences[SPDC] ?: ""
        }

    override suspend fun setSpdc(spdc: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[SPDC] = spdc
            }
        }
    }

    override val spotifyLyrics: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_LYRICS] ?: FALSE
        }

    override suspend fun setSpotifyLyrics(spotifyLyrics: Boolean) {
        withContext(Dispatchers.IO) {
            if (spotifyLyrics) {
                settingsDataStore.edit { settings ->
                    settings[SPOTIFY_LYRICS] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SPOTIFY_LYRICS] = FALSE
                }
            }
        }
    }

    override val spotifyCanvas: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_CANVAS] ?: FALSE
        }

    override suspend fun setSpotifyCanvas(spotifyCanvas: Boolean) {
        withContext(Dispatchers.IO) {
            if (spotifyCanvas) {
                settingsDataStore.edit { settings ->
                    settings[SPOTIFY_CANVAS] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SPOTIFY_CANVAS] = FALSE
                }
            }
        }
    }

    override val spotifyClientToken: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_CLIENT_TOKEN] ?: ""
        }

    override suspend fun setSpotifyClientToken(token: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[SPOTIFY_CLIENT_TOKEN] = token
            }
        }
    }

    override val spotifyClientTokenExpires: Flow<Long> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_CLIENT_TOKEN_EXPIRES] ?: 0
        }

    override suspend fun setSpotifyClientTokenExpires(expires: Long) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[SPOTIFY_CLIENT_TOKEN_EXPIRES] = expires
            }
        }
    }

    override val spotifyPersonalToken: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_PERSONAL_TOKEN] ?: ""
        }

    override suspend fun setSpotifyPersonalToken(token: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[SPOTIFY_PERSONAL_TOKEN] = token
            }
        }
    }

    override val spotifyPersonalTokenExpires: Flow<Long> =
        settingsDataStore.data.map { preferences ->
            preferences[SPOTIFY_PERSONAL_TOKEN_EXPIRES] ?: 0
        }

    override suspend fun setSpotifyPersonalTokenExpires(expires: Long) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[SPOTIFY_PERSONAL_TOKEN_EXPIRES] = expires
            }
        }
    }

    override val homeLimit: Flow<Int> =
        settingsDataStore.data.map { preferences ->
            preferences[HOME_LIMIT] ?: 5
        }

    override suspend fun setHomeLimit(limit: Int) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[HOME_LIMIT] = limit
            }
        }
    }

    override val chartKey =
        settingsDataStore.data.map { preferences ->
            preferences[CHART_KEY] ?: "ZZ"
        }

    override suspend fun setChartKey(key: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[CHART_KEY] = key
            }
        }
    }

    override val translucentBottomBar =
        settingsDataStore.data.map { preferences ->
            preferences[TRANSLUCENT_BOTTOM_BAR] ?: TRUE
        }

    override suspend fun setTranslucentBottomBar(translucent: Boolean) {
        withContext(Dispatchers.IO) {
            if (translucent) {
                settingsDataStore.edit { settings ->
                    settings[TRANSLUCENT_BOTTOM_BAR] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[TRANSLUCENT_BOTTOM_BAR] = FALSE
                }
            }
        }
    }

    override val usingProxy =
        settingsDataStore.data.map { preferences ->
            preferences[USING_PROXY] ?: FALSE
        }

    override suspend fun setUsingProxy(usingProxy: Boolean) {
        withContext(Dispatchers.IO) {
            if (usingProxy) {
                settingsDataStore.edit { settings ->
                    settings[USING_PROXY] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[USING_PROXY] = FALSE
                }
            }
        }
    }

    override val proxyType =
        settingsDataStore.data
            .map { preferences ->
                preferences[PROXY_TYPE]
            }.map {
                when (it) {
                    PROXY_TYPE_HTTP -> DataStoreManager.ProxyType.PROXY_TYPE_HTTP
                    PROXY_TYPE_SOCKS -> DataStoreManager.ProxyType.PROXY_TYPE_SOCKS
                    else -> DataStoreManager.ProxyType.PROXY_TYPE_HTTP
                }
            }

    override suspend fun setProxyType(proxyType: DataStoreManager.ProxyType) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[PROXY_TYPE] =
                    when (proxyType) {
                        DataStoreManager.ProxyType.PROXY_TYPE_HTTP -> PROXY_TYPE_HTTP
                        DataStoreManager.ProxyType.PROXY_TYPE_SOCKS -> PROXY_TYPE_SOCKS
                    }
            }
        }
    }

    override val proxyHost =
        settingsDataStore.data.map { preferences ->
            preferences[PROXY_HOST] ?: ""
        }

    override suspend fun setProxyHost(proxyHost: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[PROXY_HOST] = proxyHost
            }
        }
    }

    override val proxyPort =
        settingsDataStore.data.map { preferences ->
            preferences[PROXY_PORT] ?: 8000
        }

    override suspend fun setProxyPort(proxyPort: Int) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[PROXY_PORT] = proxyPort
            }
        }
    }

    override fun getJVMProxy(): Proxy? =
        runBlocking {
            try {
                if (usingProxy.first() == TRUE) {
                    val proxyType = proxyType.first()
                    val proxyHost = proxyHost.first()
                    val proxyPort = proxyPort.first()
                    return@runBlocking Proxy(
                        when (proxyType) {
                            DataStoreManager.ProxyType.PROXY_TYPE_HTTP -> Proxy.Type.HTTP
                            DataStoreManager.ProxyType.PROXY_TYPE_SOCKS -> Proxy.Type.SOCKS
                        },
                        InetSocketAddress(proxyHost, proxyPort),
                    )
                } else {
                    return@runBlocking null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@runBlocking null
            }
        }

    override val endlessQueue =
        settingsDataStore.data.map { preferences ->
            preferences[ENDLESS_QUEUE] ?: FALSE
        }

    override suspend fun setEndlessQueue(endlessQueue: Boolean) {
        withContext(Dispatchers.IO) {
            if (endlessQueue) {
                settingsDataStore.edit { settings ->
                    settings[ENDLESS_QUEUE] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[ENDLESS_QUEUE] = FALSE
                }
            }
        }
    }

    override val shouldShowLogInRequiredAlert =
        settingsDataStore.data.map { preferences ->
            preferences[SHOULD_SHOW_LOG_IN_REQUIRED_ALERT] ?: TRUE
        }

    override suspend fun setShouldShowLogInRequiredAlert(shouldShow: Boolean) {
        withContext(Dispatchers.IO) {
            if (shouldShow) {
                settingsDataStore.edit { settings ->
                    settings[SHOULD_SHOW_LOG_IN_REQUIRED_ALERT] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SHOULD_SHOW_LOG_IN_REQUIRED_ALERT] = FALSE
                }
            }
        }
    }

    override val autoCheckForUpdates =
        settingsDataStore.data.map { preferences ->
            preferences[AUTO_CHECK_FOR_UPDATES] ?: TRUE
        }

    override suspend fun setAutoCheckForUpdates(autoCheck: Boolean) {
        withContext(Dispatchers.IO) {
            if (autoCheck) {
                settingsDataStore.edit { settings ->
                    settings[AUTO_CHECK_FOR_UPDATES] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[AUTO_CHECK_FOR_UPDATES] = FALSE
                }
            }
        }
    }

    override val updateChannel =
        settingsDataStore.data.map { preferences ->
            preferences[UPDATE_CHANNEL] ?: GITHUB
        }

    override suspend fun setUpdateChannel(channel: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[UPDATE_CHANNEL] = channel
            }
        }
    }

    override val blurFullscreenLyrics =
        settingsDataStore.data.map { preferences ->
            preferences[BLUR_FULLSCREEN_LYRICS] ?: FALSE
        }

    override suspend fun setBlurFullscreenLyrics(blur: Boolean) {
        withContext(Dispatchers.IO) {
            if (blur) {
                settingsDataStore.edit { settings ->
                    settings[BLUR_FULLSCREEN_LYRICS] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[BLUR_FULLSCREEN_LYRICS] = FALSE
                }
            }
        }
    }

    override val blurPlayerBackground =
        settingsDataStore.data.map { preferences ->
            preferences[BLUR_PLAYER_BACKGROUND] ?: FALSE
        }

    override suspend fun setBlurPlayerBackground(blur: Boolean) {
        withContext(Dispatchers.IO) {
            if (blur) {
                settingsDataStore.edit { settings ->
                    settings[BLUR_PLAYER_BACKGROUND] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[BLUR_PLAYER_BACKGROUND] = FALSE
                }
            }
        }
    }

    override val playbackSpeed =
        settingsDataStore.data.map { preferences ->
            preferences[PLAYBACK_SPEED] ?: 1.0f
        }

    override fun setPlaybackSpeed(speed: Float) {
        runBlocking {
            settingsDataStore.edit { settings ->
                settings[PLAYBACK_SPEED] = speed
            }
        }
    }

    override val pitch =
        settingsDataStore.data.map { preferences ->
            preferences[PITCH] ?: 0
        }

    override fun setPitch(pitch: Int) {
        runBlocking {
            settingsDataStore.edit { settings ->
                settings[PITCH] = pitch
            }
        }
    }

    override val dataSyncId =
        settingsDataStore.data.map { preferences ->
            preferences[DATA_SYNC_ID] ?: ""
        }

    override suspend fun setDataSyncId(dataSyncId: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[DATA_SYNC_ID] = dataSyncId
            }
        }
    }

    override val visitorData =
        settingsDataStore.data.map { preferences ->
            preferences[VISITOR_DATA] ?: ""
        }

    override suspend fun setVisitorData(visitorData: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[VISITOR_DATA] = visitorData
            }
        }
    }

    override suspend fun setAIProvider(provider: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[stringPreferencesKey("ai_provider")] = provider
            }
        }
    }

    override val aiProvider: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[AI_PROVIDER] ?: AI_PROVIDER_GEMINI
        }

    override suspend fun setAIApiKey(apiKey: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[AI_API_KEY] = apiKey
            }
        }
    }

    override val aiApiKey: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[AI_API_KEY] ?: ""
        }

    override val useAITranslation: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[USE_AI_TRANSLATION] ?: FALSE
        }

    override suspend fun setUseAITranslation(use: Boolean) {
        withContext(Dispatchers.IO) {
            if (use) {
                settingsDataStore.edit { settings ->
                    settings[USE_AI_TRANSLATION] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[USE_AI_TRANSLATION] = FALSE
                }
            }
        }
    }

    override val customModelId =
        settingsDataStore.data.map { preferences ->
            preferences[CUSTOM_MODEL_ID] ?: ""
        }

    override suspend fun setCustomModelId(modelId: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[CUSTOM_MODEL_ID] = modelId
            }
        }
    }

    override val localPlaylistFilter: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[LOCAL_PLAYLIST_FILTER] ?: LOCAL_PLAYLIST_FILTER_OLDER_FIRST
        }

    override suspend fun setLocalPlaylistFilter(filter: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[LOCAL_PLAYLIST_FILTER] = filter
            }
        }
    }

    override val killServiceOnExit: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[KILL_SERVICE_ON_EXIT] ?: FALSE
        }

    override suspend fun setKillServiceOnExit(kill: Boolean) {
        withContext(Dispatchers.IO) {
            if (kill) {
                settingsDataStore.edit { settings ->
                    settings[KILL_SERVICE_ON_EXIT] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[KILL_SERVICE_ON_EXIT] = FALSE
                }
            }
        }
    }

    override val crossfadeEnabled: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[CROSSFADE_ENABLED] ?: FALSE
        }

    override suspend fun setCrossfadeEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (enabled) {
                settingsDataStore.edit { settings ->
                    settings[CROSSFADE_ENABLED] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[CROSSFADE_ENABLED] = FALSE
                }
            }
        }
    }

    override val crossfadeDuration: Flow<Int> =
        settingsDataStore.data.map { preferences ->
            preferences[CROSSFADE_DURATION] ?: 5000
        }

    override suspend fun setCrossfadeDuration(duration: Int) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[CROSSFADE_DURATION] = duration
            }
        }
    }

    override val youtubeSubtitleLanguage =
        settingsDataStore.data.map { preferences ->
            val languageValue = language.first()
            preferences[YOUTUBE_SUBTITLE_LANGUAGE] ?: if (languageValue.length >= 2) {
                languageValue
                    .substring(0..1)
            } else {
                "en"
            }
        }

    override suspend fun setYoutubeSubtitleLanguage(language: String) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[YOUTUBE_SUBTITLE_LANGUAGE] = language
            }
        }
    }

    override val helpBuildLyricsDatabase: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[HELP_BUILD_LYRICS_DATABASE] ?: FALSE
        }

    override suspend fun setHelpBuildLyricsDatabase(help: Boolean) {
        withContext(Dispatchers.IO) {
            if (help) {
                settingsDataStore.edit { settings ->
                    settings[HELP_BUILD_LYRICS_DATABASE] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[HELP_BUILD_LYRICS_DATABASE] = FALSE
                }
            }
        }
    }

    override val contributorName: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[CONTRIBUTOR_NAME] ?: ""
        }

    override val contributorEmail: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[CONTRIBUTOR_EMAIL] ?: ""
        }

    override suspend fun setContributorLyricsDatabase(
        contributor: Pair<String, String>?, // contributor name and email, null if anonymous
    ) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                if (contributor == null) {
                    settings[CONTRIBUTOR_NAME] = ""
                    settings[CONTRIBUTOR_EMAIL] = ""
                } else {
                    settings[CONTRIBUTOR_NAME] = contributor.first
                    settings[CONTRIBUTOR_EMAIL] = contributor.second
                }
            }
        }
    }

    override val backupDownloaded: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[BACKUP_DOWNLOADED] ?: FALSE
        }

    override suspend fun setBackupDownloaded(backupDownloaded: Boolean) {
        withContext(Dispatchers.IO) {
            if (backupDownloaded) {
                settingsDataStore.edit { settings ->
                    settings[BACKUP_DOWNLOADED] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[BACKUP_DOWNLOADED] = FALSE
                }
            }
        }
    }

    override val enableLiquidGlass: Flow<String>
        get() =
            settingsDataStore.data.map { preferences ->
                preferences[LIQUID_GLASS] ?: FALSE
            }

    override suspend fun setEnableLiquidGlass(enable: Boolean) {
        withContext(Dispatchers.IO) {
            if (enable) {
                settingsDataStore.edit { settings ->
                    settings[LIQUID_GLASS] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[LIQUID_GLASS] = FALSE
                }
            }
        }
    }

    override val explicitContentEnabled: Flow<String> =
        settingsDataStore.data.map { preferences ->
            preferences[EXPLICIT_CONTENT_ENABLED] ?: TRUE
        }

    override suspend fun setExplicitContentEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (enabled) {
                settingsDataStore.edit { settings ->
                    settings[EXPLICIT_CONTENT_ENABLED] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[EXPLICIT_CONTENT_ENABLED] = FALSE
                }
            }
        }
    }

    companion object Settings {
        val APP_VERSION = stringPreferencesKey("app_version")
        val COOKIE = stringPreferencesKey("cookie")

        val PAGE_ID = stringPreferencesKey("page_id")
        val LOGGED_IN = stringPreferencesKey("logged_in")
        val LOCATION = stringPreferencesKey("location")
        val QUALITY = stringPreferencesKey("quality")
        val NORMALIZE_VOLUME = stringPreferencesKey("normalize_volume")
        val SKIP_SILENT = stringPreferencesKey("skip_silent")
        val SAVE_STATE_OF_PLAYBACK = stringPreferencesKey("save_state_of_playback")
        val SAVE_RECENT_SONG = stringPreferencesKey("save_recent_song")
        val RECENT_SONG_MEDIA_ID_KEY = stringPreferencesKey("recent_song_media_id")
        val RECENT_SONG_POSITION_KEY = stringPreferencesKey("recent_song_position")
        val SHUFFLE_KEY = stringPreferencesKey("shuffle_key")
        val REPEAT_KEY = stringPreferencesKey("repeat_key")
        val SEND_BACK_TO_GOOGLE = stringPreferencesKey("send_back_to_google")
        val FROM_SAVED_PLAYLIST = stringPreferencesKey("from_saved_playlist")

        val KILL_SERVICE_ON_EXIT = stringPreferencesKey("kill_service_on_exit")
        val CROSSFADE_ENABLED = stringPreferencesKey("crossfade_enabled")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        val LYRICS_PROVIDER = stringPreferencesKey("lyrics_provider")
        val TRANSLATION_LANGUAGE = stringPreferencesKey("translation_language")
        val USE_TRANSLATION_LANGUAGE = stringPreferencesKey("use_translation_language")

        val SPONSOR_BLOCK_ENABLED = stringPreferencesKey("sponsor_block_enabled")
        val MAX_SONG_CACHE_SIZE = intPreferencesKey("maxSongCacheSize")
        val WATCH_VIDEO_INSTEAD_OF_PLAYING_AUDIO =
            stringPreferencesKey("watch_video_instead_of_playing_audio")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val SPDC = stringPreferencesKey("sp_dc")
        val SPOTIFY_LYRICS = stringPreferencesKey("spotify_lyrics")
        val SPOTIFY_CANVAS = stringPreferencesKey("spotify_canvas")
        val SPOTIFY_CLIENT_TOKEN = stringPreferencesKey("spotify_client_token")
        val SPOTIFY_CLIENT_TOKEN_EXPIRES = longPreferencesKey("spotify_client_token_expires")
        val SPOTIFY_PERSONAL_TOKEN = stringPreferencesKey("spotify_personal_token")
        val SPOTIFY_PERSONAL_TOKEN_EXPIRES = longPreferencesKey("spotify_personal_token_expires")
        val HOME_LIMIT = intPreferencesKey("home_limit")
        val CHART_KEY = stringPreferencesKey("chart_key")
        val TRANSLUCENT_BOTTOM_BAR = stringPreferencesKey("translucent_bottom_bar")
        val USING_PROXY = stringPreferencesKey("using_proxy")
        val PROXY_TYPE = stringPreferencesKey("proxy_type")
        val PROXY_HOST = stringPreferencesKey("proxy_host")
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val ENDLESS_QUEUE = stringPreferencesKey("endless_queue")
        val SHOULD_SHOW_LOG_IN_REQUIRED_ALERT = stringPreferencesKey("should_show_log_in_required_alert")
        val AUTO_CHECK_FOR_UPDATES = stringPreferencesKey("auto_check_for_updates")
        val UPDATE_CHANNEL = stringPreferencesKey("update_channel")
        val BLUR_FULLSCREEN_LYRICS = stringPreferencesKey("blur_fullscreen_lyrics")
        val BLUR_PLAYER_BACKGROUND = stringPreferencesKey("blur_player_background")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PITCH = intPreferencesKey("pitch")
        val OPEN_APP_TIME = intPreferencesKey("open_app_time")
        val DATA_SYNC_ID = stringPreferencesKey("data_sync_id")
        val VISITOR_DATA = stringPreferencesKey("visitor_data")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_API_KEY = stringPreferencesKey("ai_gemini_api_key")

        val CUSTOM_MODEL_ID = stringPreferencesKey("custom_model_id")

        val USE_AI_TRANSLATION = stringPreferencesKey("use_ai_translation")

        val LOCAL_PLAYLIST_FILTER = stringPreferencesKey("local_playlist_filter")
        val YOUTUBE_SUBTITLE_LANGUAGE = stringPreferencesKey("youtube_subtitle_language")
        val HELP_BUILD_LYRICS_DATABASE = stringPreferencesKey("help_build_lyrics_database")
        val CONTRIBUTOR_NAME = stringPreferencesKey("contributor_name")
        val CONTRIBUTOR_EMAIL = stringPreferencesKey("contributor_email")

        val BACKUP_DOWNLOADED = stringPreferencesKey("backup_downloaded")

        val LIQUID_GLASS = stringPreferencesKey("liquid_glass")

        val EXPLICIT_CONTENT_ENABLED = stringPreferencesKey("explicit_content_enabled")
    }
}