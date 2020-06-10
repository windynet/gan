package gan.core.system.server;

import java.io.IOException;
import java.io.InputStream;

public interface ServiceMessageHandler extends ServiceListener {
    public String getType();
    public void onReceiveMessage(InputStream is)throws IOException;
}
