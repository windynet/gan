package gan.media.ffmpeg;

import gan.core.BaseListener;
import gan.core.system.server.SystemServer;
import gan.core.utils.TextUtils;
import gan.log.FileLogger;
import gan.media.*;

import java.net.URI;
import java.net.URLEncoder;

public class FfmpegMediaServerManager implements BaseListener, MediaSourceAdapter {

    private static FfmpegMediaServerManager instance;

    static {
        instance = new FfmpegMediaServerManager();
    }

    private Object sourceLock = new Object();
    private static FileLogger mLogger = FileLogger.getInstance("/ffmpeg/info");

    public static FfmpegMediaServerManager getInstance() {
        return instance;
    }

    private FfmpegMediaServerManager(){
        MediaServerManager.getInstance().addMediaSourceAdapter(this);
    }

    @Override
    public boolean accept(MediaRequest request) {
        if(request.url.startsWith("rtmp")
                ||request.url.startsWith("http")){
            return true;
        }
        return false;
    }

    @Override
    public MediaSource getMediaSource(MediaRequest request) {
        synchronized (sourceLock){
            MediaSource source = MediaServerManager.getInstance().getMediaSource(request);
            if(source!=null){
                return source;
            }
            return createMediaSourceByPull(request);
        }
    }

    @Override
    public MediaSourceResult getMediaSourceResult(MediaRequest request) {
        return MediaSourceResult.ok(getMediaSource(request));
    }

    protected MediaSource createMediaSourceByPull(MediaRequest request){
        return new FfmpegPullMediaService().createMediaSourceByPull(request.url);
    }

    public static FileLogger getLogger(final String rtsp){
        try{
            URI uri = URI.create(rtsp);
            String host = uri.getHost();
            if(!TextUtils.isEmpty(host)){
                return FileLogger.getInstance("/ffmpeg/"+URLEncoder.encode(host))
                        .setLogcat(SystemServer.IsDebug());
            }
        }catch (Exception e){
        }
        return mLogger;
    }

}
