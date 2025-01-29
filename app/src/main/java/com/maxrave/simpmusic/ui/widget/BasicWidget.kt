package com.maxrave.simpmusic.ui.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler
import com.maxrave.simpmusic.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@UnstableApi
class BasicWidget : BaseAppWidget() {
    /**
     * Initialize given widgets to default state, where we launch Music on default click and hide
     * actions if service not running.
     */

    override fun defaultAppWidget(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        val appWidgetView =
            RemoteViews(
                context.packageName,
                R.layout.app_widget_base,
            )

        appWidgetView.setViewVisibility(
            R.id.media_titles,
            View.INVISIBLE,
        )
        appWidgetView.setImageViewResource(R.id.image, R.drawable.holder_video)
        appWidgetView.setImageViewResource(
            R.id.button_toggle_play_pause,
            R.drawable.play_widget,
        )
        appWidgetView.setImageViewResource(
            R.id.button_next,
            R.drawable.next_widget,
        )
        appWidgetView.setImageViewResource(
            R.id.button_prev,
            R.drawable.previous_widget,
        )
        appWidgetView.setImageViewResource(
            R.id.logo,
            R.mipmap.ic_launcher_round,
        )

//        linkButtons(context, appWidgetView)
        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    /**
     * Update all active widget instances by pushing changes
     */
    override fun performUpdate(
        context: Context,
        handler: SimpleMediaServiceHandler,
        appWidgetIds: IntArray?,
    ) {
        Log.d("BasicWidget", "performUpdate")
        val appWidgetView =
            RemoteViews(
                context.packageName,
                R.layout.app_widget_base,
            )
        val isPlaying = handler.player.isPlaying
        val song = runBlocking { handler.nowPlaying.first() }

        // Set the titles and artwork
        if (song?.mediaMetadata?.title.isNullOrEmpty() && song?.mediaMetadata?.artist.isNullOrEmpty()) {
            appWidgetView.setViewVisibility(
                R.id.media_titles,
                View.INVISIBLE,
            )
        } else {
            appWidgetView.setViewVisibility(
                R.id.media_titles,
                View.VISIBLE,
            )
            appWidgetView.setTextViewText(R.id.title, song?.mediaMetadata?.title)
            appWidgetView.setTextViewText(
                R.id.text,
                song?.mediaMetadata?.artist,
            )
        }
        // Set prev/next button drawables
        appWidgetView.setImageViewResource(
            R.id.button_next,
            R.drawable.next_widget,
        )
        appWidgetView.setImageViewResource(
            R.id.button_prev,
            R.drawable.previous_widget,
        )
        appWidgetView.setImageViewResource(
            R.id.button_toggle_play_pause,
            if (!isPlaying) R.drawable.play_widget else R.drawable.pause_widget,
        )
        appWidgetView.setImageViewResource(
            R.id.logo,
            R.mipmap.ic_launcher_round,
        )

        // Link actions buttons to intents
        linkButtons(context, appWidgetView)

        // Load the album cover async and push the update on completion
        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    fun updateImage(
        context: Context,
        bitmap: Bitmap,
    ) {
        Log.w("BasicWidget", "updateImage")
        val appWidgetView =
            RemoteViews(
                context.packageName,
                R.layout.app_widget_base,
            )
        appWidgetView.setImageViewBitmap(R.id.image, bitmap)
        pushUpdatePartially(context, appWidgetView)
    }

    fun updatePlayingState(
        context: Context,
        isPlaying: Boolean,
    ) {
        val appWidgetView =
            RemoteViews(
                context.packageName,
                R.layout.app_widget_base,
            )
        appWidgetView.setImageViewResource(
            R.id.button_toggle_play_pause,
            if (!isPlaying) R.drawable.play_widget else R.drawable.pause_widget,
        )
        pushUpdatePartially(context, appWidgetView)
    }

    /**
     * Link up various button actions using [PendingIntent].
     */
    @UnstableApi
    private fun linkButtons(
        context: Context,
        views: RemoteViews,
    ) {
        val action = Intent(context, MainActivity::class.java)

        // Home
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        var pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                action,
                PendingIntent.FLAG_IMMUTABLE,
            )
        views.setOnClickPendingIntent(R.id.clickable_area, pendingIntent)

        // Previous track
        pendingIntent = buildPendingIntent(context, ACTION_REWIND)
        views.setOnClickPendingIntent(R.id.button_prev, pendingIntent)

        // Play and pause
        pendingIntent = buildPendingIntent(context, ACTION_TOGGLE_PAUSE)
        views.setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent)

        // Next track
        pendingIntent = buildPendingIntent(context, ACTION_SKIP)
        views.setOnClickPendingIntent(R.id.button_next, pendingIntent)
    }

    companion object {
        const val NAME: String = "basic_widget"
        const val ACTION_TOGGLE_PAUSE = "com.maxrave.simpmusic.action.TOGGLE_PAUSE"
        const val ACTION_REWIND = "com.maxrave.simpmusic.action.REWIND"
        const val ACTION_SKIP = "com.maxrave.simpmusic.action.SKIP"
        private var mInstance: BasicWidget? = null

        val instance: BasicWidget
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = BasicWidget()
                }
                return mInstance!!
            }
    }
}