package gan.media;

import gan.core.file.FileHelper;
import gan.core.system.server.SystemServer;
import gan.log.FileLogger;
import gan.media.file.MediaSessionFile;
import gan.media.file.MediaSourceFileService;
import gan.media.rtsp.RtspMediaServiceManager;
import gan.media.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MediaServiceManager {

    static {
        sInstance = new MediaServiceManager();
    }
    private static MediaServiceManager sInstance;

    public static MediaServiceManager getInstance() {
        return sInstance;
    }

    private ConcurrentHashMap<String, MediaSource> mMapMediaSource;
    private List<MediaSourceAdapter> mMediaSourceAdapters;

    private MediaServiceManager(){
        mMapMediaSource = new ConcurrentHashMap<>();
        mMediaSourceAdapters = new ArrayList<>();
    }

    public void addMediaSourceAdapter(MediaSourceAdapter adapter){
        mMediaSourceAdapters.add(adapter);
    }

    public boolean removeMediaSourceAdapter(MediaSourceAdapter adapter){
        return mMediaSourceAdapters.remove(adapter);
    }

    public void managerMediaSource(String token, MediaSource source){
        mMapMediaSource.put(token,source);
    }

    public void removeMediaSource(String token){
        MediaSource source = mMapMediaSource.remove(token);
        if(source==null){
            token = MediaUtils.parseToken(token);
            mMapMediaSource.remove(token);
        }
    }

    private static boolean isFileSource(MediaSource source){
        return source.isFlag(Media.FLAG_FILE_SOURCE);
    }

    /**
     * @return
     */
    public MediaSourceResult getMediaSourceResult(MediaRequest request){
        MediaSource source = getMediaSource(request);
        if(source==null
                ||isFileSource(source)){
            return internalGetMediaSourceResult(request);
        }else{
            return MediaSourceResult.ok(source);
        }
    }

    public MediaSource findMediaSource(MediaRequest request){
        MediaSource source = getMediaSource(request);
        if(source==null
                ||isFileSource(source)){
            source = getRtspSource(request);
        }
        return source;
    }

    public MediaSource getMediaSource(String url){
        MediaRequest request = MediaRequest.obtainRequest(url);
        try{
            return getMediaSource(request);
        }finally {
            request.recycle();
        }
    }

    public MediaSource getMediaSource(MediaRequest request){
        MediaSource source = mMapMediaSource.get(request.url);
        if(source==null){
            source = mMapMediaSource.get(request.getName());
        }
        if(source==null){
            source = mMapMediaSource.get(request.getToken());
        }
        return source;
    }

    private MediaSourceResult internalGetMediaSourceResult(MediaRequest request){
        MediaSource source = RtspMediaServiceManager.getInstance().getRtspSource(request.getToken());
        if(source==null
                ||isFileSource(source)){
            MediaSourceResult result = MediaSourceResult.error();
            for(MediaSourceAdapter sourceAdapter:mMediaSourceAdapters){
                if(sourceAdapter.accept(request)){
                    result = sourceAdapter.getMediaSourceResult(request);
                    if(result!=null){
                        dumpSource(result.mediaSource);
                        return result;
                    }
                }
            }
            return result;
        }else{
            dumpSource(source);
            return MediaSourceResult.ok(source);
        }
    }

    public MediaSource getRtspSource(MediaRequest request){
        MediaSource source = RtspMediaServiceManager.getInstance().getRtspSource(request.getToken());
        if(source==null
                ||isFileSource(source)){
            for(MediaSourceAdapter sourceAdapter:mMediaSourceAdapters){
                if(sourceAdapter.accept(request)){
                    source = sourceAdapter.getMediaSource(request);
                    if(source!=null){
                        dumpSource(source);
                        return source;
                    }
                }
            }
        }
        dumpSource(source);
        return source;
    }

    private void dumpSource(MediaSource source){
        if(source!=null){
            FileLogger.getInfoLogger().log(String.format("dumpSource:%s",source.toString()));
        }
    }

    public MediaSource getFileMediaSource(final String uri) {
        String filePath = SystemServer.getPublicPath(uri);
        if(FileHelper.isFileExists(filePath)){
            MediaSource source = SystemServer.startServer(MediaSourceFileService.class, new MediaSessionFile(uri))
                    .setOutputEmptyAutoFinish(true);
            ((MediaSourceFileService) source).startInput();
            return source;
        }
        return null;
    }

}
