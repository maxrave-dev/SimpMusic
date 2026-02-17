package com.maxrave.simpmusic.wear.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.simpmusic.wear.ui.components.WearList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private const val KEY_WEAR_LOGIN_STATUS = "wear_login_status"
private const val KEY_WEAR_LOGIN_MESSAGE = "wear_login_message"
private const val PATH_OPEN_LOGIN_ON_PHONE = "/simpmusic/login/open"
private const val PATH_SYNC_LOGIN_FROM_PHONE = "/simpmusic/login/sync"
private const val PHONE_CAPABILITY = "simpmusic_phone_bridge"
private const val STATUS_SENDING = "sending"
private const val STATUS_SYNCING = "syncing"

private fun readableStatus(raw: String?): String =
    when (raw) {
        null, "" -> "-"
        STATUS_SENDING, STATUS_SYNCING, "connecting" -> "contacting_phone"
        "requested" -> "phone_acknowledged"
        "processing" -> "syncing_session"
        "failed" -> "failed"
        "success" -> "success"
        else -> raw
    }

@Composable
fun LoginScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dataStoreManager: DataStoreManager = remember { GlobalContext.get().get() }
    val status by dataStoreManager.getString(KEY_WEAR_LOGIN_STATUS).collectAsStateWithLifecycle(initialValue = null)
    val msg by dataStoreManager.getString(KEY_WEAR_LOGIN_MESSAGE).collectAsStateWithLifecycle(initialValue = null)
    val loggedIn by dataStoreManager.loggedIn.collectAsStateWithLifecycle(initialValue = "")
    var pendingAckTimeoutJob by remember { mutableStateOf<Job?>(null) }

    fun setStatus(
        newStatus: String,
        message: String,
    ) {
        scope.launch {
            dataStoreManager.putString(KEY_WEAR_LOGIN_STATUS, newStatus)
            dataStoreManager.putString(KEY_WEAR_LOGIN_MESSAGE, message)
        }
    }

    fun setFailure(message: String) {
        setStatus("failed", message)
    }

    fun scheduleAckTimeout(
        expectedStatus: String,
        timeoutMessage: String,
        timeoutMs: Long = 12_000L,
    ) {
        pendingAckTimeoutJob?.cancel()
        pendingAckTimeoutJob =
            scope.launch {
                delay(timeoutMs)
                val currentStatus = dataStoreManager.getString(KEY_WEAR_LOGIN_STATUS).first().orEmpty()
                if (currentStatus == expectedStatus) {
                    setFailure(timeoutMessage)
                }
            }
    }

    fun resolveReachablePhoneNodes(onResolved: (List<Node>) -> Unit) {
        val appCtx = context.applicationContext
        val capabilityClient = Wearable.getCapabilityClient(appCtx)
        val nodeClient = Wearable.getNodeClient(appCtx)

        capabilityClient
            .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .addOnSuccessListener { reachable ->
                val reachableNodes = reachable.nodes.toList()
                if (reachableNodes.isNotEmpty()) {
                    onResolved(reachableNodes)
                    return@addOnSuccessListener
                }

                capabilityClient
                    .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_ALL)
                    .addOnSuccessListener { all ->
                        if (all.nodes.isEmpty()) {
                            setFailure("SimpMusic is not installed on your phone, or it's a different build channel.")
                            return@addOnSuccessListener
                        }

                        setFailure("Phone app found but not reachable. Keep phone nearby and open SimpMusic on phone.")
                    }.addOnFailureListener {
                        nodeClient
                            .connectedNodes
                            .addOnSuccessListener { nodes ->
                                if (nodes.isEmpty()) {
                                    setFailure("No phone connected. Enable Bluetooth on both devices and retry.")
                                } else {
                                    setFailure("Failed to detect phone app capability. Open SimpMusic on phone and retry.")
                                }
                            }.addOnFailureListener { e ->
                                setFailure(e.message ?: "Failed to query paired phone.")
                            }
                    }
            }.addOnFailureListener { e ->
                setFailure(e.message ?: "Failed to query phone reachability.")
            }
    }

    fun sendMessageToNodes(
        nodes: List<Node>,
        path: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        val appCtx = context.applicationContext
        val messageClient = Wearable.getMessageClient(appCtx)
        val remaining = AtomicInteger(nodes.size)
        val anySuccess = AtomicBoolean(false)

        nodes.forEach { node ->
            messageClient
                .sendMessage(node.id, path, ByteArray(0))
                .addOnSuccessListener { anySuccess.set(true) }
                .addOnCompleteListener {
                    if (remaining.decrementAndGet() == 0) {
                        if (anySuccess.get()) onSuccess() else onFailure()
                    }
                }
        }
    }

    fun requestPhoneLogin() {
        setStatus(STATUS_SENDING, "Looking for your phone...")
        resolveReachablePhoneNodes { nodes ->
            sendMessageToNodes(
                nodes = nodes,
                path = PATH_OPEN_LOGIN_ON_PHONE,
                onSuccess = {
                    setStatus(STATUS_SENDING, "Waiting for phone app response...")
                    scheduleAckTimeout(
                        expectedStatus = STATUS_SENDING,
                        timeoutMessage = "Phone did not acknowledge login request. Open SimpMusic on your phone and retry.",
                    )
                    Toast.makeText(context, "Waiting for phone", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    setFailure("Failed to contact phone app.")
                    Toast.makeText(context, "Failed to contact phone", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    fun requestPhoneSync() {
        setStatus(STATUS_SYNCING, "Checking phone for existing sign-in...")
        resolveReachablePhoneNodes { nodes ->
            sendMessageToNodes(
                nodes = nodes,
                path = PATH_SYNC_LOGIN_FROM_PHONE,
                onSuccess = {
                    setStatus(STATUS_SYNCING, "Waiting for phone sync response...")
                    scheduleAckTimeout(
                        expectedStatus = STATUS_SYNCING,
                        timeoutMessage = "Phone did not respond to sync request. Open SimpMusic on your phone and retry.",
                    )
                },
                onFailure = {
                    setFailure("Failed to contact phone app.")
                    Toast.makeText(context, "Failed to contact phone", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    LaunchedEffect(status) {
        if (status == "success") {
            pendingAckTimeoutJob?.cancel()
            dataStoreManager.putString(KEY_WEAR_LOGIN_STATUS, "")
            dataStoreManager.putString(KEY_WEAR_LOGIN_MESSAGE, "")
            onDone()
            return@LaunchedEffect
        }

        if (status != STATUS_SENDING && status != STATUS_SYNCING) {
            pendingAckTimeoutJob?.cancel()
        }
    }

    val didAutoSyncAttempt = remember { AtomicBoolean(false) }
    LaunchedEffect(loggedIn) {
        val isLoggedIn = loggedIn == DataStoreManager.TRUE
        if (isLoggedIn) return@LaunchedEffect
        if (didAutoSyncAttempt.getAndSet(true)) return@LaunchedEffect

        setStatus(STATUS_SYNCING, "Checking phone for existing sign-in...")
        resolveReachablePhoneNodes { nodes ->
            sendMessageToNodes(
                nodes = nodes,
                path = PATH_SYNC_LOGIN_FROM_PHONE,
                onSuccess = {
                    scheduleAckTimeout(
                        expectedStatus = STATUS_SYNCING,
                        timeoutMessage = "Phone did not respond to automatic sync. Open SimpMusic on phone and tap Sync from phone.",
                    )
                },
                onFailure = { setFailure("Failed to contact phone app.") },
            )
        }
    }

    WearList {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Sign in",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "WearOS devices typically don't ship a WebView provider.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Use your phone to complete Google sign-in, then we'll sync the session back to the watch.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Important: your phone must have a compatible SimpMusic build installed (same release/dev channel as this watch build).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { requestPhoneLogin() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Open login on phone",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Phone will show a notification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { requestPhoneSync() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Sync from phone",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (!status.isNullOrBlank() || !msg.isNullOrBlank()) {
            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Status: ${readableStatus(status)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!msg.isNullOrBlank()) {
                    Text(
                        text = msg.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "When login completes on your phone, this screen should update automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tip: if you already finished signing in, go back and open Accounts to verify you are signed in.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Done")
            }
        }
    }
}
