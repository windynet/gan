package gan.media.file;


import gan.log.DebugLog;
import gan.log.FileLogger;
import gan.media.*;
import gan.media.h26x.HUtils;

import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaOutputStreamRunnableFile implements MediaOutputStreamRunnable {

    public final static int Status_Online = 0;
    public final static int Status_Offline = 1;
    private int mStatus = Status_Online;

    MediaOutputStream mOutputStream;
    Vector<PacketInfo> mRtspPacketInfos;
    private int mPacketBufferMaxCount;
    PacketInfoRecyclePool mByteBufferPool;
    private AtomicBoolean mClosed = new AtomicBoolean(false);
    MediaOutputInfo mMediaInfo;
    protected String mPacketType;
    InterceptPacketListener mInterceptPacketListener;
    private long currentVideoTime;

    public MediaOutputStreamRunnableFile(MediaOutputStream out, MediaOutputInfo mediaInfo){
        this(out,mediaInfo,4, MediaApplication.getMediaConfig().rtspFrameBufferSize);
    }

    public MediaOutputStreamRunnableFile(MediaOutputStream out, MediaOutputInfo mediaInfo, int poolSize, int capacity){
        mOutputStream = out;
        mRtspPacketInfos = new Vector<>(poolSize);
        mByteBufferPool = new PacketInfoRecyclePool(poolSize,capacity);
        mPacketBufferMaxCount = poolSize;
        mMediaInfo = mediaInfo;
        mPacketType = MediaOutputStreamRunnable.PacketType_Frame;
    }

    public MediaOutputStreamRunnableFile setPacketBufferMaxCount(int packetBufferMaxCount) {
        if(packetBufferMaxCount<1){
            throw new IllegalArgumentException("packetBufferMaxCount must>=1");
        }
        this.mPacketBufferMaxCount = packetBufferMaxCount;
        return this;
    }

    public MediaOutputStreamRunnableFile setInterceptPacketListener(InterceptPacketListener interceptPacketListener) {
        this.mInterceptPacketListener = interceptPacketListener;
        return this;
    }

    public void setStatus(int status) {
        if(status!=mStatus){
            DebugLog.info(String.format("status change:%s",status));
        }
        this.mStatus = status;
    }

    @Override
    public MediaInfo getMediaInfo() {
        return mMediaInfo;
    }

    @Override
    public String getPacketType() {
        return mPacketType;
    }

    public boolean isOffline(){
        return mStatus == Status_Offline;
    }

    private long videoOriginSampleTime;
    private long videoCurSampleTime;
    private long videoOffsetSampleTime;
    private long audioOriginSampleTime;
    private long audioCurSampleTime;
    private long audioOffsetSampelTime;
    public void putPacket(byte channel, byte[] packet, int offset,int len, long time){
        if(mClosed.get()){
            DebugLog.debug("Runnable isClosed");
            return;
        }

        updateSampleTime(channel,time);

        if(onInterceptPacket(channel,packet,offset,len,time)){
            DebugLog.debug("onInterceptPacket true");
            return;
        }

        if(channel==0){
            if(isBufferLarge()){
                synchronized (this){
                    if(mClosed.get()){
                        return;
                    }
                    try {
                        wait(time-currentVideoTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            putPacketInfo2(channel, packet, offset, len, time);
        }else{
            putPacketInfo2(channel, packet, offset, len, time);
        }
    }

    private void updateSampleTime(byte channel,long time) {
        if(channel == 0) {
            if (videoOffsetSampleTime <=0 && videoOriginSampleTime > 0) {
                videoOffsetSampleTime = time - videoOriginSampleTime;
            }
        }else {
            if (audioOffsetSampelTime <=0 && audioOriginSampleTime > 0) {
                audioOffsetSampelTime = time - audioOriginSampleTime;
            }
        }
    }

    private void putPacketInfo2(byte channel, byte[] packet, int offset,int len, long pts){
        if(channel==0){
            if (videoCurSampleTime < 0) { videoCurSampleTime = pts;}
            videoCurSampleTime += fixVideoSampleTimeOffset(videoOffsetSampleTime);
            videoOriginSampleTime = pts;
            videoOffsetSampleTime = 0;
            putPacketInfo(channel, packet, offset, len, videoCurSampleTime);
        }else{
            if (audioCurSampleTime < 0) { audioCurSampleTime = pts;}
            audioCurSampleTime += fixAudioSampleTimeOffset(audioOffsetSampelTime);
            audioOriginSampleTime = pts;
            audioOffsetSampelTime = 0;
            putPacketInfo(channel, packet, offset, len, audioCurSampleTime);
        }
    }

    public long fixVideoSampleTimeOffset(long offset){
        if(offset>20000||offset<0){
            DebugLog.debug("fixVideoSampleTimeOffset offset:%s",offset);
            return 3600;
        }
        return offset;
    }

    public long fixAudioSampleTimeOffset(long offset){
        if(offset>20000||offset<0){
            DebugLog.debug("fixVideoSampleTimeOffset offset:%s",offset);
            return 1000;
        }
        return offset;
    }

    private void putPacketInfo(byte channel, byte[] packet, int offset,int len, long pts){
        pts = Math.max(0,pts);
        PacketInfo rtspPacketInfo = mByteBufferPool.poll();
        try{
            System.arraycopy(packet, offset, rtspPacketInfo.array(), 0, len);
            rtspPacketInfo.length(len).offset(0).channel(channel).time(pts);
            mRtspPacketInfos.add(rtspPacketInfo);
        }catch (Exception e){
            e.printStackTrace();
            DebugLog.warn("putPacket e:"+rtspPacketInfo.toString());
        }
    }

    protected boolean onInterceptPacket(byte channel, byte[] packet, int offset,int len,long pts){
        if(isOffline()){
            return true;
        }

        if(channel==0){//video
            byte nalu = HUtils.getNaluByte(packet, offset, 10);
            /**
             * forbidden_zero_bit ==1  这个值应该为0，当它不为0时，表示网络传输过程中，当前NALU中可能存在错误，
             * 解码器可以考虑不对这个NALU进行解码。
             */
            if((nalu&0x80)==0x80){
                DebugLog.debug("nula forbidden_zero_bit=1 form error frame data");
                return true;
            }

            if(mInterceptPacketListener!=null){
                if(mInterceptPacketListener.onInterceptPacket(channel, packet, offset, len, pts)){
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void start(){
        if(mClosed.get()){
            return;
        }
        run();
    }

    @Override
    public void run() {
        try{
            DebugLog.info("out start");
            if(mClosed.get()){
                return;
            }
            mOutputStream.init();
            while (!mClosed.get()){
                if(!mRtspPacketInfos.isEmpty()){
                    PacketInfo rtspPacketInfo;
                    rtspPacketInfo = mRtspPacketInfos.remove(0);
                    ByteBuffer byteBuffer = rtspPacketInfo.getByteBuffer();
                    BufferInfo bufferInfo = rtspPacketInfo.getBufferInfo();
                    if(bufferInfo.channel==0){
                        currentVideoTime = bufferInfo.time;
                    }
                    mOutputStream.write(bufferInfo.channel,byteBuffer,bufferInfo);
                    mByteBufferPool.recycle(rtspPacketInfo);
                }else{
                    synchronized (this){
                        notifyAll();
                    }
                }
                try{
                    Thread.sleep(1);
                }catch (Exception e){
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
            FileLogger.getExceptionLogger().log(e);
            DebugLog.warn(e.getMessage());
        }finally {
            try{
                mClosed.set(true);
                mOutputStream.close();
                for(PacketInfo rtspPacketInfo : mRtspPacketInfos){
                    rtspPacketInfo.clear();
                }
                mRtspPacketInfos.clear();
                mByteBufferPool.release();
                DebugLog.info("thread end");
            }finally {
                synchronized (this){
                    notifyAll();
                }
            }
        }
    }

    public void clearCache(){
        for(PacketInfo rtspPacketInfo : mRtspPacketInfos){
            rtspPacketInfo.clear();
        }
        mRtspPacketInfos.clear();
    }

    public void close() {
        DebugLog.info("close");
        mClosed.set(true);
        try{
            Thread.interrupted();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean isBufferLarge(){
        return isBufferLarge(mPacketBufferMaxCount);
    }

    public boolean isBufferLarge(int count){
        return mRtspPacketInfos.size()>count;
    }
}

