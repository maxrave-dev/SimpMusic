package com.maxrave.simpmusic.wearbridge

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.wearable.Wearable
import com.maxrave.common.Config

private const val PATH_LOGIN_COOKIE = "/simpmusic/login/cookie"
private const val PATH_LOGIN_STATUS = "/simpmusic/login/status"

/**
 * Phone-side login flow triggered by the watch.
 *
 * WearOS watches often do not have a WebView provider, so we complete Google sign-in on the phone
 * and then relay the raw cookie string back to the watch via the Wear Data Layer.
 */
class WearLoginRelayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearLoginRelayScreen(onClose = { finish() }) }
    }

    companion object {
        const val EXTRA_SOURCE_NODE_ID = "source_node_id"
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WearLoginRelayScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val sourceNodeId =
        (context as? WearLoginRelayActivity)
            ?.intent
            ?.getStringExtra(WearLoginRelayActivity.EXTRA_SOURCE_NODE_ID)

    val status = remember { mutableStateOf("Open Google sign-in and complete login.") }
    val appCtx = context.applicationContext
    val messageClient = Wearable.getMessageClient(appCtx)
    val nodeClient = Wearable.getNodeClient(appCtx)

    fun sendStatusToWatch(
        state: String,
        message: String,
    ) {
        val payload = "$state|$message".toByteArray(Charsets.UTF_8)
        if (!sourceNodeId.isNullOrBlank()) {
            messageClient.sendMessage(sourceNodeId, PATH_LOGIN_STATUS, payload)
            return
        }
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, PATH_LOGIN_STATUS, payload)
            }
        }
    }

    LaunchedEffect(Unit) {
        CookieManager.getInstance().removeAllCookies(null)
        sendStatusToWatch("processing", "Phone login page opened.")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
            }
            Text(
                text = "Login for Watch",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = status.value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp),
        )

        Spacer(Modifier.height(6.dp))

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    webViewClient =
                        object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                val u = url ?: return
                                if (!u.startsWith(Config.YOUTUBE_MUSIC_MAIN_URL)) return

                                val cookie = CookieManager.getInstance().getCookie(u)
                                if (cookie.isNullOrBlank()) {
                                    status.value = "Login detected, but cookie was empty."
                                    Toast.makeText(ctx, "Cookie missing", Toast.LENGTH_SHORT).show()
                                    return
                                }

                                status.value = "Login complete. Sending to watch..."
                                sendStatusToWatch("processing", "Phone login complete. Sending session to watch...")

                                // Clean browser state first to avoid weird re-logins.
                                WebStorage.getInstance().deleteAllData()
                                CookieManager.getInstance().removeAllCookies(null)
                                CookieManager.getInstance().flush()
                                clearCache(true)
                                clearFormData()
                                clearHistory()
                                clearSslPreferences()

                                val payload = cookie.toByteArray(Charsets.UTF_8)

                                if (!sourceNodeId.isNullOrBlank()) {
                                    messageClient
                                        .sendMessage(sourceNodeId, PATH_LOGIN_COOKIE, payload)
                                        .addOnSuccessListener {
                                            messageClient.sendMessage(
                                                sourceNodeId,
                                                PATH_LOGIN_STATUS,
                                                "processing|Session sent to watch. Applying sign-in...".toByteArray(),
                                            )
                                            Toast.makeText(ctx, "Sent to watch", Toast.LENGTH_SHORT).show()
                                            onClose()
                                        }.addOnFailureListener {
                                            messageClient.sendMessage(
                                                sourceNodeId,
                                                PATH_LOGIN_STATUS,
                                                "failed|Failed to send session to watch. Keep devices connected and retry.".toByteArray(),
                                            )
                                            Toast.makeText(ctx, "Failed to send to watch", Toast.LENGTH_SHORT).show()
                                            status.value = it.message ?: "Failed to send to watch."
                                        }
                                    return
                                }

                                // Fallback: broadcast to all connected nodes.
                                nodeClient.connectedNodes
                                    .addOnSuccessListener { nodes ->
                                        if (nodes.isEmpty()) {
                                            status.value = "No watch connected."
                                            sendStatusToWatch("failed", "No watch connected.")
                                            Toast.makeText(ctx, "No watch connected", Toast.LENGTH_SHORT).show()
                                            return@addOnSuccessListener
                                        }
                                        nodes.forEach { node ->
                                            messageClient.sendMessage(node.id, PATH_LOGIN_COOKIE, payload)
                                            messageClient.sendMessage(
                                                node.id,
                                                PATH_LOGIN_STATUS,
                                                "processing|Session sent to watch. Applying sign-in...".toByteArray(),
                                            )
                                        }
                                        Toast.makeText(ctx, "Sent to watch", Toast.LENGTH_SHORT).show()
                                        onClose()
                                    }.addOnFailureListener { e ->
                                        status.value = e.message ?: "Failed to query watch nodes."
                                        sendStatusToWatch("failed", "Failed to query watch connection.")
                                        Toast.makeText(ctx, "Failed to query watch", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }

                    loadUrl(Config.LOG_IN_URL)
                }
            },
        )
    }
}
