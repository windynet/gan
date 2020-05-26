package gan.media.ffmpeg;

public interface FrameCallBack {

    public void onFrameCallBack(int channel, byte[] data, int length, long pts);

}
