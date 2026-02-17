package com.maxrave.simpmusic.wear.wearable

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.AccountRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.simpmusic.wear.auth.WearAccountManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

private const val KEY_WEAR_LOGIN_STATUS = "wear_login_status"
private const val KEY_WEAR_LOGIN_MESSAGE = "wear_login_message"
private const val PATH_LOGIN_COOKIE = "/simpmusic/login/cookie"
private const val PATH_LOGIN_STATUS = "/simpmusic/login/status"

class WearDataLayerListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            PATH_LOGIN_COOKIE -> onCookie(messageEvent)
            PATH_LOGIN_STATUS -> onStatus(messageEvent)
            else -> return
        }
    }

    private fun onStatus(messageEvent: MessageEvent) {
        val payload = runCatching { messageEvent.data?.decodeToString() }.getOrNull().orEmpty()
        val parts = payload.split("|", limit = 2)
        val status = parts.getOrNull(0).orEmpty()
        val msg = parts.getOrNull(1).orEmpty()
        if (status.isBlank() && msg.isBlank()) return

        scope.launch {
            val dataStoreManager: DataStoreManager = GlobalContext.get().get()
            if (status.isNotBlank()) {
                dataStoreManager.putString(KEY_WEAR_LOGIN_STATUS, status)
            }
            // Always update the message, even if empty, to avoid stale status text.
            dataStoreManager.putString(KEY_WEAR_LOGIN_MESSAGE, msg)
        }
    }

    private fun onCookie(messageEvent: MessageEvent) {
        val cookie = runCatching { messageEvent.data?.decodeToString() }.getOrNull()
        if (cookie.isNullOrBlank()) return

        scope.launch {
            val dataStoreManager: DataStoreManager = GlobalContext.get().get()
            val accountRepository: AccountRepository = GlobalContext.get().get()
            val commonRepository: CommonRepository = GlobalContext.get().get()

            dataStoreManager.putString(KEY_WEAR_LOGIN_STATUS, "processing")
            dataStoreManager.putString(KEY_WEAR_LOGIN_MESSAGE, "Syncing session to watch...")

            val ok =
                WearAccountManager(
                    context = applicationContext,
                    dataStoreManager = dataStoreManager,
                    accountRepository = accountRepository,
                    commonRepository = commonRepository,
                ).addAccountFromCookie(cookie)

            if (ok) {
                dataStoreManager.putString(KEY_WEAR_LOGIN_STATUS, "success")
                dataStoreManager.putString(KEY_WEAR_LOGIN_MESSAGE, "Signed in.")
            } else {
                dataStoreManager.putString(KEY_WEAR_LOGIN_STATUS, "failed")
                dataStoreManager.putString(KEY_WEAR_LOGIN_MESSAGE, "Failed to sign in. Try again.")
            }
        }
    }
}
