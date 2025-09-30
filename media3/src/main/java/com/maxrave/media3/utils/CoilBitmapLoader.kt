package com.maxrave.media3.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import java.util.concurrent.ExecutionException

@UnstableApi
class CoilBitmapLoader(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        coroutineScope.future(Dispatchers.IO) {
            BitmapFactory.decodeByteArray(data, 0, data.size)
                ?: error("Could not decode image data")
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        coroutineScope.future(Dispatchers.IO) {
            val result =
                (
                    context.imageLoader.execute(
                        ImageRequest
                            .Builder(context)
                            .data(uri)
                            .allowHardware(false)
                            .build(),
                    )
                )
            if (result is ErrorResult) {
                throw ExecutionException(result.throwable)
            }
            try {
                result.image?.toBitmap() ?: throw ExecutionException(NullPointerException())
            } catch (e: Exception) {
                throw ExecutionException(e)
            }
        }
}