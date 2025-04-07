package com.maxrave.simpmusic.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.media3.common.util.UnstableApi
import com.maxrave.simpmusic.service.SimpleMediaService
import com.maxrave.simpmusic.service.SimpleMediaServiceHandler

abstract class BaseAppWidget : AppWidgetProvider() {
    /**
     * {@inheritDoc}
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        defaultAppWidget(context, appWidgetIds)
    }

    /**
     * Handle a change notification coming over from [MusicService]
     */
    @UnstableApi
    fun notifyChange(
        context: Context,
        handler: SimpleMediaServiceHandler,
        what: String,
    ) {
        if (hasInstances(context)) {
            if (META_CHANGED == what || PLAY_STATE_CHANGED == what || REPEAT_MODE_CHANGED == what || SHUFFLE_MODE_CHANGED == what) {
                performUpdate(context, handler, null)
            }
        }
    }

    protected fun pushUpdate(
        context: Context,
        appWidgetIds: IntArray?,
        views: RemoteViews,
    ) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (appWidgetIds != null) {
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            } else {
                appWidgetManager.updateAppWidget(ComponentName(context, javaClass), views)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected fun pushUpdatePartially(
        context: Context,
        views: RemoteViews,
    ) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            appWidgetManager.partiallyUpdateAppWidget(
                appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass)),
                views,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Check against [AppWidgetManager] if there are any instances of this widget.
     */
    private fun hasInstances(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val mAppWidgetIds =
            appWidgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    javaClass,
                ),
            )
        return mAppWidgetIds.isNotEmpty()
    }

    @UnstableApi
    protected fun buildPendingIntent(
        context: Context,
        action: String,
    ): PendingIntent {
        val intent =
            Intent(context, SimpleMediaService::class.java).apply {
                this.action = action
            }
        return PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    protected abstract fun defaultAppWidget(
        context: Context,
        appWidgetIds: IntArray,
    )

    @UnstableApi
    abstract fun performUpdate(
        context: Context,
        handler: SimpleMediaServiceHandler,
        appWidgetIds: IntArray?,
    )

    companion object {
        const val NAME: String = "app_widget"
        const val APP_WIDGET_UPDATE: String = "app_widget_update"
        const val EXTRA_APP_WIDGET_NAME: String = "app_widget_name"
        const val META_CHANGED: String = "meta_changed"
        const val PLAY_STATE_CHANGED: String = "play_state_changed"
        const val REPEAT_MODE_CHANGED: String = "repeat_mode_changed"
        const val SHUFFLE_MODE_CHANGED: String = "shuffle_mode_changed"
    }
}