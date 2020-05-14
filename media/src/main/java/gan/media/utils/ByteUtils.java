package gan.media.utils;

import java.nio.ByteBuffer;

public class ByteUtils {

    public static final int MAX_BUFFER_LEN = 1024 * 1024;

    /**
     * 高字节在前（高字节序）
     * @param n
     * @return
     */
    public static byte[] toHH(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }

    public static byte[] ensureBufferCapacity(int newCapacity,byte[] buffer){
        if(newCapacity > MAX_BUFFER_LEN){
            throw new IllegalStateException("data buffer > max");
        }
        int oldCapacity = buffer.length;
        if(newCapacity>oldCapacity){
            byte[] newFrameBuffer = new byte[newCapacity+1];
            System.arraycopy(buffer, 0, newFrameBuffer, 0, oldCapacity);
            return newFrameBuffer;
        }
        return buffer;
    }

    public static ByteBuffer ensureBufferCapacity(int newCapacity,ByteBuffer buffer){
        if(newCapacity > MAX_BUFFER_LEN){
            throw new IllegalStateException("data buffer > max");
        }
        int oldCapacity = buffer.array().length;
        if(newCapacity>oldCapacity){
            ByteBuffer newFrameBuffer = ByteBuffer.allocate(newCapacity+1);
            newFrameBuffer.put(buffer);
            return newFrameBuffer;
        }
        return buffer;
    }

    public static int byteToUnsignInt8(byte[] b, int offset){
        return ((int)b[offset] & 0xFF);
    }

    public static int byte2UnsignInt8(byte data){
        return ((int)data&0xFF);
    }

    public static int byteToUnsignInt16(byte[] b, int offset){
        return (byteToUnsignInt8(b, offset) << 8) + (byteToUnsignInt8(b, offset+1));
    }

}
