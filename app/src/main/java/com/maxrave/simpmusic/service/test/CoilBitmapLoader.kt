package com.maxrave.simpmusic.service.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.guava.future
import java.util.concurrent.ExecutionException

@UnstableApi
class CoilBitmapLoader(private val context: Context): BitmapLoader {
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return GlobalScope.future(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("Could not decode image data")
        }
    }

    override fun loadBitmap(uri: Uri, options: BitmapFactory.Options?): ListenableFuture<Bitmap> {
        return GlobalScope.future(Dispatchers.IO) {
            val result =
                (context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(uri)
                        .allowHardware(false)
                        .build()
                ))
            if (result is ErrorResult) {
                throw ExecutionException(result.throwable)
            }
            try {
                (result.drawable as BitmapDrawable).bitmap
            } catch (e: Exception) {
                throw ExecutionException(e)
            }
        }
    }
}