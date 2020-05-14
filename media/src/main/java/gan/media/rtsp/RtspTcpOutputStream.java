package gan.media.rtsp;

import gan.core.system.SystemUtils;
import gan.media.BufferInfo;
import gan.media.MediaOutputStream;
import gan.media.MediaSession;
import gan.media.utils.ByteUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

public class RtspTcpOutputStream implements MediaOutputStream {

    MediaSession mSession;
    ByteBuffer mByteBuffer;

    public RtspTcpOutputStream(MediaSession session){
        mSession = session;
    }

    public MediaSession getMediaSession() {
        return mSession;
    }

    @Override
    public void init() {
        mByteBuffer = ByteBuffer.allocate(1500);
    }

    public void write(byte channel, ByteBuffer packet, BufferInfo info) throws Exception {
        mByteBuffer = ByteUtils.ensureBufferCapacity(info.length+8, mByteBuffer);
        mByteBuffer.clear();
        mByteBuffer.put((byte)0x24);
        mByteBuffer.put(channel);
        mByteBuffer.putShort((short) info.length);
        mByteBuffer.put(packet.array(),0, info.length);
        mSession.sendMessage(mByteBuffer.array(),0, mByteBuffer.position());
    }

    public void write(byte b[], int off, int len)throws IOException{
        mSession.sendMessage(b,off,len);
    }

    @Override
    public void close() {
        SystemUtils.close(mSession);
    }

}
