package com.maxrave.simpmusic.artwork

import android.content.Context
import android.graphics.Bitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.toBitmap
import com.maxrave.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Android implementation: one image file per downloaded song under `filesDir/artwork`.
 *
 * Files live in app-private storage, not the cache, so clearing the thumbnail cache or letting
 * Coil evict its 512 MiB cache cannot take a downloaded song's cover with it.
 */
class AndroidArtworkStore(
    private val context: Context,
) : ArtworkStore {
    private val directory = File(context.filesDir, ARTWORK_DIRECTORY)

    // Downloads finish in parallel; serialise writes so two songs cannot race on the same file.
    private val mutex = Mutex()

    override suspend fun save(
        videoId: String,
        url: String,
    ): Boolean =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val file = fileFor(url) ?: return@withContext false
                if (file.isFile && file.length() > 0L) return@withContext true
                directory.mkdirs()
                val temporary = File(directory, "${file.name}.tmp")
                try {
                    val result =
                        context.imageLoader.execute(
                            ImageRequest
                                .Builder(context)
                                .data(url)
                                .size(Size.ORIGINAL)
                                .allowHardware(false)
                                .build(),
                        )
                    val bitmap = result.image?.toBitmap() ?: return@withContext false
                    val written =
                        FileOutputStream(temporary).use { output ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                        }
                    if (!written) {
                        temporary.delete()
                        return@withContext false
                    }
                    if (!temporary.renameTo(file)) {
                        temporary.delete()
                        Logger.w(TAG, "Could not move artwork for $videoId into place")
                        false
                    } else {
                        true
                    }
                } catch (error: CancellationException) {
                    temporary.delete()
                    throw error
                } catch (error: Exception) {
                    temporary.delete()
                    Logger.w(TAG, "Could not save artwork for $videoId: ${error.message}")
                    false
                }
            }
        }

    override suspend fun delete(
        videoId: String,
        url: String?,
    ) = mutex.withLock {
        withContext(Dispatchers.IO) {
            // The URL names the exact image; without it, YouTube thumbnails are named after the
            // video id. Deleting only that file keeps another song's shared cover intact.
            (fileFor(url) ?: File(directory, "$videoId.jpg")).delete()
            Unit
        }
    }

    override suspend fun retainOnly(urls: Collection<String>) =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val keep = urls.mapNotNullTo(HashSet(), ::artworkFileName)
                directory.listFiles()?.forEach { file ->
                    if (file.isFile && file.name !in keep) file.delete()
                }
            }
        }

    override fun localArtwork(url: String): String? =
        fileFor(url)
            ?.takeIf { it.isFile && it.length() > 0L }
            ?.absolutePath

    private fun fileFor(url: String?): File? =
        url?.let(::artworkFileName)?.let { File(directory, it) }

    private companion object {
        const val TAG = "ArtworkStore"
    }
}
