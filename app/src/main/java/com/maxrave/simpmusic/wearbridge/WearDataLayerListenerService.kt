package com.maxrave.simpmusic.wearbridge

import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.common.R as CommonR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

private const val PATH_OPEN_LOGIN_ON_PHONE = "/simpmusic/login/open"
private const val PATH_SYNC_LOGIN_FROM_PHONE = "/simpmusic/login/sync"
private const val PATH_LOGIN_COOKIE = "/simpmusic/login/cookie"
private const val PATH_LOGIN_STATUS = "/simpmusic/login/status"
private const val CHANNEL_ID = "wear_login"
private const val NOTIFICATION_ID = 0x534D4C // "SML"

class WearDataLayerListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            PATH_OPEN_LOGIN_ON_PHONE -> postLoginNotification(sourceNodeId = messageEvent.sourceNodeId)
            PATH_SYNC_LOGIN_FROM_PHONE -> syncExistingLoginToWatch(sourceNodeId = messageEvent.sourceNodeId)
            else -> return
        }
    }

    private fun postLoginNotification(sourceNodeId: String) {
        runCatching {
            val i =
                Intent(this, WearLoginRelayActivity::class.java).apply {
                    putExtra(WearLoginRelayActivity.EXTRA_SOURCE_NODE_ID, sourceNodeId)
                }
            createNotificationChannel()

            val pending =
                PendingIntent.getActivity(
                    this,
                    0,
                    i,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val notification =
                NotificationCompat
                    .Builder(this, CHANNEL_ID)
                    .setSmallIcon(CommonR.drawable.mono)
                    .setContentTitle("SimpMusic: Sign in for watch")
                    .setContentText("Tap to open sign-in and sync to your watch.")
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        }.onSuccess {
            sendStatus(
                sourceNodeId = sourceNodeId,
                status = "requested",
                message = "Phone received request. Check phone notification.",
            )
        }.onFailure {
            sendStatus(
                sourceNodeId = sourceNodeId,
                status = "failed",
                message = "Phone could not show login notification. Open SimpMusic on phone.",
            )
        }
    }

    private fun syncExistingLoginToWatch(sourceNodeId: String) {
        scope.launch {
            val messageClient = Wearable.getMessageClient(applicationContext)

            val dataStoreManager =
                runCatching { GlobalContext.get().get<DataStoreManager>() }.getOrNull()
                    ?: run {
                        sendStatus(
                            sourceNodeId = sourceNodeId,
                            status = "failed",
                            message = "Open SimpMusic on your phone first, then try again.",
                        )
                        return@launch
                    }

            val loggedIn = dataStoreManager.loggedIn.first() == DataStoreManager.TRUE
            val cookie = dataStoreManager.cookie.first()

            if (!loggedIn || cookie.isBlank()) {
                // Update watch UI with a clearer message than "check your phone".
                sendStatus(
                    sourceNodeId = sourceNodeId,
                    status = "failed",
                    message = "No active phone session found. Open SimpMusic on your phone and sign in first.",
                )
                return@launch
            }

            // Send cookie to watch; watch will validate and persist by fetching account info.
            messageClient.sendMessage(sourceNodeId, PATH_LOGIN_COOKIE, cookie.toByteArray())
            sendStatus(
                sourceNodeId = sourceNodeId,
                status = "processing",
                message = "Syncing signed-in session from phone...",
            )
        }
    }

    private fun sendStatus(
        sourceNodeId: String,
        status: String,
        message: String,
    ) {
        Wearable
            .getMessageClient(applicationContext)
            .sendMessage(
                sourceNodeId,
                PATH_LOGIN_STATUS,
                "$status|$message".toByteArray(),
            )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Watch login",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Prompts to sign in on phone and sync session to WearOS watch."
            }
        nm.createNotificationChannel(channel)
    }
}
