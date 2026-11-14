package com.maxrave.simpmusic.viewModel

import com.maxrave.domain.repository.BrowserLoginFailure
import com.maxrave.domain.repository.BrowserProfile
import com.maxrave.domain.repository.BrowserProfileStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrowserLoginPermissionTest {
    @Test
    fun safariPermissionRowOpensFileAccessSettingsInsteadOfImportingAgain() {
        val safari = profile("safari", "Safari", BrowserProfileStatus.PERMISSION_REQUIRED)
        assertTrue(BrowserLoginUiState().requiresFileAccess(safari, isMacOS = true))
    }

    @Test
    fun chromePermissionRowAlsoOpensFileAccessSettings() {
        val chrome = profile("chrome", "Chrome", BrowserProfileStatus.PERMISSION_REQUIRED)
        assertTrue(BrowserLoginUiState().requiresFileAccess(chrome, isMacOS = true))
    }

    @Test
    fun fileDenialAfterDiscoveryOffersSettingsForTheAffectedProfileOnly() {
        val chrome = profile("chrome", "Chrome", BrowserProfileStatus.COOKIES_FOUND)
        val zen = profile("zen", "Zen", BrowserProfileStatus.COOKIES_FOUND)
        val state = BrowserLoginUiState(error = BrowserLoginFailure.FILE_ACCESS_DENIED, failedProfileId = chrome.id)
        assertTrue(state.requiresFileAccess(chrome, isMacOS = true))
        assertFalse(state.requiresFileAccess(zen, isMacOS = true))
    }

    @Test
    fun keychainDenialDoesNotPretendToRequireFullDiskAccess() {
        val chrome = profile("chrome", "Chrome", BrowserProfileStatus.COOKIES_FOUND)
        val state = BrowserLoginUiState(error = BrowserLoginFailure.ACCESS_DENIED, failedProfileId = chrome.id)
        assertFalse(state.requiresFileAccess(chrome, isMacOS = true))
    }

    @Test
    fun otherPlatformsDoNotOpenMacSystemSettings() {
        val firefox = profile("firefox", "Firefox", BrowserProfileStatus.PERMISSION_REQUIRED)
        assertFalse(BrowserLoginUiState().requiresFileAccess(firefox, isMacOS = false))
    }

    private fun profile(id: String, browser: String, status: BrowserProfileStatus) = BrowserProfile(id, browser, "Default", status)
}
