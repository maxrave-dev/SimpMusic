package com.maxrave.simpmusic.expect.ui

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// External volume changes (hardware keys, another app) aren't observable without registering a
// broadcast receiver for ACTION_VOLUME_CHANGED; a cheap 1s poll picks them up without that extra
// lifecycle to manage — see CLAUDE.md's brief for this control (Apple Music style volume row).
private const val POLL_INTERVAL_MS = 1000L

private fun AudioManager.currentVolumeFraction(): Float {
    val max = getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    if (max <= 0) return 0f
    return getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

@Composable
actual fun rememberDeviceVolumeController(): DeviceVolumeController? {
    val context = LocalContext.current
    val audioManager =
        remember(context) {
            context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } ?: return null

    // Backing state for `DeviceVolumeController.volumeFraction` — captured by the object below
    // so a write from `setVolumeFraction` is reflected immediately, without waiting on the poll.
    var fraction by remember(audioManager) { mutableFloatStateOf(audioManager.currentVolumeFraction()) }

    LaunchedEffect(audioManager) {
        while (true) {
            delay(POLL_INTERVAL_MS)
            fraction = audioManager.currentVolumeFraction()
        }
    }

    return remember(audioManager) {
        object : DeviceVolumeController {
            override val volumeFraction: Float get() = fraction

            override fun setVolumeFraction(newFraction: Float) {
                val clamped = newFraction.coerceIn(0f, 1f)
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                // FLAG 0 — no system volume UI flash; this row IS the volume UI.
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (clamped * max).roundToInt(), 0)
                // Reflect immediately rather than waiting on the next poll tick, so the slider
                // doesn't lag behind the finger.
                fraction = clamped
            }
        }
    }
}
