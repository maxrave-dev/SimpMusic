package com.maxrave.simpmusic.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.ui.theme.typo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingDialog(
    loading: Boolean,
    message: String,
) {
    val context = LocalContext.current
    if (loading) {
        Box(modifier = Modifier.fillMaxSize()) {
            BasicAlertDialog(
                onDismissRequest = { },
                modifier = Modifier.wrapContentSize(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = Color(0xFF242424),
                    tonalElevation = AlertDialogDefaults.TonalElevation,
                    shadowElevation = 1.dp,
                ) {
                    Column(
                        Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 20.dp,
                        ),
                    ) {
                        Text(
                            context.getString(R.string.loading),
                            style = typo.headlineMedium,
                        )
                        Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator()
                            Spacer(Modifier.size(15.dp))
                            Text(text = message)
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
fun LoadingDialogPreview() {
    LoadingDialog(loading = true, message = "Loading")
}