@file:Suppress("ktlint:standard:class-naming")

package com.maxrave.simpmusic.common

import android.content.Context
import android.util.Log
import com.maxrave.simpmusic.R
import java.time.LocalDateTime
import java.time.Month

object Config {
    const val SPOTIFY_LOG_IN_URL: String = "https://accounts.spotify.com/en/login"
    const val SPOTIFY_ACCOUNT_URL = "https://accounts.spotify.com/en/status"
    const val YOUTUBE_MUSIC_MAIN_URL = "https://music.youtube.com/"
    const val LOG_IN_URL =
        "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%26feature%3D__FEATURE__&hl=en"

    const val SONG_CLICK = "SONG_CLICK"
    const val VIDEO_CLICK = "VIDEO_CLICK"
    const val PLAYLIST_CLICK = "PLAYLIST_CLICK"
    const val ALBUM_CLICK = "ALBUM_CLICK"
    const val RADIO_CLICK = "RADIO_CLICK"
    const val MINIPLAYER_CLICK = "MINIPLAYER_CLICK"
    const val SHARE = "SHARE"
    const val RECOVER_TRACK_QUEUE = "RECOVER_TRACK_QUEUE"

    const val PLAYER_CACHE = "playerCache"
    const val DOWNLOAD_CACHE = "downloadCache"
    const val CANVAS_CACHE = "canvasCache"
    const val SERVICE_SCOPE = "serviceScope"
    const val MAIN_PLAYER = "mainPlayer"
    const val SECONDARY_PLAYER = "secondaryPlayer"

    val REMOVED_SONG_DATE_TIME: LocalDateTime = LocalDateTime.of(2003, Month.AUGUST, 26, 3, 0)

    val listOfHomeChip =
        listOf(
            R.string.all,
            R.string.relax,
            R.string.sleep,
            R.string.energize,
            R.string.sad,
            R.string.romance,
            R.string.feel_good,
            R.string.workout,
            R.string.party,
            R.string.commute,
            R.string.focus,
        )
}

object DownloadState {
    const val STATE_NOT_DOWNLOADED = 0
    const val STATE_PREPARING = 1
    const val STATE_DOWNLOADING = 2
    const val STATE_DOWNLOADED = 3
}

/*** Update supported location from sigma67/ytmusicapi
 *
 */
object SUPPORTED_LOCATION {
    val items: Array<CharSequence> =
        arrayOf(
            "AE",
            "AR",
            "AT",
            "AU",
            "AZ",
            "BA",
            "BD",
            "BE",
            "BG",
            "BH",
            "BO",
            "BR",
            "BY",
            "CA",
            "CH",
            "CL",
            "CO",
            "CR",
            "CY",
            "CZ",
            "DE",
            "DK",
            "DO",
            "DZ",
            "EC",
            "EE",
            "EG",
            "ES",
            "FI",
            "FR",
            "GB",
            "GE",
            "GH",
            "GR",
            "GT",
            "HK",
            "HN",
            "HR",
            "HU",
            "ID",
            "IE",
            "IL",
            "IN",
            "IQ",
            "IS",
            "IT",
            "JM",
            "JO",
            "JP",
            "KE",
            "KH",
            "KR",
            "KW",
            "KZ",
            "LA",
            "LB",
            "LI",
            "LK",
            "LT",
            "LU",
            "LV",
            "LY",
            "MA",
            "ME",
            "MK",
            "MT",
            "MX",
            "MY",
            "NG",
            "NI",
            "NL",
            "NO",
            "NP",
            "NZ",
            "OM",
            "PA",
            "PE",
            "PG",
            "PH",
            "PK",
            "PL",
            "PR",
            "PT",
            "PY",
            "QA",
            "RO",
            "RS",
            "RU",
            "SA",
            "SE",
            "SG",
            "SI",
            "SK",
            "SN",
            "SV",
            "TH",
            "TN",
            "TR",
            "TW",
            "TZ",
            "UA",
            "UG",
            "US",
            "UY",
            "VE",
            "VN",
            "YE",
            "ZA",
            "ZW",
        )
}

