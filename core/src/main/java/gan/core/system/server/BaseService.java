package gan.core.system.server;

import gan.log.FileLogger;
import gan.core.PluginHelper;
import gan.core.SyncPluginHelper;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

public abstract class BaseService extends Context{

    public final static int State_Destoryed = -2;
    public final static int State_Finishing = -1;
    public final static int State_Create = 1;
    public final static int State_Created = 2;

    private PluginHelper<ServiceListener> pluginHelper = new SyncPluginHelper<ServiceListener>();
    private HashMap<String,ServiceMessageHandler> mMapMessageHandlers;
    private Object[] parameters;
    private volatile int state=State_Create;

    protected final void create(Object... parameter){
        if(state<State_Create){
            throw new IllegalStateException(String.format("server state exception state:%s",state));
        }
        state = State_Create;
        onCreate(parameter);
        state = State_Created;
    }

    protected final void destory(){
        state = State_Finishing;
        onDestory();
        state = State_Destoryed;
    }

    public BaseService(){
    }

    protected void onCreate(Object... parameter){
        parameters = parameter;
    }

    protected void onDestory() {
        if(mMapMessageHandlers!=null){
            mMapMessageHandlers.clear();
        }
        for(ServiceListener listener:pluginHelper.getManagers(ServiceListener.class)){
            destoryPlugin(listener);
        }
    }

    public int getState() {
        return state;
    }

    public boolean isFinishing(){
        return state == State_Finishing
                ||state == State_Destoryed;
    }

    public final void finish(){
        FileLogger.getDebugLogger().log(Thread.currentThread().getStackTrace());
        ServiceManager.destoryServer(this);
    }

    public String getType(){
        return "";
    }

    public abstract void sendMessage(int b) throws IOException;

    public abstract void sendMessage(byte[] b) throws IOException;

    public abstract void sendMessage(byte[] b,int off,int len) throws IOException;

    public final void registerPlugin(ServiceListener listener) {
        if(isFinishing()){
            return;
        }
        if(listener instanceof ServerPlugin){
            ((ServerPlugin)listener).onAttachServer(this);
        }
        pluginHelper.addManager(listener);
        if(listener instanceof ServiceMessageHandler){
            registerMessageHandler((ServiceMessageHandler)listener);
        }
    }

    public final <T extends ServiceListener> Collection<T> getPlugin(
            Class<T> cls) {
        return pluginHelper.getManagers(cls);
    }

    public final void unregisterPlugin(ServiceListener listener){
        pluginHelper.removeManager(listener);
        if(listener instanceof ServiceMessageHandler){
            unregisterMessageHandler((ServiceMessageHandler)listener);
        }
        if(listener instanceof ServerPlugin){
            ((ServerPlugin)listener).onDestory();
        }
    }

    public PluginHelper<ServiceListener> getPluginHelper(){
        return pluginHelper;
    }

    public final void registerMessageHandler(ServiceMessageHandler messageHandler){
        if(isFinishing()){
            return;
        }
        if(mMapMessageHandlers==null){
            mMapMessageHandlers = new HashMap<>();
        }
        mMapMessageHandlers.put(messageHandler.getType(), messageHandler);
    }

    public final void unregisterMessageHandler(String type){
        if(mMapMessageHandlers!=null){
            mMapMessageHandlers.remove(type);
        }
    }

    public final void unregisterMessageHandler(ServiceMessageHandler messageHandler){
        unregisterMessageHandler(messageHandler.getType());
    }

    public final void destoryPlugin(ServiceListener listener){
        unregisterPlugin(listener);
    }

    public Context getContext(){
        return this;
    }

    public final <T extends Object> T getParameter(Class<T> cls){
        for(Object parameter: parameters){
            if(parameter.getClass().equals(cls)){
                return (T)parameter;
            }
        }
        return null;
    }

    public final Object getParameter(int index){
        return parameters[index];
    }

}
