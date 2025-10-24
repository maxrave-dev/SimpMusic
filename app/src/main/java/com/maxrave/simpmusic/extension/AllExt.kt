package com.maxrave.simpmusic.extension

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Service
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.runtime.Composable
import com.maxrave.common.R
import com.maxrave.domain.data.model.browse.artist.ArtistBrowse
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.viewModel.ArtistScreenData
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Suppress("deprecation")
fun Context.isMyServiceRunning(serviceClass: Class<out Service>) =
    try {
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    } catch (e: Exception) {
        false
    }

fun String?.removeDuplicateWords(): String {
    if (this == null) {
        return "null"
    } else {
        val regex = Regex("\\b(\\w+)\\b\\s*(?=.*\\b\\1\\b)")
        return this.replace(regex, "")
    }
}

fun setEnabledAll(
    v: View,
    enabled: Boolean,
) {
    v.isEnabled = enabled
    v.isFocusable = enabled
    if (v is ImageButton) {
        if (enabled) v.setColorFilter(Color.WHITE) else v.setColorFilter(Color.GRAY)
    }
    if (v is TextView) {
        v.isEnabled = enabled
    }
    if (v is ViewGroup) {
        val vg = v
        for (i in 0 until vg.childCount) setEnabledAll(vg.getChildAt(i), enabled)
    }
}

fun getScreenSize(context: Context): Point {
    val x: Int = context.resources.displayMetrics.widthPixels
    val y: Int = context.resources.displayMetrics.heightPixels
    return Point(x, y)
}

fun ArrayList<String>.removeConflicts(): ArrayList<String> {
    val nonConflictingSet = HashSet<String>()
    val nonConflictingList = ArrayList<String>()

    for (item in this) {
        if (nonConflictingSet.add(item)) {
            nonConflictingList.add(item)
        }
    }

    return nonConflictingList
}

fun <T> Iterable<T>.indexMap(): Map<T, Int> {
    val map = mutableMapOf<T, Int>()
    forEachIndexed { i, v ->
        map[v] = i
    }
    return map
}

infix fun <E> Collection<E>.symmetricDifference(other: Collection<E>): Set<E> {
    val left = this subtract other
    val right = other subtract this
    return left union right
}

fun LocalDateTime.formatTimeAgo(context: Context): String {
    val now = LocalDateTime.now()
    val hoursDiff = ChronoUnit.HOURS.between(this, now)
    val daysDiff = ChronoUnit.DAYS.between(this, now)
    val monthsDiff = ChronoUnit.MONTHS.between(this, now)

    return when {
        monthsDiff >= 1 -> context.getString(R.string.month_s_ago, monthsDiff)
        daysDiff >= 30 -> context.getString(R.string.month_s_ago, daysDiff / 30)
        hoursDiff >= 24 -> context.getString(R.string.day_s_ago, daysDiff)
        hoursDiff > 1 -> context.getString(R.string.hour_s_ago, hoursDiff)
        hoursDiff <= 1 -> context.getString(R.string.recently)
        else -> context.getString(R.string.unknown)
    }
}

fun formatDuration(
    duration: Long,
    context: Context,
): String {
    if (duration < 0L) return context.getString(R.string.na_na)
    val minutes: Long = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
    val seconds: Long = (
        TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS) -
            minutes * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)
    )
    return String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
}

fun parseTimestampToMilliseconds(timestamp: String): Double {
    val parts = timestamp.split(":")
    val totalSeconds =
        when (parts.size) {
            2 -> {
                try {
                    val minutes = parts[0].toDouble()
                    val seconds = parts[1].toDouble()
                    (minutes * 60 + seconds)
                } catch (e: NumberFormatException) {
                    // Handle parsing error
                    e.printStackTrace()
                    return 0.0
                }
            }

            3 -> {
                try {
                    val hours = parts[0].toDouble()
                    val minutes = parts[1].toDouble()
                    val seconds = parts[2].toDouble()
                    (hours * 3600 + minutes * 60 + seconds)
                } catch (e: NumberFormatException) {
                    // Handle parsing error
                    e.printStackTrace()
                    return 0.0
                }
            }

            else -> {
                // Handle incorrect format
                return 0.0
            }
        }
    return totalSeconds * 1000
}

operator fun File.div(child: String): File = File(this, child)

fun InputStream.zipInputStream(): ZipInputStream = ZipInputStream(this)

fun OutputStream.zipOutputStream(): ZipOutputStream = ZipOutputStream(this)

fun Long?.bytesToMB(): Long {
    val mbInBytes = 1024 * 1024
    return this?.div(mbInBytes) ?: 0L
}

fun getSizeOfFile(dir: File): Long {
    var dirSize: Long = 0
    if (!dir.listFiles().isNullOrEmpty()) {
        for (f in dir.listFiles()!!) {
            dirSize += f.length()
            if (f.isDirectory) {
                dirSize += getSizeOfFile(f)
            }
        }
    }
    return dirSize
}

fun ArtistBrowse.toArtistScreenData(): ArtistScreenData =
    ArtistScreenData(
        title = this.name,
        imageUrl = this.thumbnails?.lastOrNull()?.url,
        subscribers = this.subscribers,
        playCount = this.views,
        isChannel = this.songs == null,
        channelId = this.channelId,
        radioParam = this.radioId,
        shuffleParam = this.shuffleId,
        description = this.description,
        listSongParam = this.songs?.browseId,
        popularSongs = this.songs?.results?.map { it.toTrack() } ?: emptyList(),
        singles = this.singles,
        albums = this.albums,
        video =
            this.video?.let { video ->
                ArtistBrowse.Videos(video.map { it.toTrack() }, this.videoList)
            },
        related = this.related,
        featuredOn = this.featuredOn ?: emptyList(),
    )

fun isAppInForeground(): Boolean {
    val appProcessInfo = RunningAppProcessInfo()
    ActivityManager.getMyMemoryState(appProcessInfo)
    return appProcessInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND
}

fun isValidProxyHost(host: String): Boolean {
    // Regular expression to validate proxy host (without port)
    val proxyHostRegex =
        Regex(
            pattern = "^(?!-)[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(?<!-)\$",
            options = setOf(RegexOption.IGNORE_CASE),
        )

    // Return true if the host matches the regex or is an IP address
    return proxyHostRegex.matches(host) || isIPAddress(host)
}

private fun isIPAddress(host: String): Boolean {
    // Check if the host is an IPv4 address
    val ipv4Regex =
        Regex(
            pattern = "^([0-9]{1,3}\\.){3}[0-9]{1,3}\$",
        )
    if (ipv4Regex.matches(host)) {
        return host.split('.').all { it.toInt() in 0..255 }
    }

    // Check if the host is an IPv6 address
    val ipv6Regex =
        Regex(
            pattern = "^[0-9a-fA-F:]+$",
        )
    return ipv6Regex.matches(host)
}

fun String.isTwoLetterCode(): Boolean {
    val regex = "^[A-Za-z]{2}$".toRegex()
    return regex.matches(this)
}

@Composable
fun String?.ifNullOrEmpty(defaultValue: @Composable () -> String): String = if (isNullOrEmpty()) defaultValue() else this