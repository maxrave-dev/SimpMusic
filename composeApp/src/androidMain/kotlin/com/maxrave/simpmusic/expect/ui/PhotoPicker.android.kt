package com.maxrave.simpmusic.expect.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.maxrave.logger.Logger

@Composable
actual fun photoPickerResult(onResultUri: (String?) -> Unit): PhotoPickerLauncher {
    val context = LocalContext.current
    val resultLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                Logger.d("ID", Build.ID.toString())
                val intentRef = activityResult.data
                val data = intentRef?.data
                if (data != null) {
                    val contentResolver = context.contentResolver

                    val takeFlags: Int =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    // Check for the freshest data.
                    context.grantUriPermission(
                        context.packageName,
                        data,
                        takeFlags,
                    )
                    contentResolver?.takePersistableUriPermission(data, takeFlags)
                    val uri = data.toString()
                    onResultUri(uri)
                }
            }
        }
    return object : PhotoPickerLauncher {
        override fun launch() {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_OPEN_DOCUMENT
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            resultLauncher.launch(intent)
        }
    }
}