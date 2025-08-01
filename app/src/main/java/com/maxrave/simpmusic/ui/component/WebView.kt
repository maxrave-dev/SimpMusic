package com.maxrave.simpmusic.ui.component

import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.LogInViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun GetDataSyncIdBottomSheet(
    cookie: String,
    onDismissRequest: () -> Unit,
    viewModel: LogInViewModel = koinViewModel(),
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            onDismissRequest()
        },
        containerColor = Color.Black,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = .5f),
        sheetState = sheetState,
        modifier =
            Modifier
                .fillMaxHeight(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        Column {
            TopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors().copy(
                        containerColor = Color.Transparent,
                    ),
                title = {
                    Text(
                        text = stringResource(R.string.retrieve_youtube_data),
                        style = typo.bodyMedium,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_keyboard_arrow_down_24),
                            contentDescription = "",
                            tint = Color.White,
                        )
                    }
                },
            )
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    CookieManager.getInstance().setCookie(
                        Config.YOUTUBE_MUSIC_MAIN_URL,
                        cookie,
                    )
                    WebView(context).apply {
                        webViewClient =
                            object : WebViewClient() {
                                override fun onPageFinished(
                                    view: WebView?,
                                    url: String?,
                                ) {
                                    loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                                    loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")
                                }
                            }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                @UnstableApi
                                fun onRetrieveVisitorData(newVisitorData: String?) {
                                    if (newVisitorData != null) {
                                        viewModel.setVisitorData(newVisitorData)
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(R.string.visitor_data_retrieved),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        // Clear all the cookies
                                        CookieManager.getInstance().removeAllCookies(null)
                                        CookieManager.getInstance().flush()
                                    }
                                }

                                @JavascriptInterface
                                @UnstableApi
                                fun onRetrieveDataSyncId(newDataSyncId: String?) {
                                    if (newDataSyncId != null) {
                                        viewModel.setDataSyncId(newDataSyncId.substringBefore("||"))
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(R.string.data_sync_id_retrieved),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        // Clear all the cookies
                                        CookieManager.getInstance().removeAllCookies(null)
                                        CookieManager.getInstance().flush()
                                    }
                                }
                            },
                            "Android",
                        )
                        loadUrl(Config.YOUTUBE_MUSIC_MAIN_URL)
                    }
                },
            )
        }
    }
}