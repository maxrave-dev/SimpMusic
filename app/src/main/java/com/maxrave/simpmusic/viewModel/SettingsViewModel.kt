package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.app.usage.StorageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.maxrave.kotlinytmusicscraper.models.simpmusic.GithubResponse
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DB_NAME
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.QUALITY
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.SETTINGS_FILENAME
import com.maxrave.simpmusic.common.VIDEO_QUALITY
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.entities.GoogleAccountEntity
import com.maxrave.simpmusic.extension.bytesToMB
import com.maxrave.simpmusic.extension.div
import com.maxrave.simpmusic.extension.getSizeOfFile
import com.maxrave.simpmusic.extension.zipInputStream
import com.maxrave.simpmusic.extension.zipOutputStream
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.service.test.download.DownloadUtils
import com.maxrave.simpmusic.utils.LocalResource
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry

@UnstableApi
class SettingsViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    private val databasePath: String? = mainRepository.getDatabasePath()
    private val playerCache: SimpleCache by inject(qualifier = named(Config.PLAYER_CACHE))
    private val downloadCache: SimpleCache by inject(qualifier = named(Config.DOWNLOAD_CACHE))
    private val canvasCache: SimpleCache by inject(qualifier = named(Config.CANVAS_CACHE))
    private val downloadUtils: DownloadUtils by inject()

    private var _location: MutableStateFlow<String?> = MutableStateFlow(null)
    val location: StateFlow<String?> = _location
    private var _language: MutableStateFlow<String?> = MutableStateFlow(null)
    val language: StateFlow<String?> = _language
    private var _loggedIn: MutableStateFlow<String?> = MutableStateFlow(null)
    val loggedIn: StateFlow<String?> = _loggedIn
    private var _normalizeVolume: MutableStateFlow<String?> = MutableStateFlow(null)
    val normalizeVolume: StateFlow<String?> = _normalizeVolume
    private var _skipSilent: MutableStateFlow<String?> = MutableStateFlow(null)
    val skipSilent: StateFlow<String?> = _skipSilent
    private var _savedPlaybackState: MutableStateFlow<String?> = MutableStateFlow(null)
    val savedPlaybackState: StateFlow<String?> = _savedPlaybackState
    private var _saveRecentSongAndQueue: MutableStateFlow<String?> = MutableStateFlow(null)
    val saveRecentSongAndQueue: StateFlow<String?> = _saveRecentSongAndQueue
    private var _lastCheckForUpdate: MutableStateFlow<String?> = MutableStateFlow(null)
    val lastCheckForUpdate: StateFlow<String?> = _lastCheckForUpdate
    private var _githubResponse = MutableStateFlow<GithubResponse?>(null)
    val githubResponse: StateFlow<GithubResponse?> = _githubResponse
    private var _sponsorBlockEnabled: MutableStateFlow<String?> = MutableStateFlow(null)
    val sponsorBlockEnabled: StateFlow<String?> = _sponsorBlockEnabled
    private var _sponsorBlockCategories: MutableStateFlow<ArrayList<String>?> =
        MutableStateFlow(null)
    val sponsorBlockCategories: StateFlow<ArrayList<String>?> = _sponsorBlockCategories
    private var _sendBackToGoogle: MutableStateFlow<String?> = MutableStateFlow(null)
    val sendBackToGoogle: StateFlow<String?> = _sendBackToGoogle
    private var _mainLyricsProvider: MutableStateFlow<String?> = MutableStateFlow(null)
    val mainLyricsProvider: StateFlow<String?> = _mainLyricsProvider
    private var _musixmatchLoggedIn: MutableStateFlow<String?> = MutableStateFlow(null)
    val musixmatchLoggedIn: StateFlow<String?> = _musixmatchLoggedIn
    private var _translationLanguage: MutableStateFlow<String?> = MutableStateFlow(null)
    val translationLanguage: StateFlow<String?> = _translationLanguage
    private var _useTranslation: MutableStateFlow<String?> = MutableStateFlow(null)
    val useTranslation: StateFlow<String?> = _useTranslation
    private var _playerCacheLimit: MutableStateFlow<Int?> = MutableStateFlow(null)
    val playerCacheLimit: StateFlow<Int?> = _playerCacheLimit
    private var _playVideoInsteadOfAudio: MutableStateFlow<String?> = MutableStateFlow(null)
    val playVideoInsteadOfAudio: StateFlow<String?> = _playVideoInsteadOfAudio
    private var _videoQuality: MutableStateFlow<String?> = MutableStateFlow(null)
    val videoQuality: StateFlow<String?> = _videoQuality
    private var _thumbCacheSize = MutableStateFlow<Long?>(null)
    val thumbCacheSize: StateFlow<Long?> = _thumbCacheSize
    private var _canvasCacheSize: MutableStateFlow<Long?> = MutableStateFlow(null)
    val canvasCacheSize: StateFlow<Long?> = _canvasCacheSize
    private var _homeLimit = MutableStateFlow<Int?>(null)
    val homeLimit: StateFlow<Int?> = _homeLimit
    private var _translucentBottomBar: MutableStateFlow<String?> = MutableStateFlow(null)
    val translucentBottomBar: StateFlow<String?> = _translucentBottomBar
    private var _usingProxy = MutableStateFlow(false)
    val usingProxy: StateFlow<Boolean> = _usingProxy
    private var _proxyType = MutableStateFlow(DataStoreManager.Settings.ProxyType.PROXY_TYPE_HTTP)
    val proxyType: StateFlow<DataStoreManager.Settings.ProxyType> = _proxyType
    private var _proxyHost = MutableStateFlow("")
    val proxyHost: StateFlow<String> = _proxyHost
    private var _proxyPort = MutableStateFlow(8000)
    val proxyPort: StateFlow<Int> = _proxyPort
    private var _autoCheckUpdate = MutableStateFlow(false)
    val autoCheckUpdate: StateFlow<Boolean> = _autoCheckUpdate
    private var _blurFullscreenLyrics = MutableStateFlow(false)
    val blurFullscreenLyrics: StateFlow<Boolean> = _blurFullscreenLyrics
    private var _blurPlayerBackground = MutableStateFlow(false)
    val blurPlayerBackground: StateFlow<Boolean> = _blurPlayerBackground
    private val _aiProvider = MutableStateFlow<String>(DataStoreManager.AI_PROVIDER_OPENAI)
    val aiProvider: StateFlow<String> = _aiProvider
    private val _isHasApiKey = MutableStateFlow<Boolean>(false)
    val isHasApiKey: StateFlow<Boolean> = _isHasApiKey
    private val _useAITranslation = MutableStateFlow<Boolean>(false)
    val useAITranslation: StateFlow<Boolean> = _useAITranslation
    private val _customModelId = MutableStateFlow<String>("")
    val customModelId: StateFlow<String> = _customModelId
    private val _crossfadeEnabled = MutableStateFlow<Boolean>(false)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled
    private val _crossfadeDuration = MutableStateFlow<Int>(5000)
    val crossfadeDuration: StateFlow<Int> = _crossfadeDuration
    private val _youtubeSubtitleLanguage = MutableStateFlow<String>("")
    val youtubeSubtitleLanguage: StateFlow<String> = _youtubeSubtitleLanguage

    private var _helpBuildLyricsDatabase: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val helpBuildLyricsDatabase: StateFlow<Boolean> = _helpBuildLyricsDatabase
    private var _contributor: MutableStateFlow<Pair<String, String>> = MutableStateFlow(Pair("", ""))
    val contributor: StateFlow<Pair<String, String>> = _contributor

    private var _alertData: MutableStateFlow<SettingAlertState?> = MutableStateFlow(null)
    val alertData: StateFlow<SettingAlertState?> = _alertData

    private var _basicAlertData: MutableStateFlow<SettingBasicAlertState?> = MutableStateFlow(null)
    val basicAlertData: StateFlow<SettingBasicAlertState?> = _basicAlertData

    // Fraction of storage
    private var _fraction: MutableStateFlow<SettingsStorageSectionFraction> =
        MutableStateFlow(
            SettingsStorageSectionFraction(),
        )
    val fraction: StateFlow<SettingsStorageSectionFraction> = _fraction

    // Biến để lưu trữ và hiển thị trạng thái killServiceOnExit
    private var _killServiceOnExit: MutableStateFlow<String?> = MutableStateFlow(null)
    val killServiceOnExit: StateFlow<String?> = _killServiceOnExit

    init {
        getYoutubeSubtitleLanguage()
        getHelpBuildLyricsDatabase()
    }

    fun getAudioSessionId() = simpleMediaServiceHandler.player.audioSessionId

    fun getData() {
        getLocation()
        getLanguage()
        getQuality()
        getPlayerCacheSize()
        getDownloadedCacheSize()
        getPlayerCacheLimit()
        getLoggedIn()
        getNormalizeVolume()
        getSkipSilent()
        getSavedPlaybackState()
        getSendBackToGoogle()
        getSaveRecentSongAndQueue()
        getLastCheckForUpdate()
        getSponsorBlockEnabled()
        getSponsorBlockCategories()
        getTranslationLanguage()
        getYoutubeSubtitleLanguage()
        getLyricsProvider()
        getUseTranslation()
        getMusixmatchLoggedIn()
        getHomeLimit()
        getPlayVideoInsteadOfAudio()
        getVideoQuality()
        getThumbCacheSize()
        getSpotifyLogIn()
        getSpotifyLyrics()
        getSpotifyCanvas()
        getUsingProxy()
        getCanvasCache()
        getTranslucentBottomBar()
        getAutoCheckUpdate()
        getBlurFullscreenLyrics()
        getBlurPlayerBackground()
        getAIProvider()
        getAIApiKey()
        getAITranslation()
        getCustomModelId()
        getKillServiceOnExit()
        getCrossfadeEnabled()
        getCrossfadeDuration()
        getContributorNameAndEmail()
        viewModelScope.launch {
            calculateDataFraction()
        }
    }

    private fun getContributorNameAndEmail() {
        viewModelScope.launch {
            combine(dataStoreManager.contributorName, dataStoreManager.contributorEmail) { name, email ->
                name to email
            }.collect { contributor ->
                _contributor.value = contributor
            }
        }
    }

    fun setContributorName(name: String) {
        viewModelScope.launch {
            dataStoreManager.setContributorLyricsDatabase(name to contributor.value.second)
            getContributorNameAndEmail()
        }
    }

    fun setContributorEmail(email: String) {
        viewModelScope.launch {
            dataStoreManager.setContributorLyricsDatabase(contributor.value.first to email)
            getContributorNameAndEmail()
        }
    }

    private fun getCustomModelId() {
        viewModelScope.launch {
            dataStoreManager.customModelId.collect { customModelId ->
                _customModelId.value = customModelId
            }
        }
    }

    fun setCustomModelId(modelId: String) {
        viewModelScope.launch {
            dataStoreManager.setCustomModelId(modelId)
            getCustomModelId()
        }
    }

    private fun getAIProvider() {
        viewModelScope.launch {
            dataStoreManager.aiProvider.collect { aiProvider ->
                _aiProvider.value = aiProvider
            }
        }
    }

    fun setAIProvider(provider: String) {
        viewModelScope.launch {
            dataStoreManager.setAIProvider(provider)
            getAIProvider()
        }
    }

    private fun getAITranslation() {
        viewModelScope.launch {
            dataStoreManager.useAITranslation.collect { useAITranslation ->
                _useAITranslation.value = useAITranslation == DataStoreManager.TRUE
            }
        }
    }

    fun setAITranslation(useAITranslation: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setUseAITranslation(useAITranslation)
            getAITranslation()
        }
    }

    private fun getAIApiKey() {
        viewModelScope.launch {
            dataStoreManager.aiApiKey.collect { aiApiKey ->
                if (aiApiKey.isNotEmpty()) {
                    _isHasApiKey.value = true
                    log("getAIApiKey: $aiApiKey", Log.DEBUG)
                } else {
                    _isHasApiKey.value = false
                }
            }
        }
    }

    fun setAIApiKey(apiKey: String) {
        viewModelScope.launch {
            dataStoreManager.setAIApiKey(apiKey)
            getAIApiKey()
        }
    }

    private fun getBlurFullscreenLyrics() {
        viewModelScope.launch {
            dataStoreManager.blurFullscreenLyrics.collect { blurFullscreenLyrics ->
                _blurFullscreenLyrics.value = blurFullscreenLyrics == DataStoreManager.TRUE
            }
        }
    }

    fun setBlurFullscreenLyrics(blurFullscreenLyrics: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setBlurFullscreenLyrics(blurFullscreenLyrics)
            getBlurFullscreenLyrics()
        }
    }

    private fun getBlurPlayerBackground() {
        viewModelScope.launch {
            dataStoreManager.blurPlayerBackground.collect { blurPlayerBackground ->
                _blurPlayerBackground.value = blurPlayerBackground == DataStoreManager.TRUE
            }
        }
    }

    fun setBlurPlayerBackground(blurPlayerBackground: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setBlurPlayerBackground(blurPlayerBackground)
            getBlurPlayerBackground()
        }
    }

    private fun getAutoCheckUpdate() {
        viewModelScope.launch {
            dataStoreManager.autoCheckForUpdates.collect { autoCheckUpdate ->
                _autoCheckUpdate.value = autoCheckUpdate == DataStoreManager.TRUE
            }
        }
    }

    fun setAutoCheckUpdate(autoCheckUpdate: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setAutoCheckForUpdates(autoCheckUpdate)
            getAutoCheckUpdate()
        }
    }

    private fun getCanvasCache() {
        _canvasCacheSize.value = canvasCache.cacheSpace
    }

    fun setAlertData(alertData: SettingAlertState?) {
        _alertData.value = alertData
    }

    fun setBasicAlertData(alertData: SettingBasicAlertState?) {
        _basicAlertData.value = alertData
    }

    private fun getUsingProxy() {
        viewModelScope.launch {
            dataStoreManager.usingProxy.collectLatest { usingProxy ->
                if (usingProxy == DataStoreManager.TRUE) {
                    getProxy()
                }
                _usingProxy.value = usingProxy == DataStoreManager.TRUE
            }
        }
    }

    fun setUsingProxy(usingProxy: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setUsingProxy(usingProxy)
            getUsingProxy()
            getProxy()
        }
    }

    private fun getProxy() {
        viewModelScope.launch {
            val host =
                launch {
                    dataStoreManager.proxyHost.collect {
                        _proxyHost.value = it
                    }
                }
            val port =
                launch {
                    dataStoreManager.proxyPort.collect {
                        _proxyPort.value = it
                    }
                }
            val type =
                launch {
                    dataStoreManager.proxyType.collect {
                        _proxyType.value = it
                        log("getProxy: $it", Log.DEBUG)
                    }
                }
            host.join()
            port.join()
            type.join()
        }
    }

    fun setProxy(
        proxyType: DataStoreManager.Settings.ProxyType,
        host: String,
        port: Int,
    ) {
        log("setProxy: $proxyType, $host, $port", Log.DEBUG)
        viewModelScope.launch {
            dataStoreManager.setProxyType(proxyType)
            dataStoreManager.setProxyHost(host)
            dataStoreManager.setProxyPort(port)
        }
    }

    private suspend fun calculateDataFraction() {
        withContext(Dispatchers.Default) {
            val mStorageStatsManager =
                application.getSystemService(StorageStatsManager::class.java)
            if (mStorageStatsManager != null) {
                val totalByte =
                    mStorageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT).bytesToMB()
                val freeSpace =
                    mStorageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT).bytesToMB()
                val usedSpace = totalByte - freeSpace
                val simpMusicSize = getSizeOfFile(application.filesDir).bytesToMB()
                val thumbSize = (application.imageLoader.diskCache?.size ?: 0L).bytesToMB()
                val otherApp = simpMusicSize.let { usedSpace.minus(it) - thumbSize }
                val databaseSize =
                    simpMusicSize - playerCache.cacheSpace.bytesToMB() - downloadCache.cacheSpace.bytesToMB() - canvasCache.cacheSpace.bytesToMB()
                if (totalByte ==
                    freeSpace + otherApp + simpMusicSize + thumbSize
                ) {
                    withContext(Dispatchers.Main) {
                        _fraction.update {
                            it.copy(
                                otherApp = otherApp.toFloat().div(totalByte.toFloat()),
                                downloadCache =
                                    downloadCache.cacheSpace
                                        .bytesToMB()
                                        .toFloat()
                                        .div(totalByte.toFloat()),
                                playerCache =
                                    playerCache.cacheSpace
                                        .bytesToMB()
                                        .toFloat()
                                        .div(totalByte.toFloat()),
                                canvasCache =
                                    canvasCache.cacheSpace
                                        .bytesToMB()
                                        .toFloat()
                                        .div(totalByte.toFloat()),
                                thumbCache = thumbSize.toFloat().div(totalByte.toFloat()),
                                freeSpace = freeSpace.toFloat().div(totalByte.toFloat()),
                                appDatabase = databaseSize.toFloat().div(totalByte.toFloat()),
                            )
                        }
                        log("calculateDataFraction: $totalByte, $freeSpace, $usedSpace, $simpMusicSize, $otherApp, $databaseSize", Log.WARN)
                        log("calculateDataFraction: ${_fraction.value}", Log.WARN)
                        log("calculateDataFraction: ${_fraction.value.combine()}", Log.WARN)
                    }
                }
            }
        }
    }

    fun getTranslucentBottomBar() {
        viewModelScope.launch {
            dataStoreManager.translucentBottomBar.collect { translucentBottomBar ->
                _translucentBottomBar.emit(translucentBottomBar)
            }
        }
    }

    fun setTranslucentBottomBar(translucentBottomBar: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setTranslucentBottomBar(translucentBottomBar)
            getTranslucentBottomBar()
        }
    }

    fun getThumbCacheSize() {
        viewModelScope.launch {
            val diskCache = application.imageLoader.diskCache
            _thumbCacheSize.emit(diskCache?.size)
        }
    }

    fun getVideoQuality() {
        viewModelScope.launch {
            dataStoreManager.videoQuality.collect { videoQuality ->
                when (videoQuality) {
                    VIDEO_QUALITY.items[0].toString() -> _videoQuality.emit(VIDEO_QUALITY.items[0].toString())
                    VIDEO_QUALITY.items[1].toString() -> _videoQuality.emit(VIDEO_QUALITY.items[1].toString())
                }
            }
        }
    }

    fun getTranslationLanguage() {
        viewModelScope.launch {
            dataStoreManager.translationLanguage.collect { translationLanguage ->
                _translationLanguage.emit(translationLanguage)
            }
        }
    }

    fun setTranslationLanguage(language: String) {
        viewModelScope.launch {
            dataStoreManager.setTranslationLanguage(language)
            getTranslationLanguage()
        }
    }

    fun getUseTranslation() {
        viewModelScope.launch {
            dataStoreManager.enableTranslateLyric.collect { useTranslation ->
                _useTranslation.emit(useTranslation)
            }
        }
    }

    fun setUseTranslation(useTranslation: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setEnableTranslateLyric(useTranslation)
            getUseTranslation()
        }
    }

    fun getMusixmatchLoggedIn() {
        viewModelScope.launch {
            dataStoreManager.musixmatchLoggedIn.collect { musixmatchLoggedIn ->
                _musixmatchLoggedIn.emit(musixmatchLoggedIn)
            }
        }
    }

    fun setMusixmatchLoggedIn(loggedIn: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setMusixmatchLoggedIn(loggedIn)
            getMusixmatchLoggedIn()
        }
    }

    fun getLyricsProvider() {
        viewModelScope.launch {
            dataStoreManager.lyricsProvider.collect { mainLyricsProvider ->
                _mainLyricsProvider.emit(mainLyricsProvider)
            }
        }
    }

    fun setLyricsProvider(provider: String) {
        viewModelScope.launch {
            dataStoreManager.setLyricsProvider(provider)
            getLyricsProvider()
        }
    }

    fun getLocation() {
        viewModelScope.launch {
            dataStoreManager.location.collect { location ->
                _location.emit(location)
            }
        }
    }

    fun getLoggedIn() {
        viewModelScope.launch {
            dataStoreManager.loggedIn.collect { loggedIn ->
                _loggedIn.emit(loggedIn)
            }
        }
    }

    fun changeLocation(location: String) {
        viewModelScope.launch {
            dataStoreManager.setLocation(location)
            getLocation()
        }
    }

    fun getSaveRecentSongAndQueue() {
        viewModelScope.launch {
            dataStoreManager.saveRecentSongAndQueue.collect { saved ->
                _saveRecentSongAndQueue.emit(saved)
            }
        }
    }

    fun getLastCheckForUpdate() {
        viewModelScope.launch {
            dataStoreManager.getString("CheckForUpdateAt").first().let { lastCheckForUpdate ->
                _githubResponse.emit(null)
                _lastCheckForUpdate.emit(lastCheckForUpdate)
            }
        }
    }

    fun getSponsorBlockEnabled() {
        viewModelScope.launch {
            dataStoreManager.sponsorBlockEnabled.first().let { enabled ->
                _sponsorBlockEnabled.emit(enabled)
            }
        }
    }

    fun setSponsorBlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSponsorBlockEnabled(enabled)
            getSponsorBlockEnabled()
        }
    }

    fun getPlayVideoInsteadOfAudio() {
        viewModelScope.launch {
            dataStoreManager.watchVideoInsteadOfPlayingAudio.collect { playVideoInsteadOfAudio ->
                _playVideoInsteadOfAudio.emit(playVideoInsteadOfAudio)
            }
        }
    }

    fun setPlayVideoInsteadOfAudio(playVideoInsteadOfAudio: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setWatchVideoInsteadOfPlayingAudio(playVideoInsteadOfAudio)
            getPlayVideoInsteadOfAudio()
        }
    }

    fun getSponsorBlockCategories() {
        viewModelScope.launch {
            dataStoreManager.getSponsorBlockCategories().let {
                log("getSponsorBlockCategories: $it", Log.WARN)
                _sponsorBlockCategories.emit(it)
            }
        }
    }

    fun setSponsorBlockCategories(list: ArrayList<String>) {
        log("setSponsorBlockCategories: $list", Log.WARN)
        viewModelScope.launch {
            runBlocking(Dispatchers.IO) {
                dataStoreManager.setSponsorBlockCategories(list)
            }
            getSponsorBlockCategories()
        }
    }

    private var _quality: MutableStateFlow<String?> = MutableStateFlow(null)
    val quality: StateFlow<String?> = _quality

    fun getQuality() {
        viewModelScope.launch {
            dataStoreManager.quality.collect { quality ->
                when (quality) {
                    QUALITY.items[0].toString() -> _quality.emit(QUALITY.items[0].toString())
                    QUALITY.items[1].toString() -> _quality.emit(QUALITY.items[1].toString())
                }
            }
        }
    }

    fun changeVideoQuality(item: String) {
        viewModelScope.launch {
            if (VIDEO_QUALITY.items.contains(item)) {
                dataStoreManager.setVideoQuality(item)
            }
            getVideoQuality()
        }
    }

    fun changeQuality(qualityItem: String?) {
        viewModelScope.launch {
            dataStoreManager.setQuality(qualityItem ?: QUALITY.items.first().toString())
            getQuality()
        }
    }

    private val _cacheSize: MutableStateFlow<Long?> = MutableStateFlow(null)
    var cacheSize: StateFlow<Long?> = _cacheSize

    @UnstableApi
    fun getPlayerCacheSize() {
        _cacheSize.value = playerCache.cacheSpace
    }

    @UnstableApi
    fun clearPlayerCache() {
        viewModelScope.launch {
            playerCache.keys.forEach { key ->
                playerCache.removeResource(key)
            }
            makeToast(getString(R.string.clear_player_cache))
            _cacheSize.value = playerCache.cacheSpace
        }
    }

    private val _downloadedCacheSize: MutableStateFlow<Long?> = MutableStateFlow(null)
    var downloadedCacheSize: StateFlow<Long?> = _downloadedCacheSize

    @UnstableApi
    fun getDownloadedCacheSize() {
        _downloadedCacheSize.value = downloadCache.cacheSpace
    }

    @UnstableApi
    fun clearDownloadedCache() {
        viewModelScope.launch {
            downloadCache.keys.forEach { key ->
                downloadCache.removeResource(key)
            }
            mainRepository.getDownloadedSongs().singleOrNull()?.let { songs ->
                songs.forEach { song ->
                    mainRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
            makeToast(getString(R.string.clear_downloaded_cache))
            _cacheSize.value = playerCache.cacheSpace
            downloadUtils.removeAllDownloads()
        }
    }

    @UnstableApi
    fun clearCanvasCache() {
        viewModelScope.launch {
            canvasCache.keys.forEach { key ->
                canvasCache.removeResource(key)
            }
            makeToast(getString(R.string.clear_canvas_cache))
            _canvasCacheSize.value = canvasCache.cacheSpace
        }
    }

    fun backup(uri: Uri) {
        runCatching {
            application.applicationContext.contentResolver.openOutputStream(uri)?.use {
                it.buffered().zipOutputStream().use { outputStream ->
                    (application.filesDir / "datastore" / "$SETTINGS_FILENAME.preferences_pb").inputStream().buffered().use { inputStream ->
                        outputStream.putNextEntry(ZipEntry("$SETTINGS_FILENAME.preferences_pb"))
                        inputStream.copyTo(outputStream)
                    }
                    runBlocking(Dispatchers.IO) {
                        mainRepository.databaseDaoCheckpoint()
                    }
                    FileInputStream(databasePath).use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(DB_NAME))
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }.onSuccess {
            makeToast(getString(R.string.backup_create_success))
        }.onFailure {
            it.printStackTrace()
            makeToast(getString(R.string.backup_create_failed))
        }
    }

    @UnstableApi
    fun restore(uri: Uri) {
        runCatching {
            application.applicationContext.contentResolver.openInputStream(uri)?.use {
                it.zipInputStream().use { inputStream ->
                    var entry =
                        try {
                            inputStream.nextEntry
                        } catch (e: Exception) {
                            null
                        }
                    var count = 0
                    while (entry != null && count < 2) {
                        when (entry.name) {
                            "$SETTINGS_FILENAME.preferences_pb" -> {
                                (application.filesDir / "datastore" / "$SETTINGS_FILENAME.preferences_pb").outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }

                            DB_NAME -> {
                                runBlocking(Dispatchers.IO) {
                                    mainRepository.databaseDaoCheckpoint()
                                    mainRepository.closeDatabase()
                                }
                                FileOutputStream(databasePath).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        count++
                        entry = inputStream.nextEntry
                    }
                }
            }
            makeToast(getString(R.string.restore_success))
            application.stopService(Intent(application, SimpleMediaService::class.java))
            getData()
            val ctx = application.applicationContext
            val pm: PackageManager = ctx.packageManager
            val intent = pm.getLaunchIntentForPackage(ctx.packageName)
            val mainIntent = Intent.makeRestartActivityTask(intent?.component)
            ctx.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }.onFailure {
            it.printStackTrace()
            makeToast(getString(R.string.restore_failed))
        }
    }

    fun getLanguage() {
        viewModelScope.launch {
            dataStoreManager.getString(SELECTED_LANGUAGE).collect { language ->
                _language.emit(language)
            }
        }
    }

    @UnstableApi
    fun changeLanguage(code: String) {
        viewModelScope.launch {
            dataStoreManager.putString(SELECTED_LANGUAGE, code)
            Log.w("SettingsViewModel", "changeLanguage: $code")
            getLanguage()
            val localeList =
                LocaleListCompat.forLanguageTags(
                    if (code == "id-ID") {
                        if (Build.VERSION.SDK_INT >= 35) {
                            "id-ID"
                        } else {
                            "in-ID"
                        }
                    } else {
                        code
                    },
                )
            Log.d("Language", localeList.toString())
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }

    fun clearCookie() {
        viewModelScope.launch {
            dataStoreManager.setCookie("")
            dataStoreManager.setLoggedIn(false)
        }
    }

    fun getNormalizeVolume() {
        viewModelScope.launch {
            dataStoreManager.normalizeVolume.collect { normalizeVolume ->
                _normalizeVolume.emit(normalizeVolume)
            }
        }
    }

    @UnstableApi
    fun setNormalizeVolume(normalizeVolume: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setNormalizeVolume(normalizeVolume)
            getNormalizeVolume()
        }
    }

    fun getSendBackToGoogle() {
        viewModelScope.launch {
            dataStoreManager.sendBackToGoogle.collect { sendBackToGoogle ->
                _sendBackToGoogle.emit(sendBackToGoogle)
            }
        }
    }

    fun setSendBackToGoogle(sendBackToGoogle: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSendBackToGoogle(sendBackToGoogle)
            getSendBackToGoogle()
        }
    }

    fun getSkipSilent() {
        viewModelScope.launch {
            dataStoreManager.skipSilent.collect { skipSilent ->
                _skipSilent.emit(skipSilent)
            }
        }
    }

    @UnstableApi
    fun setSkipSilent(skip: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSkipSilent(skip)
            getSkipSilent()
        }
    }

    fun getSavedPlaybackState() {
        viewModelScope.launch {
            dataStoreManager.saveStateOfPlayback.collect { savedPlaybackState ->
                _savedPlaybackState.emit(savedPlaybackState)
            }
        }
    }

    fun setSavedPlaybackState(savedPlaybackState: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSaveStateOfPlayback(savedPlaybackState)
            getSavedPlaybackState()
        }
    }

    fun setSaveLastPlayed(b: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSaveRecentSongAndQueue(b)
            getSaveRecentSongAndQueue()
        }
    }

    fun clearMusixmatchCookie() {
        viewModelScope.launch {
            dataStoreManager.setMusixmatchCookie("")
            dataStoreManager.setMusixmatchLoggedIn(false)
            makeToast(getString(R.string.logged_out))
        }
    }

    fun getPlayerCacheLimit() {
        viewModelScope.launch {
            dataStoreManager.maxSongCacheSize.collect {
                _playerCacheLimit.emit(it)
            }
        }
    }

    fun setPlayerCacheLimit(size: Int) {
        viewModelScope.launch {
            dataStoreManager.setMaxSongCacheSize(size)
            getPlayerCacheLimit()
        }
    }

    private var _googleAccounts: MutableStateFlow<LocalResource<List<GoogleAccountEntity>>> =
        MutableStateFlow(LocalResource.Loading())
    val googleAccounts: StateFlow<LocalResource<List<GoogleAccountEntity>>> = _googleAccounts

    fun getAllGoogleAccount() {
        Log.w("getAllGoogleAccount", "getAllGoogleAccount: Go to function")
        viewModelScope.launch {
            _googleAccounts.emit(LocalResource.Loading())
            mainRepository.getGoogleAccounts().collectLatest { accounts ->
                Log.w("getAllGoogleAccount", "getAllGoogleAccount: $accounts")
                if (!accounts.isNullOrEmpty()) {
                    _googleAccounts.emit(LocalResource.Success(accounts))
                } else {
                    if (loggedIn.value == DataStoreManager.TRUE) {
                        mainRepository
                            .getAccountInfo(
                                dataStoreManager.cookie.first(),
                            ).collect {
                                Log.w("getAllGoogleAccount", "getAllGoogleAccount: $it")
                                if (it != null) {
                                    dataStoreManager.putString("AccountName", it.name)
                                    dataStoreManager.putString(
                                        "AccountThumbUrl",
                                        it.thumbnails.lastOrNull()?.url ?: "",
                                    )
                                    mainRepository
                                        .insertGoogleAccount(
                                            GoogleAccountEntity(
                                                email = it.email,
                                                name = it.name,
                                                thumbnailUrl = it.thumbnails.lastOrNull()?.url ?: "",
                                                cache = mainRepository.getYouTubeCookie(),
                                                isUsed = true,
                                            ),
                                        ).singleOrNull()
                                        ?.let { account ->
                                            Log.w("getAllGoogleAccount", "inserted: $account")
                                        }
                                    getAllGoogleAccount()
                                } else {
                                    _googleAccounts.emit(LocalResource.Success(emptyList()))
                                }
                            }
                    } else {
                        _googleAccounts.emit(LocalResource.Success(emptyList()))
                    }
                }
            }
        }
    }

    suspend fun addAccount(cookie: String): Boolean {
        val currentCookie = dataStoreManager.cookie.first()
        val currentLoggedIn = dataStoreManager.loggedIn.first() == DataStoreManager.TRUE
        try {
            runBlocking {
                dataStoreManager.setCookie(cookie)
                dataStoreManager.setLoggedIn(true)
            }
            return mainRepository
                .getAccountInfo(
                    cookie,
                ).lastOrNull()
                ?.let { accountInfo ->
                    Log.d("getAllGoogleAccount", "addAccount: $accountInfo")
                    mainRepository.getGoogleAccounts().lastOrNull()?.forEach {
                        Log.d("getAllGoogleAccount", "set used: $it start")
                        mainRepository
                            .updateGoogleAccountUsed(it.email, false)
                            .singleOrNull()
                            ?.let {
                                Log.w("getAllGoogleAccount", "set used: $it")
                            }
                    }
                    dataStoreManager.putString("AccountName", accountInfo.name)
                    dataStoreManager.putString(
                        "AccountThumbUrl",
                        accountInfo.thumbnails.lastOrNull()?.url ?: "",
                    )
                    mainRepository
                        .insertGoogleAccount(
                            GoogleAccountEntity(
                                email = accountInfo.email,
                                name = accountInfo.name,
                                thumbnailUrl = accountInfo.thumbnails.lastOrNull()?.url ?: "",
                                cache = cookie,
                                isUsed = true,
                            ),
                        ).firstOrNull()
                        ?.let {
                            log("addAccount: $it", Log.WARN)
                        }
                    dataStoreManager.setLoggedIn(true)
                    dataStoreManager.setCookie(cookie)
                    getAllGoogleAccount()
                    getLoggedIn()
                    true
                } ?: run {
                Log.w("getAllGoogleAccount", "addAccount: Account info is null")
                runBlocking {
                    dataStoreManager.setCookie(currentCookie)
                    dataStoreManager.setLoggedIn(currentLoggedIn)
                }
                false
            }
        } catch (e: Exception) {
            Log.e("getAllGoogleAccount", "addAccount: ${e.message}", e)
            runBlocking {
                dataStoreManager.setCookie(currentCookie)
                dataStoreManager.setLoggedIn(currentLoggedIn)
            }
            return false
        }
    }

    fun setUsedAccount(acc: GoogleAccountEntity?) {
        viewModelScope.launch {
            if (acc != null) {
                googleAccounts.value.data?.forEach {
                    mainRepository
                        .updateGoogleAccountUsed(it.email, false)
                        .singleOrNull()
                        ?.let {
                            Log.w("getAllGoogleAccount", "set used: $it")
                        }
                }
                dataStoreManager.putString("AccountName", acc.name)
                dataStoreManager.putString("AccountThumbUrl", acc.thumbnailUrl)
                mainRepository
                    .updateGoogleAccountUsed(acc.email, true)
                    .singleOrNull()
                    ?.let {
                        Log.w("getAllGoogleAccount", "set used: $it")
                    }
                dataStoreManager.setCookie(acc.cache ?: "")
                dataStoreManager.setLoggedIn(true)
                delay(500)
                getAllGoogleAccount()
                getLoggedIn()
            } else {
                googleAccounts.value.data?.forEach {
                    mainRepository
                        .updateGoogleAccountUsed(it.email, false)
                        .singleOrNull()
                        ?.let {
                            Log.w("getAllGoogleAccount", "set used: $it")
                        }
                }
                dataStoreManager.putString("AccountName", "")
                dataStoreManager.putString("AccountThumbUrl", "")
                dataStoreManager.setLoggedIn(false)
                dataStoreManager.setCookie("")
                delay(500)
                getAllGoogleAccount()
                getLoggedIn()
            }
        }
    }

    fun logOutAllYouTube() {
        viewModelScope.launch {
            googleAccounts.value.data?.forEach { account ->
                mainRepository.deleteGoogleAccount(account.email)
            }
            dataStoreManager.putString("AccountName", "")
            dataStoreManager.putString("AccountThumbUrl", "")
            dataStoreManager.setLoggedIn(false)
            dataStoreManager.setCookie("")
            delay(500)
            getAllGoogleAccount()
            getLoggedIn()
        }
    }

    @ExperimentalCoilApi
    fun clearThumbnailCache() {
        viewModelScope.launch {
            application.imageLoader.diskCache?.clear()
            Toast
                .makeText(
                    getApplication(),
                    application.getString(R.string.clear_thumbnail_cache),
                    Toast.LENGTH_SHORT,
                ).show()
            getThumbCacheSize()
        }
    }

    private var _spotifyLogIn: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val spotifyLogIn: StateFlow<Boolean> = _spotifyLogIn

    fun getSpotifyLogIn() {
        viewModelScope.launch {
            dataStoreManager.spdc.collect { loggedIn ->
                if (loggedIn.isNotEmpty()) {
                    _spotifyLogIn.emit(true)
                } else {
                    _spotifyLogIn.emit(false)
                }
            }
        }
    }

    fun setSpotifyLogIn(loggedIn: Boolean) {
        viewModelScope.launch {
            _spotifyLogIn.emit(loggedIn)
            if (!loggedIn) {
                dataStoreManager.setSpdc("")
                delay(500)
            }
            getSpotifyLogIn()
        }
    }

    private var _spotifyLyrics: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val spotifyLyrics: StateFlow<Boolean> = _spotifyLyrics

    private var _spotifyCanvas: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val spotifyCanvas: StateFlow<Boolean> = _spotifyCanvas

    fun getSpotifyLyrics() {
        viewModelScope.launch {
            dataStoreManager.spotifyLyrics.collect {
                if (it == DataStoreManager.TRUE) {
                    _spotifyLyrics.emit(true)
                } else {
                    _spotifyLyrics.emit(false)
                }
            }
        }
    }

    fun setSpotifyLyrics(loggedIn: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSpotifyLyrics(loggedIn)
            getSpotifyLyrics()
        }
    }

    fun getSpotifyCanvas() {
        viewModelScope.launch {
            dataStoreManager.spotifyCanvas.collect {
                if (it == DataStoreManager.TRUE) {
                    _spotifyCanvas.emit(true)
                } else {
                    _spotifyCanvas.emit(false)
                }
            }
        }
    }

    fun setSpotifyCanvas(loggedIn: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setSpotifyCanvas(loggedIn)
            getSpotifyCanvas()
        }
    }

    fun getHomeLimit() {
        viewModelScope.launch {
            dataStoreManager.homeLimit.collect {
                _homeLimit.emit(it)
            }
        }
    }

    fun setHomeLimit(limit: Int) {
        viewModelScope.launch {
            dataStoreManager.setHomeLimit(limit)
            getHomeLimit()
        }
    }

    // Lấy giá trị của killServiceOnExit từ DataStore
    fun getKillServiceOnExit() {
        viewModelScope.launch {
            dataStoreManager.killServiceOnExit.collect { killServiceOnExit ->
                _killServiceOnExit.emit(killServiceOnExit)
            }
        }
    }

    // Lưu giá trị killServiceOnExit vào DataStore
    fun setKillServiceOnExit(kill: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setKillServiceOnExit(kill)
            getKillServiceOnExit()
        }
    }

    private fun getCrossfadeEnabled() {
        viewModelScope.launch {
            dataStoreManager.crossfadeEnabled.collect { crossfadeEnabled ->
                _crossfadeEnabled.value = crossfadeEnabled == DataStoreManager.TRUE
            }
        }
    }

    fun setCrossfadeEnabled(crossfadeEnabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setCrossfadeEnabled(crossfadeEnabled)
            getCrossfadeEnabled()
        }
    }

    private fun getCrossfadeDuration() {
        viewModelScope.launch {
            dataStoreManager.crossfadeDuration.collect { duration ->
                _crossfadeDuration.value = duration
            }
        }
    }

    fun setCrossfadeDuration(duration: Int) {
        viewModelScope.launch {
            dataStoreManager.setCrossfadeDuration(duration)
            getCrossfadeDuration()
        }
    }

    fun getYoutubeSubtitleLanguage() {
        viewModelScope.launch {
            dataStoreManager.youtubeSubtitleLanguage.collect { language ->
                _youtubeSubtitleLanguage.emit(language)
            }
        }
    }

    fun setYoutubeSubtitleLanguage(language: String) {
        viewModelScope.launch {
            dataStoreManager.setYoutubeSubtitleLanguage(language)
            getYoutubeSubtitleLanguage()
        }
    }

    fun getHelpBuildLyricsDatabase() {
        viewModelScope.launch {
            dataStoreManager.helpBuildLyricsDatabase.collect { helpBuildLyricsDatabase ->
                _helpBuildLyricsDatabase.emit(helpBuildLyricsDatabase == DataStoreManager.TRUE)
            }
        }
    }

    fun setHelpBuildLyricsDatabase(help: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setHelpBuildLyricsDatabase(help)
            getHelpBuildLyricsDatabase()
        }
    }
}

