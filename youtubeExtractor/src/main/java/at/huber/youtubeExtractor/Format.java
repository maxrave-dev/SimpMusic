package at.huber.youtubeExtractor;

import android.support.annotation.NonNull;

public class Format {

    public enum VCodec {
        H263, H264, MPEG4, VP8, VP9, NONE
    }

    public enum ACodec {
        MP3, AAC, VORBIS, OPUS, NONE
    }

    private final int itag;
    private final String ext;
    private final int height;
    private final int fps;
    private VCodec vCodec;
    private ACodec aCodec;
    private final int audioBitrate;
    private final boolean isDashContainer;
    private final boolean isHlsContent;

    Format(int itag, String ext, int height, VCodec vCodec, ACodec aCodec, boolean isDashContainer) {
        this.itag = itag;
        this.ext = ext;
        this.height = height;
        this.fps = 30;
        this.audioBitrate = -1;
        this.isDashContainer = isDashContainer;
        this.isHlsContent = false;
    }

    Format(int itag, String ext, VCodec vCodec, ACodec aCodec, int audioBitrate, boolean isDashContainer) {
        this.itag = itag;
        this.ext = ext;
        this.height = -1;
        this.fps = 30;
        this.audioBitrate = audioBitrate;
        this.isDashContainer = isDashContainer;
        this.isHlsContent = false;
    }

    Format(int itag, String ext, int height, VCodec vCodec, ACodec aCodec, int audioBitrate,
           boolean isDashContainer) {
        this.itag = itag;
        this.ext = ext;
        this.height = height;
        this.fps = 30;
        this.audioBitrate = audioBitrate;
        this.isDashContainer = isDashContainer;
        this.isHlsContent = false;
    }

    Format(int itag, String ext, int height, VCodec vCodec, ACodec aCodec, int audioBitrate,
           boolean isDashContainer, boolean isHlsContent) {
        this.itag = itag;
        this.ext = ext;
        this.height = height;
        this.fps = 30;
        this.audioBitrate = audioBitrate;
        this.isDashContainer = isDashContainer;
        this.isHlsContent = isHlsContent;
    }

    Format(int itag, String ext, int height, VCodec vCodec, int fps, ACodec aCodec, boolean isDashContainer) {
        this.itag = itag;
        this.ext = ext;
        this.height = height;
        this.audioBitrate = -1;
        this.fps = fps;
        this.isDashContainer = isDashContainer;
        this.isHlsContent = false;
    }

    /**
     * Get the frames per second
     */
    public int getFps() {
        return fps;
    }

    /**
     * Audio bitrate in kbit/s or -1 if there is no audio track.
     */
    public int getAudioBitrate() {
        return audioBitrate;
    }

    /**
     * An identifier used by youtube for different formats.
     */
    public int getItag() {
        return itag;
    }

    /**
     * The file extension and conainer format like "mp4"
     */
    public String getExt() {
        return ext;
    }

    public boolean isDashContainer() {
        return isDashContainer;
    }

    public ACodec getAudioCodec() {
        return aCodec;
    }

    public VCodec getVideoCodec() {
        return vCodec;
    }

    public boolean isHlsContent() {
        return isHlsContent;
    }

    /**
     * The pixel height of the video stream or -1 for audio files.
     */
    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Format format = (Format) o;

        if (itag != format.itag) return false;
        if (height != format.height) return false;
        if (fps != format.fps) return false;
        if (audioBitrate != format.audioBitrate) return false;
        if (isDashContainer != format.isDashContainer) return false;
        if (isHlsContent != format.isHlsContent) return false;
        if (ext != null ? !ext.equals(format.ext) : format.ext != null) return false;
        if (vCodec != format.vCodec) return false;
        return aCodec == format.aCodec;

    }

    @Override
    public int hashCode() {
        int result = itag;
        result = 31 * result + (ext != null ? ext.hashCode() : 0);
        result = 31 * result + height;
        result = 31 * result + fps;
        result = 31 * result + (vCodec != null ? vCodec.hashCode() : 0);
        result = 31 * result + (aCodec != null ? aCodec.hashCode() : 0);
        result = 31 * result + audioBitrate;
        result = 31 * result + (isDashContainer ? 1 : 0);
        result = 31 * result + (isHlsContent ? 1 : 0);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "Format{" +
                "itag=" + itag +
                ", ext='" + ext + '\'' +
                ", height=" + height +
                ", fps=" + fps +
                ", vCodec=" + vCodec +
                ", aCodec=" + aCodec +
                ", audioBitrate=" + audioBitrate +
                ", isDashContainer=" + isDashContainer +
                ", isHlsContent=" + isHlsContent +
                '}';
    }
}
