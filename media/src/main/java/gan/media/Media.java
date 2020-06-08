package gan.media;

public class Media {

    /**
     * 文件 源
     */
    public final static int FLAG_FILE_SOURCE = 0x00000001;

    public static class MediaCodec{
        public final static String CODEC_AAC = "aac";
        public final static String CODEC_MPEG4_GENERIC = "MPEG4-GENERIC";
        public final static String CODEC_H264 = "h264";
        public final static String CODEC_H265 = "h265";
        public final static String CODEC_H265_hevc = "hevc";
    }
}
