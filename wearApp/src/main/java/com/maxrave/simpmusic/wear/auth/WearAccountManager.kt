package com.maxrave.simpmusic.wear.auth

import android.content.Context
import com.maxrave.common.Config
import com.maxrave.domain.data.entities.GoogleAccountEntity
import com.maxrave.domain.extension.toNetScapeString
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AccountRepository
import com.maxrave.domain.repository.CommonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.withContext
import java.io.File

class WearAccountManager(
    private val context: Context,
    private val dataStoreManager: DataStoreManager,
    private val accountRepository: AccountRepository,
    private val commonRepository: CommonRepository,
) {
    /**
     * Startup bootstrap for watch standalone mode.
     *
     * Priority:
     * 1) Existing DataStore cookie (if present) -> mark logged-in immediately.
     * 2) Otherwise restore from used account in DB.
     */
    suspend fun bootstrapSessionAtStartup(): Boolean =
        withContext(Dispatchers.IO) {
            val currentCookie = dataStoreManager.cookie.first()
            val currentPageId = dataStoreManager.pageId.first()
            if (currentCookie.isNotBlank()) {
                dataStoreManager.setCookie(currentCookie, currentPageId)
                dataStoreManager.setLoggedIn(true)

                val used = accountRepository.getUsedGoogleAccount().firstOrNull()
                if (used != null) {
                    dataStoreManager.putString("AccountName", used.name)
                    dataStoreManager.putString("AccountThumbUrl", used.thumbnailUrl)
                    used.netscapeCookie?.let {
                        commonRepository.writeTextToFile(it, File(context.filesDir, "ytdlp-cookie.txt").path)
                    }
                }
                return@withContext true
            }

            restoreUsedAccountIfNeeded()
        }

    /**
     * Restores the previously selected account into DataStore/cookie state.
     *
     * On WearOS this must run early (app start), otherwise repositories may execute with an empty
     * cookie and fail with `LOGIN_REQUIRED` until the user opens account UI.
     */
    suspend fun restoreUsedAccountIfNeeded(): Boolean =
        withContext(Dispatchers.IO) {
            val used = accountRepository.getUsedGoogleAccount().firstOrNull()
            val fallback = accountRepository.getGoogleAccounts().firstOrNull()?.orEmpty()?.firstOrNull()
            val candidate = used ?: fallback ?: return@withContext false

            val currentCookie = dataStoreManager.cookie.first()
            val currentPageId = dataStoreManager.pageId.first()
            val currentLoggedIn = dataStoreManager.loggedIn.first() == DataStoreManager.TRUE
            val targetCookie = candidate.cache.orEmpty()
            val targetPageId = candidate.pageId.orEmpty()

            val alreadySynced =
                currentLoggedIn &&
                    currentCookie.isNotBlank() &&
                    currentCookie == targetCookie &&
                    currentPageId == targetPageId
            if (alreadySynced) return@withContext true

            setUsedAccount(candidate)
            true
        }

    /**
     * Adds a YouTube Music account based on the raw cookie string returned by WebView.
     *
     * Mirrors the phone app logic in `SettingsViewModel.addAccount`, but lives in the Wear module
     * so the watch can be standalone.
     */
    suspend fun addAccountFromCookie(cookie: String): Boolean =
        withContext(Dispatchers.IO) {
            val currentCookie = dataStoreManager.cookie.first()
            val currentPageId = dataStoreManager.pageId.first()
            val currentLoggedIn = dataStoreManager.loggedIn.first() == DataStoreManager.TRUE

            try {
                dataStoreManager.setCookie(cookie, "")
                dataStoreManager.setLoggedIn(true)

                val accountInfoList =
                    accountRepository
                        .getAccountInfo(cookie)
                        .lastOrNull()
                        ?.takeIf { it.isNotEmpty() }
                        ?: run {
                            // Restore old state on failure.
                            dataStoreManager.setCookie(currentCookie, currentPageId)
                            dataStoreManager.setLoggedIn(currentLoggedIn)
                            return@withContext false
                        }

                accountRepository.getGoogleAccounts().lastOrNull()?.orEmpty()?.forEach { acc ->
                    accountRepository.updateGoogleAccountUsed(acc.email, false).singleOrNull()
                }

                dataStoreManager.putString("AccountName", accountInfoList.first().name)
                dataStoreManager.putString(
                    "AccountThumbUrl",
                    accountInfoList.first().thumbnails.lastOrNull()?.url ?: "",
                )

                // Export cookies for ytdlp/newpipe-based extractors.
                val cookieItem = commonRepository.getCookiesFromInternalDatabase(Config.YOUTUBE_MUSIC_MAIN_URL)
                val netscapeCookie = cookieItem.toNetScapeString()
                commonRepository.writeTextToFile(netscapeCookie, File(context.filesDir, "ytdlp-cookie.txt").path)

                accountInfoList.forEachIndexed { index, account ->
                    accountRepository
                        .insertGoogleAccount(
                            GoogleAccountEntity(
                                email = account.email,
                                name = account.name,
                                thumbnailUrl = account.thumbnails.lastOrNull()?.url ?: "",
                                cache = cookie,
                                isUsed = index == 0,
                                netscapeCookie = netscapeCookie,
                                pageId = account.pageId,
                            ),
                        ).firstOrNull()
                }

                dataStoreManager.setLoggedIn(true)
                dataStoreManager.setCookie(cookie, accountInfoList.first().pageId)
                true
            } catch (_: Throwable) {
                dataStoreManager.setCookie(currentCookie, currentPageId)
                dataStoreManager.setLoggedIn(currentLoggedIn)
                false
            }
        }

    suspend fun setUsedAccount(acc: GoogleAccountEntity?) {
        withContext(Dispatchers.IO) {
            val all = accountRepository.getGoogleAccounts().lastOrNull()?.orEmpty().orEmpty()
            all.forEach { existing ->
                accountRepository.updateGoogleAccountUsed(existing.email, false).singleOrNull()
            }

            if (acc == null) {
                dataStoreManager.putString("AccountName", "")
                dataStoreManager.putString("AccountThumbUrl", "")
                dataStoreManager.setLoggedIn(false)
                dataStoreManager.setCookie("", null)
                return@withContext
            }

            dataStoreManager.putString("AccountName", acc.name)
            dataStoreManager.putString("AccountThumbUrl", acc.thumbnailUrl)
            accountRepository.updateGoogleAccountUsed(acc.email, true).singleOrNull()

            acc.netscapeCookie?.let {
                commonRepository.writeTextToFile(it, File(context.filesDir, "ytdlp-cookie.txt").path)
            }

            dataStoreManager.setCookie(acc.cache ?: "", acc.pageId)
            dataStoreManager.setLoggedIn(true)
        }
    }

    suspend fun logOutAll() {
        withContext(Dispatchers.IO) {
            accountRepository.getGoogleAccounts().lastOrNull()?.orEmpty()?.forEach { account ->
                accountRepository.deleteGoogleAccount(account.email)
            }
            setUsedAccount(null)
        }
    }
}
