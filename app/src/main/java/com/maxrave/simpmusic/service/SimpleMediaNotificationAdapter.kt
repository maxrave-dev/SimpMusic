package com.maxrave.simpmusic.service

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerNotificationManager
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.maxrave.simpmusic.R

@UnstableApi
class SimpleMediaNotificationAdapter(
    private val context: Context,
    private val pendingIntent: PendingIntent?
) : PlayerNotificationManager.MediaDescriptionAdapter {

    override fun getCurrentContentTitle(player: Player): CharSequence {
//        Log.d("SimpleMediaNotificationAdapter", "getCurrentContentTitle: ${player.currentMediaItem?.mediaMetadata?.title}")
     return player.currentMediaItem?.mediaMetadata?.title ?: ""
    }
    override fun createCurrentContentIntent(player: Player): PendingIntent? =
        pendingIntent

    override fun getCurrentContentText(player: Player): CharSequence =
        player.currentMediaItem?.mediaMetadata?.artist ?: ""

    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        val request = ImageRequest.Builder(context)
            .placeholder(R.drawable.holder)
            .diskCacheKey(player.currentMediaItem?.mediaId)
            .diskCachePolicy(CachePolicy.ENABLED)
            .data(player.mediaMetadata.artworkUri)
            .target(
                onStart = {
                },
                onSuccess = { result ->
                    callback.onBitmap(result.toBitmap())
                },
                onError = { error ->
                    Log.d("SimpleMediaNotificationAdapter", "onError: $error")
                    callback.onBitmap((AppCompatResources.getDrawable(context, R.drawable.holder) as BitmapDrawable).bitmap)
                }
            )
            .build()
        ImageLoader(context).enqueue(request)
        return null
    }
}