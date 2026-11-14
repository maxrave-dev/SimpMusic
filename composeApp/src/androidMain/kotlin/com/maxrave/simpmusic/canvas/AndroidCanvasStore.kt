package com.maxrave.simpmusic.canvas

import android.content.Context
import com.maxrave.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Android implementation: one directory per downloaded song under `filesDir/canvas`.
 *
 * The files live in app-private storage, not the media cache, so clearing the online canvas
 * cache or letting the player's cache evict entries cannot take a downloaded song's canvas with
 * it. Every download is assembled in a temporary directory and renamed into place, so a partial
 * canvas is never visible to the player.
 */
class AndroidCanvasStore(
    private val context: Context,
) : CanvasStore {
    private val directory = File(context.filesDir, CANVAS_DIRECTORY)
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    // Downloads finish in parallel; serialise canvas writes so a burst of downloads cannot open
    // dozens of media connections at once, and two songs cannot race on the same directory.
    private val mutex = Mutex()

    override suspend fun save(
        videoId: String,
        url: String,
        thumbUrl: String?,
        isVideo: Boolean,
    ): PinnedCanvas? =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val target = directoryFor(videoId) ?: return@withContext null
                val source = if (url.isHlsUrl()) CanvasSource.AM else CanvasSource.SPOTIFY
                val existing = readPinned(target)
                if (existing != null && existing.source == source && existing.isVideo == isVideo) {
                    return@withContext existing
                }

                directory.mkdirs()
                val temporary = File(directory, "$videoId.tmp")
                temporary.deleteRecursively()
                temporary.mkdirs()
                try {
                    val downloaded =
                        when {
                            source == CanvasSource.AM -> downloadHls(url, temporary)
                            isVideo -> downloadFile(url, File(temporary, CANVAS_VIDEO_FILE))
                            else -> downloadFile(url, File(temporary, CANVAS_IMAGE_FILE))
                        }
                    if (!downloaded) {
                        temporary.deleteRecursively()
                        return@withContext null
                    }
                    if (!thumbUrl.isNullOrBlank()) {
                        // A missing still is not a failed canvas: the clip itself is the feature.
                        downloadFile(thumbUrl, File(temporary, CANVAS_THUMB_FILE))
                    }
                    File(temporary, CANVAS_SOURCE_FILE).writeText("${source.name.lowercase()}\n$url\n")
                    if (target.exists()) target.deleteRecursively()
                    if (!temporary.renameTo(target)) {
                        temporary.deleteRecursively()
                        Logger.w(TAG, "Could not move canvas for $videoId into place")
                        return@withContext null
                    }
                    readPinned(target)
                } catch (error: CancellationException) {
                    temporary.deleteRecursively()
                    throw error
                } catch (error: Exception) {
                    temporary.deleteRecursively()
                    Logger.w(TAG, "Could not pin canvas for $videoId: ${error.message}")
                    null
                }
            }
        }

    override suspend fun pinned(videoId: String): PinnedCanvas? =
        withContext(Dispatchers.IO) {
            directoryFor(videoId)?.let(::readPinned)
        }

    override suspend fun delete(videoId: String) =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                directoryFor(videoId)?.deleteRecursively()
                File(directory, "$videoId.tmp").deleteRecursively()
                Unit
            }
        }

    override suspend fun retainOnly(videoIds: Collection<String>) =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val keep = videoIds.toHashSet()
                directory.listFiles()?.forEach { file ->
                    if (file.name !in keep) file.deleteRecursively()
                }
                Unit
            }
        }

    override suspend fun clear() =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                directory.deleteRecursively()
                Unit
            }
        }

    private suspend fun downloadHls(
        url: String,
        target: File,
    ): Boolean {
        val (master, finalUrl) = fetchText(url) ?: return false
        // The repository normally resolves the ladder before handing the url over. A master here
        // means that resolution failed; choosing a rendition in this layer would duplicate that
        // policy, so leave the song to the online path instead of pinning a stream we cannot
        // choose safely.
        if (master.contains("#EXT-X-STREAM-INF")) {
            Logger.w(TAG, "Not pinning a master playlist: $finalUrl")
            return false
        }
        val rewritten = rewriteCanvasPlaylist(master, finalUrl)
        for (entry in rewritten.entries) {
            coroutineContext.ensureActive()
            if (!downloadFile(entry.sourceUrl, File(target, entry.fileName))) return false
        }
        File(target, CANVAS_PLAYLIST_FILE).writeText(rewritten.text)
        return true
    }

    private fun downloadFile(
        url: String,
        target: File,
    ): Boolean {
        target.parentFile?.mkdirs()
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Logger.w(TAG, "Canvas request failed: ${response.code} $url")
                    return false
                }
                val body = response.body
                target.outputStream().buffered().use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                }
                target.length() > 0L
            }
        } catch (error: Exception) {
            target.delete()
            Logger.w(TAG, "Canvas download failed: ${error.message} $url")
            false
        }
    }

    private fun fetchText(url: String): Pair<String, String>? =
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Logger.w(TAG, "Canvas playlist request failed: ${response.code} $url")
                    return null
                }
                val body = response.body.string()
                body to response.request.url.toString()
            }
        } catch (error: Exception) {
            Logger.w(TAG, "Canvas playlist fetch failed: ${error.message} $url")
            null
        }

    private fun readPinned(target: File): PinnedCanvas? {
        if (!target.isDirectory) return null
        val playlist = target.existingFile(CANVAS_PLAYLIST_FILE)
        val video = target.existingFile(CANVAS_VIDEO_FILE)
        val image = target.existingFile(CANVAS_IMAGE_FILE)
        val file = playlist ?: video ?: image ?: return null
        val source =
            target
                .existingFile(CANVAS_SOURCE_FILE)
                ?.readText()
                ?.lineSequence()
                ?.firstOrNull()
                ?.trim()
                ?.lowercase()
                ?.let { value -> CanvasSource.entries.firstOrNull { it.name.lowercase() == value } }
                ?: if (playlist != null) CanvasSource.AM else CanvasSource.SPOTIFY
        return PinnedCanvas(
            url = canvasFileUrl(file.absolutePath),
            thumbUrl = target.existingFile(CANVAS_THUMB_FILE)?.let { canvasFileUrl(it.absolutePath) },
            isVideo = playlist != null || video != null,
            source = source,
        )
    }

    private fun File.existingFile(name: String): File? =
        File(this, name).takeIf { it.isFile && it.length() > 0L }

    private fun directoryFor(videoId: String): File? =
        videoId.takeIf(::isCanvasId)?.let { File(directory, it) }

    private fun String.isHlsUrl(): Boolean = contains(".m3u8", ignoreCase = true)

    private companion object {
        const val TAG = "CanvasStore"
    }
}
