package gan.media.ffmpeg;

import gan.core.system.SystemUtils;
import gan.core.system.server.ServerPlugin;
import gan.core.system.server.SystemServer;
import gan.log.FileLogger;
import gan.media.MediaApplication;
import gan.media.MediaSessionString;
import gan.media.MediaSource;
import gan.media.PacketInfo;
import gan.media.h26x.HUtils;
import gan.media.rtsp.Rtsp2Fmp4ServerPlugin;
import gan.media.rtsp.RtspMediaService;
import gan.media.rtsp.Sdp;

import java.io.FileOutputStream;

/**
 *使用ffmpeg拉流转输出
 */
public class FfmpegPullMediaService implements Runnable, FrameCallBack {

    String mUrl;
    Ffmpeg ffmpeg;
    RtspMediaService mRtspMediaServer;
    PacketInfo mVideoDataPacketInfo;
    PacketInfo mTempPacketInfo;
    volatile boolean runing;
    FileLogger mLogger;

    public FfmpegPullMediaService(){
        ffmpeg = new Ffmpeg();
        ffmpeg.setFrameCallBack(this);

        int bufferSize = MediaApplication.getMediaConfig().rtspFrameBufferSize*2;
        mTempPacketInfo = new PacketInfo(bufferSize);
        mVideoDataPacketInfo = new PacketInfo(bufferSize);
    }

    public MediaSource createMediaSourceByPull(String url){
        if(runing){
            return null;
        }
        mUrl = url;
        mLogger = FfmpegMediaServiceManager.getLogger(url);
        mRtspMediaServer = SystemServer.startServer(RtspMediaService.class, new MediaSessionString(url));
        mRtspMediaServer.setHasAudio(false);
        mRtspMediaServer.registerPlugin(new Rtsp2Fmp4ServerPlugin());
        mRtspMediaServer.setOutputEmptyAutoFinish(true);
        mRtspMediaServer.startInputStream(url, Sdp.SDP_OnlyVideo);
        mRtspMediaServer.registerPlugin(new ServerPlugin<RtspMediaService>(){
            @Override
            protected void onDestory() {
                super.onDestory();
                stopRun();
            }
        });
        SystemServer.executeThread(this);
        return mRtspMediaServer;
    }

    public void stopRun(){
        runing=false;
    }

    public void destroy(){
        runing = false;
        if(mRtspMediaServer!=null){
            mRtspMediaServer.finish();
        }
        SystemUtils.close(fos);
    }

    @Override
    public void run() {
        try{
            runing = true;
            mLogger.log("ffmpeg create");
            long ret = ffmpeg.create();
            mLogger.log("ffmpeg create:%s", ret);
            if(ret>0){
                ret = ffmpeg.call("mp4", mUrl);
                mLogger.log("ffmpeg call:%s", ret);
                if(ret>=0){
                    while (runing){
                        ffmpeg.parseFrame();
                    }
                }
            }
        }finally {
            if(ffmpeg!=null){
                ffmpeg.destroy();
            }
            destroy();

            mLogger.log("ffmpeg exit");
        }
    }

    FileOutputStream fos;
    long lastVideoPts,videoPts;
    @Override
    public void onFrameCallBack(int channel,byte[] data, int length, long pts) {
        if(mRtspMediaServer!=null){
            if(channel==0){//视频

//                try{
//                    if(fos == null){
//                        fos = FileHelper.createFileOutputStream(SystemServer.getRootPath(String.format("/logs/frame/frame_%s",SystemServer.currentTimeMillis())));
//                    }
//                    fos.write(data, 0, length);
//                }catch (Exception e){
//                    e.printStackTrace();
//                }

                mVideoDataPacketInfo.putAndCopy(data, 0, length);
                mVideoDataPacketInfo.time(pts);
                parseVideoFrame();
            }else{
                //
            }
        }
    }

    public void onFrame(byte channel, byte[] data,int offset, int length, long pts){
        if(runing){
            mRtspMediaServer.putFrame(channel, data, offset, length, pts);
        }
    }

    long lastVideoTime;
    public void parseVideoFrame(){
        PacketInfo packet = mVideoDataPacketInfo;
        packet.offset(0);
        int startIndex = HUtils.findStartCodeOffset(packet.array(), 0, packet.offsetLength());
        if(startIndex>=0){
            packet.offsets(startIndex);
        }else{
            return;
        }

        int packetLength;
        while((packetLength = packet.offsetLength())>0) {
            int frameLen = HUtils.frameLen(packet.array(), packet.offset(), packetLength);
            if(frameLen>=packetLength){
                startIndex= HUtils.findStartCodeOffset(packet.array(), packet.offset(), packet.offsetLength());
                if(startIndex>0){
                    packet.offsets(startIndex);
                }
                mTempPacketInfo.clear();
                mTempPacketInfo.putAndCopy(packet.array(), packet.offset(), packet.offsetLength());
                mTempPacketInfo.time(mVideoDataPacketInfo.time());
                mVideoDataPacketInfo.clear();
                mVideoDataPacketInfo.putAndCopy(mTempPacketInfo.array(), 0, mTempPacketInfo.length());
                mVideoDataPacketInfo.time(lastVideoTime = mTempPacketInfo.time());
                mTempPacketInfo.clear();
                break;
            }
            onFrame(packet.channel(), packet.array(), packet.offset(), frameLen, fixVideoTime(packet.time()));
            packet.offsets(frameLen);
        }
    }

    public long fixVideoTime(long time){
        return videoPts+=3600;
    }

}
