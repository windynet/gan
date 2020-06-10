package gan.core.system.server;

public class Context {

    public static <T extends BaseService> T startServer(Class<T> cls, Object... paramster){
        return ServiceManager.getInstance().startServer(cls,paramster);
    }

}
