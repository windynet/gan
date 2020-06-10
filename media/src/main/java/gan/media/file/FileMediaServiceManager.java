package gan.media.file;

import gan.core.file.FileHelper;
import gan.core.system.server.SystemServer;
import gan.core.utils.TextUtils;
import gan.log.FileLogger;
import gan.media.*;

import java.net.URI;
import java.net.URLEncoder;

public class FileMediaServiceManager implements MediaSourceAdapter {

    private static FileMediaServiceManager instance;

    static {
        instance = new FileMediaServiceManager();
    }

    private static FileLogger mLogger = FileLogger.getInstance("/file/info");

    public static FileMediaServiceManager getInstance() {
        return instance;
    }

    private FileMediaServiceManager(){
        MediaServiceManager.getInstance().addMediaSourceAdapter(this);
    }

    public static String findFilePath(String path){
        return SystemServer.getPublicPath(String.format("/file/%s", path));
    }

    @Override
    public boolean accept(MediaRequest request) {
        String filePath = request.url;
        if(FileHelper.isFileExists(filePath)){
            return true;
        }
        filePath = findFilePath(filePath);
        if(FileHelper.isFileExists(filePath)){
            return true;
        }
        return false;
    }

    @Override
    public MediaSource getMediaSource(MediaRequest request) {
        return getMediaSourceResult(request).mediaSource;
    }

    @Override
    public MediaSourceResult getMediaSourceResult(MediaRequest request) {
        return getMediaSourceResultByPull(request);
    }

    public MediaSourceResult getMediaSourceResultByPull(MediaRequest request){
        return new FilePullMediaService().createMediaSourceByPull(request.url);
    }

    public static FileLogger getLogger(final String rtsp){
        try{
            URI uri = URI.create(rtsp);
            String host = uri.getHost();
            if(!TextUtils.isEmpty(host)){
                return FileLogger.getInstance("/file/"+URLEncoder.encode(host))
                        .setLogcat(SystemServer.IsDebug());
            }
        }catch (Exception e){
        }
        return mLogger;
    }

}
