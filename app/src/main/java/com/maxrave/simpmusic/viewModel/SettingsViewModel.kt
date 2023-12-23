package com.maxrave.simpmusic.viewModel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.YouTubeLocale
import com.maxrave.kotlinytmusicscraper.models.simpmusic.GithubResponse
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.DB_NAME
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.common.QUALITY
import com.maxrave.simpmusic.common.SELECTED_LANGUAGE
import com.maxrave.simpmusic.common.SETTINGS_FILENAME
import com.maxrave.simpmusic.common.VIDEO_QUALITY
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.db.DatabaseDao
import com.maxrave.simpmusic.data.db.MusicDatabase
import com.maxrave.simpmusic.data.repository.MainRepository
import com.maxrave.simpmusic.di.DownloadCache
import com.maxrave.simpmusic.di.PlayerCache
import com.maxrave.simpmusic.extension.div
import com.maxrave.simpmusic.extension.zipInputStream
import com.maxrave.simpmusic.extension.zipOutputStream
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.ui.MainActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private var mainRepository: MainRepository,
    private var database: MusicDatabase,
    private var databaseDao: DatabaseDao,
    @PlayerCache val playerCache: SimpleCache,
    @DownloadCache val downloadCache: SimpleCache,
    ) : AndroidViewModel(application) {

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private var _location: MutableLiveData<String> = MutableLiveData()
    val location: LiveData<String> = _location
    private var _language: MutableLiveData<String?> = MutableLiveData()
    val language: LiveData<String?> = _language
    private var _loggedIn: MutableLiveData<String> = MutableLiveData()
    val loggedIn: LiveData<String> = _loggedIn
    private var _normalizeVolume: MutableLiveData<String> = MutableLiveData()
    val normalizeVolume: LiveData<String> = _normalizeVolume
    private var _skipSilent: MutableLiveData<String> = MutableLiveData()
    val skipSilent: LiveData<String> = _skipSilent
    private var _pipedInstance: MutableLiveData<String> = MutableLiveData()
    val pipedInstance: LiveData<String> = _pipedInstance
    private var _savedPlaybackState: MutableLiveData<String> = MutableLiveData()
    val savedPlaybackState: LiveData<String> = _savedPlaybackState
    private var _saveRecentSongAndQueue: MutableLiveData<String> = MutableLiveData()
    val saveRecentSongAndQueue: LiveData<String> = _saveRecentSongAndQueue
    private var _lastCheckForUpdate: MutableLiveData<String?> = MutableLiveData()
    val lastCheckForUpdate: LiveData<String?> = _lastCheckForUpdate
    private var _githubResponse = MutableLiveData<GithubResponse?>()
    val githubResponse: LiveData<GithubResponse?> = _githubResponse
    private var _sponsorBlockEnabled: MutableLiveData<String> = MutableLiveData()
    val sponsorBlockEnabled: LiveData<String> = _sponsorBlockEnabled
    private var _sponsorBlockCategories: MutableLiveData<ArrayList<String>> = MutableLiveData()
    val sponsorBlockCategories: LiveData<ArrayList<String>> = _sponsorBlockCategories
    private var _sendBackToGoogle: MutableLiveData<String> = MutableLiveData()
    val sendBackToGoogle: LiveData<String> = _sendBackToGoogle
    private var _mainLyricsProvider: MutableLiveData<String> = MutableLiveData()
    val mainLyricsProvider: LiveData<String> = _mainLyricsProvider
    private var _musixmatchLoggedIn: MutableLiveData<String> = MutableLiveData()
    val musixmatchLoggedIn: LiveData<String> = _musixmatchLoggedIn
    private var _translationLanguage: MutableLiveData<String> = MutableLiveData()
    val translationLanguage: LiveData<String> = _translationLanguage
    private var _useTranslation: MutableLiveData<String> = MutableLiveData()
    val useTranslation: LiveData<String> = _useTranslation
    private var _playerCacheLimit: MutableLiveData<Int> = MutableLiveData()
    val playerCacheLimit: LiveData<Int> = _playerCacheLimit
    private var _playVideoInsteadOfAudio: MutableLiveData<String> = MutableLiveData()
    val playVideoInsteadOfAudio: LiveData<String> = _playVideoInsteadOfAudio
    private var _videoQuality: MutableLiveData<String> = MutableLiveData()
    val videoQuality: LiveData<String> = _videoQuality
    fun getVideoQuality() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.videoQuality.collect { videoQuality ->
                    when (videoQuality) {
                        VIDEO_QUALITY.items[0].toString() -> _videoQuality.postValue(VIDEO_QUALITY.items[0].toString())
                        VIDEO_QUALITY.items[1].toString() -> _videoQuality.postValue(VIDEO_QUALITY.items[1].toString())
                    }
                }
            }
        }
    }

    fun getTranslationLanguage() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.translationLanguage.collect { translationLanguage ->
                    _translationLanguage.postValue(translationLanguage)
                }
            }
        }
    }

    fun setTranslationLanguage(language: String) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.setTranslationLanguage(language)
                getTranslationLanguage()
            }
        }
    }
    fun getUseTranslation() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.enableTranslateLyric.collect { useTranslation ->
                    _useTranslation.postValue(useTranslation)
                }
            }
        }
    }
    fun setUseTranslation(useTranslation: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.setEnableTranslateLyric(useTranslation)
                getUseTranslation()
            }
        }
    }
    fun getMusixmatchLoggedIn() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.musixmatchLoggedIn.collect { musixmatchLoggedIn ->
                    _musixmatchLoggedIn.postValue(musixmatchLoggedIn)
                }
            }
        }
    }
    fun setMusixmatchLoggedIn(loggedIn: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.setMusixmatchLoggedIn(loggedIn)
                getMusixmatchLoggedIn()
            }
        }
    }
    fun getLyricsProvider() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.lyricsProvider.collect { mainLyricsProvider ->
                    _mainLyricsProvider.postValue(mainLyricsProvider)
                }
            }
        }
    }
    fun setLyricsProvider(provider: String) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.setLyricsProvider(provider)
                getLyricsProvider()
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            mainRepository.checkForUpdate().collect {response ->
                dataStoreManager.putString("CheckForUpdateAt", System.currentTimeMillis().toString())
                _githubResponse.postValue(response)
            }
        }
    }

    fun getLocation() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.location.collect { location ->
                    _location.postValue(location)
                }
            }
        }
    }
    fun getLoggedIn() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.loggedIn.collect { loggedIn ->
                    _loggedIn.postValue(loggedIn)
                }
            }
        }
    }

    fun changeLocation(location: String) {
        viewModelScope.launch {
            withContext((Dispatchers.Main)) {
                dataStoreManager.setLocation(location)
                YouTube.locale = YouTubeLocale(location, language.value!!)
                getLocation()
            }
        }
    }
    fun getSaveRecentSongAndQueue() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.saveRecentSongAndQueue.collect { saved ->
                    _saveRecentSongAndQueue.postValue(saved)
                }
            }
        }
    }
    fun getLastCheckForUpdate() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.getString("CheckForUpdateAt").first().let { lastCheckForUpdate ->
                    _lastCheckForUpdate.postValue(lastCheckForUpdate)
                }
            }
        }
    }

    fun getSponsorBlockEnabled() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.sponsorBlockEnabled.first().let { enabled ->
                    _sponsorBlockEnabled.postValue(enabled)
                }
            }
        }
    }

    fun setSponsorBlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.setSponsorBlockEnabled(enabled)
                getSponsorBlockEnabled()
            }
        }
    }

    fun getPlayVideoInsteadOfAudio() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.watchVideoInsteadOfPlayingAudio.collect { playVideoInsteadOfAudio ->
                    _playVideoInsteadOfAudio.postValue(playVideoInsteadOfAudio)
                }
            }
        }
    }

    fun setPlayVideoInsteadOfAudio(playVideoInsteadOfAudio: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.setWatchVideoInsteadOfPlayingAudio(playVideoInsteadOfAudio)
                getPlayVideoInsteadOfAudio()
            }
        }
    }

    fun getSponsorBlockCategories() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.getSponsorBlockCategories().let {
                    _sponsorBlockCategories.postValue(it)
                }
            }
        }
    }
    fun setSponsorBlockCategories(list: ArrayList<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.setSponsorBlockCategories(list)
                getSponsorBlockCategories()
            }
        }
    }
    private var _quality: MutableLiveData<String> = MutableLiveData()
    val quality: LiveData<String> = _quality

    fun getQuality() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.quality.collect { quality ->
                    when (quality) {
                        QUALITY.items[0].toString() -> _quality.postValue(QUALITY.items[0].toString())
                        QUALITY.items[1].toString() -> _quality.postValue(QUALITY.items[1].toString())
                    }
                }
            }
        }
    }

    fun changeVideoQuality(checkedIndex: Int) {
        viewModelScope.launch {
            withContext((Dispatchers.Main)) {
                when (checkedIndex) {
                    0 -> dataStoreManager.setVideoQuality(VIDEO_QUALITY.items[0].toString())
                    1 -> dataStoreManager.setVideoQuality(VIDEO_QUALITY.items[1].toString())
                }
                getVideoQuality()
            }
        }
    }

    fun changeQuality(checkedIndex: Int) {
        viewModelScope.launch {
            withContext((Dispatchers.Main)) {
                when (checkedIndex) {
                    0 -> dataStoreManager.setQuality(QUALITY.items[0].toString())
                    1 -> dataStoreManager.setQuality(QUALITY.items[1].toString())
                }
                getQuality()
            }
        }
    }

    private val _cacheSize: MutableLiveData<Long> = MutableLiveData()
    var cacheSize: LiveData<Long> = _cacheSize

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

    private val _downloadedCacheSize: MutableLiveData<Long> = MutableLiveData()
    var downloadedCacheSize: LiveData<Long> = _downloadedCacheSize

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
            mainRepository.getDownloadedSongs().collect {songs ->
                songs?.forEach { song ->
                    mainRepository.updateDownloadState(song.videoId, DownloadState.STATE_NOT_DOWNLOADED)
                }
            }
            Toast.makeText(getApplication(), "Download cache cleared", Toast.LENGTH_SHORT).show()
            _cacheSize.value = playerCache.cacheSpace
        }
    }

    fun backup(context: Context, uri: Uri) {
        runCatching {
            context.applicationContext.contentResolver.openOutputStream(uri)?.use {
                it.buffered().zipOutputStream().use { outputStream ->
                    (context.filesDir/"datastore"/"$SETTINGS_FILENAME.preferences_pb").inputStream().buffered().use { inputStream ->
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
            Toast.makeText(context,
                context.getString(R.string.backup_create_success), Toast.LENGTH_SHORT).show()
        }.onFailure {
            it.printStackTrace()
            Toast.makeText(context,
                context.getString(R.string.backup_create_failed), Toast.LENGTH_SHORT).show()
        }
    }

    @UnstableApi
    fun restore(context: Context, uri: Uri) {
        runCatching {
            runBlocking { dataStoreManager.restore(true)}
            context.applicationContext.contentResolver.openInputStream(uri)?.use {
                it.zipInputStream().use { inputStream ->
                    var entry = inputStream.nextEntry
                    var count = 0
                    while (entry != null && count < 2) {
                        when (entry.name) {
                            "$SETTINGS_FILENAME.preferences_pb" -> {
                                (context.filesDir/"datastore"/"$SETTINGS_FILENAME.preferences_pb").outputStream().use { outputStream ->
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
            withContext(Dispatchers.Main){
                dataStoreManager.getString(SELECTED_LANGUAGE).collect { language ->
                    _language.postValue(language)
                }
            }
        }
    }

    @UnstableApi
    fun changeLanguage(code: String) {
        viewModelScope.launch {
            withContext((Dispatchers.Main)) {
                dataStoreManager.putString(SELECTED_LANGUAGE, code)
                YouTube.locale = YouTubeLocale(location.value!!, code)
                getLanguage()
            }
        }
    }

    fun clearCookie() {
        viewModelScope.launch {
            withContext((Dispatchers.Main)) {
                dataStoreManager.setCookie("")
                YouTube.cookie = null
                dataStoreManager.setLoggedIn(false)
            }
        }
    }

    fun getNormalizeVolume() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.normalizeVolume.collect { normalizeVolume ->
                    _normalizeVolume.postValue(normalizeVolume)
                }
            }
        }
    }
    @UnstableApi
    fun setNormalizeVolume(normalizeVolume: Boolean) {
        viewModelScope.launch {
            withContext((Dispatchers.Main)) {
                dataStoreManager.setNormalizeVolume(normalizeVolume)
                getNormalizeVolume()
            }
        }
    }
    fun getSendBackToGoogle() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.sendBackToGoogle.collect { sendBackToGoogle ->
                    _sendBackToGoogle.postValue(sendBackToGoogle)
                }
            }
        }
    }
    fun setSendBackToGoogle(sendBackToGoogle: Boolean) {
        viewModelScope.launch {
            withContext((Dispatchers.Main)) {
                dataStoreManager.setSendBackToGoogle(sendBackToGoogle)
                getSendBackToGoogle()
            }
        }
    }
    fun getSkipSilent() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.skipSilent.collect { skipSilent ->
                    _skipSilent.postValue(skipSilent)

                }
            }
        }
    }

    @UnstableApi
    fun setSkipSilent(skip: Boolean) {
        viewModelScope.launch {
            withContext((Dispatchers.Main)) {
                dataStoreManager.setSkipSilent(skip)
                getSkipSilent()
            }
        }
    }
    fun getSavedPlaybackState() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                dataStoreManager.saveStateOfPlayback.collect { savedPlaybackState ->
                    _savedPlaybackState.postValue(savedPlaybackState)
                }
            }
        }
    }
    fun setSavedPlaybackState(savedPlaybackState: Boolean) {
        viewModelScope.launch {
            withContext((Dispatchers.Main)) {
                dataStoreManager.setSaveStateOfPlayback(savedPlaybackState)
                getSavedPlaybackState()
            }
        }
    }

    fun setSaveLastPlayed(b: Boolean) {
        viewModelScope.launch {
            withContext((Dispatchers.Main)) {
                dataStoreManager.setSaveRecentSongAndQueue(b)
                getSaveRecentSongAndQueue()
            }
        }
    }

    fun clearMusixmatchCookie() {
        viewModelScope.launch {
            withContext((Dispatchers.Main)) {
                dataStoreManager.setMusixmatchCookie("")
                YouTube.musixMatchCookie = null
                dataStoreManager.setMusixmatchLoggedIn(false)
            }
        }
    }

    fun getPlayerCacheLimit() {
        viewModelScope.launch {
            dataStoreManager.maxSongCacheSize.collect {
                _playerCacheLimit.postValue(it)
            }
        }
    }
    fun setPlayerCacheLimit(size: Int) {
        viewModelScope.launch {
            dataStoreManager.setMaxSongCacheSize(size)
            getPlayerCacheLimit()
        }
    }
}