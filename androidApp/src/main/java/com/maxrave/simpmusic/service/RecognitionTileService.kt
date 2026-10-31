package com.maxrave.simpmusic.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.net.toUri
import com.maxrave.simpmusic.MainActivity

/**
 * Quick Settings tile for song recognition, a shortcut into the app: it opens
 * simpmusic://recognize, which shows Search with the recognition sheet listening. The sheet does
 * the recording and asks for the microphone itself.
 */
class RecognitionTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        // A shortcut, not a switch, so it is always drawn off.
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent =
            Intent(Intent.ACTION_VIEW, "simpmusic://recognize".toUri(), this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // The Intent overload throws UnsupportedOperationException from Android 14 on.
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        } else {
            // Below Android 14 only; the lint check flags the call whatever the version guard.
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
