package com.maxrave.simpmusic.service.test.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.maxrave.simpmusic.MainActivity
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.utils.ComposeResUtils
import kotlinx.coroutines.runBlocking

object NotificationHandler {
    private const val CHANNEL_ID = "transactions_reminder_channel"

    suspend fun createReminderNotification(
        context: Context,
        noti: NotificationModel,
    ) {
        //  No back-stack when launched
        val action = Intent(context, MainActivity::class.java)
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        action.data = "simpmusic://notification".toUri()
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                action,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val bitmap =
            runBlocking {
                val loader = ImageLoader(context)
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(
                            noti.single
                                .firstOrNull()
                                ?.thumbnails
                                ?.lastOrNull()
                                ?.url
                                ?: noti.album
                                    .firstOrNull()
                                    ?.thumbnails
                                    ?.lastOrNull()
                                    ?.url,
                        ).allowHardware(false) // Disable hardware bitmaps.
                        .build()

                return@runBlocking when (val result = loader.execute(request)) {
                    is SuccessResult -> {
                        result.image.toBitmap()
                    }

                    else -> {
                        AppCompatResources
                            .getDrawable(context, R.drawable.holder)
                            ?.toBitmap(128, 128)
                    }
                }
            }
        val builder =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.mono)
                .setContentTitle(noti.name)
                .setContentText(
                    if (noti.single.isNotEmpty()) {
                        "${ComposeResUtils.getResString(ComposeResUtils.StringType.NEW_SINGLES)}: ${noti.single.joinToString { it.title }}"
                    } else {
                        "${ComposeResUtils.getResString(ComposeResUtils.StringType.NEW_ALBUMS)}: ${noti.album.joinToString { it.title }}"
                    },
                ).setPriority(NotificationCompat.PRIORITY_HIGH)
                .setLargeIcon(bitmap)
                .setContentIntent(pendingIntent) // For launching the MainActivity
                .setAutoCancel(true) // Remove notification when tapped
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(noti.hashCode(), builder.build())
        }
    }

    /**
     * Required on Android O+
     */
    fun createNotificationChannel(context: Context) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val name = "Update Followed Artists"
            val descriptionText =
                "This channel sends notification when followed artists release new music"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel =
                NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
            // Register the channel with the system

            notificationManager.createNotificationChannel(channel)
        }
    }

    private const val BLOG_CHANNEL_ID = "blog_updates_channel"

    /**
     * Separate channel so users can silence blog updates without affecting artist-release
     * notifications.
     */
    fun createBlogNotificationChannel(context: Context) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(BLOG_CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(
                    BLOG_CHANNEL_ID,
                    "Blog updates",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Notifies when a new blog post is published"
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a local notification for a new blog post. Tapping opens [url] in the browser
     * (ACTION_VIEW). No image is loaded — the feed carries no per-item thumbnail.
     * The notification id is derived from [url] so the same post never stacks duplicates.
     */
    fun createBlogNotification(
        context: Context,
        title: String,
        text: String?,
        url: String,
    ) {
        val action =
            Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                url.hashCode(),
                action,
                PendingIntent.FLAG_IMMUTABLE,
            )
        val builder =
            NotificationCompat
                .Builder(context, BLOG_CHANNEL_ID)
                .setSmallIcon(R.drawable.mono)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(url.hashCode(), builder.build())
        }
    }
}