object SUPPORTED_LANGUAGE {
    val items: Array<CharSequence> =
        arrayOf(
            "English",
            "Tiếng Việt",
            "Italiano",
            "Deutsch",
            "Русский",
            "Türkçe",
            "Suomi",
            "Polski",
            "Português",
            "Français",
            "Español",
            "简体中文",
            "Bahasa Indonesia",
            "اللغة العربية",
            "日本語",
            "繁體中文",
            "Українська",
            "עברית",
            "Azerbaijani",
            "हिन्दी",
            "ภาษาไทย",
            "Nederlands",
            "한국어",
            "Català",
            "فارسی",
            "български",
        )
    val codes: Array<String> =
        arrayOf(
            "en-US",
            "vi-VN",
            "it-IT",
            "de-DE",
            "ru-RU",
            "tr-TR",
            "fi-FI",
            "pl-PL",
            "pt-PT",
            "fr-FR",
            "es-ES",
            "zh-CN",
            "id-ID",
            "ar-SA",
            "ja-JP",
            "zh-Hant-TW",
            "uk-UA",
            "iw-IL",
            "az-AZ",
            "hi-IN",
            "th-TH",
            "nl-NL",
            "ko-KR",
            "ca-ES",
            "fa-AF",
            "bg-BG",
        )

    fun getLanguageFromCode(code: String?): String {
        val index = codes.indexOf(code)
        Log.d("Config", "getLanguageFromCode: $index")
        if (index == -1) {
            return "English"
        }
        Log.w("Config", "getLanguageFromCode: ${items.get(index)}")
        return (items.getOrNull(index) ?: "English").toString()
    }

    fun getCodeFromLanguage(language: String?): String {
        val index = items.indexOf(language ?: "English")
        Log.d("Config", "getCodeFromLanguage: $index")
        if (index == -1) {
            return "en-US"
        }
        Log.w("Config", "getCodeFromLanguage: ${codes.getOrNull(index)}")
        return (codes.getOrNull(index) ?: "en-US").toString()
    }
}

object QUALITY {
    val items: Array<CharSequence> = arrayOf("Low - 66kps", "High - 129kps or Maximum - 256kps (w/ YouTube Premium)")
    val itags: Array<Int> = arrayOf(250, 251)
}

object VIDEO_QUALITY {
    val items: Array<CharSequence> = arrayOf("1080p", "720p", "360p")
    val itags: Array<Int> = arrayOf(137, 136, 134)
}

object LYRICS_PROVIDER {
    val items: Array<CharSequence> = arrayOf("Musixmatch", "YouTube Transcript")
}

object LIMIT_CACHE_SIZE {
    val items: Array<CharSequence> = arrayOf("100MB", "250MB", "500MB", "1GB", "2GB", "5GB", "8GB", "∞")
    val data: Array<Int> = arrayOf(100, 250, 500, 1000, 2000, 5000, 8000, -1)

    fun getDataFromItem(item: CharSequence?): Int {
        val index = items.indexOf(item)
        return data.getOrNull(index) ?: -1
    }

    fun getItemFromData(input: Int?): CharSequence {
        val index = data.indexOf(input)
        return items.getOrNull(index) ?: "∞"
    }
}

object SPONSOR_BLOCK {
    val list: Array<CharSequence> =
        arrayOf("sponsor", "selfpromo", "interaction", "intro", "outro", "preview", "music_offtopic", "poi_highlight", "filler")
    val listName: Array<Int> =
        arrayOf(
            R.string.sponsor,
            R.string.self_promotion,
            R.string.interaction,
            R.string.intro,
            R.string.outro,
            R.string.preview,
            R.string.music_off_topic,
            R.string.poi_highlight,
            R.string.filler,
        )

    fun fromDbToName(
        context: Context,
        list: List<CharSequence>,
    ): List<String> {
        val result = mutableListOf<String>()
        for (item in list) {
            val index = list.indexOf(item)
            result.add(context.getString(listName[index]))
        }
        return result
    }

