package gan.media.parser;

import gan.core.StreamHelper;
import gan.core.system.SystemUtils;
import gan.log.DebugLog;
import gan.media.utils.ByteUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class RtpOverTcpStreamParser implements StreamParser{

    int mBufferSize;
    boolean mInputStreaming;
    PacketListener mPacketListener;

    public RtpOverTcpStreamParser(int bufferSize,PacketListener packetListener){
        mBufferSize = bufferSize;
        mPacketListener = packetListener;
    }

    @Override
    public void start() {
        DebugLog.info("start");
        mInputStreaming = true;
        fileTime = System.currentTimeMillis();
    }

    long fileTime;
    @Override
    public void inputStream(InputStream is) throws IOException{
        byte[] buf = new byte[mBufferSize];
        byte[] b1 = new byte[1];
        byte[] b2 = new byte[2];
        ByteBuffer packet = ByteBuffer.allocate(mBufferSize);
        while (mInputStreaming){
            if(read2(is,b1,mInputStreaming)>0){
                byte $ = b1[0];
                if($==0x24){
                    if(read2(is,b1,mInputStreaming)>0){
                        byte channel = b1[0];
                        if(read2(is,b2,mInputStreaming)>0){
                            int length = ByteUtils.byteToUnsignInt16(b2,0);
                            packet.clear();
                            buf = ByteUtils.ensureBufferCapacity(length,buf);
                            if(read2(is, buf, length, mInputStreaming)>0){
                                packet = ByteUtils.ensureBufferCapacity(length, packet);
                                packet.clear();
                                packet.put(buf,0, length);
                                onTcpPacket((byte) channel, packet, 0, length);
                            }
                        }
                    }
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        if(mInputStreaming){
            DebugLog.info("stop");
        }
        mInputStreaming = false;
        SystemUtils.close(fos);
    }

    public boolean isStreaming() {
        return mInputStreaming;
    }

    private int read2(InputStream is, byte[] b, boolean run) throws IOException {
        int len = StreamHelper.read(is,b,run);
        if(len<=-1){
            stop();
        }
        return len;
    }

    private int read2(InputStream is, byte[] b, int length,boolean run) throws IOException {
        int len = StreamHelper.read(is, b, length, run);
        if(len<=-1){
            stop();
        }
        return len;
    }

    FileOutputStream fos;
    protected void onTcpPacket(byte channel,ByteBuffer packet,int offset,int length){
        if(mPacketListener!=null){
            mPacketListener.onTcpPacket(channel, packet, offset, length);
        }
    }

    public static interface PacketListener{
        public void onTcpPacket(byte channel,ByteBuffer packet,int offset,int length);
    }
}
