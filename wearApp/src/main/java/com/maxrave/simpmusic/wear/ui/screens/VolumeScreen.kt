package com.maxrave.simpmusic.wear.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.maxrave.simpmusic.wear.ui.components.ThinProgressBar
import com.maxrave.simpmusic.wear.ui.components.WearList
import kotlinx.coroutines.delay

@Composable
fun VolumeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val audioManager =
        remember {
            (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        }
    var maxVolume by remember { mutableIntStateOf(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)) }
    var currentVolume by remember {
        mutableIntStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, maxVolume),
        )
    }

    fun syncVolume() {
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, maxVolume)
    }

    fun setVolume(target: Int) {
        val clamped = target.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        currentVolume = clamped
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(800L)
            syncVolume()
        }
    }

    val fraction = currentVolume.toFloat() / maxVolume.toFloat()
    val percent = (fraction * 100f).toInt().coerceIn(0, 100)

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
                    text = "Volume",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        item {
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleLarge,
            )
        }

        item {
            ThinProgressBar(progress = fraction)
        }

        item { Spacer(Modifier.height(6.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { setVolume(currentVolume - 1) },
                    enabled = currentVolume > 0,
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Volume down")
                }
                IconButton(
                    onClick = { setVolume(currentVolume + 1) },
                    enabled = currentVolume < maxVolume,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Volume up")
                }
            }
        }

        item { Spacer(Modifier.height(6.dp)) }

        item {
            FilledTonalButton(
                onClick = { setVolume(maxVolume) },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentVolume < maxVolume,
            ) {
                Text("Max volume")
            }
        }
    }
}
