package at.huber.youtubeExtractor;

import android.support.annotation.NonNull;

public class VideoMeta {

    private static final String IMAGE_BASE_URL = "http://i.ytimg.com/vi/";

    private final String videoId;
    private final String title;
    private final String shortDescript;

    private final String author;
    private final String channelId;

    private final long videoLength;
    private final long viewCount;

    private final boolean isLiveStream;

    protected VideoMeta(String videoId, String title, String author, String channelId,
                        long videoLength, long viewCount, boolean isLiveStream, String shortDescript) {
        this.videoId = videoId;
        this.title = title;
        this.author = author;
        this.channelId = channelId;
        this.videoLength = videoLength;
        this.viewCount = viewCount;
        this.isLiveStream = isLiveStream;
        this.shortDescript = shortDescript;
    }

    // 120 x 90
    public String getThumbUrl() {
        return IMAGE_BASE_URL + videoId + "/default.jpg";
    }

    // 320 x 180
    public String getMqImageUrl() {
        return IMAGE_BASE_URL + videoId + "/mqdefault.jpg";
    }

    // 480 x 360
    public String getHqImageUrl() {
        return IMAGE_BASE_URL + videoId + "/hqdefault.jpg";
    }

    // 640 x 480
    public String getSdImageUrl() {
        return IMAGE_BASE_URL + videoId + "/sddefault.jpg";
    }

    // Max Res
    public String getMaxResImageUrl() {
        return IMAGE_BASE_URL + videoId + "/maxresdefault.jpg";
    }

    public String getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getChannelId() {
        return channelId;
    }

    public boolean isLiveStream() {
        return isLiveStream;
    }

    /**
     * The video length in seconds.
     */
    public long getVideoLength() {
        return videoLength;
    }

    public long getViewCount() {
        return viewCount;
    }

    public String getShortDescription() {
        return shortDescript;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VideoMeta videoMeta = (VideoMeta) o;

        if (videoLength != videoMeta.videoLength) return false;
        if (viewCount != videoMeta.viewCount) return false;
        if (isLiveStream != videoMeta.isLiveStream) return false;
        if (videoId != null ? !videoId.equals(videoMeta.videoId) : videoMeta.videoId != null)
            return false;
        if (title != null ? !title.equals(videoMeta.title) : videoMeta.title != null) return false;
        if (author != null ? !author.equals(videoMeta.author) : videoMeta.author != null)
            return false;
        return channelId != null ? channelId.equals(videoMeta.channelId) : videoMeta.channelId == null;

    }

    @Override
    public int hashCode() {
        int result = videoId != null ? videoId.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (channelId != null ? channelId.hashCode() : 0);
        result = 31 * result + (int) (videoLength ^ (videoLength >>> 32));
        result = 31 * result + (int) (viewCount ^ (viewCount >>> 32));
        result = 31 * result + (isLiveStream ? 1 : 0);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "VideoMeta{" +
                "videoId='" + videoId + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", channelId='" + channelId + '\'' +
                ", videoLength=" + videoLength +
                ", viewCount=" + viewCount +
                ", isLiveStream=" + isLiveStream +
                '}';
    }
}