data class SettingsStorageSectionFraction(
    val otherApp: Float = 0f,
    val downloadCache: Float = 0f,
    val playerCache: Float = 0f,
    val canvasCache: Float = 0f,
    val thumbCache: Float = 0f,
    val appDatabase: Float = 0f,
    val freeSpace: Float = 0f,
) {
    fun combine(): Float = otherApp + downloadCache + playerCache + canvasCache + thumbCache + appDatabase + freeSpace
}

data class SettingAlertState(
    val title: String,
    val message: String? = null,
    val textField: TextFieldData? = null,
    val selectOne: SelectData? = null,
    val multipleSelect: SelectData? = null,
    val confirm: Pair<String, (SettingAlertState) -> Unit>,
    val dismiss: String,
) {
    data class TextFieldData(
        val label: String,
        val value: String = "",
        // User typing string -> (true or false, If false, show error message)
        val verifyCodeBlock: ((String) -> Pair<Boolean, String?>)? = null,
    )

    data class SelectData(
        // Selected / Data
        val listSelect: List<Pair<Boolean, String>>,
    ) {
        fun getSelected(): String = listSelect.firstOrNull { it.first }?.second ?: ""

        fun getListSelected(): List<String> = listSelect.filter { it.first }.map { it.second }
    }
}

data class SettingBasicAlertState(
    val title: String,
    val message: String? = null,
    val confirm: Pair<String, () -> Unit>,
    val dismiss: String,
)