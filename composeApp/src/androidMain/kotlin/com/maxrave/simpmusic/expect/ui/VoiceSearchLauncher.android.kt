package com.maxrave.simpmusic.expect.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.maxrave.logger.Logger

@Composable
actual fun rememberVoiceSearchLauncher(onResult: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val query = results[0]
                onResult(query)
            }
        }
    }

    return remember {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for a song, or hum a tune")
                // A known trick to prompt Google app to try sound search if it detects humming
                putExtra("android.speech.extra.DICTATION_MODE", true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            try {
                launcher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                Logger.e("VoiceSearch", "Speech recognition not available", e)
                Toast.makeText(context, "Voice search is not available on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
