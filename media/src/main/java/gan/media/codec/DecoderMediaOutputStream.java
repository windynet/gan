package gan.media.codec;

import gan.media.BufferInfo;
import gan.media.MediaOutputStream;
import gan.media.MediaSession;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DecoderMediaOutputStream implements MediaOutputStream {

    public MediaSession getMediaSession() {
        return null;
    }

    @Override
    public void init() {

    }

    @Override
    public void write(byte channel, ByteBuffer packet, BufferInfo bufferInfo) throws IOException {

    }

    @Override
    public void close() {

    }

}
