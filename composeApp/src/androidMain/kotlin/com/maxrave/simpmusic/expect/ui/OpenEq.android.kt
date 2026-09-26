package com.maxrave.simpmusic.expect.ui

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.maxrave.logger.Logger
import kotlinx.coroutines.runBlocking
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.getString
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.no_equalizer

@Composable
actual fun openEqResult(audioSessionId: Int): OpenEqLauncher {
    val context = LocalContext.current
    val resultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    return object : OpenEqLauncher {
        override fun launch() {
            val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            val packageManager = context.packageManager
            val resolveInfo: List<*> = packageManager.queryIntentActivities(eqIntent, 0)
            Logger.d("EQ", resolveInfo.toString())
            if (resolveInfo.isEmpty()) {
                showToast(runBlocking { getString(Res.string.no_equalizer) }, ToastGravity.Bottom)
            } else {
                resultLauncher.launch(eqIntent)
            }
        }
    }
}