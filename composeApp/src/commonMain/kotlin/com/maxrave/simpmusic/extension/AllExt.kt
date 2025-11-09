package com.maxrave.simpmusic.extension

import androidx.compose.runtime.Composable
import com.maxrave.common.SponsorBlockType
import com.maxrave.domain.data.model.browse.artist.ArtistBrowse
import com.maxrave.domain.extension.now
import com.maxrave.domain.utils.FilterState
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.viewModel.ArtistScreenData
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.custom_order
import simpmusic.composeapp.generated.resources.day_s_ago
import simpmusic.composeapp.generated.resources.filler
import simpmusic.composeapp.generated.resources.hour_s_ago
import simpmusic.composeapp.generated.resources.interaction
import simpmusic.composeapp.generated.resources.intro
import simpmusic.composeapp.generated.resources.month_s_ago
import simpmusic.composeapp.generated.resources.music_off_topic
import simpmusic.composeapp.generated.resources.na_na
import simpmusic.composeapp.generated.resources.newer_first
import simpmusic.composeapp.generated.resources.older_first
import simpmusic.composeapp.generated.resources.outro
import simpmusic.composeapp.generated.resources.poi_highlight
import simpmusic.composeapp.generated.resources.preview
import simpmusic.composeapp.generated.resources.recently
import simpmusic.composeapp.generated.resources.self_promotion
import simpmusic.composeapp.generated.resources.sponsor
import simpmusic.composeapp.generated.resources.title
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.time.ExperimentalTime

fun String?.removeDuplicateWords(): String {
    if (this == null) {
        return "null"
    } else {
        val regex = Regex("\\b(\\w+)\\b\\s*(?=.*\\b\\1\\b)")
        return this.replace(regex, "")
    }
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

@OptIn(ExperimentalTime::class)
@Composable
fun LocalDateTime.formatTimeAgo(): String {
    val now = now()
    val duration =
        this
            .toInstant(TimeZone.currentSystemDefault())
            .periodUntil(now.toInstant(TimeZone.currentSystemDefault()), TimeZone.currentSystemDefault())

    val monthsDiff = duration.months + (duration.years * 12)
    val daysDiff = duration.days

    // For hours, we need to calculate manually since Period doesn't include hours
    val thisInstant = this.toInstant(TimeZone.currentSystemDefault())
    val nowInstant = now.toInstant(TimeZone.currentSystemDefault())
    val hoursDiff = (nowInstant - thisInstant).inWholeHours

    return when {
        monthsDiff >= 1 -> stringResource(Res.string.month_s_ago, monthsDiff)
        daysDiff >= 1 -> stringResource(Res.string.day_s_ago, daysDiff)
        hoursDiff >= 2 -> stringResource(Res.string.hour_s_ago, hoursDiff)
        else -> stringResource(Res.string.recently)
    }
}

@Composable
fun formatDuration(duration: Long): String {
    if (duration < 0L) return stringResource(Res.string.na_na)
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

fun FilterState.displayNameRes(): StringResource =
    when (this) {
        FilterState.NewerFirst -> Res.string.newer_first
        FilterState.OlderFirst -> Res.string.older_first
        FilterState.Title -> Res.string.title
        FilterState.CustomOrder -> Res.string.custom_order
    }

@Composable
fun String?.ifNullOrEmpty(defaultValue: @Composable () -> String): String = if (isNullOrEmpty()) defaultValue() else this

@Composable
fun SponsorBlockType.displayString(): String =
    when (this) {
        SponsorBlockType.FILLER -> stringResource(Res.string.filler)
        SponsorBlockType.INTERACTION -> stringResource(Res.string.interaction)
        SponsorBlockType.INTRO -> stringResource(Res.string.intro)
        SponsorBlockType.MUSIC_OFF_TOPIC -> stringResource(Res.string.music_off_topic)
        SponsorBlockType.OUTRO -> stringResource(Res.string.outro)
        SponsorBlockType.POI_HIGHLIGHT -> stringResource(Res.string.poi_highlight)
        SponsorBlockType.PREVIEW -> stringResource(Res.string.preview)
        SponsorBlockType.SELF_PROMOTION -> stringResource(Res.string.self_promotion)
        SponsorBlockType.SPONSOR -> stringResource(Res.string.sponsor)
    }