package gan.server.web.websocket;

import gan.log.DebugLog;
import gan.log.FileLogger;
import gan.core.system.server.SystemServer;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketServiceManager {

    private final static String Tag = WebSocketServiceManager.class.getName();

    static {
        sInstance = new WebSocketServiceManager();
    }
    private static WebSocketServiceManager sInstance;
    private static FileLogger mLogger = FileLogger.getInstance("/websocket/info");
    private ConcurrentHashMap<WebSocketSession,WebSocketService> mMapSesstions = new ConcurrentHashMap<>();

    public static WebSocketServiceManager getsInstance() {
        return sInstance;
    }

    private WebSocketServiceManager(){
    }

    public static FileLogger getLogger(){
        return mLogger;
    }

    /**
     * 连接建立成功调用的方法
     * @param session  可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    protected void onOpen(WebSocketSession session){
        mLogger.log("onOpen WebSocketSession id:%s",session.getId());
        WebSocketService server = SystemServer.startServer(WebSocketService.class,session);
        mMapSesstions.put(session,server);
    }

    /**
     * 连接关闭调用的方法
     */
    protected void onClose(WebSocketSession session){
        mLogger.log("onClose WebSocketSession id:%s",session.getId());
        destoryServer(session);
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送过来的消息
     * @param session 可选的参数
     */
    protected void onTextMessage(String message, WebSocketSession session) {
        DebugLog.debug("onTextMessage message:"+message);
        try {
            WebSocketService server = mMapSesstions.get(session);
            if(server!=null){
                server.onMessage(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
            DebugLog.debug("onTextMessage e:"+e.getMessage());
        }
    }

    protected void onBinaryMessage(BinaryMessage message, WebSocketSession session) {
        DebugLog.debug("onBinaryMessage message:");
    }

    /**
     * 发生错误时调用
     * @param session
     * @param error
     */
    protected void onError(WebSocketSession session, Throwable error){
        error.printStackTrace();
        mLogger.log("onError sessionid:%s",session.getId());
        destoryServer(session);
    }

    public void destoryServer(WebSocketSession session){
        WebSocketService server = mMapSesstions.remove(session);
        if(server!=null){
            server.finish();
        }
    }

}