    fun fromNameToDb(
        context: Context,
        input: List<String>,
    ): List<CharSequence> {
        val allString = fromDbToName(context, list.toList())
        val listIndex =
            allString.map {
                allString.indexOf(it)
            }
        val result =
            listIndex.mapNotNull {
                list.getOrNull(it)
            }
        return result
    }
}

object CHART_SUPPORTED_COUNTRY {
    val items =
        arrayOf(
            "US",
            "ZZ",
            "AR",
            "AU",
            "AT",
            "BE",
            "BO",
            "BR",
            "CA",
            "CL",
            "CO",
            "CR",
            "CZ",
            "DK",
            "DO",
            "EC",
            "EG",
            "SV",
            "EE",
            "FI",
            "FR",
            "DE",
            "GT",
            "HN",
            "HU",
            "IS",
            "IN",
            "ID",
            "IE",
            "IL",
            "IT",
            "JP",
            "KE",
            "LU",
            "MX",
            "NL",
            "NZ",
            "NI",
            "NG",
            "NO",
            "PA",
            "PY",
            "PE",
            "PL",
            "PT",
            "RO",
            "RU",
            "SA",
            "RS",
            "ZA",
            "KR",
            "ES",
            "SE",
            "CH",
            "TZ",
            "TR",
            "UG",
            "UA",
            "AE",
            "GB",
            "UY",
            "ZW",
        )
    val itemsData =
        arrayOf(
            "United States",
            "Global",
            "Argentina",
            "Australia",
            "Austria",
            "Belgium",
            "Bolivia",
            "Brazil",
            "Canada",
            "Chile",
            "Colombia",
            "Costa Rica",
            "Czech Republic",
            "Denmark",
            "Dominican Republic",
            "Ecuador",
            "Egypt",
            "El Salvador",
            "Estonia",
            "Finland",
            "France",
            "Germany",
            "Guatemala",
            "Honduras",
            "Hungary",
            "Iceland",
            "India",
            "Indonesia",
            "Ireland",
            "Israel",
            "Italy",
            "Japan",
            "Kenya",
            "Luxembourg",
            "Mexico",
            "Netherlands",
            "New Zealand",
            "Nicaragua",
            "Nigeria",
            "Norway",
            "Panama",
            "Paraguay",
            "Peru",
            "Poland",
            "Portugal",
            "Romania",
            "Russia",
            "Saudi Arabia",
            "Serbia",
            "South Africa",
            "South Korea",
            "Spain",
            "Sweden",
            "Switzerland",
            "Tanzania",
            "Turkey",
            "Uganda",
            "Ukraine",
            "United Arab Emirates",
            "United Kingdom",
            "Uruguay",
            "Zimbabwe",
        )
}

object MEDIA_CUSTOM_COMMAND {
    const val LIKE = "like"
    const val REPEAT = "repeat"
    const val RADIO = "radio"
    const val SHUFFLE = "shuffle"
}

object MEDIA_NOTIFICATION {
    const val NOTIFICATION_ID = 200
    const val NOTIFICATION_CHANNEL_NAME = "SimpMusic Playback Notification"
    const val NOTIFICATION_CHANNEL_ID = "SimpMusic Playback Notification ID"
}

const val SETTINGS_FILENAME = "settings"

const val DB_NAME = "Music Database"

const val FIRST_TIME_MIGRATION = "first_time_migration"
const val SELECTED_LANGUAGE = "selected_language"

const val STATUS_DONE = "status_done"

const val RESTORE_SUCCESSFUL = "restore_successful"

const val LOCAL_PLAYLIST_ID_SAVED_QUEUE = "LOCAL_PLAYLIST_ID_SAVED_QUEUE"
const val LOCAL_PLAYLIST_ID_DOWNLOADED = "LOCAL_PLAYLIST_ID_DOWNLOADED"
const val LOCAL_PLAYLIST_ID_LIKED = "LOCAL_PLAYLIST_ID_LIKED"
const val LOCAL_PLAYLIST_ID = "LOCAL_PLAYLIST_ID"
const val ASC = "ASC"
const val DESC = "DESC"