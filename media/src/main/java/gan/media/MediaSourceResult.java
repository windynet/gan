package gan.media;

import gan.web.base.Result;

public class MediaSourceResult extends Result {

    public boolean isFile;
    public MediaSource mediaSource;

    public MediaSource getMediaSource() {
        return mediaSource;
    }

    public MediaSourceResult setMediaSource(MediaSource mediaSource) {
        this.mediaSource = mediaSource;
        return this;
    }

    public static MediaSourceResult ok(MediaSource source){
        return Result.ok(MediaSourceResult.class)
                .setMediaSource(source);
    }

    public static MediaSourceResult error(){
        return Result.error(MediaSourceResult.class);
    }

    public static MediaSourceResult error(String message){
        return (MediaSourceResult) Result.error(MediaSourceResult.class)
                .setMessage(message);
    }

    public boolean isOk(){
        return ok;
    }

    public boolean isError(){
        return !ok;
    }

    public static boolean isOk(MediaSourceResult result){
        return result!=null&&result.ok;
    }

    public static boolean isError(MediaSourceResult result){
        return result==null||!result.ok;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

}
