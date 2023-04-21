package at.huber.youtubeExtractor;

import android.support.annotation.NonNull;

public class YtFile {

    private final Format format;
    private final String url;

    YtFile(Format format, String url) {
        this.format = format;
        this.url = url;
    }

    /**
     * The url to download the file.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Format data for the specific file.
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Format data for the specific file.
     */
    @Deprecated
    public Format getMeta() {
        return format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        YtFile ytFile = (YtFile) o;

        if (format != null ? !format.equals(ytFile.format) : ytFile.format != null) return false;
        return url != null ? url.equals(ytFile.url) : ytFile.url == null;
    }

    @Override
    public int hashCode() {
        int result = format != null ? format.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "YtFile{" +
                "format=" + format +
                ", url='" + url + '\'' +
                '}';
    }
}
