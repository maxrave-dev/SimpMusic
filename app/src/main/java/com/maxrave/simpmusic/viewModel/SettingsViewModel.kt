package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
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
import com.maxrave.simpmusic.data.db.DatabaseDao
import com.maxrave.simpmusic.data.db.MusicDatabase
import com.maxrave.simpmusic.data.db.entities.GoogleAccountEntity
import com.maxrave.simpmusic.extension.div
import com.maxrave.simpmusic.extension.zipInputStream
import com.maxrave.simpmusic.extension.zipOutputStream
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.ui.MainActivity
import com.maxrave.simpmusic.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.annotation.KoinViewModel
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import kotlin.system.exitProcess

@UnstableApi
@KoinViewModel
class SettingsViewModel(
    private val application: Application,
) : BaseViewModel(application) {
    override val tag: String = "SettingsViewModel"

    private val database: MusicDatabase by inject()
    private val databaseDao: DatabaseDao by inject()
    val playerCache: SimpleCache by inject(named(Config.PLAYER_CACHE))
    val downloadCache: SimpleCache by inject(named(Config.DOWNLOAD_CACHE))

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
    private var _pipedInstance: MutableStateFlow<String?> = MutableStateFlow(null)
    val pipedInstance: StateFlow<String?> = _pipedInstance
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
    private var _homeLimit = MutableStateFlow<Int?>(null)
    val homeLimit: StateFlow<Int?> = _homeLimit
    private var _translucentBottomBar: MutableStateFlow<String?> = MutableStateFlow(null)
    val translucentBottomBar: StateFlow<String?> = _translucentBottomBar

    fun getAudioSessionId() = simpleMediaServiceHandler.player.audioSessionId

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

    fun checkForUpdate() {
        viewModelScope.launch {
            mainRepository.checkForUpdate().collect { response ->
                dataStoreManager.putString(
                    "CheckForUpdateAt",
                    System.currentTimeMillis().toString(),
                )
                _githubResponse.emit(response)
            }
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
            YouTube.locale = YouTubeLocale(location, language.value!!)
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
                _sponsorBlockCategories.emit(it)
            }
        }
    }

    fun setSponsorBlockCategories(list: ArrayList<String>) {
        viewModelScope.launch {
            dataStoreManager.setSponsorBlockCategories(list)
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

    fun changeVideoQuality(checkedIndex: Int) {
        viewModelScope.launch {
            when (checkedIndex) {
                0 -> dataStoreManager.setVideoQuality(VIDEO_QUALITY.items[0].toString())
                1 -> dataStoreManager.setVideoQuality(VIDEO_QUALITY.items[1].toString())
            }
            getVideoQuality()
        }
    }

    fun changeQuality(checkedIndex: Int) {
        viewModelScope.launch {
            when (checkedIndex) {
                0 -> dataStoreManager.setQuality(QUALITY.items[0].toString())
                1 -> dataStoreManager.setQuality(QUALITY.items[1].toString())
            }
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
            Toast.makeText(getApplication(), "Player cache cleared", Toast.LENGTH_SHORT).show()
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
            mainRepository.getDownloadedSongs().collect { songs ->
                songs?.forEach { song ->
                    mainRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
            Toast.makeText(getApplication(), "Download cache cleared", Toast.LENGTH_SHORT).show()
            _cacheSize.value = playerCache.cacheSpace
        }
    }

    fun backup(
        context: Context,
        uri: Uri,
    ) {
        runCatching {
            context.applicationContext.contentResolver.openOutputStream(uri)?.use {
                it.buffered().zipOutputStream().use { outputStream ->
                    (context.filesDir / "datastore" / "$SETTINGS_FILENAME.preferences_pb").inputStream().buffered().use { inputStream ->
                        outputStream.putNextEntry(ZipEntry("$SETTINGS_FILENAME.preferences_pb"))
                        inputStream.copyTo(outputStream)
                    }
                    runBlocking(Dispatchers.IO) {
                        databaseDao.checkpoint()
                    }
                    FileInputStream(database.openHelper.writableDatabase.path).use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(DB_NAME))
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }.onSuccess {
            Toast
                .makeText(
                    context,
                    context.getString(R.string.backup_create_success),
                    Toast.LENGTH_SHORT,
                ).show()
        }.onFailure {
            it.printStackTrace()
            Toast
                .makeText(
                    context,
                    context.getString(R.string.backup_create_failed),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    @UnstableApi
    fun restore(
        context: Context,
        uri: Uri,
    ) {
        runCatching {
            context.applicationContext.contentResolver.openInputStream(uri)?.use {
                it.zipInputStream().use { inputStream ->
                    var entry = inputStream.nextEntry
                    var count = 0
                    while (entry != null && count < 2) {
                        when (entry.name) {
                            "$SETTINGS_FILENAME.preferences_pb" -> {
                                (context.filesDir / "datastore" / "$SETTINGS_FILENAME.preferences_pb").outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }

                            DB_NAME -> {
                                runBlocking(Dispatchers.IO) {
                                    databaseDao.checkpoint()
                                }
                                database.close()
                                FileOutputStream(database.openHelper.writableDatabase.path).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        count++
                        entry = inputStream.nextEntry
                    }
                }
            }
            Toast
                .makeText(
                    context,
                    context.getString(R.string.restore_success),
                    Toast.LENGTH_SHORT,
                ).show()
            context.stopService(Intent(context, SimpleMediaService::class.java))
            context.startActivity(Intent(context, MainActivity::class.java))
            exitProcess(0)
        }.onFailure {
            it.printStackTrace()
            Toast.makeText(context, context.getString(R.string.restore_failed), Toast.LENGTH_SHORT).show()
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
            YouTube.locale = YouTubeLocale(location.value!!, code.substring(0..1))
            getLanguage()
        }
    }

    fun clearCookie() {
        viewModelScope.launch {
            dataStoreManager.setCookie("")
            YouTube.cookie = null
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
            YouTube.musixMatchCookie = null
            dataStoreManager.setMusixmatchLoggedIn(false)
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

    private var _googleAccounts: MutableStateFlow<ArrayList<GoogleAccountEntity>?> =
        MutableStateFlow(null)
    val googleAccounts: MutableStateFlow<ArrayList<GoogleAccountEntity>?> = _googleAccounts

    private var _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading: MutableStateFlow<Boolean> = _loading

    fun getAllGoogleAccount() {
        Log.w("getAllGoogleAccount", "getAllGoogleAccount: Go to function")
        viewModelScope.launch {
            _loading.value = true
            mainRepository.getGoogleAccounts().collect { accounts ->
                Log.w("getAllGoogleAccount", "getAllGoogleAccount: $accounts")
                if (!accounts.isNullOrEmpty()) {
                    _googleAccounts.value = accounts as ArrayList<GoogleAccountEntity>
                    _loading.value = false
                } else {
                    if (loggedIn.value == DataStoreManager.TRUE) {
                        mainRepository.getAccountInfo().collect {
                            Log.w("getAllGoogleAccount", "getAllGoogleAccount: $it")
                            if (it != null) {
                                dataStoreManager.putString("AccountName", it.name)
                                dataStoreManager.putString(
                                    "AccountThumbUrl",
                                    it.thumbnails.lastOrNull()?.url ?: "",
                                )
                                mainRepository.insertGoogleAccount(
                                    GoogleAccountEntity(
                                        email = it.email,
                                        name = it.name,
                                        thumbnailUrl = it.thumbnails.lastOrNull()?.url ?: "",
                                        cache = YouTube.cookie,
                                        isUsed = true,
                                    ),
                                )
                                delay(500)
                                getAllGoogleAccount()
                            } else {
                                _googleAccounts.value = null
                                _loading.value = false
                            }
                        }
                    } else {
                        _googleAccounts.value = null
                        _loading.value = false
                    }
                }
            }
        }
    }

    fun addAccount() {
        viewModelScope.launch {
            mainRepository.getAccountInfo().collect { accountInfo ->
                if (accountInfo != null) {
                    googleAccounts.value?.forEach {
                        mainRepository.updateGoogleAccountUsed(it.email, false)
                    }
                    dataStoreManager.putString("AccountName", accountInfo.name)
                    dataStoreManager.putString(
                        "AccountThumbUrl",
                        accountInfo.thumbnails.lastOrNull()?.url ?: "",
                    )
                    mainRepository.insertGoogleAccount(
                        GoogleAccountEntity(
                            email = accountInfo.email,
                            name = accountInfo.name,
                            thumbnailUrl = accountInfo.thumbnails.lastOrNull()?.url ?: "",
                            cache = YouTube.cookie,
                            isUsed = true,
                        ),
                    )
                    dataStoreManager.setLoggedIn(true)
                    dataStoreManager.setCookie(YouTube.cookie ?: "")
                    delay(500)
                    getAllGoogleAccount()
                    getLoggedIn()
                }
            }
        }
    }

    fun setUsedAccount(acc: GoogleAccountEntity?) {
        viewModelScope.launch {
            if (acc != null) {
                googleAccounts.value?.forEach {
                    mainRepository.updateGoogleAccountUsed(it.email, false)
                }
                dataStoreManager.putString("AccountName", acc.name)
                dataStoreManager.putString("AccountThumbUrl", acc.thumbnailUrl)
                mainRepository.updateGoogleAccountUsed(acc.email, true)
                YouTube.cookie = acc.cache
                dataStoreManager.setCookie(acc.cache ?: "")
                dataStoreManager.setLoggedIn(true)
                delay(500)
                getAllGoogleAccount()
                getLoggedIn()
            } else {
                googleAccounts.value?.forEach {
                    mainRepository.updateGoogleAccountUsed(it.email, false)
                }
                dataStoreManager.putString("AccountName", "")
                dataStoreManager.putString("AccountThumbUrl", "")
                dataStoreManager.setLoggedIn(false)
                dataStoreManager.setCookie("")
                YouTube.cookie = null
                delay(500)
                getAllGoogleAccount()
                getLoggedIn()
            }
        }
    }

    fun logOutAllYouTube() {
        viewModelScope.launch {
            googleAccounts.value?.forEach { account ->
                mainRepository.deleteGoogleAccount(account.email)
            }
            dataStoreManager.putString("AccountName", "")
            dataStoreManager.putString("AccountThumbUrl", "")
            dataStoreManager.setLoggedIn(false)
            dataStoreManager.setCookie("")
            YouTube.cookie = null
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
}