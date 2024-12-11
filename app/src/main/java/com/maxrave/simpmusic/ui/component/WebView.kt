package com.maxrave.simpmusic.ui.component

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.maxrave.simpmusic.R

@Composable
fun WebView(
    modifier: Modifier = Modifier,
    url: String,
    onFinished: () -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView?,
                            url: String?,
                        ) {
                            onFinished()
                        }
                    }
//                settings.javaScriptEnabled = true
                loadUrl(url)
            }
        },
    )
}

@Preview
@Composable
fun WebViewPreview() {
    Column {
        NormalAppBar(
            title = {
                Text(text = "Title")
            },
            leftIcon = {
                IconButton(onClick = { }) {
                    Icon(
                        painterResource(id = R.drawable.baseline_arrow_back_ios_new_24),
                        contentDescription = "Back",
                    )
                }
            },
            rightIcon = {
                IconButton(onClick = { }) {
                    Icon(
                        painterResource(id = R.drawable.baseline_more_vert_24),
                        contentDescription = "Back",
                    )
                }
            },
        )
        WebView(modifier = Modifier.fillMaxSize(), url = "https://www.google.com", onFinished = {})
    }
}