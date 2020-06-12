package gan.media.rtsp;


import gan.core.system.server.ServiceListener;

public interface RtspController extends ServiceListener {

    public void play(float start,float end);

    public void scale(float scale);

    public void pause();

}
