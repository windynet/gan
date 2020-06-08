package gan.media.ffmpeg;

import gan.core.file.FileHelper;
import gan.core.system.SystemUtils;
import gan.core.utils.TextUtils;
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
        return nativeOpen(handle, format_name, url);
    }

    public long duration(){
        return nativeDuration(handle, null, null);
    }

    public long seek(long seek){
        return nativeSeek(handle, seek);
    }

    public int parseFrame(){
        return loopFrame(handle);
    }

    public int call(String url){
        return nativeOpen(handle, guessFormatName(url), url);
    }

    private String guessFormatName(String url){
        String ext = FileHelper.getFileExt(url,null);
        if(!TextUtils.isEmpty(ext)){
            return ext.trim().toLowerCase();
        }
        if(url.startsWith("rtmp")){
            return "flv";
        }
        return "";
    }

    private native long nativeCreate();

    private native int nativeDestroy(long handle);

    private native int nativeOpen(long handle,String format_name,String url);

    private native int loopFrame(long handle);

    private native long nativeDuration(long handle,String format_name,String url);

    private native long nativeCurrentTime(long handle);

    private native long nativeSeek(long handle, long seekTime);
}
