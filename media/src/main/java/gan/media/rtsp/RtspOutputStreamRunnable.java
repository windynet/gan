package gan.media.rtsp;

import gan.log.DebugLog;
import gan.media.*;
import gan.media.h264.H264NAL;
import gan.media.h264.H264Utils;

import java.util.Vector;

public class RtspOutputStreamRunnable extends MediaOutputStreamRunnable1 {

    public RtspOutputStreamRunnable(MediaOutputStream out, MediaOutputInfo mediaSession) {
        super(out, mediaSession,1500, MediaOutputStreamRunnable.PacketType_Rtp);
        setPacketBufferMaxCount(5);
    }

    @Override
    protected void discardPacketInCache(Vector<PacketInfo> packetInfos, PacketInfoRecyclePool pool) {
        super.discardPacketInCache(packetInfos, pool);
        PacketInfo info = packetInfos.get(0);
        if(info!=null){
            if(info.channel()==0){
                if(isSpsOrPpsOrIType(info)){
                    packetInfos.clear();
                    return;
                }else {
                    if(isBufferLarge(8)){
                        return;
                    }
                }
            }

            if(isIgnorePacket(info)){
                DebugLog.debug("remove packetInfos index 0");
                packetInfos.remove(0);
                info.clear();
                pool.recycle(info);
            }
        }
    }

    @Override
    protected boolean onInterceptPacket(byte channel, byte[] packet, int offset, int len, long time) {
        if(isBufferLarge()){
            return isIgnorePacket(channel,packet,offset,len);
        }
        if(isBufferLarge(20)){
            return true;
        }
        return super.onInterceptPacket(channel, packet, offset, len, time);
    }

    private boolean isIgnorePacket(PacketInfo info){
        BufferInfo bufferInfo = info.getBufferInfo();
        return isIgnorePacket(bufferInfo.channel, info.array(), bufferInfo.offset,bufferInfo.length);
    }

    private boolean isSpsOrPpsOrIType(PacketInfo info){
        BufferInfo bufferInfo = info.getBufferInfo();
        return isSpsOrPpsOrIType(bufferInfo.channel, info.array(), bufferInfo.offset,bufferInfo.length);
    }

    private boolean isSpsOrPpsOrIType(byte channel, byte[] packet, int offset, int len){
        byte FUindicator = packet[offset+12];
        byte FuHeader = packet[offset+13];
        byte nal_fua = (byte)((FUindicator&0xe0)|(FuHeader&0x1f)); // FU_A nal
        return H264Utils.isNulaType(nal_fua, H264NAL.NAL_SPS)
                ||H264Utils.isNulaType(nal_fua, H264NAL.NAL_PPS)
                ||H264Utils.isNulaType(nal_fua, H264NAL.NAL_SLICE_IDR);
    }

    private boolean isIgnorePacket(byte channel, byte[] packet, int offset, int len){
        if(channel==0){
            byte FUindicator = packet[offset+12];
            byte NRI = (byte)((FUindicator<<1)>>6);
            return NRI<=1;
        }else{
            return false;
        }
    }

}
