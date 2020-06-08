package gan.media.file;

import gan.core.file.FileHelper;
import gan.core.system.SystemUtils;
import gan.core.system.server.ServerPlugin;
import gan.core.system.server.SystemServer;
import gan.log.FileLogger;
import gan.media.*;
import gan.media.ffmpeg.Ffmpeg;
import gan.media.ffmpeg.FrameCallBack;
import gan.media.h26x.HUtils;
import gan.media.rtsp.RtspFrame2RtpPlugin;
import gan.media.rtsp.RtspMediaServer;
import gan.media.rtsp.Sdp;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class FilePullMediaService implements Runnable, FrameCallBack {

    String mUrl;
    Ffmpeg ffmpeg;
    RtspMediaServer mRtspMediaServer;
    PacketInfo mVideoDataPacketInfo;
    PacketInfo mTempPacketInfo;
    volatile boolean runing;
    FileLogger mLogger;

    public FilePullMediaService(){
        ffmpeg = new Ffmpeg();
        ffmpeg.setFrameCallBack(this);

        int bufferSize = MediaApplication.getMediaConfig().rtspFrameBufferSize*2;
        mTempPacketInfo = new PacketInfo(bufferSize);
        mVideoDataPacketInfo = new PacketInfo(bufferSize);
    }

    MediaSourceResult result;
    public MediaSourceResult createMediaSourceByPull(String url){
        if(runing){
            return null;
        }

        if(FileHelper.isFileExists(url)){
            mUrl = url;
        }else{
            mUrl = FileMediaServerManager.findFilePath(url);
        }

        result = MediaSourceResult.error("not find");
        result.setFile(true);
        if(!FileHelper.isFileExists(mUrl)){
            result.setMessage("file not find");
            return result;
        }

        mLogger = FileMediaServerManager.getLogger(url);
        SystemServer.executeThread(this);

        synchronized (this){
            try {
                wait(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(result.ok){
            mRtspMediaServer = SystemServer.startServer(RtspMediaServer.class, new MediaSessionString(url));
            mRtspMediaServer.registerPlugin(new RtspFrame2RtpPlugin());
            mRtspMediaServer.setOutputEmptyAutoFinish(true);
            mRtspMediaServer.addFlag(Media.FLAG_FILE_SOURCE);
            mRtspMediaServer.startInputStream(url, Sdp.SDP);
            mRtspMediaServer.registerPlugin(new ServerPlugin<RtspMediaServer>(){
                @Override
                protected void onDestory() {
                    super.onDestory();
                    stopRun();
                }
            });
            result.mediaSource = mRtspMediaServer;
            return result;
        }else{
            stopRun();
        }
        return result;
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
                ret = ffmpeg.call(mUrl);
                mLogger.log("ffmpeg call:%s", ret);
                if(ret>=0){
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.setDuration(ffmpeg.duration());
                    result.setData(fileInfo);
                    result.asOk();
                    notifyResult();
                    while (runing){
                        ffmpeg.parseFrame();
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    notifyResult();
                }
            }else{
                notifyResult();
            }
        }finally {
            if(ffmpeg!=null){
                ffmpeg.destroy();
            }
            try{
                destroy();
                mLogger.log("ffmpeg exit");
            }finally {
                notifyResult();
            }
        }
    }

    private void notifyResult(){
        synchronized (this){
            notifyAll();
        }
    }

    FileOutputStream fos;
    long videoPts;
    @Override
    public void onFrameCallBack(int channel,byte[] data, int length, long pts) {
        if(mRtspMediaServer!=null){
            if(channel==0){//视频
                mVideoDataPacketInfo.putAndCopy(data, 0, length);
                mVideoDataPacketInfo.time(pts);
                parseVideoFrame();
            }else{
                if(isMPEG4(data, 0, length)){
                    onFrame(2,  data, 0, length, pts);
                }else{
                    onAudioFrame(channel, data, length, pts);
                }
            }
        }
    }

    public void onFrame(int channel, byte[] data,int offset, int length, long pts){
        if(runing){
            if(mRtspMediaServer.isFinishing()){
                return;
            }
            mRtspMediaServer.putFrame((byte) channel, data, offset, length, pts);
        }
    }

    public boolean isMPEG4(byte[] data, int offset, int length){
        if((SystemUtils.byteToUnsignInt16(data,offset)&0xfff0) == 0xfff0){
            byte type = data[offset+1];
            return (byte)(type&0x08)==0;
        }
        return false;
    }

    ByteBuffer mAACTempBuffer = ByteBuffer.allocate(12000);
    public void onAudioFrame(int channel,byte[] data, int length, long pts){
        int packetLen = length+7;
        byte[] head = new byte[7];
        addADTStoPacket(head, packetLen);
        mAACTempBuffer.clear();
        mAACTempBuffer.put(head);
        mAACTempBuffer.put(data, 0, length);
        onFrame(2,  mAACTempBuffer.array(), 0, packetLen, pts);
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF1;
        packet[2] = (byte) (((2 - 1) << 6) + (11 << 2) + (1 >> 2));
        packet[3] = (byte) (((1 & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
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
