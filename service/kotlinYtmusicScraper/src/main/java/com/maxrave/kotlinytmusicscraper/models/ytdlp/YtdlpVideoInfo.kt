package com.maxrave.kotlinytmusicscraper.models.ytdlp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YtdlpVideoInfo(
    @SerialName("abr") val abr: Double? = null,
    @SerialName("acodec") val acodec: String? = null,
    @SerialName("age_limit") val ageLimit: Int? = null,
    @SerialName("album") val album: String? = null,
    @SerialName("alt_title") val altTitle: String? = null,
    @SerialName("artist") val artist: String? = null,
    @SerialName("artists") val artists: List<String?>? = null,
    @SerialName("aspect_ratio") val aspectRatio: Double? = null,
    @SerialName("asr") val asr: Int? = null,
    @SerialName("audio_channels") val audioChannels: Int? = null,
    @SerialName("automatic_captions") val automaticCaptions: AutomaticCaptions? = null,
    @SerialName("availability") val availability: String? = null,
    @SerialName("categories") val categories: List<String?>? = null,
    @SerialName("channel") val channel: String? = null,
    @SerialName("channel_follower_count") val channelFollowerCount: Int? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("channel_is_verified") val channelIsVerified: Boolean? = null,
    @SerialName("channel_url") val channelUrl: String? = null,
    @SerialName("comment_count") val commentCount: Int? = null,
    @SerialName("creator") val creator: String? = null,
    @SerialName("creators") val creators: List<String?>? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("display_id") val displayId: String? = null,
    @SerialName("duration") val duration: Int? = null,
    @SerialName("duration_string") val durationString: String? = null,
    @SerialName("dynamic_range") val dynamicRange: String? = null,
    @SerialName("epoch") val epoch: Int? = null,
    @SerialName("ext") val ext: String? = null,
    @SerialName("extractor") val extractor: String? = null,
    @SerialName("extractor_key") val extractorKey: String? = null,
    @SerialName("filesize_approx") val filesizeApprox: Int? = null,
    @SerialName("format") val format: String? = null,
    @SerialName("format_id") val formatId: String? = null,
    @SerialName("format_note") val formatNote: String? = null,
    @SerialName("_format_sort_fields") val formatSortFields: List<String?>? = null,
    @SerialName("formats") val formats: List<Format?>? = null,
    @SerialName("fps") val fps: Int? = null,
    @SerialName("fulltitle") val fulltitle: String? = null,
    @SerialName("heatmap") val heatmap: List<Heatmap?>? = null,
    @SerialName("height") val height: Int? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("is_live") val isLive: Boolean? = null,
    @SerialName("like_count") val likeCount: Int? = null,
    @SerialName("live_status") val liveStatus: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("original_url") val originalUrl: String? = null,
    @SerialName("playable_in_embed") val playableInEmbed: Boolean? = null,
    @SerialName("protocol") val protocol: String? = null,
    @SerialName("release_year") val releaseYear: Int? = null,
    @SerialName("requested_formats") val requestedFormats: List<RequestedFormat?>? = null,
    @SerialName("resolution") val resolution: String? = null,
    @SerialName("subtitles") val subtitles: Subtitles? = null,
    @SerialName("tags") val tags: List<String?>? = null,
    @SerialName("tbr") val tbr: Double? = null,
    @SerialName("thumbnail") val thumbnail: String? = null,
    @SerialName("thumbnails") val thumbnails: List<Thumbnail?>? = null,
    @SerialName("timestamp") val timestamp: Int? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("track") val track: String? = null,
    @SerialName("_type") val type: String? = null,
    @SerialName("upload_date") val uploadDate: String? = null,
    @SerialName("uploader") val uploader: String? = null,
    @SerialName("vbr") val vbr: Double? = null,
    @SerialName("vcodec") val vcodec: String? = null,
    @SerialName("_version") val version: Version? = null,
    @SerialName("view_count") val viewCount: Int? = null,
    @SerialName("was_live") val wasLive: Boolean? = null,
    @SerialName("webpage_url") val webpageUrl: String? = null,
    @SerialName("webpage_url_basename") val webpageUrlBasename: String? = null,
    @SerialName("webpage_url_domain") val webpageUrlDomain: String? = null,
    @SerialName("width") val width: Int? = null,
) {
    @Serializable
    class AutomaticCaptions

    @Serializable
    data class Format(
        @SerialName("abr") val abr: Double? = null,
        @SerialName("acodec") val acodec: String? = null,
        @SerialName("aspect_ratio") val aspectRatio: Double? = null,
        @SerialName("asr") val asr: Int? = null,
        @SerialName("audio_channels") val audioChannels: Int? = null,
        @SerialName("audio_ext") val audioExt: String? = null,
        @SerialName("available_at") val availableAt: Int? = null,
        @SerialName("columns") val columns: Int? = null,
        @SerialName("container") val container: String? = null,
        @SerialName("downloader_options") val downloaderOptions: Format.DownloaderOptions? = null,
        @SerialName("dynamic_range") val dynamicRange: String? = null,
        @SerialName("ext") val ext: String? = null,
        @SerialName("filesize") val filesize: Int? = null,
        @SerialName("filesize_approx") val filesizeApprox: Int? = null,
        @SerialName("format") val format: String? = null,
        @SerialName("format_id") val formatId: String? = null,
        @SerialName("format_note") val formatNote: String? = null,
        @SerialName("fps") val fps: Double? = null,
        @SerialName("fragments") val fragments: List<Format.Fragment?>? = null,
        @SerialName("has_drm") val hasDrm: Boolean? = null,
        @SerialName("height") val height: Int? = null,
        @SerialName("http_headers") val httpHeaders: Format.HttpHeaders? = null,
        @SerialName("language_preference") val languagePreference: Int? = null,
        @SerialName("protocol") val protocol: String? = null,
        @SerialName("quality") val quality: Double? = null,
        @SerialName("resolution") val resolution: String? = null,
        @SerialName("rows") val rows: Int? = null,
        @SerialName("source_preference") val sourcePreference: Int? = null,
        @SerialName("tbr") val tbr: Double? = null,
        @SerialName("url") val url: String? = null,
        @SerialName("vbr") val vbr: Double? = null,
        @SerialName("vcodec") val vcodec: String? = null,
        @SerialName("video_ext") val videoExt: String? = null,
        @SerialName("width") val width: Int? = null,
    ) {
        @Serializable
        data class DownloaderOptions(
            @SerialName("http_chunk_size") val httpChunkSize: Int? = null,
        )

        @Serializable
        data class Fragment(
            @SerialName("duration") val duration: Double? = null,
            @SerialName("url") val url: String? = null,
        )

        @Serializable
        data class HttpHeaders(
            @SerialName("Accept") val accept: String? = null,
            @SerialName("Accept-Language") val acceptLanguage: String? = null,
            @SerialName("Sec-Fetch-Mode") val secFetchMode: String? = null,
            @SerialName("User-Agent") val userAgent: String? = null,
        )
    }

    @Serializable
    data class Heatmap(
        @SerialName("end_time") val endTime: Double? = null,
        @SerialName("start_time") val startTime: Double? = null,
        @SerialName("value") val value: Double? = null,
    )

    @Serializable
    data class RequestedFormat(
        @SerialName("abr") val abr: Double? = null,
        @SerialName("acodec") val acodec: String? = null,
        @SerialName("aspect_ratio") val aspectRatio: Double? = null,
        @SerialName("asr") val asr: Int? = null,
        @SerialName("audio_channels") val audioChannels: Int? = null,
        @SerialName("audio_ext") val audioExt: String? = null,
        @SerialName("available_at") val availableAt: Int? = null,
        @SerialName("container") val container: String? = null,
        @SerialName("downloader_options") val downloaderOptions: RequestedFormat.DownloaderOptions? = null,
        @SerialName("dynamic_range") val dynamicRange: String? = null,
        @SerialName("ext") val ext: String? = null,
        @SerialName("filesize") val filesize: Int? = null,
        @SerialName("filesize_approx") val filesizeApprox: Int? = null,
        @SerialName("format") val format: String? = null,
        @SerialName("format_id") val formatId: String? = null,
        @SerialName("format_note") val formatNote: String? = null,
        @SerialName("fps") val fps: Int? = null,
        @SerialName("has_drm") val hasDrm: Boolean? = null,
        @SerialName("height") val height: Int? = null,
        @SerialName("http_headers") val httpHeaders: RequestedFormat.HttpHeaders? = null,
        @SerialName("language_preference") val languagePreference: Int? = null,
        @SerialName("protocol") val protocol: String? = null,
        @SerialName("quality") val quality: Double? = null,
        @SerialName("resolution") val resolution: String? = null,
        @SerialName("source_preference") val sourcePreference: Int? = null,
        @SerialName("tbr") val tbr: Double? = null,
        @SerialName("url") val url: String? = null,
        @SerialName("vbr") val vbr: Double? = null,
        @SerialName("vcodec") val vcodec: String? = null,
        @SerialName("video_ext") val videoExt: String? = null,
        @SerialName("width") val width: Int? = null,
    ) {
        @Serializable
        data class DownloaderOptions(
            @SerialName("http_chunk_size") val httpChunkSize: Int? = null,
        )

        @Serializable
        data class HttpHeaders(
            @SerialName("Accept") val accept: String? = null,
            @SerialName("Accept-Language") val acceptLanguage: String? = null,
            @SerialName("Sec-Fetch-Mode") val secFetchMode: String? = null,
            @SerialName("User-Agent") val userAgent: String? = null,
        )
    }

    @Serializable
    class Subtitles

    @Serializable
    data class Thumbnail(
        @SerialName("height") val height: Int? = null,
        @SerialName("id") val id: String? = null,
        @SerialName("preference") val preference: Int? = null,
        @SerialName("resolution") val resolution: String? = null,
        @SerialName("url") val url: String? = null,
        @SerialName("width") val width: Int? = null,
    )

    @Serializable
    data class Version(
        @SerialName("release_git_head") val releaseGitHead: String? = null,
        @SerialName("repository") val repository: String? = null,
        @SerialName("version") val version: String? = null,
    )
}