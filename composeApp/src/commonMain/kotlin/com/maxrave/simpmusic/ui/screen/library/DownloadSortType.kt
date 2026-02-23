package com.maxrave.simpmusic.ui.screen.library

/**
 * Sort types for the Downloaded songs screen.
 * Each variant represents a sort criterion + direction.
 * Persistence is handled via [toKey]/[fromKey] serialization.
 */
sealed class DownloadSortType {
    data object TitleAsc : DownloadSortType()
    data object TitleDesc : DownloadSortType()
    data object DateNewest : DownloadSortType()
    data object DateOldest : DownloadSortType()
    data object ArtistAsc : DownloadSortType()
    data object ArtistDesc : DownloadSortType()

    fun toKey(): String =
        when (this) {
            TitleAsc -> KEY_TITLE_ASC
            TitleDesc -> KEY_TITLE_DESC
            DateNewest -> KEY_DATE_NEWEST
            DateOldest -> KEY_DATE_OLDEST
            ArtistAsc -> KEY_ARTIST_ASC
            ArtistDesc -> KEY_ARTIST_DESC
        }

    companion object {
        private const val KEY_TITLE_ASC = "title_asc"
        private const val KEY_TITLE_DESC = "title_desc"
        private const val KEY_DATE_NEWEST = "date_newest"
        private const val KEY_DATE_OLDEST = "date_oldest"
        private const val KEY_ARTIST_ASC = "artist_asc"
        private const val KEY_ARTIST_DESC = "artist_desc"

        /** Preference key used with DataStoreManager */
        const val PREFERENCE_KEY = "download_sort_type"

        /** Default sort when no preference is stored */
        val DEFAULT = DateNewest

        fun fromKey(key: String?): DownloadSortType =
            when (key) {
                KEY_TITLE_ASC -> TitleAsc
                KEY_TITLE_DESC -> TitleDesc
                KEY_DATE_NEWEST -> DateNewest
                KEY_DATE_OLDEST -> DateOldest
                KEY_ARTIST_ASC -> ArtistAsc
                KEY_ARTIST_DESC -> ArtistDesc
                else -> DEFAULT
            }

        /** All available sort options, in display order */
        val entries: List<DownloadSortType> =
            listOf(DateNewest, DateOldest, TitleAsc, TitleDesc, ArtistAsc, ArtistDesc)
    }
}
