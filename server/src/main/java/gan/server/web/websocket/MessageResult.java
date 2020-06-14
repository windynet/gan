package gan.server.web.websocket;

import gan.web.base.Result;

public class MessageResult extends Result {

    public String mediacodec;

    public MessageResult setMediaCodec(String mediacodec) {
        this.mediacodec = mediacodec;
        return this;
    }

    public String getMediaCodec() {
        return mediacodec;
    }
}
