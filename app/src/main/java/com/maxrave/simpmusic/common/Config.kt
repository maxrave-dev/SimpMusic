package com.maxrave.simpmusic.common

import com.maxrave.simpmusic.R

object Config {
    enum class SyncState {
        LINE_SYNCED,
        UNSYNCED,
        NOT_FOUND
    }

    const val YOUTUBE_MUSIC_MAIN_URL = "https://music.youtube.com/"
    const val LOG_IN_URL = "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%26feature%3D__FEATURE__&hl=en"

    const val SONG_CLICK = "SONG_CLICK"
    const val VIDEO_CLICK = "VIDEO_CLICK"
    const val PLAYLIST_CLICK = "PLAYLIST_CLICK"
    const val ALBUM_CLICK = "ALBUM_CLICK"
    const val RADIO_CLICK = "RADIO_CLICK"
    const val MINIPLAYER_CLICK = "MINIPLAYER_CLICK"
    const val SHARE = "SHARE"
    const val RECOVER_TRACK_QUEUE = "RECOVER_TRACK_QUEUE"

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
    val items: Array<CharSequence> = arrayOf("AE", "AR", "AT", "AU", "AZ", "BA", "BD", "BE", "BG", "BH", "BO", "BR", "BY", "CA", "CH", "CL",
        "CO", "CR", "CY", "CZ", "DE", "DK", "DO", "DZ", "EC", "EE", "EG", "ES", "FI", "FR", "GB", "GE",
        "GH", "GR", "GT", "HK", "HN", "HR", "HU", "ID", "IE", "IL", "IN", "IQ", "IS", "IT", "JM", "JO",
        "JP", "KE", "KH", "KR", "KW", "KZ", "LA", "LB", "LI", "LK", "LT", "LU", "LV", "LY", "MA", "ME",
        "MK", "MT", "MX", "MY", "NG", "NI", "NL", "NO", "NP", "NZ", "OM", "PA", "PE", "PG", "PH", "PK",
        "PL", "PR", "PT", "PY", "QA", "RO", "RS", "RU", "SA", "SE", "SG", "SI", "SK", "SN", "SV", "TH",
        "TN", "TR", "TW", "TZ", "UA", "UG", "US", "UY", "VE", "VN", "YE", "ZA", "ZW")
}
object SUPPORTED_LANGUAGE {
    val items: Array<CharSequence> = arrayOf(
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
        "Bahasa Indonesia"
    )
    val codes: Array<String> = arrayOf(
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
        "in-ID"
    )
}
object QUALITY {
    val items: Array<CharSequence> = arrayOf("Low - 66kps", "High - 129kps")
    val itags: Array<Int> = arrayOf(250, 251)
}

object VIDEO_QUALITY {
    val items: Array<CharSequence> = arrayOf("720p", "360p")
    val itags: Array<Int> = arrayOf(22, 18)
}

object LYRICS_PROVIDER {
    val items: Array<CharSequence> = arrayOf("Musixmatch", "YouTube Transcript")
}
object LIMIT_CACHE_SIZE {
    val items: Array<CharSequence> = arrayOf("100MB", "250MB", "500MB", "1GB", "2GB", "5GB", "8GB", "∞")
    val data: Array<Int> = arrayOf(100, 250, 500, 1000, 2000, 5000, 8000, -1)
}

object SPONSOR_BLOCK {
    val list: Array<CharSequence> = arrayOf("sponsor", "selfpromo", "interaction", "intro", "outro", "preview", "music_offtopic", "poi_highlight", "filler")
    val listName: Array<Int> = arrayOf(
        R.string.sponsor,
        R.string.self_promotion,
        R.string.interaction,
        R.string.intro,
        R.string.outro,
        R.string.preview,
        R.string.music_off_topic,
        R.string.poi_highlight,
        R.string.filler)
}

object MEDIA_CUSTOM_COMMAND {
    const val LIKE = "like"
    const val REPEAT = "repeat"
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