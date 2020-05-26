package gan.media.ffmpeg;

import gan.core.system.SystemUtils;
import gan.log.DebugLog;

import java.io.FileOutputStream;

public class Ffmpeg {

    static{
        System.loadLibrary("ffmpeg");
    }

    private long handle;
    private FrameCallBack mFrameCallBack;

    public void setFrameCallBack(FrameCallBack frameCallBack) {
        this.mFrameCallBack = frameCallBack;
    }

    @Override
    protected void finalize() throws Throwable {
        try{
            destroy();
        }finally {
            super.finalize();
        }
    }

    public final long create(){
        DebugLog.debug( "create in thread:"+Thread.currentThread().getId());
        return handle = nativeCreate();
    }

    public final void destroy(){
        SystemUtils.close(fos);
        synchronized (this){
            if(handle>0){
                nativeDestroy(handle);
                handle=-1;
            }
        }
    }

    FileOutputStream fos;
    public void onFrame(int channel,byte[] data, int length,long pts){
        if(mFrameCallBack!=null){
            mFrameCallBack.onFrameCallBack(channel, data, length, pts);
        }
    }

    public int call(String format_name, String url){
        return call(handle, format_name, url);
    }

    public int parseFrame(){
        return loopFrame(handle);
    }

    public void call(String url){
        call(handle, guessFormatName(url), url);
    }

    private String guessFormatName(String url){
        if(url.startsWith("rtmp")){
            return "flv";
        }
        return "";
    }

    private native long nativeCreate();

    private native int nativeDestroy(long handle);

    private native int call(long handle,String format_name,String url);

    private native int loopFrame(long handle);
}
