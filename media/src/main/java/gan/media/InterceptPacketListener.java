package gan.media;

public interface InterceptPacketListener {
    boolean onInterceptPacket(byte channel, byte[] packet, int offset, int len, long pts);
}