package com.maxrave.domain.data.model.streams

data class YouTubeWatchEndpoint(
    val videoId: String? = null,
    val playlistId: String? = null,
    val playlistSetVideoId: String? = null,
    val params: String? = null,
    val index: Int? = null,
    val watchEndpointMusicSupportedConfigs: WatchEndpointMusicSupportedConfigs? = null,
) {
    data class WatchEndpointMusicSupportedConfigs(
        val watchEndpointMusicConfig: WatchEndpointMusicConfig,
    ) {
        data class WatchEndpointMusicConfig(
            val musicVideoType: String,
        ) {
            companion object {
                const val MUSIC_VIDEO_TYPE_OMV = "MUSIC_VIDEO_TYPE_OMV"
                const val MUSIC_VIDEO_TYPE_UGC = "MUSIC_VIDEO_TYPE_UGC"
                const val MUSIC_VIDEO_TYPE_ATV = "MUSIC_VIDEO_TYPE_ATV"
            }
        }
    }
}